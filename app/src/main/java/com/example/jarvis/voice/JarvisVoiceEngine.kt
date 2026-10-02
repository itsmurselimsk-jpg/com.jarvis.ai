package com.example.jarvis.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Authoritative, Centralized Voice Engine for J.A.R.V.I.S.
 *
 * Exclusively coordinates Text-To-Speech generation and playback via ElevenLabs.
 * Ensures:
 * - Dynamic generation with Voice ID dIttBl4oQhi4hifzkuq5
 * - Zero fallback to robotic Android TextToSpeech
 * - Full acoustic barge-in / speech interruption
 * - Non-leaking security and robust error states
 */
object JarvisVoiceEngine {
    private const val TAG = "JarvisVoiceEngine"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeJob: Job? = null

    val provider: ElevenLabsVoiceProvider = ElevenLabsVoiceProvider()

    private var mediaPlayer: MediaPlayer? = null
    private var activeAudioFile: File? = null

    private var bargeInDetector: AcousticBargeInDetector? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _currentError = MutableStateFlow<VoiceError?>(null)
    val currentError: StateFlow<VoiceError?> = _currentError.asStateFlow()

    // Callback when user barge-in (interruption) triggers
    var onBargeInTriggered: (() -> Unit)? = null

    /**
     * Synthesizes text into MP3 audio bytes using ElevenLabs.
     */
    suspend fun synthesize(
        context: Context,
        text: String,
        config: JarvisVoiceConfig = VoiceSettingsRepository.getInstance(context).voiceConfig.value
    ): Result<ByteArray> {
        val apiKey = VoiceSettingsRepository.getInstance(context).getApiKey()
        return provider.synthesize(text, config, apiKey)
    }

    /**
     * Primary entry point for speaking JARVIS responses.
     */
    fun speak(
        context: Context,
        text: String,
        config: JarvisVoiceConfig = VoiceSettingsRepository.getInstance(context).voiceConfig.value,
        onDone: (() -> Unit)? = null
    ) {
        val cleanedText = TtsSanitizer.cleanForHumanSpeech(text)
        if (cleanedText.isBlank() || !config.enabled) {
            onDone?.invoke()
            return
        }

        // Cancel previous speech immediately (no overlapping speech)
        stop()

        _currentError.value = null
        _isSpeaking.value = true
        _lastSpokenText.value = cleanedText

        activeJob = scope.launch {
            val apiKey = VoiceSettingsRepository.getInstance(context).getApiKey()
            if (apiKey.isBlank()) {
                Log.w(TAG, "ElevenLabs API Key not configured")
                _isSpeaking.value = false
                _currentError.value = VoiceError.AuthenticationError()
                onDone?.invoke()
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                provider.synthesize(cleanedText, config, apiKey)
            }

            result.fold(
                onSuccess = { audioBytes ->
                    playAudioBytes(context, audioBytes, onDone)
                },
                onFailure = { error ->
                    Log.w(TAG, "Voice synthesis failed: ${error.message}")
                    _isSpeaking.value = false
                    _currentError.value = VoiceError.SynthesisError(error.message ?: "Synthesis failed")
                    onDone?.invoke()
                }
            )
        }
    }

    /**
     * Real Test Voice action using test text: "Hello. I am JARVIS. How can I help you?"
     */
    fun testVoice(
        context: Context,
        onDone: (() -> Unit)? = null
    ) {
        val config = VoiceSettingsRepository.getInstance(context).voiceConfig.value
        speak(
            context = context,
            text = JarvisVoiceConfig.TEST_PHRASE,
            config = config,
            onDone = onDone
        )
    }

    /**
     * Plays raw synthesized audio bytes with audio focus and acoustic barge-in monitoring.
     */
    private fun playAudioBytes(
        context: Context,
        bytes: ByteArray,
        onDone: (() -> Unit)?
    ) {
        try {
            val tempFile = File(context.cacheDir, "jarvis_tts_${System.currentTimeMillis()}.mp3")
            FileOutputStream(tempFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
            activeAudioFile = tempFile

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            requestAudioFocus(audioManager)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    stopBargeIn()
                    abandonAudioFocus(audioManager)
                    cleanAudioFile()
                    _isSpeaking.value = false
                    onDone?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    stopBargeIn()
                    abandonAudioFocus(audioManager)
                    cleanAudioFile()
                    _isSpeaking.value = false
                    _currentError.value = VoiceError.PlaybackError()
                    onDone?.invoke()
                    true
                }
                start()
                _isSpeaking.value = true
                startBargeIn(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaPlayer playback", e)
            _isSpeaking.value = false
            _currentError.value = VoiceError.PlaybackError(e.message ?: "Playback error")
            cleanAudioFile()
            onDone?.invoke()
        }
    }

    /**
     * Starts acoustic barge-in detection to interrupt speech when user speaks.
     */
    private fun startBargeIn(context: Context) {
        stopBargeIn()
        try {
            bargeInDetector = AcousticBargeInDetector(context) {
                // User spoke! Interrupt immediately
                Log.i(TAG, "Barge-in triggered by user speech: interrupting JARVIS")
                stop()
                onBargeInTriggered?.invoke()
            }.apply {
                startMonitoring()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start barge-in detector", e)
        }
    }

    private fun stopBargeIn() {
        try {
            bargeInDetector?.stopMonitoring()
            bargeInDetector = null
        } catch (_: Exception) {}
    }

    /**
     * Immediately stops current speech generation and playback.
     */
    fun stop() {
        activeJob?.cancel()
        activeJob = null
        provider.cancel()
        stopBargeIn()

        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (_: Exception) {}

        cleanAudioFile()
        _isSpeaking.value = false
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isSpeaking.value = false
            }
        } catch (_: Exception) {}
    }

    fun resume() {
        try {
            if (mediaPlayer != null && !_isSpeaking.value) {
                mediaPlayer?.start()
                _isSpeaking.value = true
            }
        } catch (_: Exception) {}
    }

    fun isSpeaking(): Boolean = _isSpeaking.value

    fun setVoice(config: JarvisVoiceConfig, context: Context) {
        VoiceSettingsRepository.getInstance(context).saveConfig(config)
    }

    fun release() {
        stop()
    }

    private fun cleanAudioFile() {
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
}
