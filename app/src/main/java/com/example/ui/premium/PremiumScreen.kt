package com.example.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PaymentStatus
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextPrimary
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid

val GoldAccent = Color(0xFFFFD700)
val GoldGradient = Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    viewModel: PremiumViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = NeliVoid,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NeliPlay Premium",
                            color = NeliTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("premium_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Rudi",
                            tint = NeliTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeliVoid
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // If user currently has active Premium, display subscription summary card
                if (uiState.isSubscriptionActive) {
                    ActiveSubscriptionCard(
                        profile = uiState.profile,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Ongeza au Badilisha Kifurushi:",
                        color = NeliTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    // Hero Premium Header
                    PremiumHeroBanner()
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Features / Benefits List
                PremiumBenefitsSection()

                Spacer(modifier = Modifier.height(24.dp))

                // Subscription Plans Selector
                Text(
                    text = "Chagua Kifurushi chako:",
                    color = NeliTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                uiState.run {
                    viewModel.availablePlans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            isSelected = selectedPlan == plan.id,
                            onClick = { viewModel.selectPlan(plan.id) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Mobile Money Phone Input Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NeliSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = NeliCyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nambari ya Simu (Malipo ya PalmPesa)",
                                color = NeliTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Inakubali Vodacom M-Pesa, Tigo Pesa, Airtel Money na Halopesa.",
                            color = NeliTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.phoneNumber,
                            onValueChange = { viewModel.onPhoneNumberChanged(it) },
                            placeholder = { Text("07xxxxxxxx au 06xxxxxxxx", color = NeliTextSecondary.copy(alpha = 0.6f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = uiState.error != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = NeliTextPrimary,
                                unfocusedTextColor = NeliTextPrimary,
                                focusedBorderColor = NeliCyanAccent,
                                unfocusedBorderColor = NeliSurfaceVariant,
                                cursorColor = NeliCyanAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("premium_phone_input")
                        )

                        if (uiState.error != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.error ?: "",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Payment CTA Button
                val selectedPlanItem = viewModel.availablePlans.find { it.id == uiState.selectedPlan }
                val buttonPriceLabel = selectedPlanItem?.formattedPrice ?: "TSh 10,000"

                Button(
                    onClick = { viewModel.startPayment() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("subscribe_premium_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jiunge Sasa ($buttonPriceLabel)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NeliTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Malipo salama kwa PalmPesa. Hakuna makato ya ziada.",
                        color = NeliTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Payment Process Dialog Overlay
            if (uiState.paymentStatus != PaymentStatus.IDLE) {
                PaymentStatusDialog(
                    status = uiState.paymentStatus,
                    message = uiState.message,
                    error = uiState.error,
                    orderId = uiState.createdPaymentResult?.orderId ?: uiState.currentPayment?.orderId ?: "",
                    amount = uiState.createdPaymentResult?.amount ?: uiState.currentPayment?.amount ?: 0L,
                    phone = uiState.createdPaymentResult?.buyerPhone ?: uiState.phoneNumber,
                    onVerifyManually = { viewModel.verifyPaymentManually() },
                    onCancel = { viewModel.cancelPayment() },
                    onDismiss = { viewModel.resetPaymentState() },
                    onSuccessAction = {
                        viewModel.resetPaymentState()
                        onNavigateBack()
                    }
                )
            }

            // Guest / Not Logged In Dialog Prompt
            if (uiState.showLoginPrompt) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissLoginPrompt() },
                    containerColor = NeliSurface,
                    title = {
                        Text(
                            text = "Akaunti Inahitajika",
                            color = NeliTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Ingia au fungua akaunti ili kuendelea.\n\nIli kujiunga na NeliPlay Premium, unahitaji kuwa na akaunti ili usajili wako uhifadhiwe salama na uweze kutumia kwenye vifaa vyako vyote.",
                            color = NeliTextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.dismissLoginPrompt()
                                onNavigateToAccount()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary),
                            modifier = Modifier.testTag("login_prompt_confirm_button")
                        ) {
                            Text("Ingia / Fungua Akaunti", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissLoginPrompt() }) {
                            Text("Baadaye", color = NeliTextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ActiveSubscriptionCard(
    profile: com.example.data.model.UserProfile?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NeliSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NeliPlay Premium ✓",
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00C853).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Active",
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Kifurushi:", color = NeliTextSecondary, fontSize = 12.sp)
                    Text(
                        text = profile?.planDisplayName ?: "Premium VIP",
                        color = NeliTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Mwisho wa Usajili:", color = NeliTextSecondary, fontSize = 12.sp)
                    Text(
                        text = profile?.formattedExpiryDate?.ifBlank { "Inatumika" } ?: "Inatumika",
                        color = NeliTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumHeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E2640),
                        NeliSurface
                    )
                )
            )
            .border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GoldAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Furahia NeliPlay Premium",
                color = NeliTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tazama filamu na tamthilia zote bora za Kiswahili na ulimwenguni bila vizuizi.",
                color = NeliTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PremiumBenefitsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NeliSurface)
            .padding(16.dp)
    ) {
        Text(
            text = "Faida za Kujiunga na Premium:",
            color = NeliTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        BenefitItem(text = "Tazama bila matangazo yanayokatisha (Ad-Free)")
        BenefitItem(text = "Upatikanaji wa filamu mpya na tamthilia za kipekee za Kiswahili")
        BenefitItem(text = "Ubora wa juu wa video (HD & Crystal-Clear Streaming)")
        BenefitItem(text = "Pakua utazame nje ya mtandao popote pale bila intaneti")
        BenefitItem(text = "Msaada wa haraka wa kipaumbele")
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = NeliTextPrimary,
            fontSize = 13.sp
        )
    }
}

@Composable
fun PlanCard(
    plan: PremiumPlanItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) GoldAccent else NeliSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("plan_card_${plan.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1B2238) else NeliSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = GoldAccent,
                    unselectedColor = NeliTextSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.title,
                        color = NeliTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (plan.badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) GoldAccent else NeliBluePrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = plan.badge,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = plan.durationLabel,
                    color = NeliTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plan.description,
                    color = NeliTextSecondary.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = plan.formattedPrice,
                color = if (isSelected) GoldAccent else NeliTextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PaymentStatusDialog(
    status: PaymentStatus,
    message: String?,
    error: String?,
    orderId: String,
    amount: Long,
    phone: String,
    onVerifyManually: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onSuccessAction: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (status != PaymentStatus.CREATING_PAYMENT && status != PaymentStatus.VERIFYING_PAYMENT) {
                onDismiss()
            }
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NeliSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (status) {
                    PaymentStatus.CREATING_PAYMENT -> {
                        CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Inatayarisha Malipo...",
                            color = NeliTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tafadhali subiri kidogo tunapowasiliana na seva ya PalmPesa.",
                            color = NeliTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    PaymentStatus.WAITING_FOR_PAYMENT -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Thibitisha Malipo kwenye Simu",
                            color = NeliTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ombi la TSh ${amount} limetumwa kwenda $phone.\n\nAngalia simu yako sasa na uweke PIN ya simu ili kukamilisha malipo ya PalmPesa.",
                            color = NeliTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        if (orderId.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Namba ya Oda: $orderId",
                                color = NeliCyanAccent,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onVerifyManually,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("payment_verify_button")
                        ) {
                            Text("Nimeshaweka PIN / Thibitisha", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeliTextSecondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("payment_cancel_button")
                        ) {
                            Text("Ghairi Malipo")
                        }
                    }

                    PaymentStatus.VERIFYING_PAYMENT -> {
                        CircularProgressIndicator(color = NeliCyanAccent, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Inathibitisha Malipo...",
                            color = NeliTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Inakagua hali ya malipo na mtandao wa PalmPesa.",
                            color = NeliTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    PaymentStatus.PAYMENT_SUCCESS -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Hongera! Malipo Yamethibitishwa!",
                            color = NeliTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message ?: "Usajili wako wa NeliPlay Premium umewashwa kikamilifu. Furahia filamu na tamthilia zote bila kikomo.",
                            color = NeliTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onSuccessAction,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("payment_success_button")
                        ) {
                            Text("Anza Kutazama", fontWeight = FontWeight.Bold)
                        }
                    }

                    PaymentStatus.PAYMENT_PENDING -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Malipo Bado Yanathibitishwa",
                            color = NeliTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message ?: "Ikiwa umeshaweka PIN yako kwenye simu, tafadhali subiri sekunde chache kisha ugonge Thibitisha Tena.",
                            color = NeliTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onVerifyManually,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("payment_retry_verify_button")
                        ) {
                            Text("Thibitisha Tena", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onDismiss) {
                            Text("Funga", color = NeliTextSecondary)
                        }
                    }

                    else -> { // PAYMENT_FAILED, SERVER_ERROR, PAYMENT_CANCELLED
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5252).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (status == PaymentStatus.PAYMENT_CANCELLED) "Malipo Yameghairiwa" else "Malipo Hayajakamilika",
                            color = NeliTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: message ?: "Hitilafu imetokea wakati wa malipo. Hakuna pesa iliyokatwa.",
                            color = NeliTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = NeliBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("payment_error_dismiss_button")
                        ) {
                            Text("Sawa / Jaribu Tena", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
