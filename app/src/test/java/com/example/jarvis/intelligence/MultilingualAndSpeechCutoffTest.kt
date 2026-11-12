package com.example.jarvis.intelligence

import com.example.jarvis.personality.JarvisPersonality
import com.example.jarvis.personality.LanguageStyle
import com.example.jarvis.provider.JarvisAutonomousBrain
import com.example.jarvis.voice.LanguageDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultilingualAndSpeechCutoffTest {

    @Test
    fun testLanguageDetectionAcrossScripts() {
        // Bengali script
        val bengaliStyle = JarvisPersonality.detectLanguageStyle("আমি জার্ভিস বলছি, আপনি কেমন আছেন?")
        assertEquals(LanguageStyle.BENGALI, bengaliStyle)

        // Banglish
        val banglishStyle = JarvisPersonality.detectLanguageStyle("kemon acho bhai ki khobor?")
        assertEquals(LanguageStyle.BANGLISH, banglishStyle)

        // Hindi script
        val hindiStyle = JarvisPersonality.detectLanguageStyle("नमस्ते जार्विस, आप कैसे हैं?")
        assertEquals(LanguageStyle.HINDI, hindiStyle)

        // Hinglish
        val hinglishStyle = JarvisPersonality.detectLanguageStyle("Bhai bolate bolate band Kyon Ho Raha Hai bata sakte ho")
        assertEquals(LanguageStyle.HINGLISH, hinglishStyle)

        // English
        val englishStyle = JarvisPersonality.detectLanguageStyle("What is the current system telemetry status?")
        assertEquals(LanguageStyle.ENGLISH, englishStyle)
    }

    @Test
    fun testSpeechCutoffQuestionDiagnosticResponse() {
        val query = "Bhai bolate bolate band Kyon Ho Raha Hai bata sakte ho"
        val response = JarvisAutonomousBrain.generateAutonomousResponse(query)

        // Verifies the user's specific problem is clearly diagnosed and technical fix explained
        assertTrue(response.contains("Acoustic Barge-In"))
        assertTrue(response.contains("Echo Cancellation") || response.contains("AEC") || response.contains("3,200 RMS"))
        assertTrue(response.contains("VOICE_COMMUNICATION") || response.contains("11,500"))
    }

    @Test
    fun testBanglaSpeechCutoffDiagnosticResponse() {
        val query = "bolte bolte theme jacche keno bhai"
        val response = JarvisAutonomousBrain.generateAutonomousResponse(query)

        assertTrue(response.contains("Acoustic Barge-In") || response.contains("৩,২০০ RMS"))
        assertTrue(response.contains("VOICE_COMMUNICATION") || response.contains("Acoustic Echo Canceler"))
    }
}
