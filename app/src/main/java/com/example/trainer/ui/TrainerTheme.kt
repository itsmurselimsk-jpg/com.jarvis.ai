package com.example.trainer.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TrainerBackground = Color(0xFF070A0F)
val TrainerSurface = Color(0xFF0E1520)
val TrainerCardSurface = Color(0xFF141F2D)
val TrainerCardBorder = Color(0xFF00E5FF).copy(alpha = 0.25f)
val TrainerCyan = Color(0xFF00E5FF)
val TrainerElectricBlue = Color(0xFF00B4D8)
val TrainerGreen = Color(0xFF00E676)
val TrainerAmber = Color(0xFFFFD600)
val TrainerRed = Color(0xFFFF5252)
val TrainerTextPrimary = Color(0xFFE2E8F0)
val TrainerTextSecondary = Color(0xFF94A3B8)

private val TrainerDarkColorScheme = darkColorScheme(
    primary = TrainerCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003847),
    onPrimaryContainer = TrainerCyan,
    secondary = TrainerElectricBlue,
    onSecondary = Color.Black,
    background = TrainerBackground,
    onBackground = TrainerTextPrimary,
    surface = TrainerSurface,
    onSurface = TrainerTextPrimary,
    surfaceVariant = TrainerCardSurface,
    onSurfaceVariant = TrainerTextSecondary,
    error = TrainerRed,
    onError = Color.Black
)

@Composable
fun JarvisTrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrainerDarkColorScheme,
        content = content
    )
}
