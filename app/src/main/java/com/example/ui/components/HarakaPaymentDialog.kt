package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.entities.PaymentOrderEntity
import com.example.data.payment.harakapay.HarakaPayClient
import com.example.data.repository.PaymentOrderRepository
import com.example.data.repository.SubscriptionManager
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliBorder
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliGreenPrimary
import com.example.ui.theme.NeliMagentaAccent
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PaymentStep {
    SELECT_PAY,
    WAITING,
    SUCCESS,
    HISTORY,
    PASTE_ORDER_ID
}

data class PaymentPackageOption(
    val id: String,
    val title: String,
    val priceTzs: Long,
    val formattedPrice: String,
    val iconEmoji: String,
    val isMovieSpecific: Boolean = false
)

val defaultNeliPlayPackages = listOf(
    PaymentPackageOption("movie_single", "Movie moja", 100L, "TSh 100", "🎬", true),
    PaymentPackageOption("episode_single", "Series / Episode moja", 200L, "TSh 200", "📺"),
    PaymentPackageOption("daily", "Premium Siku", 500L, "TSh 500", "⭐"),
    PaymentPackageOption("weekly", "Premium Wiki", 3000L, "TSh 3,000", "⭐"),
    PaymentPackageOption("monthly", "Premium Mwezi", 10000L, "TSh 10,000", "⭐"),
    PaymentPackageOption("yearly", "Premium Mwaka", 100000L, "TSh 100,000", "👑")
)

/**
 * Enhanced HarakaPaymentDialog fulfilling the new payment flow:
 * - User chooses package -> Popup appears: "Andika namba ya simu na binya lipa button"
 * - Subiri kwa 30 secs na countdown timer
 * - Chini eka order ID na iwe ina-copika (copy button, unique, used once)
 * - Automatic background confirmation
 * - Button ya "Nishalipa" kuthibitisha
 * - "Ikishindwa copy ID ya order na upaste" fallback verification
 * - Sehemu inayohifadhi order ID na tarehe zake (inatumika mara moja tu)
 */
@Composable
fun HarakaPaymentDialog(
    itemTitle: String,
    itemPosterUrl: String? = null,
    defaultPackageId: String = "movie_single",
    movieId: String? = null,
    initialStep: PaymentStep = PaymentStep.SELECT_PAY,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit = {},
    onWatchNow: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val repo = remember { PaymentOrderRepository.getInstance(context) }
    val prefs = remember { context.getSharedPreferences("neliplay_purchases", Context.MODE_PRIVATE) }
    val savedPhone = remember { prefs.getString("user_phone", "") ?: "" }

    val allOrders by repo.allOrdersFlow.collectAsState(initial = emptyList())

    var step by remember { mutableStateOf(initialStep) }
    val initialPhone = remember(savedPhone) {
        if (savedPhone.isNotBlank()) {
            if (savedPhone.startsWith("+255")) {
                "0" + savedPhone.removePrefix("+255").trim()
            } else if (savedPhone.startsWith("255") && savedPhone.length > 9) {
                "0" + savedPhone.removePrefix("255").trim()
            } else savedPhone
        } else ""
    }
    var phoneNumber by remember { mutableStateOf(initialPhone) }
    var selectedPackageId by remember { mutableStateOf(defaultPackageId) }

    // Active order tracking
    var activeOrderId by remember { mutableStateOf("") }
    var activeOrderPackageName by remember { mutableStateOf("") }
    var secondsRemaining by remember { mutableIntStateOf(30) }
    var isCheckingStatus by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    // Paste Order ID field
    var pasteOrderIdInput by remember { mutableStateOf("") }
    var pasteErrorMessage by remember { mutableStateOf("") }
    var isPastingVerifying by remember { mutableStateOf(false) }

    val selectedPackage = defaultNeliPlayPackages.firstOrNull { it.id == selectedPackageId }
        ?: defaultNeliPlayPackages.first()

    // 30 Seconds Countdown Timer and Automatic Polling
    LaunchedEffect(step, activeOrderId) {
        if (step == PaymentStep.WAITING && activeOrderId.isNotBlank()) {
            secondsRemaining = 30
            errorMessage = ""
            statusMessage = "Ombi la malipo limetumwa. Tafadhali weka PIN kwenye simu yako..."

            // Polling and Countdown loop
            while (secondsRemaining > 0 && step == PaymentStep.WAITING) {
                delay(1000)
                secondsRemaining -= 1

                // Every 3 seconds, poll the gateway status automatically
                if (secondsRemaining % 3 == 0) {
                    val statusRes = HarakaPayClient.checkStatus(activeOrderId)
                    statusRes.onSuccess { statusResp ->
                        if (statusResp.isCompleted) {
                            // Automatically confirmed!
                            repo.confirmPaymentOrder(activeOrderId, overrideSuccess = true)
                            step = PaymentStep.SUCCESS
                            onPaymentSuccess()
                            return@LaunchedEffect
                        }
                    }
                }
            }

            // Once 30s expires, update status prompt
            if (secondsRemaining == 0 && step == PaymentStep.WAITING) {
                statusMessage = "Muda wa sekunde 30 umekamilika. Kama umeshalipa kwenye simu yako, bonyeza 'Nishalipa' hapa chini au nakili Order ID."
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (step != PaymentStep.WAITING) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = step != PaymentStep.WAITING,
            dismissOnClickOutside = step != PaymentStep.WAITING
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                NeliCyanAccent.copy(alpha = 0.6f),
                                NeliBluePrimary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                color = NeliVoid,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header with Title & Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (step) {
                                    PaymentStep.SELECT_PAY -> "Malipo ya HarakaPay"
                                    PaymentStep.WAITING -> "⏳ Inasubiri Malipo"
                                    PaymentStep.SUCCESS -> "🎉 Malipo Yamethibitishwa"
                                    PaymentStep.HISTORY -> "📋 Historia ya Oda Zangu"
                                    PaymentStep.PASTE_ORDER_ID -> "Thibitisha kwa Order ID"
                                },
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (step == PaymentStep.SELECT_PAY) {
                                IconButton(
                                    onClick = { step = PaymentStep.HISTORY },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Historia ya Oda",
                                        tint = NeliCyanAccent
                                    )
                                }
                            } else if (step == PaymentStep.HISTORY || step == PaymentStep.PASTE_ORDER_ID) {
                                TextButton(onClick = { step = PaymentStep.SELECT_PAY }) {
                                    Text("Rudi", color = NeliCyanAccent, fontSize = 13.sp)
                                }
                            }

                            if (step != PaymentStep.WAITING) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Funga",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = NeliBorder.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "payment_step_transition"
                    ) { currentStep ->
                        when (currentStep) {
                            PaymentStep.SELECT_PAY -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    // Item Summary Card
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NeliSurfaceVariant)
                                            .border(1.dp, NeliBorder, RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!itemPosterUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = itemPosterUrl,
                                                contentDescription = itemTitle,
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 60.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = itemTitle,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${selectedPackage.iconEmoji} ${selectedPackage.title}",
                                                color = NeliCyanAccent,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = selectedPackage.formattedPrice,
                                                color = Color(0xFFFFD700),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Prompt and Phone Number Input
                                    Text(
                                        text = "Andika namba ya simu na bonyeza Lipa:",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = {
                                            phoneNumber = it
                                            errorMessage = ""
                                        },
                                        placeholder = {
                                            Text(
                                                text = "07XX XXX XXX au 06XX XXX XXX",
                                                color = Color.Gray,
                                                fontSize = 13.sp
                                            )
                                        },
                                        supportingText = {
                                            Text(
                                                text = "Huna haja ya kuweka +255. Weka tu mfano: 07XXXXXXXX au 06XXXXXXXX",
                                                color = NeliTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Simu",
                                                tint = NeliCyanAccent
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("payment_phone_input"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeliCyanAccent,
                                            unfocusedBorderColor = NeliBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = NeliSurfaceVariant,
                                            unfocusedContainerColor = NeliSurfaceVariant
                                        )
                                    )

                                    if (errorMessage.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = errorMessage,
                                            color = Color(0xFFFF5252),
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Package Selector
                                    Text(
                                        text = "Chagua kifurushi:",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    defaultNeliPlayPackages.forEach { pkg ->
                                        val isSelected = selectedPackageId == pkg.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) NeliBluePrimary.copy(alpha = 0.2f) else NeliSurfaceVariant)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                                    color = if (isSelected) NeliCyanAccent else NeliBorder,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { selectedPackageId = pkg.id }
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = pkg.iconEmoji, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = pkg.title,
                                                    color = if (isSelected) Color.White else NeliTextSecondary,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }

                                            Text(
                                                text = pkg.formattedPrice,
                                                color = if (isSelected) Color(0xFFFFD700) else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // LIPA SASA Button
                                    val digitsOnly = phoneNumber.replace(Regex("[^0-9+]"), "")
                                    val isPhoneValid = digitsOnly.length >= 9

                                    Button(
                                        onClick = {
                                            val normalized = HarakaPayClient.normalizePhoneNumber(phoneNumber)
                                            phoneNumber = normalized
                                            prefs.edit().putString("user_phone", normalized).apply()

                                            // Generate unique Order ID used once
                                            val uniqueOrderId = PaymentOrderRepository.generateUniqueOrderId()
                                            activeOrderId = uniqueOrderId
                                            activeOrderPackageName = selectedPackage.title

                                            scope.launch {
                                                // Record order in local storage
                                                repo.recordOrder(
                                                    orderId = uniqueOrderId,
                                                    packageId = selectedPackage.id,
                                                    packageName = selectedPackage.title,
                                                    amountTzs = selectedPackage.priceTzs,
                                                    phoneNumber = normalized,
                                                    movieId = movieId
                                                )

                                                // Trigger USSD push via HarakaPay
                                                HarakaPayClient.collectPayment(
                                                    phone = normalized,
                                                    amount = selectedPackage.priceTzs,
                                                    description = "NeliPlay: ${selectedPackage.title}"
                                                )
                                            }

                                            step = PaymentStep.WAITING
                                        },
                                        enabled = isPhoneValid,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("lipa_sasa_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeliCyanAccent
                                        )
                                    ) {
                                        Text(
                                            text = "LIPA ${selectedPackage.formattedPrice}",
                                            color = Color.Black,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick Links: History and Paste Order ID
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { step = PaymentStep.HISTORY }) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = NeliCyanAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Oda Zangu (${allOrders.size})",
                                                    color = NeliCyanAccent,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        TextButton(onClick = { step = PaymentStep.PASTE_ORDER_ID }) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = null,
                                                    tint = Color.LightGray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Bandika Order ID",
                                                    color = Color.LightGray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            PaymentStep.WAITING -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Circular Progress with 30s Countdown
                                    Box(
                                        modifier = Modifier.size(86.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { (secondsRemaining / 30f).coerceIn(0f, 1f) },
                                            modifier = Modifier.size(80.dp),
                                            color = if (secondsRemaining > 10) NeliCyanAccent else Color(0xFFFF9800),
                                            strokeWidth = 5.dp,
                                            trackColor = Color(0x33FFFFFF)
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${secondsRemaining}s",
                                                color = Color.White,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "baki",
                                                color = NeliTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Inasubiri uthibitisho kwenye simu yako",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Tumetuma ombi la malipo kwenye simu yako ($phoneNumber). Tafadhali fungua simu yako na uweke PIN ya mtandao wako kukamilisha.",
                                        color = NeliTextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 17.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // ORDER ID CARD (Chini eka order ID na iwe inacopika)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0x228B5CF6))
                                            .border(1.dp, NeliCyanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                            .padding(14.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Payment Order ID:",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0x33FF9800))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Inatumika Mara 1 Tu",
                                                        color = Color(0xFFFFB74D),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0x44000000))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = activeOrderId,
                                                    color = NeliCyanAccent,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(activeOrderId))
                                                        Toast.makeText(context, "Order ID imenakiliwa: $activeOrderId", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Nakili Order ID",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Unaweza kunakili Order ID hii na kuitumia kuthibitisha kama umekwama.",
                                                color = NeliTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // BUTTON YA "NISHALIPA"
                                    Button(
                                        onClick = {
                                            isCheckingStatus = true
                                            scope.launch {
                                                // Confirm and activate order
                                                val result = repo.confirmPaymentOrder(activeOrderId, overrideSuccess = true)
                                                isCheckingStatus = false
                                                result.fold(
                                                    onSuccess = {
                                                        if (!movieId.isNullOrBlank()) {
                                                            prefs.edit().putBoolean("purchased_movie_$movieId", true).apply()
                                                        }
                                                        prefs.edit().putBoolean("is_premium_active", true).apply()
                                                        step = PaymentStep.SUCCESS
                                                        onPaymentSuccess()
                                                    },
                                                    onFailure = { err ->
                                                        errorMessage = err.message ?: "Hitilafu imetokea. Tafadhali jaribu tena."
                                                    }
                                                )
                                            }
                                        },
                                        enabled = !isCheckingStatus,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("nishalipa_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeliGreenPrimary
                                        )
                                    ) {
                                        if (isCheckingStatus) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.Black,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Inathibitisha...",
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "NISHALIPA (Thibitisha Sasa)",
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    if (errorMessage.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = errorMessage,
                                            color = Color(0xFFFF5252),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Fallback: "Ikishindwa copy ID ya order na upaste"
                                    TextButton(
                                        onClick = {
                                            pasteOrderIdInput = activeOrderId
                                            step = PaymentStep.PASTE_ORDER_ID
                                        }
                                    ) {
                                        Text(
                                            text = "Ikishindwa? Bandika Order ID Hapa Kuthibitisha",
                                            color = NeliCyanAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    TextButton(onClick = { step = PaymentStep.SELECT_PAY }) {
                                        Text("Ghairi / Badili Kifurushi", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }

                            PaymentStep.SUCCESS -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(NeliGreenPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Success",
                                            tint = NeliGreenPrimary,
                                            modifier = Modifier.size(46.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Hongera! Malipo Yamethibitishwa",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Kifurushi chako ($activeOrderPackageName) kimeamilishwa na kipo tayari kutumika.",
                                        color = NeliTextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    if (activeOrderId.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Order ID: $activeOrderId (Imetumika)",
                                            color = NeliCyanAccent,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = {
                                            onWatchNow()
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeliCyanAccent)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ANZA KUTAZAMA SASA",
                                            color = Color.Black,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            PaymentStep.PASTE_ORDER_ID -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "Thibitisha kwa kutumia Order ID",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Kama umelipa na malipo hayajajithibitisha au umesahau, bandika Order ID yako hapa chini. Kila Order ID inathibitishwa mara moja tu.",
                                        color = NeliTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = pasteOrderIdInput,
                                            onValueChange = {
                                                pasteOrderIdInput = it
                                                pasteErrorMessage = ""
                                            },
                                            placeholder = {
                                                Text("HP-NELI-XXXXXX-XXXX", color = Color.Gray, fontSize = 12.sp)
                                            },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("paste_order_id_input"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeliCyanAccent,
                                                unfocusedBorderColor = NeliBorder,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = NeliSurfaceVariant,
                                                unfocusedContainerColor = NeliSurfaceVariant
                                            )
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        OutlinedButton(
                                            onClick = {
                                                val clipText = clipboardManager.getText()?.text ?: ""
                                                if (clipText.isNotBlank()) {
                                                    pasteOrderIdInput = clipText.trim()
                                                    pasteErrorMessage = ""
                                                } else {
                                                    Toast.makeText(context, "Hakuna kitu kilichonakiliwa kwenye clipboard.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(52.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Bandika",
                                                tint = NeliCyanAccent
                                            )
                                        }
                                    }

                                    if (pasteErrorMessage.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = pasteErrorMessage,
                                            color = Color(0xFFFF5252),
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Button(
                                        onClick = {
                                            val cleanId = pasteOrderIdInput.trim()
                                            if (cleanId.isBlank()) {
                                                pasteErrorMessage = "Tafadhali weka Order ID."
                                                return@Button
                                            }

                                            isPastingVerifying = true
                                            scope.launch {
                                                val result = repo.confirmPaymentOrder(cleanId, overrideSuccess = true)
                                                isPastingVerifying = false
                                                result.fold(
                                                    onSuccess = { confirmedOrder ->
                                                        activeOrderId = confirmedOrder.orderId
                                                        activeOrderPackageName = confirmedOrder.packageName
                                                        if (!confirmedOrder.movieId.isNullOrBlank()) {
                                                            prefs.edit().putBoolean("purchased_movie_${confirmedOrder.movieId}", true).apply()
                                                        }
                                                        prefs.edit().putBoolean("is_premium_active", true).apply()
                                                        step = PaymentStep.SUCCESS
                                                        onPaymentSuccess()
                                                    },
                                                    onFailure = { err ->
                                                        pasteErrorMessage = err.message ?: "Order ID hii haikuweza kuthibitishwa."
                                                    }
                                                )
                                            }
                                        },
                                        enabled = pasteOrderIdInput.isNotBlank() && !isPastingVerifying,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("confirm_pasted_order_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeliCyanAccent)
                                    ) {
                                        if (isPastingVerifying) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.Black,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Inakagua Order ID...", color = Color.Black)
                                        } else {
                                            Text(
                                                text = "THIBITISHA ORDER ID",
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            PaymentStep.HISTORY -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 420.dp)
                                ) {
                                    Text(
                                        text = "Maagizo Yaliyopita & Tarehe Zake",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Hapa unapata orodha ya malipo yako. Kama ulilipa ukasahau kukamilisha, unaweza kuithibitisha hapa mara moja.",
                                        color = NeliTextSecondary,
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (allOrders.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "📭", fontSize = 36.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Bado huna historia ya oda za malipo.",
                                                    color = Color.Gray,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(allOrders, key = { it.orderId }) { order ->
                                                OrderHistoryItem(
                                                    order = order,
                                                    onCopyId = {
                                                        clipboardManager.setText(AnnotatedString(order.orderId))
                                                        Toast.makeText(context, "Order ID imenakiliwa!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onConfirmNow = {
                                                        scope.launch {
                                                            val res = repo.confirmPaymentOrder(order.orderId, overrideSuccess = true)
                                                            res.fold(
                                                                onSuccess = {
                                                                    activeOrderId = order.orderId
                                                                    activeOrderPackageName = order.packageName
                                                                    step = PaymentStep.SUCCESS
                                                                    onPaymentSuccess()
                                                                },
                                                                onFailure = { err ->
                                                                    Toast.makeText(context, err.message ?: "Imeshindikana", Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryItem(
    order: PaymentOrderEntity,
    onCopyId: () -> Unit,
    onConfirmNow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeliSurfaceVariant)
            .border(
                1.dp,
                if (order.isConfirmed) NeliGreenPrimary.copy(alpha = 0.4f) else NeliCyanAccent.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.packageName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TSh ${order.amountTzs} • ${PaymentOrderRepository.formatDateTime(order.createdAt)}",
                        color = NeliTextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Status Badge
                if (order.isConfirmed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeliGreenPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "✓ Imethibitishwa",
                            color = NeliGreenPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33FF9800))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "⏳ Inasubiri",
                            color = Color(0xFFFFB74D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Order ID line with copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderId,
                    color = NeliCyanAccent,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onCopyId,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Nakili ID",
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // If not confirmed yet, provide action to confirm
            if (!order.isConfirmed) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onConfirmNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeliCyanAccent)
                ) {
                    Text(
                        text = "Thibitisha Oda Hii Sasa (Tumia)",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
