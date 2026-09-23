package com.example.jarvis.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.ProviderSettings
import com.example.jarvis.model.VoiceSynthesisEngine
import com.example.jarvis.security.EncryptedStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Human Voice Synthesis Engine for J.A.R.V.I.S.
 * Bridges Gemini Studio Cloud Human Voice generation with on-device Neural WaveNet TTS.
 */
object HumanVoiceEngine {
    private const val TAG = "HumanVoiceEngine"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeSpeechJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var activeAudioFile: File? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastVoiceUsed = MutableStateFlow("On-Device Neural")
    val lastVoiceUsed: StateFlow<String> = _lastVoiceUsed.asStateFlow()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private var audioFocusRequest: AudioFocusRequest? = null

    /**
     * Primary entry point for speaking text with human realism.
     */
    fun speak(
        context: Context,
        text: String,
        speechRate: Float,
        pitch: Float,
        locale: Locale?,
        voiceProfile: VoiceProfileType,
        settings: ProviderSettings?,
        bridge: AndroidBridge,
        onDone: (() -> Unit)? = null
    ) {
        val cleanedText = TtsSanitizer.cleanForHumanSpeech(text)
        if (cleanedText.isBlank()) {
            onDone?.invoke()
            return
        }

        // Cancel any ongoing speech
        stop(bridge)

        val effectiveSettings = settings ?: loadSettingsFallback(context)
        val engine = effectiveSettings.voiceSynthesisEngine
        val apiKey = effectiveSettings.customApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }

        val canUseGeminiStudio = (engine == VoiceSynthesisEngine.GEMINI_STUDIO || engine == VoiceSynthesisEngine.HYBRID_AUTO) &&
                apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                isNetworkAvailable(context)

        if (canUseGeminiStudio) {
            _isSpeaking.value = true
            activeSpeechJob = scope.launch {
                val success = synthesizeWithGeminiStudio(
                    context = context,
                    text = cleanedText,
                    apiKey = apiKey,
                    voiceName = effectiveSettings.geminiVoiceName.ifBlank { voiceProfile.geminiVoiceName },
                    onDone = {
                        _isSpeaking.value = false
                        onDone?.invoke()
                    }
                )

                if (!success) {
                    // Fallback seamlessly to calibrated on-device neural voice
                    Log.i(TAG, "Gemini Studio voice unavailable, fallback to Neural on-device TTS")
                    _lastVoiceUsed.value = "Neural Device (${voiceProfile.profileName})"
                    bridge.speakDeviceNeural(
                        sanitizedText = cleanedText,
                        speechRate = speechRate,
                        pitch = pitch,
                        locale = locale,
                        voiceProfile = voiceProfile,
                        onDone = onDone
                    )
                }
            }
        } else {
            // Direct On-Device Neural Synthesis
            _lastVoiceUsed.value = "Neural Device (${voiceProfile.profileName})"
            bridge.speakDeviceNeural(
                sanitizedText = cleanedText,
                speechRate = speechRate,
                pitch = pitch,
                locale = locale,
                voiceProfile = voiceProfile,
                onDone = onDone
            )
        }
    }

    /**
     * Call Gemini TTS API with AUDIO response modality and prebuilt voice configuration.
     */
    private suspend fun synthesizeWithGeminiStudio(
        context: Context,
        text: String,
        apiKey: String,
        voiceName: String,
        onDone: () -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val model = "gemini-2.0-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val prompt = "Speak naturally as J.A.R.V.I.S., a helpful, intelligent personal AI assistant: $text"

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // Generation config with AUDIO modality & Voice Name
            val genConfig = JSONObject()
            val modalities = JSONArray().apply { put("AUDIO") }
            genConfig.put("responseModalities", modalities)

            val speechConfig = JSONObject()
            val voiceConfig = JSONObject()
            val prebuilt = JSONObject().put("voiceName", voiceName)
            voiceConfig.put("prebuiltVoiceConfig", prebuilt)
            speechConfig.put("voiceConfig", voiceConfig)
            genConfig.put("speechConfig", speechConfig)

            rootJson.put("generationConfig", genConfig)

            val body = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini Studio TTS returned HTTP ${response.code}: ${response.body?.string()}")
                return@withContext false
            }

            val responseBody = response.body?.string() ?: return@withContext false
            val parsed = JSONObject(responseBody)
            val parts = parsed.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")

            var base64Audio: String? = null
            var mimeType = "audio/wav"

            if (parts != null) {
                for (idx in 0 until parts.length()) {
                    val part = parts.optJSONObject(idx) ?: continue
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        base64Audio = inlineData.optString("data")
                        mimeType = inlineData.optString("mimeType", "audio/wav")
                        break
                    }
                }
            }

            if (base64Audio.isNullOrBlank()) {
                Log.w(TAG, "No audio inlineData found in Gemini response")
                return@withContext false
            }

            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            if (audioBytes == null || audioBytes.isEmpty()) {
                return@withContext false
            }

            // Write to local cache file
            val extension = if (mimeType.contains("mp3")) "mp3" else "wav"
            val tempFile = File(context.cacheDir, "jarvis_human_voice_${System.currentTimeMillis()}.$extension")
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
                fos.flush()
            }

            withContext(Dispatchers.Main) {
                playAudioFile(context, tempFile, voiceName, onDone)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error in synthesizeWithGeminiStudio", e)
            return@withContext false
        }
    }

    /**
     * Plays generated audio file via MediaPlayer with proper audio focus and cleanup.
     */
    private fun playAudioFile(
        context: Context,
        file: File,
        voiceName: String,
        onDone: () -> Unit
    ) {
        try {
            stopAudio()
            activeAudioFile = file
            _lastVoiceUsed.value = "Gemini Studio ($voiceName)"

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            requestAudioFocus(audioManager)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    abandonAudioFocus(audioManager)
                    cleanActiveAudioFile()
                    _isSpeaking.value = false
                    onDone()
                }
                setOnErrorListener { _, _, _ ->
                    abandonAudioFocus(audioManager)
                    cleanActiveAudioFile()
                    _isSpeaking.value = false
                    onDone()
                    true
                }
                start()
                _isSpeaking.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            cleanActiveAudioFile()
            _isSpeaking.value = false
            onDone()
        }
    }

    fun stop(bridge: AndroidBridge? = null) {
        activeSpeechJob?.cancel()
        activeSpeechJob = null
        stopAudio()
        _isSpeaking.value = false
        bridge?.stopDeviceTts()
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (_: Exception) {}
        cleanActiveAudioFile()
    }

    private fun cleanActiveAudioFile() {
        try {
            activeAudioFile?.delete()
            activeAudioFile = null
        } catch (_: Exception) {}
    }

    private fun requestAudioFocus(audioManager: AudioManager?) {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioFocus(audioManager: AudioManager?) {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun loadSettingsFallback(context: Context): ProviderSettings {
        return try {
            val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            val encKey = prefs.getString("encrypted_custom_api_key", "") ?: ""
            val decKey = if (encKey.isNotEmpty()) EncryptedStorage.decrypt(encKey) else ""
            val engineId = prefs.getString("voice_synthesis_engine", VoiceSynthesisEngine.HYBRID_AUTO.id) ?: VoiceSynthesisEngine.HYBRID_AUTO.id
            val voiceName = prefs.getString("gemini_voice_name", "Puck") ?: "Puck"
            ProviderSettings(
                customApiKey = decKey,
                voiceSynthesisEngine = VoiceSynthesisEngine.fromId(engineId),
                geminiVoiceName = voiceName
            )
        } catch (_: Exception) {
            ProviderSettings()
        }
    }
}
