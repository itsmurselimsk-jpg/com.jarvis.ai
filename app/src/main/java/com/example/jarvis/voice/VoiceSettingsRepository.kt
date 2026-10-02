package com.example.jarvis.voice

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.jarvis.security.EncryptedStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class VoiceSettingsRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "jarvis_voice_settings_prefs"
        private const val KEY_CONFIG_JSON = "voice_config_json"
        private const val KEY_ENCRYPTED_API_KEY = "encrypted_elevenlabs_api_key"

        @Volatile
        private var instance: VoiceSettingsRepository? = null

        fun getInstance(context: Context): VoiceSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: VoiceSettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _voiceConfig = MutableStateFlow(loadConfig())
    val voiceConfig: StateFlow<JarvisVoiceConfig> = _voiceConfig.asStateFlow()

    private val _isConnected = MutableStateFlow(hasValidApiKey())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private fun loadConfig(): JarvisVoiceConfig {
        val jsonStr = prefs.getString(KEY_CONFIG_JSON, null)
        if (!jsonStr.isNullOrBlank()) {
            return try {
                JarvisVoiceConfig.fromJson(JSONObject(jsonStr))
            } catch (_: Exception) {
                JarvisVoiceConfig()
            }
        }
        return JarvisVoiceConfig()
    }

    fun saveConfig(config: JarvisVoiceConfig) {
        _voiceConfig.value = config
        prefs.edit().putString(KEY_CONFIG_JSON, config.toJson().toString()).apply()
    }

    fun saveApiKey(plainApiKey: String) {
        if (plainApiKey.isNotBlank()) {
            val encrypted = EncryptedStorage.encrypt(plainApiKey.trim())
            prefs.edit().putString(KEY_ENCRYPTED_API_KEY, encrypted).apply()
        } else {
            prefs.edit().remove(KEY_ENCRYPTED_API_KEY).apply()
        }
        _isConnected.value = hasValidApiKey()
    }

    fun getApiKey(): String {
        val encrypted = prefs.getString(KEY_ENCRYPTED_API_KEY, null)
        if (!encrypted.isNullOrBlank()) {
            val decrypted = EncryptedStorage.decrypt(encrypted)
            if (decrypted.isNotBlank()) {
                return decrypted
            }
        }
        // Fallback to BuildConfig injected from Secrets panel / .env
        val buildKey = try {
            val k = BuildConfig.ELEVENLABS_API_KEY
            if (k.isNotBlank() && k != "MY_ELEVENLABS_API_KEY") k else ""
        } catch (_: Exception) {
            ""
        }
        if (buildKey.isNotBlank()) {
            return buildKey
        }

        // Fallback to system environment variable
        return try {
            val envKey = System.getenv("ELEVENLABS_API_KEY")
            if (!envKey.isNullOrBlank() && envKey != "MY_ELEVENLABS_API_KEY") envKey else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun hasValidApiKey(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_ELEVENLABS_API_KEY"
    }

    fun updateFromSync(
        voiceId: String? = null,
        speakingRate: Float? = null,
        stability: Float? = null,
        similarity: Float? = null,
        style: Float? = null,
        speakerBoost: Boolean? = null
    ) {
        val cur = _voiceConfig.value
        val updated = cur.copy(
            voiceId = voiceId?.ifBlank { cur.voiceId } ?: cur.voiceId,
            speakingRate = speakingRate ?: cur.speakingRate,
            stability = stability ?: cur.stability,
            similarity = similarity ?: cur.similarity,
            style = style ?: cur.style,
            speakerBoost = speakerBoost ?: cur.speakerBoost
        )
        saveConfig(updated)
    }
}
