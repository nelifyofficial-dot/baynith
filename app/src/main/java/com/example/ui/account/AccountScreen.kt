package com.example.ui.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.NeliPlayLogo
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliGreenSuccess
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid
import com.example.util.MovieRecommendationScheduler
import com.example.util.RegionService

/**
 * Clean, modern Netflix-style Account Screen with seamless Google Sign-In.
 * VIP badges and subscription plans are completely hidden.
 */
@Composable
fun AccountScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    viewModel: AccountViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    val isUserLoggedIn = uiState.currentUser != null && !uiState.currentUser!!.isAnonymous

    Scaffold(
        containerColor = NeliVoid,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeliVoid)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeliPlayLogo(size = 32)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Akaunti",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x18FFFFFF))
                        .clickable(onClick = onNavigateToSettings)
                        .testTag("account_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Mipangilio",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentWidth = if (maxWidth >= 600.dp) 560.dp else maxWidth

            Column(
                modifier = Modifier
                    .width(contentWidth)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 1. Netflix-Style Profile Header Card
                NetflixProfileCard(
                    isLoggedIn = isUserLoggedIn,
                    displayName = uiState.currentUser?.displayName ?: uiState.profile?.displayName,
                    email = uiState.currentUser?.email,
                    photoUrl = uiState.currentUser?.photoUrl?.toString(),
                    isSigningIn = uiState.isSigningIn,
                    onSignInWithGoogle = {
                        viewModel.signInWithGoogle(context)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Section: Maudhui Yangu (My List / Library)
                NetflixSectionTitle(title = "Maudhui Yangu")
                Spacer(modifier = Modifier.height(8.dp))

                NetflixMenuContainer {
                    NetflixMenuItem(
                        icon = Icons.Default.Favorite,
                        iconTint = Color(0xFFE50914), // Netflix Red
                        title = "Orodha Yangu (Favorites)",
                        subtitle = "Filamu ulizozipenda na kuzihifadhi",
                        badge = if (uiState.favoritesCount > 0) "${uiState.favoritesCount}" else null,
                        onClick = onNavigateToFavorites
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF), thickness = 0.8.dp)

                    NetflixMenuItem(
                        icon = Icons.Default.Download,
                        iconTint = NeliCyanAccent,
                        title = "Vipakuliwa (Downloads)",
                        subtitle = "Tazama filamu na series bila mtandao",
                        onClick = onNavigateToDownloads
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF), thickness = 0.8.dp)

                    NetflixMenuItem(
                        icon = Icons.Default.History,
                        iconTint = Color(0xFF64B5F6),
                        title = "Endelea Kutazama",
                        subtitle = "Rejea pale ulipoishia kutazama",
                        badge = if (uiState.continueWatchingCount > 0) "${uiState.continueWatchingCount}" else null,
                        onClick = onNavigateToHome
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Section: Mipangilio ya Programu (App Preferences)
                NetflixSectionTitle(title = "Mipangilio ya Programu")
                Spacer(modifier = Modifier.height(8.dp))

                NetflixMenuContainer {
                    NetflixMenuItem(
                        icon = Icons.Default.Settings,
                        iconTint = Color(0xFFB0BEC5),
                        title = "Ubora wa Video na Mipangilio",
                        subtitle = "Mipangilio ya kucheza video na data",
                        onClick = onNavigateToSettings
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF), thickness = 0.8.dp)

                    NetflixMenuItem(
                        icon = Icons.Default.NotificationsActive,
                        iconTint = Color(0xFFFFB300),
                        title = "Arifa za Filamu Mpya",
                        subtitle = "Mapendekezo ya filamu 2 kila siku saa sita mchana",
                        badge = "Imewashwa",
                        onClick = {
                            MovieRecommendationScheduler.scheduleNext(context, 1000L)
                            Toast.makeText(context, "Arifa ya majaribio inatumwa kwenye simu yako sasa...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF), thickness = 0.8.dp)

                    NetflixMenuItem(
                        icon = Icons.Default.Language,
                        iconTint = Color(0xFF4CAF50),
                        title = "Nchi na Eneo",
                        subtitle = "${uiState.countryObj.flagEmoji} ${uiState.countryObj.name} (${uiState.effectiveCountryCode})",
                        onClick = { showCountryDialog = true }
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF), thickness = 0.8.dp)

                    NetflixMenuItem(
                        icon = Icons.Default.SupportAgent,
                        iconTint = Color(0xFF26A69A),
                        title = "Huduma kwa Wateja na Msaada",
                        subtitle = "Wasiliana nasi kupitia WhatsApp",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/255712345678"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "WhatsApp haijapatikana", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Section: Akaunti & Usalama
                NetflixSectionTitle(title = "Akaunti & Usalama")
                Spacer(modifier = Modifier.height(8.dp))

                NetflixMenuContainer {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x18FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF90CAF9),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Uthibitishaji wa Akaunti",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isUserLoggedIn) "Google Identity Services • Imelindwa" else "Akaunti ya muda (Mgeni)",
                                color = NeliTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (isUserLoggedIn) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = NeliGreenSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isUserLoggedIn) {
                        HorizontalDivider(color = Color(0x14FFFFFF), thickness = 0.8.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.signInWithGoogle(context) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Badilisha Akaunti ya Google",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Gusa ili kubadilisha akaunti nyingine ya Google",
                                    color = NeliTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Sign Out / Session Controls
                if (isUserLoggedIn) {
                    OutlinedButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("account_logout_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFFF5252)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FF5252))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Toka Kwenye Akaunti (Sign Out)",
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { showDeleteAccountDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Futa Akaunti Yangu",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App version footer
                Text(
                    text = "NeliPlay v2.4.0 • Kiswahili Movies & Series",
                    color = Color.Gray.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Dialogs
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Toka Kwenye Akaunti", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Je, una uhakika unataka kutoka kwenye akaunti yako ya NeliPlay?", color = NeliTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Toka", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Ghairi", color = Color.Gray)
                }
            },
            containerColor = NeliSurfaceElevated
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Futa Akaunti", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) },
            text = { Text("Kitendo hiki kitafuta taarifa zako zote na orodha yako ya filamu. Je, unataka kuendelea?", color = NeliTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Ndio, Futa", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Ghairi", color = Color.Gray)
                }
            },
            containerColor = NeliSurfaceElevated
        )
    }

    if (showCountryDialog) {
        val supportedCountries = RegionService.getAllCountries()
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = { Text("Chagua Nchi Yako", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    for (country in supportedCountries) {
                        val isSelected = country.code.equals(uiState.effectiveCountryCode, true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectCountry(country.code)
                                    showCountryDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectCountry(country.code)
                                    showCountryDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NeliCyanAccent,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${country.flagEmoji} ${country.name}",
                                color = if (isSelected) Color.White else NeliTextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryDialog = false }) {
                    Text("Funga", color = NeliCyanAccent)
                }
            },
            containerColor = NeliSurfaceElevated
        )
    }
}

/**
 * Netflix-style Profile Header Card with Google sign in
 */
@Composable
private fun NetflixProfileCard(
    isLoggedIn: Boolean,
    displayName: String?,
    email: String?,
    photoUrl: String?,
    isSigningIn: Boolean,
    onSignInWithGoogle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E1E24), Color(0xFF121216))
                )
            )
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Netflix-style Profile Box Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE50914), Color(0xFF831010))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName ?: if (isLoggedIn) "Mtumiaji wa NeliPlay" else "Mgeni",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    if (isLoggedIn && !email.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = email,
                                color = NeliTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "Hujaunganisha akaunti ya Google",
                            color = NeliTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // If not logged in, show Google Sign-In button right inside the card
            if (!isLoggedIn) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSignInWithGoogle,
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_sign_in_account_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F1F1F)
                    )
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            color = Color(0xFF4285F4),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Inaingia...",
                            color = Color(0xFF1F1F1F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ingia na Google",
                            color = Color(0xFF1F1F1F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetflixSectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFFCCCCCC),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun NetflixMenuContainer(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NeliSurfaceVariant)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
    ) {
        content()
    }
}

@Composable
private fun NetflixMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = NeliTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2200E676))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    color = NeliGreenSuccess,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(13.dp)
        )
    }
}
