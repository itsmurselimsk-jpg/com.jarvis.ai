package com.example.jarvis.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Intelligent acoustic energy monitor used during JARVIS speech synthesis.
 * Uses VOICE_COMMUNICATION audio source, hardware Acoustic Echo Cancellation (AEC),
 * and dynamic speaker-baseline calibration so JARVIS's own speaker audio never
 * self-triggers barge-in and cuts speech off midway.
 */
class AcousticBargeInDetector(
    private val context: Context,
    private val onBargeInDetected: () -> Unit
) {
    companion object {
        private const val TAG = "AcousticBargeIn"
        private const val SAMPLE_RATE = 16000
        // High threshold: Normal phone speaker feedback into mic is 4,000-8,500 RMS.
        // Direct human speech close to mic is 14,000-24,000 RMS.
        private const val MIN_ABSOLUTE_RMS_THRESHOLD = 11500.0
        private const val REQUIRED_CONSECUTIVE_FRAMES = 5 // ~320ms sustained vocal energy
        private const val CALIBRATION_PERIOD_MS = 1500L
    }

    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    @Volatile
    private var isMonitoring = false
    private var monitorThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun startMonitoring() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        stopMonitoring()

        try {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
            if (minBufSize <= 0) return

            // 1. Prefer VOICE_COMMUNICATION for system-level echo cancellation
            var record: AudioRecord? = null
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    minBufSize * 2
                )
            } catch (e: Exception) {
                Log.w(TAG, "VOICE_COMMUNICATION AudioSource failed, falling back to MIC: ${e.message}")
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    minBufSize * 2
                )
            }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return
            }

            audioRecord = record

            // 2. Attach hardware AEC and Noise Suppression if available
            val sessionId = record.audioSessionId
            if (sessionId != 0) {
                try {
                    if (AcousticEchoCanceler.isAvailable()) {
                        echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                            enabled = true
                        }
                    }
                    if (NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                            enabled = true
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to attach audiofx: ${e.message}")
                }
            }

            record.startRecording()
            isMonitoring = true

            monitorThread = Thread {
                val buffer = ShortArray(1024)
                var consecutiveSpeechFrames = 0
                val startTime = System.currentTimeMillis()
                var baselineRms = 2000.0

                while (isMonitoring && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            val sample = buffer[i].toDouble()
                            sum += sample * sample
                        }
                        val rms = Math.sqrt(sum / read)
                        val elapsed = System.currentTimeMillis() - startTime

                        // Dynamic calibration: learn current speaker playback energy floor
                        if (elapsed < CALIBRATION_PERIOD_MS) {
                            if (rms > baselineRms) {
                                baselineRms = (baselineRms * 0.8) + (rms * 0.2)
                            }
                            continue
                        }

                        // Adaptive threshold: must exceed both minimum absolute and 2.0x speaker baseline
                        val effectiveThreshold = Math.max(MIN_ABSOLUTE_RMS_THRESHOLD, baselineRms * 2.0)

                        if (rms > effectiveThreshold) {
                            consecutiveSpeechFrames++
                            if (consecutiveSpeechFrames >= REQUIRED_CONSECUTIVE_FRAMES) {
                                isMonitoring = false
                                Log.i(TAG, "Genuine user barge-in detected (RMS: $rms, threshold: $effectiveThreshold), pausing TTS")
                                mainHandler.post {
                                    onBargeInDetected()
                                }
                                break
                            }
                        } else {
                            consecutiveSpeechFrames = 0
                            // Slowly decay baseline tracking to stay accurate
                            baselineRms = (baselineRms * 0.95) + (rms * 0.05)
                        }
                    }
                }
            }.apply {
                name = "AcousticBargeInDetectorThread"
                isDaemon = true
                priority = Thread.NORM_PRIORITY
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start acoustic barge-in detector: ${e.message}")
            stopMonitoring()
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
        try {
            monitorThread?.interrupt()
            monitorThread = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            echoCanceler?.release()
            echoCanceler = null
            noiseSuppressor?.release()
            noiseSuppressor = null
        } catch (_: Exception) {}
    }
}
