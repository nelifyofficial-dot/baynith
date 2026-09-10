package com.example.ui.account

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.model.UserProfile
import com.example.ui.components.CountryPickerDialog
import com.example.ui.components.NeliPlayLogo
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliLiveRed
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextPrimary
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import com.example.util.Country
import com.example.util.RegionService
import com.google.firebase.auth.FirebaseUser

@Composable
fun AccountScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    viewModel: AccountViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showCountryPickerDialog by remember { mutableStateOf(false) }
    var showPremiumInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message, uiState.error) {
        val msg = uiState.message ?: uiState.error
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val user = uiState.currentUser
    val profile = uiState.profile
    val isSignedIn = user != null
    val country = uiState.countryObj

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NeliVoid,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NeliVoid)
                .statusBarsPadding()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("account_screen")
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Account",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeliTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSignedIn) "Manage your profile & synced library" else "Optional sign-in for cloud synchronization",
                        fontSize = 13.sp,
                        color = NeliTextSecondary
                    )
                }

                // Region Indicator Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeliSurfaceElevated)
                        .border(1.dp, NeliSurfaceVariant, RoundedCornerShape(10.dp))
                        .clickable { showCountryPickerDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("region_selector_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = country.flagEmoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = country.code,
                            color = NeliCyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ==========================================
            // SWITCH BETWEEN STATE A AND STATE B
            // ==========================================
            if (isSignedIn && user != null) {
                // STATE B — LOGGED IN
                LoggedInSection(
                    user = user,
                    profile = profile,
                    country = country,
                    favoritesCount = uiState.favoritesCount,
                    continueWatchingCount = uiState.continueWatchingCount,
                    onNavigateToWatchlist = onNavigateToFavorites,
                    onNavigateToWatchHistory = onNavigateToFavorites,
                    onNavigateToDownloads = onNavigateToDownloads,
                    onOpenPremium = { showPremiumInfoDialog = true },
                    onNavigateToSettings = onNavigateToSettings,
                    onChangeCountry = { showCountryPickerDialog = true },
                    onSignOutClick = { showSignOutDialog = true },
                    onDeleteAccountClick = { showDeleteAccountDialog = true }
                )
            } else {
                // STATE A — NOT LOGGED IN
                NotLoggedInSection(
                    country = country,
                    isGoogleSigningIn = uiState.isGoogleSigningIn,
                    onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                    onContinueWithoutAccount = onNavigateToHome,
                    onSelectCountry = { showCountryPickerDialog = true },
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Country Picker Dialog
    if (showCountryPickerDialog) {
        CountryPickerDialog(
            selectedCountryCode = uiState.effectiveCountryCode,
            onCountrySelected = { selected ->
                viewModel.selectCountry(selected.code)
            },
            onDismissRequest = { showCountryPickerDialog = false }
        )
    }

    // Premium Status Info Dialog
    if (showPremiumInfoDialog) {
        val isPremium = profile?.isPremium == true || profile?.isAdmin == true
        AlertDialog(
            onDismissRequest = { showPremiumInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = NeliCyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPremium) "NeliPlay Premium Active" else "NeliPlay Membership",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (isPremium)
                            "You have unlimited access to all high-definition streams, Swahili dubbed releases, and ad-free playback."
                        else
                            "All standard movies and series are free to browse and stream. Sign in enables cloud sync across all your devices.",
                        color = NeliTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Current Plan: ${if (profile?.isAdmin == true) "Administrator" else if (profile?.isPremium == true) "VIP Premium" else "Standard (Free)"}",
                        color = NeliCyanAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPremiumInfoDialog = false }) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = NeliSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out? Your cloud watchlist, history, and profile will remain safely stored on Firebase and restored next time you sign in.",
                    color = NeliTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut(context)
                    }
                ) {
                    Text(
                        text = "Sign Out",
                        color = Color(0xFFFF8A80),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = NeliSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isDeletingAccount) showDeleteAccountDialog = false },
            title = {
                Text(
                    text = "Delete your NeliPlay account?",
                    color = NeliLiveRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "This action is permanent and cannot be undone. Account-related cloud data (including your watchlist, watch history, and user profile) will be deleted immediately according to NeliPlay's data policy.",
                    color = NeliTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isDeletingAccount,
                    onClick = {
                        viewModel.deleteAccount(context)
                        showDeleteAccountDialog = false
                    }
                ) {
                    if (uiState.isDeletingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeliLiveRed)
                    } else {
                        Text("Delete Account", color = NeliLiveRed, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isDeletingAccount,
                    onClick = { showDeleteAccountDialog = false }
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = NeliSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * STATE A — NOT LOGGED IN
 * Clean cinematic card with NeliPlay branding, "Continue with Google", and "Continue without account".
 */
@Composable
private fun NotLoggedInSection(
    country: Country,
    isGoogleSigningIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onSelectCountry: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("not_logged_in_section")
    ) {
        // Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NeliSurface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeliSurfaceVariant, RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // NeliPlay Logo Branding
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    NeliBluePrimary.copy(alpha = 0.4f),
                                    NeliCyanAccent.copy(alpha = 0.25f)
                                )
                            )
                        )
                        .border(1.5.dp, NeliCyanAccent.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "NeliPlay",
                        tint = NeliCyanAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NeliPlay",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign in to sync your watchlist, history and preferences.",
                    fontSize = 14.sp,
                    color = NeliTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // BUTTON: Continue with Google
                Button(
                    onClick = onGoogleSignIn,
                    enabled = !isGoogleSigningIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F1F1F),
                        disabledContainerColor = Color.White.copy(alpha = 0.7f),
                        disabledContentColor = Color(0xFF1F1F1F).copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_with_google_button")
                ) {
                    if (isGoogleSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = NeliBluePrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Connecting Google...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F1F1F)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BUTTON: Continue without account
                OutlinedButton(
                    onClick = onContinueWithoutAccount,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeliTextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(NeliSurfaceVariant, NeliBluePrimary.copy(alpha = 0.5f))
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("continue_without_account_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = NeliCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue without account",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeliTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Login is optional. You can browse, search, and watch movies freely without an account.",
                    fontSize = 11.sp,
                    color = NeliTextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Country Selection Card
        Text(
            text = "YOUR COUNTRY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeliCyanAccent,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        AccountItemRow(
            icon = Icons.Default.Public,
            title = "Country",
            badgeText = "${country.flagEmoji}  ${country.name}",
            onClick = onSelectCountry,
            modifier = Modifier.testTag("country_select_row")
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Features Info
        Text(
            text = "WHY SIGN IN?",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeliTextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        FeatureHighlightCard(
            icon = Icons.Default.CloudSync,
            title = "Watchlist & History Sync",
            description = "Never lose your saved titles and continue watching right where you left off on any device."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureHighlightCard(
            icon = Icons.Default.Devices,
            title = "Cross-Device Continuity",
            description = "Start watching on your phone and resume seamlessly on your tablet or TV."
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App Settings
        Text(
            text = "APPLICATION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeliTextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        AccountItemRow(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = onNavigateToSettings,
            modifier = Modifier.testTag("account_settings_row")
        )

        Spacer(modifier = Modifier.height(10.dp))

        AccountItemRow(
            icon = Icons.Default.Info,
            title = "App Version",
            badgeText = "v${BuildConfig.VERSION_NAME}",
            onClick = {}
        )
    }
}

/**
 * STATE B — LOGGED IN
 * Shows Profile Photo, Name, Email, Country, Premium Status, and Action Buttons:
 * Watchlist, Watch History, Downloads, Premium, Settings, Delete Account, Sign Out.
 */
@Composable
private fun LoggedInSection(
    user: FirebaseUser,
    profile: UserProfile?,
    country: Country,
    favoritesCount: Int,
    continueWatchingCount: Int,
    onNavigateToWatchlist: () -> Unit,
    onNavigateToWatchHistory: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onOpenPremium: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onChangeCountry: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("logged_in_section")
    ) {
        // User Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NeliSurface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeliSurfaceVariant, RoundedCornerShape(22.dp))
                .testTag("logged_in_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo
                    val photoUrl = user.photoUrl?.toString() ?: profile?.photoUrl
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .border(2.dp, NeliCyanAccent, CircleShape)
                        )
                    } else {
                        val initial = (user.displayName ?: profile?.displayName ?: "U")
                            .take(1)
                            .uppercase()
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(NeliBluePrimary, NeliCyanAccent)
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Name
                        val displayName = when {
                            !user.displayName.isNullOrBlank() -> user.displayName!!
                            !profile?.displayName.isNullOrBlank() -> profile!!.displayName
                            else -> "NeliPlay User"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeliTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = NeliCyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Email
                        Text(
                            text = user.email ?: profile?.email ?: "",
                            fontSize = 13.sp,
                            color = NeliTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Country & Premium Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Country Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeliSurfaceVariant)
                                    .clickable(onClick = onChangeCountry)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${country.flagEmoji} ${country.name}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Premium Status Badge
                            val tierLabel = when {
                                profile?.isAdmin == true -> "Admin"
                                profile?.isPremium == true -> "Premium VIP"
                                else -> "Free Tier"
                            }
                            val tierColor = when {
                                profile?.isAdmin == true -> Color(0xFFFF9800)
                                profile?.isPremium == true -> NeliCyanAccent
                                else -> Color(0xFF64B5F6)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tierColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = tierLabel,
                                    color = tierColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = NeliSurfaceVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatColumn(
                        count = "$favoritesCount",
                        label = "Watchlist",
                        icon = Icons.Default.Favorite,
                        iconTint = Color(0xFFFF5252),
                        onClick = onNavigateToWatchlist
                    )

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(NeliSurfaceVariant)
                    )

                    StatColumn(
                        count = "$continueWatchingCount",
                        label = "History",
                        icon = Icons.Default.History,
                        iconTint = NeliCyanAccent,
                        onClick = onNavigateToWatchHistory
                    )

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(NeliSurfaceVariant)
                    )

                    StatColumn(
                        count = country.code,
                        label = "Country",
                        icon = Icons.Default.Public,
                        iconTint = Color(0xFF4CAF50),
                        onClick = onChangeCountry
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Required Buttons: Watchlist, Watch History, Downloads, Premium, Settings, Delete Account, Sign Out
        Text(
            text = "LIBRARY & CONTENT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeliCyanAccent,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        AccountItemRow(
            icon = Icons.Default.Favorite,
            title = "Watchlist",
            badgeText = "$favoritesCount saved",
            onClick = onNavigateToWatchlist,
            modifier = Modifier.testTag("account_watchlist_button")
        )

        Spacer(modifier = Modifier.height(10.dp))

        AccountItemRow(
            icon = Icons.Default.History,
            title = "Watch History",
            badgeText = "$continueWatchingCount items",
            onClick = onNavigateToWatchHistory,
            modifier = Modifier.testTag("account_history_button")
        )

        Spacer(modifier = Modifier.height(10.dp))

        AccountItemRow(
            icon = Icons.Default.Download,
            title = "Downloads",
            onClick = onNavigateToDownloads,
            modifier = Modifier.testTag("account_downloads_button")
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PREFERENCES & MEMBERSHIP",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeliTextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        AccountItemRow(
            icon = Icons.Default.WorkspacePremium,
            title = "Premium",
            badgeText = if (profile?.isPremium == true) "Active" else "Free Tier",
            onClick = onOpenPremium,
            modifier = Modifier.testTag("account_premium_button")
        )

        Spacer(modifier = Modifier.height(10.dp))

        AccountItemRow(
            icon = Icons.Default.Public,
            title = "Country",
            badgeText = "${country.flagEmoji}  ${country.name}",
            onClick = onChangeCountry,
            modifier = Modifier.testTag("account_country_button")
        )

        Spacer(modifier = Modifier.height(10.dp))

        AccountItemRow(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = onNavigateToSettings,
            modifier = Modifier.testTag("account_settings_button")
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "ACCOUNT ACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF8A80),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // Sign Out Button
        AccountItemRow(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Sign Out",
            iconTint = Color(0xFFFF8A80),
            titleColor = Color(0xFFFF8A80),
            onClick = onSignOutClick,
            modifier = Modifier.testTag("account_sign_out_button")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Delete Account Button
        AccountItemRow(
            icon = Icons.Default.DeleteForever,
            title = "Delete Account",
            iconTint = NeliLiveRed,
            titleColor = NeliLiveRed,
            onClick = onDeleteAccountClick,
            modifier = Modifier.testTag("account_delete_account_button")
        )
    }
}

@Composable
private fun StatColumn(
    count: String,
    label: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = count,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = NeliTextSecondary
        )
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NeliSurface)
            .border(1.dp, NeliSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NeliBluePrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeliCyanAccent,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeliTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = NeliTextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AccountItemRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = NeliTextSecondary,
    titleColor: Color = NeliTextPrimary,
    badgeText: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NeliSurface)
            .border(1.dp, NeliSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeliSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = NeliCyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = NeliTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
