package com.example.jarvis.voice

import java.io.InputStream

/**
 * Abstraction layer for Text-To-Speech providers.
 * Allows ElevenLabs to be the primary provider while maintaining clean decoupling.
 */
interface VoiceProvider {
    val providerType: VoiceProviderType

    /**
     * Synthesizes text into complete audio bytes.
     */
    suspend fun synthesize(
        text: String,
        config: JarvisVoiceConfig,
        apiKey: String
    ): Result<ByteArray>

    /**
     * Streams audio input stream for low-latency playback.
     */
    suspend fun stream(
        text: String,
        config: JarvisVoiceConfig,
        apiKey: String
    ): Result<InputStream>
}
