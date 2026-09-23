package com.example.jarvis.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.JarvisState
import com.example.jarvis.ui.components.ChatGPTVoiceOrb

@Composable
fun VoiceScreen(
    jarvisState: JarvisState,
    isListening: Boolean,
    isSpeaking: Boolean,
    liveTranscript: String,
    lastResponse: String,
    speechSupported: Boolean,
    voiceRmsDb: Float = 0f,
    isContinuousModeActive: Boolean = false,
    isMicMuted: Boolean = false,
    isSpeakerEnabled: Boolean = true,
    wakeWordStatus: String = "Active",
    currentLanguage: String = "Multilingual",
    currentVoiceProfile: String = "Natural",
    isDarkTheme: Boolean = false,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSpeakText: (String, Float, Float) -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleContinuousMode: () -> Unit = {},
    onToggleMicMute: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    onInterruptAndListen: () -> Unit = {},
    onNavigateVoiceSetup: () -> Unit = {},
    onNavigateVoiceProfiles: () -> Unit = {},
    onCloseVoiceMode: () -> Unit = {}
) {
    var speechSpeed by remember { mutableFloatStateOf(1.0f) }
    var speechPitch by remember { mutableFloatStateOf(1.0f) }
    var showTuningPanel by remember { mutableStateOf(false) }

    val bg = if (isDarkTheme) Color(0xFF171717) else Color(0xFFFAFAFA)
    val cardBg = if (isDarkTheme) Color(0xFF262626) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkTheme) Color(0xFFECECF1) else Color(0xFF0D0D0D)
    val textSecondary = if (isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF707070)
    val buttonBg = if (isDarkTheme) Color(0xFF2E2E2E) else Color(0xFFEFEFEF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Back / Menu (Left) and Tune / Settings (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .clickable { onCloseVoiceMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Voice Tuning Menu Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .clickable { showTuningPanel = !showTuningPanel },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Voice Tuning",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tuning Overlay if toggled
            AnimatedVisibility(visible = showTuningPanel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voice Speed: ${"%.1f".format(speechSpeed)}x", fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                        Text("Pitch: ${"%.1f".format(speechPitch)}x", fontSize = 13.sp, color = textSecondary)
                    }
                    Slider(
                        value = speechSpeed,
                        onValueChange = { speechSpeed = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF2563EB), activeTrackColor = Color(0xFF2563EB))
                    )
                }
            }

            // Central Area: Live Fluid Orb & Status Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Fluid Voice Orb
                ChatGPTVoiceOrb(
                    size = 270.dp,
                    isActive = isListening || isSpeaking,
                    isSpeaking = isSpeaking,
                    isListening = isListening,
                    audioRmsDb = voiceRmsDb,
                    primaryColor = Color(0xFF3B82F6),
                    secondaryColor = Color(0xFF93C5FD),
                    cloudColor = Color(0xFFDBEAFE)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Status message or live transcription
                val statusText = when {
                    isSpeaking -> "Speaking..."
                    isListening && liveTranscript.isNotBlank() -> liveTranscript
                    isListening -> "Listening..."
                    isMicMuted -> "Microphone muted"
                    else -> "Tap to start conversation"
                }

                Text(
                    text = statusText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                if (lastResponse.isNotBlank() && !isSpeaking) {
                    Text(
                        text = lastResponse,
                        fontSize = 13.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom Action Bar: [+ Ask ChatGPT pill], [Mute Mic], [Close ✕]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "+ Ask ChatGPT" capsule pill button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(cardBg)
                        .clickable {
                            onCloseVoiceMode()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add prompt",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Ask JARVIS",
                        fontSize = 15.sp,
                        color = textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Mute Microphone toggle button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isMicMuted) Color(0xFFFF4D4F) else cardBg)
                        .clickable { onToggleMicMute() }
                        .testTag("voice_mute_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute Microphone",
                        tint = if (isMicMuted) Color.White else textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // End / Close Voice Mode button (Black circular button with X)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) Color(0xFF2E2E2E) else Color(0xFF0D0D0D))
                        .clickable {
                            if (isListening) onStopListening()
                            if (isSpeaking) onStopSpeaking()
                            onCloseVoiceMode()
                        }
                        .testTag("voice_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "End Voice Mode",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
