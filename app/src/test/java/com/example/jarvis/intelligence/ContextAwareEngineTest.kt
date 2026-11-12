package com.example.jarvis.intelligence

import com.example.jarvis.brain.ContextAwareEngine
import com.example.jarvis.model.DeviceTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContextAwareEngineTest {

    private lateinit var engine: ContextAwareEngine

    @Before
    fun setUp() {
        engine = ContextAwareEngine(maxHistoryTurns = 4)
    }

    @Test
    fun testDialogueHistoryTrackingAndBoundedSlidingWindow() {
        engine.recordTurn("What is Kotlin?", "Kotlin is a modern statically typed programming language.")
        engine.recordTurn("Who developed it?", "JetBrains developed Kotlin.")

        val turns = engine.getRecentTurns()
        assertEquals(4, turns.size)
        assertEquals("User", turns[0].role)
        assertEquals("What is Kotlin?", turns[0].text)
        assertEquals("JARVIS", turns[1].role)

        val formattedPrompt = engine.formatConversationHistoryForPrompt()
        assertTrue(formattedPrompt.contains("Kotlin"))
        assertTrue(formattedPrompt.contains("JetBrains"))

        // Add 5 more turns to overflow maxHistoryTurns (4 turns = 8 messages)
        repeat(5) { i ->
            engine.recordTurn("Question $i", "Answer $i")
        }
        val boundedTurns = engine.getRecentTurns()
        assertTrue(boundedTurns.size <= 8)
    }

    @Test
    fun testCoreferenceResolutionForEnglishHindiAndBengali() {
        engine.recordTurn("What is Docker?", "Docker is an open-source containerization platform.")

        // English pronoun
        val resolvedEnglish = engine.resolveCoreference("Who created it?")
        assertTrue(resolvedEnglish.contains("Referring to 'Docker'"))

        // Hindi pronoun
        val resolvedHindi = engine.resolveCoreference("Uske features kya hain?")
        assertTrue(resolvedHindi.contains("Referring to 'Docker'"))

        // Bengali pronoun
        val resolvedBengali = engine.resolveCoreference("Seita kivabe use korbo?")
        assertTrue(resolvedBengali.contains("Referring to 'Docker'"))

        // Query without pronouns
        val nonPronoun = engine.resolveCoreference("What time is the sunset?")
        assertEquals("What time is the sunset?", nonPronoun)
    }

    @Test
    fun testTemporalAndSituationalGrounding() {
        val telemetry = DeviceTelemetry(
            batteryPercent = 88,
            isCharging = true,
            networkType = "Wi-Fi (Connected)",
            volumePercent = 75
        )

        val contextStr = engine.getTemporalAndSituationalContext(telemetry)
        assertTrue(contextStr.contains("Current Time:"))
        assertTrue(contextStr.contains("Current Date:"))
        assertTrue(contextStr.contains("Battery 88% (Charging)"))
        assertTrue(contextStr.contains("Wi-Fi"))
    }
}
