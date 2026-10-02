package com.example.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Authoritative Session State Machine for Real-Time Continuous Voice Conversation.
 *
 * Rules:
 * 1. The session is only terminated when the user explicitly turns it OFF.
 * 2. Automatic restart on onResults(), onEndOfSpeech(), silence timeouts, and recoverable errors.
 * 3. Exactly ONE SpeechRecognizer instance is maintained at any time.
 * 4. Full acoustic barge-in: User speech interrupts ElevenLabs playback immediately and captures new directive.
 * 5. Audio focus and echo protection cleanly coordinate between SpeechRecognizer and ElevenLabs playback.
 */
enum class LiveVoiceSessionState(val displayName: String) {
    OFF("OFF"),
    STARTING("STARTING"),
    LISTENING("LISTENING FOR SPEECH"),
    USER_SPEAKING("USER SPEAKING"),
    PROCESSING("PROCESSING DIRECTIVE"),
    JARVIS_SPEAKING("JARVIS RESPONDING"),
    RESTARTING("RESTARTING"),
    ERROR("RECOVERING")
}

object LiveVoiceSessionManager {
    private const val TAG = "LiveVoiceSessionMgr"

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _sessionState = MutableStateFlow(LiveVoiceSessionState.OFF)
    val sessionState: StateFlow<LiveVoiceSessionState> = _sessionState.asStateFlow()

    private val _isLiveSessionActive = MutableStateFlow(false)
    val isLiveSessionActive: StateFlow<Boolean> = _isLiveSessionActive.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _voiceRmsDb = MutableStateFlow(0f)
    val voiceRmsDb: StateFlow<Float> = _voiceRmsDb.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var activeSpeechRecognizer: SpeechRecognizer? = null
    private var restartRunnable: Runnable? = null
    private var appContext: Context? = null
    val echoDetector: EchoDetectionEngine = EchoDetectionEngine()

    // Callbacks for agent pipeline
    var onUserSpeechFinalized: ((String) -> Unit)? = null
    var onSessionStateChanged: ((LiveVoiceSessionState) -> Unit)? = null

    // Error recovery counters
    private var consecutiveErrors = 0
    private var consecutiveSilences = 0

    init {
        // Wire acoustic barge-in from JarvisVoiceEngine
        JarvisVoiceEngine.onBargeInTriggered = {
            if (_isLiveSessionActive.value) {
                notifyBargeInTriggered()
            }
        }
    }

    /**
     * User turns Live Voice Conversation ON.
     * The microphone remains continuously active until endSession() is called.
     */
    fun startSession(context: Context) {
        appContext = context.applicationContext
        _isLiveSessionActive.value = true
        _isMicMuted.value = false
        _lastError.value = null
        consecutiveErrors = 0
        consecutiveSilences = 0
        updateState(LiveVoiceSessionState.STARTING)

        cancelRestart()
        mainHandler.post {
            startListeningInternal()
        }
    }

    /**
     * The ONLY action that permanently terminates the live session.
     */
    fun endSession() {
        _isLiveSessionActive.value = false
        updateState(LiveVoiceSessionState.OFF)
        _isListening.value = false
        _voiceRmsDb.value = 0f
        _liveTranscript.value = ""

        cancelRestart()
        mainHandler.post {
            safeDestroyRecognizer()
            JarvisVoiceEngine.stop()
        }
    }

    fun toggleMicMute() {
        _isMicMuted.value = !_isMicMuted.value
        if (_isMicMuted.value) {
            cancelRestart()
            mainHandler.post {
                try {
                    activeSpeechRecognizer?.stopListening()
                    activeSpeechRecognizer?.cancel()
                } catch (_: Exception) {}
                _isListening.value = false
                _voiceRmsDb.value = 0f
            }
        } else if (_isLiveSessionActive.value) {
            if (_sessionState.value != LiveVoiceSessionState.JARVIS_SPEAKING &&
                _sessionState.value != LiveVoiceSessionState.PROCESSING
            ) {
                scheduleRestart(100L)
            }
        }
    }

    /**
     * Called when AgentBrain starts speaking JARVIS response.
     */
    fun notifyJarvisStartedSpeaking(spokenText: String) {
        if (!_isLiveSessionActive.value) return
        updateState(LiveVoiceSessionState.JARVIS_SPEAKING)
        echoDetector.notifyTtsStarted(spokenText)

        // Pause recognizer so microphone is clear for ElevenLabs playback and AcousticBargeInDetector
        cancelRestart()
        mainHandler.post {
            try {
                activeSpeechRecognizer?.stopListening()
                activeSpeechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isListening.value = false
            _voiceRmsDb.value = 0f
        }
    }

    /**
     * Called when JARVIS response finishes speaking naturally.
     * Automatically resumes microphone listening without user intervention.
     */
    fun notifyJarvisFinishedSpeaking() {
        if (!_isLiveSessionActive.value) return
        echoDetector.notifyTtsFinished()
        updateState(LiveVoiceSessionState.RESTARTING)

        // Automatic restart for multi-turn conversation
        scheduleRestart(150L)
    }

    /**
     * Called when user speech interrupts JARVIS during playback.
     */
    fun notifyBargeInTriggered() {
        if (!_isLiveSessionActive.value) return
        Log.i(TAG, "Acoustic Barge-In detected: user interrupted JARVIS playback")

        // 1. Immediately stop audio output
        JarvisVoiceEngine.stop()
        echoDetector.notifyTtsFinished()
        updateState(LiveVoiceSessionState.RESTARTING)

        // 2. Immediately restart recognition
        scheduleRestart(100L)
    }

    /**
     * Safely schedules a controlled restart with debounce/backoff.
     */
    fun scheduleRestart(delayMs: Long) {
        if (!_isLiveSessionActive.value || _isMicMuted.value) return
        cancelRestart()

        restartRunnable = Runnable {
            if (_isLiveSessionActive.value &&
                !_isMicMuted.value &&
                _sessionState.value != LiveVoiceSessionState.JARVIS_SPEAKING &&
                _sessionState.value != LiveVoiceSessionState.PROCESSING
            ) {
                startListeningInternal()
            }
        }
        mainHandler.postDelayed(restartRunnable!!, delayMs)
    }

    private fun cancelRestart() {
        restartRunnable?.let {
            mainHandler.removeCallbacks(it)
            restartRunnable = null
        }
    }

    private fun updateState(newState: LiveVoiceSessionState) {
        _sessionState.value = newState
        onSessionStateChanged?.invoke(newState)
    }

    /**
     * Starts a single recognition pass using a clean SpeechRecognizer instance.
     */
    private fun startListeningInternal() {
        if (!_isLiveSessionActive.value || _isMicMuted.value) return

        // Do not interrupt active JARVIS speech or LLM processing
        if (_sessionState.value == LiveVoiceSessionState.JARVIS_SPEAKING ||
            _sessionState.value == LiveVoiceSessionState.PROCESSING
        ) {
            return
        }

        updateState(LiveVoiceSessionState.STARTING)

        try {
            safeDestroyRecognizer()

            val ctx = appContext ?: return
            if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
                Log.w(TAG, "SpeechRecognizer is unavailable on this device")
                _sessionState.value = LiveVoiceSessionState.ERROR
                _lastError.value = "Speech recognition unavailable"
                return
            }

            val recognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
            activeSpeechRecognizer = recognizer
            recognizer.setRecognitionListener(createRecognitionListener())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Error starting SpeechRecognizer: ${e.message}")
            _isListening.value = false
            consecutiveErrors++
            scheduleRestart((300L * consecutiveErrors).coerceAtMost(2500L))
        }
    }

    private fun safeDestroyRecognizer() {
        try {
            activeSpeechRecognizer?.stopListening()
            activeSpeechRecognizer?.cancel()
            activeSpeechRecognizer?.destroy()
        } catch (_: Exception) {}
        activeSpeechRecognizer = null
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                consecutiveErrors = 0
                updateState(LiveVoiceSessionState.LISTENING)
            }

            override fun onBeginningOfSpeech() {
                updateState(LiveVoiceSessionState.USER_SPEAKING)
            }

            override fun onRmsChanged(rmsdB: Float) {
                _voiceRmsDb.value = rmsdB.coerceIn(0f, 15f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                _voiceRmsDb.value = 0f
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { partial ->
                    _liveTranscript.value = partial
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _voiceRmsDb.value = 0f
                consecutiveSilences = 0

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim() ?: ""

                if (recognized.isNotBlank()) {
                    val cleaned = echoDetector.cleanupLeadingEcho(recognized)
                    if (echoDetector.shouldRejectAsEcho(cleaned)) {
                        Log.d(TAG, "Ignored echo feedback: \"$cleaned\"")
                        if (_isLiveSessionActive.value) {
                            scheduleRestart(200L)
                        }
                        return
                    }

                    _liveTranscript.value = cleaned
                    updateState(LiveVoiceSessionState.PROCESSING)

                    // Deliver user speech to AgentBrain
                    onUserSpeechFinalized?.invoke(cleaned)
                } else {
                    // Empty speech result (noise or quick silence)
                    _liveTranscript.value = ""
                    if (_isLiveSessionActive.value) {
                        scheduleRestart(200L)
                    }
                }
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _voiceRmsDb.value = 0f

                Log.d(TAG, "SpeechRecognizer onError: $error (sessionState=${_sessionState.value})")

                if (!_isLiveSessionActive.value) return

                // If recognizer was cancelled intentionally for TTS or processing, ignore
                if (_sessionState.value == LiveVoiceSessionState.JARVIS_SPEAKING ||
                    _sessionState.value == LiveVoiceSessionState.PROCESSING
                ) {
                    return
                }

                // Recoverable errors
                when (error) {
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        // Silence timeout: normal pause in conversation.
                        // Do NOT terminate session. Restart listening promptly.
                        consecutiveSilences++
                        val delay = if (consecutiveSilences > 2) 400L else 200L
                        updateState(LiveVoiceSessionState.RESTARTING)
                        scheduleRestart(delay)
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> {
                        consecutiveErrors++
                        val delay = (350L * consecutiveErrors).coerceAtMost(2000L)
                        updateState(LiveVoiceSessionState.RESTARTING)
                        scheduleRestart(delay)
                    }
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_AUDIO -> {
                        consecutiveErrors++
                        val delay = (500L * consecutiveErrors).coerceAtMost(3000L)
                        updateState(LiveVoiceSessionState.RESTARTING)
                        scheduleRestart(delay)
                    }
                    else -> {
                        consecutiveErrors++
                        updateState(LiveVoiceSessionState.RESTARTING)
                        scheduleRestart(500L)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
