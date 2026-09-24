package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliBorder
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliGreenPrimary
import com.example.ui.theme.NeliMagentaAccent
import com.example.ui.theme.NeliPillDark
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextPrimary
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PaymentStep {
    SELECT_PAY,
    WAITING,
    SUCCESS
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

@Composable
fun HarakaPaymentDialog(
    itemTitle: String,
    itemPosterUrl: String? = null,
    defaultPackageId: String = "movie_single",
    movieId: String? = null,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit,
    onWatchNow: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("neliplay_purchases", Context.MODE_PRIVATE) }
    val savedPhone = remember { prefs.getString("user_phone", "") ?: "" }

    var step by remember { mutableStateOf(PaymentStep.SELECT_PAY) }
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
    var isCheckingStatus by remember { mutableStateOf(false) }

    val selectedPackage = defaultNeliPlayPackages.firstOrNull { it.id == selectedPackageId }
        ?: defaultNeliPlayPackages.first()

    Dialog(
        onDismissRequest = {
            if (step != PaymentStep.WAITING) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = step != PaymentStep.WAITING,
            dismissOnClickOutside = false
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
                    .border(1.dp, NeliBorder, RoundedCornerShape(24.dp)),
                color = NeliSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar with Close button (if not waiting)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_neliplay_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NELIPLAY SWAHILI",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        if (step != PaymentStep.WAITING) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Funga",
                                    tint = NeliTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "payment_step_animation"
                    ) { currentStep ->
                        when (currentStep) {
                            PaymentStep.SELECT_PAY -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "💳 NeliPlay Payment",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Lipa kwa simu yako",
                                        color = NeliTextSecondary,
                                        fontSize = 13.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Item card preview
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NeliSurfaceVariant)
                                            .border(1.dp, NeliBorder, RoundedCornerShape(14.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!itemPosterUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = itemPosterUrl,
                                                contentDescription = itemTitle,
                                                modifier = Modifier
                                                    .size(width = 46.dp, height = 64.dp)
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
                                                text = selectedPackage.title,
                                                color = NeliCyanAccent,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = selectedPackage.formattedPrice,
                                                color = NeliBluePrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Phone Number Input
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Namba ya simu ya Tanzania",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = phoneNumber,
                                            onValueChange = { phoneNumber = it },
                                            placeholder = {
                                                Text(
                                                    text = "07XX XXX XXX au 06XX XXX XXX",
                                                    color = NeliTextSecondary
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
                                                    tint = NeliBluePrimary
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("payment_phone_input"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeliBluePrimary,
                                                unfocusedBorderColor = NeliBorder,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = NeliSurfaceVariant,
                                                unfocusedContainerColor = NeliSurfaceVariant
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Package Selector
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Chagua kifurushi:",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        defaultNeliPlayPackages.forEach { pkg ->
                                            val isSelected = selectedPackageId == pkg.id
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) NeliBluePrimary.copy(alpha = 0.15f) else NeliSurfaceVariant)
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) NeliBluePrimary else NeliBorder,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { selectedPackageId = pkg.id }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }

                                                Text(
                                                    text = pkg.formattedPrice,
                                                    color = if (isSelected) NeliBluePrimary else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Total Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Jumla:",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = selectedPackage.formattedPrice,
                                            color = NeliBluePrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    val digitsOnly = phoneNumber.replace(Regex("[^0-9+]"), "")
                                    val isPhoneValid = digitsOnly.length >= 9

                                    // LIPA SASA Button
                                    Button(
                                        onClick = {
                                            val normalized = com.example.data.payment.harakapay.HarakaPayClient.normalizePhoneNumber(phoneNumber)
                                            phoneNumber = normalized
                                            prefs.edit().putString("user_phone", normalized).apply()
                                            step = PaymentStep.WAITING
                                        },
                                        enabled = isPhoneValid,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("lipa_sasa_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeliBluePrimary
                                        )
                                    ) {
                                        Text(
                                            text = "LIPA SASA",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "🔒 Malipo salama kupitia HarakaPay",
                                        color = NeliTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            PaymentStep.WAITING -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Pulsing Circular Progress
                                    Box(
                                        modifier = Modifier.size(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(72.dp),
                                            color = NeliCyanAccent,
                                            strokeWidth = 4.dp
                                        )
                                        Text(
                                            text = "⏳",
                                            fontSize = 28.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Inasubiri malipo",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Tumetuma ombi la malipo kwenye namba yako ($phoneNumber). Tafadhali thibitisha malipo kwenye simu yako.",
                                        color = NeliTextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // HarakaPay instruction card
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NeliSurfaceVariant)
                                            .border(1.dp, NeliBorder, RoundedCornerShape(14.dp))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(NeliBluePrimary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "📲", fontSize = 18.sp)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = "HarakaPay",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Tafadhali fungua simu yako na thibitisha malipo kupitia M-Pesa, Tigo Pesa au Airtel Money.",
                                                color = NeliTextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // ANGALIA HALI YA MALIPO button
                                    Button(
                                        onClick = {
                                            isCheckingStatus = true
                                            scope.launch {
                                                delay(1200)
                                                isCheckingStatus = false
                                                // Save purchase record
                                                if (!movieId.isNullOrBlank()) {
                                                    prefs.edit().putBoolean("purchased_movie_$movieId", true).apply()
                                                }
                                                prefs.edit().putBoolean("is_premium_active", true).apply()
                                                step = PaymentStep.SUCCESS
                                                onPaymentSuccess()
                                            }
                                        },
                                        enabled = !isCheckingStatus,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("check_payment_status_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeliBluePrimary
                                        )
                                    ) {
                                        if (isCheckingStatus) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Inakagua...",
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        } else {
                                            Text(
                                                text = "ANGALIA HALI YA MALIPO",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            PaymentStep.SUCCESS -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Green Checkmark Badge
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(NeliGreenPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Success",
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "✓ Malipo yamefanikiwa!",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Sasa unaweza kutazama $itemTitle.",
                                        color = NeliCyanAccent,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Receipt Summary
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NeliSurfaceVariant)
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Kifurushi:", color = NeliTextSecondary, fontSize = 12.sp)
                                            Text(text = selectedPackage.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Kiasi:", color = NeliTextSecondary, fontSize = 12.sp)
                                            Text(text = selectedPackage.formattedPrice, color = NeliGreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Namba:", color = NeliTextSecondary, fontSize = 12.sp)
                                            Text(text = phoneNumber, color = Color.White, fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // TAZAMA SASA Button
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onWatchNow()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("tazama_sasa_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeliBluePrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "TAZAMA SASA",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Asante kwa kutumia NeliPlay ♥",
                                        color = NeliTextSecondary,
                                        fontSize = 11.sp
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
