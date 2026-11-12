package com.example.jarvis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.jarvis.model.DeviceTelemetry
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.ui.theme.ThemeManager

@Composable
fun TopBar(
    telemetry: DeviceTelemetry,
    isOnline: Boolean = true,
    onProfileClick: () -> Unit = {},
    onThemeClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activeTheme by ThemeManager.currentTheme.collectAsState()

    val animatedPrimaryColor by animateColorAsState(
        targetValue = activeTheme.primaryColor,
        animationSpec = tween(350),
        label = "top_bar_primary"
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = activeTheme.accentColor,
        animationSpec = tween(350),
        label = "top_bar_accent"
    )

    val transition = rememberInfiniteTransition(label = "pulse_beacon")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        activeTheme.surfaceColor.copy(alpha = 0.95f),
                        activeTheme.surfaceElevated.copy(alpha = 0.6f)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: JARVIS Identity & Beacon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Official JARVIS Emblem Badge with dynamic theme ring
                Image(
                    painter = painterResource(id = R.drawable.jarvis_logo_round),
                    contentDescription = "JARVIS Logo",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, animatedPrimaryColor.copy(alpha = 0.8f), CircleShape),
                    contentScale = ContentScale.Fit
                )

                // Pulsing Green/Theme Beacon
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) animatedPrimaryColor.copy(alpha = alpha) else Color.Red)
                )

                Text(
                    text = "J.A.R.V.I.S.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = animatedAccentColor
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(activeTheme.surfaceColor)
                        .border(0.6.dp, activeTheme.borderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isOnline) "ONLINE" else "STANDBY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isOnline) JarvisGreen else JarvisTextDim
                    )
                }
            }

            // Right: Telemetry Badges, Quick Theme Switcher, and Operator Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Network badge
                val netLabel = when {
                    telemetry.networkType.contains("WIFI", ignoreCase = true) -> "WiFi"
                    telemetry.networkType.contains("CELL", ignoreCase = true) || telemetry.networkType.contains("MOBILE", ignoreCase = true) -> "5G"
                    telemetry.networkType.contains("OFFLINE", ignoreCase = true) -> "OFF"
                    else -> telemetry.networkType.take(4)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Network",
                        tint = animatedPrimaryColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = netLabel,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextSecondary,
                        maxLines = 1
                    )
                }

                // Battery badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        contentDescription = "Battery",
                        tint = if (telemetry.batteryPercent <= 20) Color(0xFFFF5252) else animatedPrimaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${telemetry.batteryPercent}%",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextSecondary,
                        maxLines = 1
                    )
                }

                // Quick Color / Theme Switcher Icon
                IconButton(
                    onClick = {
                        if (onThemeClick != null) {
                            onThemeClick()
                        } else {
                            ThemeManager.cycleNextTheme(context)
                        }
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(activeTheme.primaryColor.copy(alpha = 0.15f))
                        .border(1.dp, animatedPrimaryColor.copy(alpha = 0.5f), CircleShape)
                        .testTag("top_bar_theme_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Switch App Color Theme",
                        tint = animatedPrimaryColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Operator Profile Icon Button
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("top_bar_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Operator Profile",
                        tint = animatedAccentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
