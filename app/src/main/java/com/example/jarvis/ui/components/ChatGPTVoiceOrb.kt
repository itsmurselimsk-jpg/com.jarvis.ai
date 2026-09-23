package com.example.jarvis.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Advanced Fluid Animated Voice Orb (Matching OpenAI ChatGPT Advanced Voice Mode)
 * Renders an ethereal, organic morphing watercolor gradient sphere with dynamic sound wave pulsation.
 */
@Composable
fun ChatGPTVoiceOrb(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    isActive: Boolean = true,
    isSpeaking: Boolean = false,
    isListening: Boolean = false,
    audioRmsDb: Float = 0f,
    primaryColor: Color = Color(0xFF4C82FB),
    secondaryColor: Color = Color(0xFF90B5FF),
    cloudColor: Color = Color(0xFFE0ECFF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceOrbTransition")

    // Slow organic rotation & breathing
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathing"
    )

    val waveShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveShift"
    )

    // Dynamic scale driven by RMS / active state
    val rmsNormalized = (audioRmsDb.coerceIn(0f, 60f) / 60f)
    val dynamicPulse = if (isSpeaking || isListening) 1f + (rmsNormalized * 0.25f) else 1f
    val effectiveScale = breathingScale * dynamicPulse

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (this.size.minDimension / 2f) * 0.82f * effectiveScale

            // Draw outer ambient halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isListening || isSpeaking) 0.35f else 0.18f),
                        secondaryColor.copy(alpha = if (isListening || isSpeaking) 0.15f else 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.55f
                ),
                center = center,
                radius = radius * 1.55f
            )

            // Draw multi-layered organic fluid cloud gradients
            // Layer 1: Base smooth blue-sky sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cloudColor,
                        secondaryColor.copy(alpha = 0.95f),
                        primaryColor
                    ),
                    center = Offset(center.x, center.y - (radius * 0.35f)),
                    radius = radius * 1.25f
                ),
                center = center,
                radius = radius
            )

            // Layer 2: Animated rotating organic gradient overlay (morphing clouds)
            val rotRad = Math.toRadians(rotation.toDouble())
            val offset1X = center.x + (cos(rotRad) * radius * 0.3f).toFloat()
            val offset1Y = center.y + (sin(rotRad) * radius * 0.3f).toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.70f),
                        secondaryColor.copy(alpha = 0.40f),
                        Color.Transparent
                    ),
                    center = Offset(offset1X, offset1Y),
                    radius = radius * 0.75f
                ),
                center = center,
                radius = radius
            )

            // Layer 3: Dynamic soft white aura wave across center
            val waveOffset = (waveShift * 2 * Math.PI)
            val waveX = center.x + (cos(waveOffset) * radius * 0.2f).toFloat()
            val waveY = center.y + (sin(waveOffset) * radius * 0.2f).toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        cloudColor.copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(waveX, waveY),
                    radius = radius * 0.60f
                ),
                center = center,
                radius = radius
            )

            // Subtle top atmospheric glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    center = Offset(center.x, center.y - (radius * 0.75f)),
                    radius = radius * 0.7f
                ),
                center = center,
                radius = radius
            )
        }
    }
}
