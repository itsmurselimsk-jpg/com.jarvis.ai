package com.example

import com.example.jarvis.model.ProviderSettings
import com.example.jarvis.model.VoiceSynthesisEngine
import com.example.jarvis.voice.JarvisVoiceConfig
import com.example.jarvis.voice.SupportedLanguage
import com.example.jarvis.voice.TtsSanitizer
import com.example.jarvis.voice.VoiceError
import com.example.jarvis.voice.VoiceProfileType
import com.example.jarvis.voice.VoiceProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JarvisHumanVoiceUnitTest {

    @Test
    fun testVoiceSynthesisEngineResolution() {
        assertEquals(VoiceSynthesisEngine.ELEVENLABS, VoiceSynthesisEngine.fromId("elevenlabs"))
        assertEquals(VoiceSynthesisEngine.HYBRID_AUTO, VoiceSynthesisEngine.fromId("hybrid_auto"))
        assertEquals(VoiceSynthesisEngine.ELEVENLABS, VoiceSynthesisEngine.fromId("unknown_engine"))

        // Verify descriptive metadata exists
        assertTrue(VoiceSynthesisEngine.ELEVENLABS.title.contains("ElevenLabs"))
        assertTrue(VoiceSynthesisEngine.ELEVENLABS.subtitle.contains(JarvisVoiceConfig.DEFAULT_VOICE_ID))
    }

    @Test
    fun testJarvisVoiceConfigDefaults() {
        val config = JarvisVoiceConfig()
        assertEquals(VoiceProviderType.ELEVENLABS, config.provider)
        assertEquals("dIttBl4oQhi4hifzkuq5", config.voiceId)
        assertEquals("eleven_multilingual_v2", config.model)
        assertEquals(0.50f, config.stability, 0.01f)
        assertEquals(0.75f, config.similarity, 0.01f)
        assertEquals(0.0f, config.style, 0.01f)
        assertTrue(config.speakerBoost)
        assertTrue(config.enabled)

        // Test JSON round-trip
        val json = config.toJson()
        val restored = JarvisVoiceConfig.fromJson(json)
        assertEquals(config.voiceId, restored.voiceId)
        assertEquals(config.model, restored.model)
        assertEquals(config.stability, restored.stability, 0.01f)
    }

    @Test
    fun testVoiceErrorStates() {
        val authErr = VoiceError.AuthenticationError()
        assertTrue(authErr.userMessage.contains("Voice authentication issue"))
        assertTrue(authErr.technicalDetail.contains("API key"))

        val invalidIdErr = VoiceError.InvalidVoiceIdError("dIttBl4oQhi4hifzkuq5")
        assertTrue(invalidIdErr.technicalDetail.contains("dIttBl4oQhi4hifzkuq5"))
        assertTrue(invalidIdErr.userMessage.contains("Response displayed in text"))

        val netErr = VoiceError.NetworkError()
        assertTrue(netErr.userMessage.contains("network"))

        val rateErr = VoiceError.RateLimitError()
        assertTrue(rateErr.userMessage.contains("rate limit"))
    }

    @Test
    fun testVoiceProfilesAndGeminiVoices() {
        val bettany = VoiceProfileType.fromName("JARVIS Bettany")
        assertEquals(VoiceProfileType.BETTANY, bettany)
        assertEquals("Puck", bettany.geminiVoiceName)
        assertEquals("en-GB", bettany.preferredLocaleTag)
        assertTrue(bettany.tagline.contains("Bettany"))

        val friday = VoiceProfileType.fromName("F.R.I.D.A.Y. Female")
        assertEquals(VoiceProfileType.FRIDAY, friday)
        assertEquals("Kore", friday.geminiVoiceName)

        val deep = VoiceProfileType.fromName("JARVIS Deep")
        assertEquals(VoiceProfileType.DEEP, deep)
        assertEquals("Charon", deep.geminiVoiceName)

        val natural = VoiceProfileType.fromName("JARVIS Natural")
        assertEquals(VoiceProfileType.NATURAL, natural)

        // Verify fallback
        val fallback = VoiceProfileType.fromName("NonExistentProfile")
        assertNotNull(fallback)
    }

    @Test
    fun testCleanForHumanSpeechMarkdownStripping() {
        val markdown = "### System Status\n**JARVIS** is `online`! [View Docs](https://example.com)\n* System 1: Normal\n---"
        val cleaned = TtsSanitizer.cleanForHumanSpeech(markdown)

        assertFalse(cleaned.contains("###"))
        assertFalse(cleaned.contains("**"))
        assertFalse(cleaned.contains("`"))
        assertFalse(cleaned.contains("https://example.com"))
        assertFalse(cleaned.contains("---"))
        assertTrue(cleaned.contains("JARVIS is online"))
        assertTrue(cleaned.contains("View Docs"))
    }

    @Test
    fun testCleanForHumanSpeechCadence() {
        val input = "Good day Sir, all telemetry streams are optimal. Ready for instructions."
        val cleaned = TtsSanitizer.cleanForHumanSpeech(input)

        assertTrue(cleaned.contains("Good day Sir"))
        assertTrue(cleaned.contains("Ready for instructions"))
        assertEquals(cleaned, TtsSanitizer.cleanForHumanSpeech(cleaned))
    }

    @Test
    fun testProviderSettingsDefaultAudioEngine() {
        val settings = ProviderSettings()
        assertEquals(VoiceSynthesisEngine.ELEVENLABS, settings.voiceSynthesisEngine)
        assertEquals("Puck", settings.geminiVoiceName)
        assertEquals("JARVIS Natural", settings.voiceProfileName)
    }

    @Test
    fun testMultilingualGreetings() {
        val bengali = SupportedLanguage.fromCode("bn")
        assertEquals(SupportedLanguage.BENGALI, bengali)
        assertTrue(bengali.greetingPhrase.contains("স্যার"))

        val hindi = SupportedLanguage.fromCode("hi")
        assertEquals(SupportedLanguage.HINDI, hindi)
        assertTrue(hindi.greetingPhrase.contains("सर"))

        val hinglish = SupportedLanguage.fromCode("hi-Latn")
        assertEquals(SupportedLanguage.HINGLISH, hinglish)
        assertTrue(hinglish.greetingPhrase.contains("Sir"))
    }
}
