package com.example.jarvis.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.ProviderSettings
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisSurfaceElevated
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.voice.JarvisVoiceConfig
import com.example.jarvis.voice.JarvisVoiceEngine
import com.example.jarvis.voice.SupportedLanguage
import com.example.jarvis.voice.VoiceSettingsRepository

/**
 * Clean, production-ready Voice Configuration Screen for J.A.R.V.I.S.
 * Dedicated exclusively to the ElevenLabs Cloned Voice Engine (Voice ID: dIttBl4oQhi4hifzkuq5).
 */
@Composable
fun VoiceSelectionScreen(
    currentSettings: ProviderSettings,
    onUpdateSettings: (ProviderSettings) -> Unit,
    onTestSpeak: (String, Float, Float) -> Unit
) {
    val context = LocalContext.current
    val voiceRepo = remember { VoiceSettingsRepository.getInstance(context) }
    val voiceConfig by voiceRepo.voiceConfig.collectAsState()
    val isConnected by voiceRepo.isConnected.collectAsState()
    val isSpeaking by JarvisVoiceEngine.isSpeaking.collectAsState()
    val currentError by JarvisVoiceEngine.currentError.collectAsState()

    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var languageDropdownOpen by remember { mutableStateOf(false) }
    var testPhraseInput by remember { mutableStateOf(JarvisVoiceConfig.TEST_PHRASE) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("voice_selection_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title Header
        item {
            Column {
                Text(
                    text = "ELEVENLABS VOICE ARCHITECTURE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )
                Text(
                    text = "Official Cloned J.A.R.V.I.S. Voice Engine (Multilingual v2)",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )
            }
        }

        // Voice Engine Status Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceElevated)
                    .border(
                        1.dp,
                        if (isSpeaking) JarvisGreen else if (isConnected) JarvisCyan.copy(alpha = 0.5f) else JarvisAmber,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSpeaking -> JarvisGreen
                                            isConnected -> JarvisCyan
                                            else -> JarvisAmber
                                        }
                                    )
                            )
                            Text(
                                text = when {
                                    isSpeaking -> "SYNTHESIZING / PLAYING SPEECH"
                                    isConnected -> "ELEVENLABS ENGINE ONLINE"
                                    else -> "SETUP REQUIRED — API KEY NEEDED"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = when {
                                    isSpeaking -> JarvisGreen
                                    isConnected -> JarvisCyan
                                    else -> JarvisAmber
                                }
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Status",
                            tint = if (isSpeaking) JarvisGreen else JarvisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Cloned Voice ID", fontSize = 10.sp, color = JarvisTextDim)
                            Text(
                                text = JarvisVoiceConfig.DEFAULT_VOICE_ID,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Model", fontSize = 10.sp, color = JarvisTextDim)
                            Text(
                                text = voiceConfig.model,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyanBright
                            )
                        }
                    }
                }
            }
        }

        // Active Error Banner (if error occurred during synthesis/playback)
        if (currentError != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisRed.copy(alpha = 0.12f))
                        .border(1.dp, JarvisRed.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = JarvisRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "VOICE ENGINE NOTICE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisRed
                            )
                            Text(
                                text = currentError?.userMessage ?: "Voice temporarily unavailable.",
                                fontSize = 12.sp,
                                color = JarvisTextPrimary
                            )
                            Text(
                                text = "JARVIS will still display responses in text format safely.",
                                fontSize = 10.sp,
                                color = JarvisTextDim
                            )
                        }
                    }
                }
            }
        }

        // Secure ElevenLabs API Key Configuration
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceElevated)
                    .border(0.5.dp, JarvisBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CREDENTIAL SECURITY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isConnected) JarvisGreen.copy(alpha = 0.2f) else JarvisAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isConnected) "KEY CONFIGURED" else "NOT CONFIGURED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) JarvisGreen else JarvisAmber
                            )
                        }
                    }

                    Text(
                        text = "Enter your ElevenLabs API Key. Stored securely in Android encrypted preferences. Never logged or exposed.",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("elevenlabs_api_key_input"),
                        label = { Text("ElevenLabs API Key", color = JarvisTextDim) },
                        placeholder = { Text(if (isConnected) "••••••••••••••••••••••••" else "xi-api-key...", color = JarvisTextDim) },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = JarvisTextDim
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    voiceRepo.saveApiKey(apiKeyInput.trim())
                                    apiKeyInput = ""
                                    Toast.makeText(context, "ElevenLabs API Key securely saved!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = apiKeyInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_api_key_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(Modifier.width(6.dp))
                            Text("Save Key", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        if (isConnected) {
                            OutlinedButton(
                                onClick = {
                                    voiceRepo.saveApiKey("")
                                    Toast.makeText(context, "API Key removed.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisRed)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }

        // Natural JARVIS Voice Calibration (ElevenLabs official parameters)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceElevated)
                    .border(0.5.dp, JarvisBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "VOICE ACOUSTIC CALIBRATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyan
                            )
                        }

                        IconButton(
                            onClick = {
                                voiceRepo.saveConfig(
                                    voiceConfig.copy(
                                        stability = 0.50f,
                                        similarity = 0.75f,
                                        style = 0.0f,
                                        speakingRate = 1.0f,
                                        speakerBoost = true
                                    )
                                )
                                Toast.makeText(context, "Reset to optimal JARVIS profile", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Defaults", tint = JarvisTextDim, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Stability
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stability (Calm vs Expressive)", fontSize = 11.sp, color = JarvisTextPrimary)
                            Text(String.format("%.2f", voiceConfig.stability), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
                        }
                        Text("Higher values produce calm, steady, consistent intelligence; lower values increase emotional range.", fontSize = 9.sp, color = JarvisTextDim)
                        Slider(
                            value = voiceConfig.stability,
                            onValueChange = { voiceRepo.saveConfig(voiceConfig.copy(stability = it)) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                        )
                    }

                    // Similarity Boost
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Clarity + Similarity Boost", fontSize = 11.sp, color = JarvisTextPrimary)
                            Text(String.format("%.2f", voiceConfig.similarity), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
                        }
                        Text("Matches original cloned JARVIS voice acoustics precisely.", fontSize = 9.sp, color = JarvisTextDim)
                        Slider(
                            value = voiceConfig.similarity,
                            onValueChange = { voiceRepo.saveConfig(voiceConfig.copy(similarity = it)) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                        )
                    }

                    // Style Exaggeration
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Style Exaggeration", fontSize = 11.sp, color = JarvisTextPrimary)
                            Text(String.format("%.2f", voiceConfig.style), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
                        }
                        Text("Subtle tonal dynamism. Kept low for natural, intelligent cadence.", fontSize = 9.sp, color = JarvisTextDim)
                        Slider(
                            value = voiceConfig.style,
                            onValueChange = { voiceRepo.saveConfig(voiceConfig.copy(style = it)) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                        )
                    }

                    // Speaking Speed
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pacing / Speed", fontSize = 11.sp, color = JarvisTextPrimary)
                            Text(String.format("%.2fx", voiceConfig.speakingRate), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
                        }
                        Slider(
                            value = voiceConfig.speakingRate,
                            onValueChange = { voiceRepo.saveConfig(voiceConfig.copy(speakingRate = it)) },
                            valueRange = 0.7f..1.4f,
                            colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                        )
                    }

                    // Speaker Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Acoustic Speaker Boost", fontSize = 12.sp, color = JarvisTextPrimary)
                            Text("Enhances clarity and audio presence of cloned voice", fontSize = 9.sp, color = JarvisTextDim)
                        }
                        Switch(
                            checked = voiceConfig.speakerBoost,
                            onCheckedChange = { voiceRepo.saveConfig(voiceConfig.copy(speakerBoost = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyan,
                                checkedTrackColor = JarvisCyan.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }

        // Live Voice Interruption & Acoustic Barge-in Notice
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F1E33))
                    .border(0.5.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Barge-in",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "ACOUSTIC BARGE-IN ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisCyan
                        )
                        Text(
                            text = "When JARVIS is speaking, talk naturally to immediately halt playback and process your new directive.",
                            fontSize = 10.sp,
                            color = JarvisTextSecondary
                        )
                    }
                }
            }
        }

        // Language & Speech Output Master Toggles
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceElevated)
                    .border(0.5.dp, JarvisBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Speech Output Enabled", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = JarvisTextPrimary)
                            Text("Automatically speak all JARVIS responses", fontSize = 10.sp, color = JarvisTextDim)
                        }
                        Switch(
                            checked = currentSettings.autoSpeakResponses && voiceConfig.enabled,
                            onCheckedChange = { enabled ->
                                voiceRepo.saveConfig(voiceConfig.copy(enabled = enabled))
                                onUpdateSettings(currentSettings.copy(autoSpeakResponses = enabled))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                        )
                    }

                    // Language Selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { languageDropdownOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            val currentLang = SupportedLanguage.fromCode(currentSettings.languageCode)
                            Text("Language: ${currentLang.displayName} (${currentLang.nativeName})", fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = languageDropdownOpen,
                            onDismissRequest = { languageDropdownOpen = false }
                        ) {
                            SupportedLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text("${lang.displayName} - ${lang.nativeName}") },
                                    onClick = {
                                        onUpdateSettings(currentSettings.copy(languageCode = lang.code))
                                        voiceRepo.saveConfig(voiceConfig.copy(language = lang.code))
                                        languageDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Test Voice Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceElevated)
                    .border(0.5.dp, JarvisBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "TEST CLONED JARVIS VOICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyan
                    )

                    OutlinedTextField(
                        value = testPhraseInput,
                        onValueChange = { testPhraseInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_voice_text_input"),
                        label = { Text("Test Text to Synthesize", color = JarvisTextDim) },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (testPhraseInput.isNotBlank()) {
                                    JarvisVoiceEngine.speak(
                                        context = context,
                                        text = testPhraseInput,
                                        config = voiceConfig,
                                        onDone = {}
                                    )
                                }
                            },
                            enabled = !isSpeaking,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_speak_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(6.dp))
                            Text("Synthesize & Play", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        if (isSpeaking) {
                            Button(
                                onClick = { JarvisVoiceEngine.stop() },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisRed)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("Stop", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}
