package com.example.jarvis.voice

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Production ElevenLabs Voice Provider for J.A.R.V.I.S.
 * Calls official ElevenLabs API for Voice ID dIttBl4oQhi4hifzkuq5.
 * Strictly adheres to security rules: never logs credentials, handles errors gracefully.
 */
class ElevenLabsVoiceProvider(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) : VoiceProvider {

    companion object {
        private const val TAG = "ElevenLabsVoiceProvider"
        private const val BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override val providerType: VoiceProviderType = VoiceProviderType.ELEVENLABS

    private var activeCall: Call? = null

    override suspend fun synthesize(
        text: String,
        config: JarvisVoiceConfig,
        apiKey: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_ELEVENLABS_API_KEY") {
            return@withContext Result.failure(
                Exception(VoiceError.AuthenticationError("ElevenLabs API key is not configured.").userMessage)
            )
        }

        val voiceId = config.voiceId.ifBlank { JarvisVoiceConfig.DEFAULT_VOICE_ID }
        val endpoint = "$BASE_URL/$voiceId"

        val requestBodyJson = buildPayload(text, config)

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("xi-api-key", apiKey)
            .addHeader("Accept", "audio/mpeg")
            .post(requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val call = httpClient.newCall(request)
            activeCall = call
            val response = call.execute()

            when (response.code) {
                200 -> {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        Result.success(bytes)
                    } else {
                        Result.failure(Exception(VoiceError.SynthesisError("Empty audio received from ElevenLabs.").userMessage))
                    }
                }
                401 -> {
                    Log.w(TAG, "ElevenLabs authentication failed (401)")
                    Result.failure(Exception(VoiceError.AuthenticationError().userMessage))
                }
                404 -> {
                    Log.w(TAG, "ElevenLabs Voice ID not found (404)")
                    Result.failure(Exception(VoiceError.InvalidVoiceIdError(voiceId).userMessage))
                }
                429 -> {
                    Log.w(TAG, "ElevenLabs quota or rate limit exceeded (429)")
                    Result.failure(Exception(VoiceError.RateLimitError().userMessage))
                }
                else -> {
                    Log.w(TAG, "ElevenLabs API error: HTTP ${response.code}")
                    Result.failure(Exception(VoiceError.SynthesisError("HTTP ${response.code} from ElevenLabs.").userMessage))
                }
            }
        } catch (e: Exception) {
            if (activeCall?.isCanceled() == true) {
                Result.failure(Exception(VoiceError.Cancelled().userMessage))
            } else {
                Log.w(TAG, "ElevenLabs network failure: ${e.message}")
                Result.failure(Exception(VoiceError.NetworkError(e.message ?: "Network error").userMessage))
            }
        } finally {
            activeCall = null
        }
    }

    override suspend fun stream(
        text: String,
        config: JarvisVoiceConfig,
        apiKey: String
    ): Result<InputStream> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_ELEVENLABS_API_KEY") {
            return@withContext Result.failure(
                Exception(VoiceError.AuthenticationError("ElevenLabs API key is missing.").userMessage)
            )
        }

        val voiceId = config.voiceId.ifBlank { JarvisVoiceConfig.DEFAULT_VOICE_ID }
        val endpoint = "$BASE_URL/$voiceId/stream"

        val requestBodyJson = buildPayload(text, config)

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("xi-api-key", apiKey)
            .addHeader("Accept", "audio/mpeg")
            .post(requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val call = httpClient.newCall(request)
            activeCall = call
            val response = call.execute()

            if (response.isSuccessful) {
                val stream = response.body?.byteStream()
                if (stream != null) {
                    Result.success(stream)
                } else {
                    Result.failure(Exception(VoiceError.SynthesisError("Null response stream").userMessage))
                }
            } else {
                when (response.code) {
                    401 -> Result.failure(Exception(VoiceError.AuthenticationError().userMessage))
                    404 -> Result.failure(Exception(VoiceError.InvalidVoiceIdError(voiceId).userMessage))
                    429 -> Result.failure(Exception(VoiceError.RateLimitError().userMessage))
                    else -> Result.failure(Exception(VoiceError.SynthesisError("HTTP ${response.code}").userMessage))
                }
            }
        } catch (e: Exception) {
            if (activeCall?.isCanceled() == true) {
                Result.failure(Exception(VoiceError.Cancelled().userMessage))
            } else {
                Result.failure(Exception(VoiceError.NetworkError(e.message ?: "Stream error").userMessage))
            }
        }
    }

    fun cancel() {
        try {
            activeCall?.cancel()
            activeCall = null
        } catch (_: Exception) {}
    }

    private fun buildPayload(text: String, config: JarvisVoiceConfig): JSONObject {
        val root = JSONObject()
        root.put("text", text)
        root.put("model_id", config.model.ifBlank { JarvisVoiceConfig.DEFAULT_MODEL_ID })

        val settings = JSONObject()
        settings.put("stability", config.stability.toDouble())
        settings.put("similarity_boost", config.similarity.toDouble())
        settings.put("style", config.style.toDouble())
        settings.put("use_speaker_boost", config.speakerBoost)

        root.put("voice_settings", settings)
        return root
    }
}
