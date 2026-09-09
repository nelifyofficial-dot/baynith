package com.example.ui.account

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextPrimary
import com.example.ui.theme.NeliTextSecondary
import com.example.ui.theme.NeliVoid

/**
 * Account Screen designed to match the NeliPlay Account Page reference:
 * - Profile header with avatar, name, and email
 * - Subscription status card ("Premium")
 * - Action items: Settings, Help & Support, About, Log Out
 */
@Composable
fun AccountScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeliVoid)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("account_screen")
    ) {
        Text(
            text = "Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NeliTextPrimary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // User Profile Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NeliSurface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeliBluePrimary, NeliCyanAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Alex Baynith",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeliTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "alexbaynith@gmail.com",
                    fontSize = 13.sp,
                    color = NeliTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Subscription Row
        AccountItemRow(
            icon = Icons.Default.Star,
            iconTint = NeliCyanAccent,
            title = "Subscription",
            badgeText = "Premium",
            onClick = { /* In-app subscription info */ }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Settings Row
        AccountItemRow(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Help & Support
        AccountItemRow(
            icon = Icons.AutoMirrored.Filled.Help,
            title = "Help & Support",
            onClick = { /* Help info */ }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Log Out
        AccountItemRow(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Log Out",
            iconTint = Color(0xFFFF5252),
            titleColor = Color(0xFFFF5252),
            onClick = { /* Fast logout / reset */ }
        )

        Spacer(modifier = Modifier.height(80.dp))
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
            .clip(RoundedCornerShape(12.dp))
            .background(NeliSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeliBluePrimary.copy(alpha = 0.25f))
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
                tint = NeliTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
