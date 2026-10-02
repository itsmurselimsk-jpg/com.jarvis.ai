package com.example.jarvis.voice

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ElevenLabsVoiceIntegrationTest {

    @Test
    fun testVoiceConfigurationAndConstants() {
        val config = JarvisVoiceConfig()
        assertEquals(VoiceProviderType.ELEVENLABS, config.provider)
        assertEquals("dIttBl4oQhi4hifzkuq5", config.voiceId)
        assertEquals("eleven_multilingual_v2", config.model)
        assertEquals(0.50f, config.stability, 0.001f)
        assertEquals(0.75f, config.similarity, 0.001f)
        assertEquals(0.0f, config.style, 0.001f)
        assertTrue(config.speakerBoost)
        assertTrue(config.enabled)
    }

    @Test
    fun testJarvisVoiceEngineUsesElevenLabsProvider() {
        assertNotNull(JarvisVoiceEngine.provider)
        assertTrue(JarvisVoiceEngine.provider is ElevenLabsVoiceProvider)
        assertEquals(VoiceProviderType.ELEVENLABS, JarvisVoiceEngine.provider.providerType)
    }

    @Test
    fun testApiKeyResolutionFromEnvironment() {
        val context = RuntimeEnvironment.getApplication()
        val repo = VoiceSettingsRepository.getInstance(context)
        val apiKey = repo.getApiKey()

        // Verify API key is read from environment/BuildConfig/secure prefs
        val envKey = System.getenv("ELEVENLABS_API_KEY")
        if (!envKey.isNullOrBlank() && envKey != "MY_ELEVENLABS_API_KEY") {
            assertEquals(envKey, apiKey)
            assertTrue(repo.hasValidApiKey())
        }
    }

    @Test
    fun testRealElevenLabsVoiceSynthesisIfKeyPresent() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = VoiceSettingsRepository.getInstance(context)
        val apiKey = repo.getApiKey()

        if (apiKey.isNotBlank() && apiKey != "MY_ELEVENLABS_API_KEY") {
            val provider = ElevenLabsVoiceProvider()
            val config = JarvisVoiceConfig()

            val result = provider.synthesize(
                text = "Hello. I am JARVIS. How can I help you?",
                config = config,
                apiKey = apiKey
            )

            assertTrue("Synthesis should succeed with valid ElevenLabs key", result.isSuccess)
            val audioBytes = result.getOrNull()
            assertNotNull(audioBytes)
            assertTrue("Audio bytes size should be > 1000 bytes", (audioBytes?.size ?: 0) > 1000)

            // Verify MP3 header (either ID3 or sync frame 0xFF)
            val firstByte = audioBytes!![0].toInt() and 0xFF
            val isMp3 = (firstByte == 0x49 && audioBytes[1].toInt() == 0x44 && audioBytes[2].toInt() == 0x33) ||
                    (firstByte == 0xFF)
            assertTrue("Generated audio must be valid MP3 format", isMp3)
        }
    }

    @Test
    fun testBargeInAndStopImmediateCancellation() {
        val context = RuntimeEnvironment.getApplication()

        // Start speaking
        JarvisVoiceEngine.speak(
            context = context,
            text = "Testing barge-in interruption capability",
            onDone = {}
        )

        // Trigger immediate stop / barge-in
        JarvisVoiceEngine.stop()

        assertFalse("Engine must not be speaking after stop()", JarvisVoiceEngine.isSpeaking())
    }

    @Test
    fun testAuthenticationErrorHandling() = runBlocking {
        val provider = ElevenLabsVoiceProvider()
        val config = JarvisVoiceConfig()

        // Test with invalid key
        val result = provider.synthesize(
            text = "Test",
            config = config,
            apiKey = "invalid_fake_key_12345"
        )

        assertTrue(result.isFailure)
        val errorMsg = result.exceptionOrNull()?.message ?: ""
        assertTrue(errorMsg.contains("Voice authentication issue") || errorMsg.contains("API key"))
    }
}
