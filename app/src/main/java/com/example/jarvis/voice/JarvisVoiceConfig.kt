package com.example.jarvis.voice

import org.json.JSONObject

enum class VoiceProviderType(val displayName: String) {
    ELEVENLABS("ElevenLabs")
}

/**
 * Centralized Voice Configuration for J.A.R.V.I.S.
 * Single source of truth for the cloned ElevenLabs Voice.
 */
data class JarvisVoiceConfig(
    val provider: VoiceProviderType = VoiceProviderType.ELEVENLABS,
    val voiceId: String = DEFAULT_VOICE_ID,
    val language: String = "auto",
    val model: String = DEFAULT_MODEL_ID,
    val speakingRate: Float = 1.0f,
    val stability: Float = 0.50f,
    val similarity: Float = 0.75f,
    val style: Float = 0.0f,
    val speakerBoost: Boolean = true,
    val enabled: Boolean = true
) {
    companion object {
        const val DEFAULT_VOICE_ID = "dIttBl4oQhi4hifzkuq5"
        const val DEFAULT_MODEL_ID = "eleven_multilingual_v2"
        const val TEST_PHRASE = "Hello. I am JARVIS. How can I help you?"

        fun fromJson(json: JSONObject): JarvisVoiceConfig {
            val providerStr = json.optString("provider", VoiceProviderType.ELEVENLABS.name)
            val providerType = try {
                VoiceProviderType.valueOf(providerStr)
            } catch (_: Exception) {
                VoiceProviderType.ELEVENLABS
            }

            return JarvisVoiceConfig(
                provider = providerType,
                voiceId = json.optString("voiceId", DEFAULT_VOICE_ID).ifBlank { DEFAULT_VOICE_ID },
                language = json.optString("language", "auto"),
                model = json.optString("model", DEFAULT_MODEL_ID).ifBlank { DEFAULT_MODEL_ID },
                speakingRate = json.optDouble("speakingRate", 1.0).toFloat(),
                stability = json.optDouble("stability", 0.50).toFloat(),
                similarity = json.optDouble("similarity", 0.75).toFloat(),
                style = json.optDouble("style", 0.0).toFloat(),
                speakerBoost = json.optBoolean("speakerBoost", true),
                enabled = json.optBoolean("enabled", true)
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("provider", provider.name)
            put("voiceId", voiceId)
            put("language", language)
            put("model", model)
            put("speakingRate", speakingRate.toDouble())
            put("stability", stability.toDouble())
            put("similarity", similarity.toDouble())
            put("style", style.toDouble())
            put("speakerBoost", speakerBoost)
            put("enabled", enabled)
        }
    }
}
