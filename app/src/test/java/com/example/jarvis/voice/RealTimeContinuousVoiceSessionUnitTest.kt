package com.example.jarvis.voice

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RealTimeContinuousVoiceSessionUnitTest {

    @Test
    fun testLiveVoiceSessionLifecycleAndMultiTurnFlow() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val sessionMgr = LiveVoiceSessionManager

        // Ensure clean initial state
        sessionMgr.endSession()
        assertEquals(LiveVoiceSessionState.OFF, sessionMgr.sessionState.value)
        assertFalse(sessionMgr.isLiveSessionActive.value)

        // Track state transitions and received speech
        val processedDirectives = mutableListOf<String>()
        sessionMgr.onUserSpeechFinalized = { text ->
            processedDirectives.add(text)
        }

        // Start Live Voice Session
        sessionMgr.startSession(context)
        assertTrue("Session must be active", sessionMgr.isLiveSessionActive.value)

        // Turn 1: User speaks "Hi JARVIS"
        sessionMgr.notifyJarvisStartedSpeaking("Hello Sir. All systems fully operational.")
        assertEquals(LiveVoiceSessionState.JARVIS_SPEAKING, sessionMgr.sessionState.value)
        sessionMgr.notifyJarvisFinishedSpeaking()
        assertEquals(LiveVoiceSessionState.RESTARTING, sessionMgr.sessionState.value)

        // Turn 2: User speaks "What's my battery?"
        sessionMgr.notifyJarvisStartedSpeaking("Your battery is at 94 percent and currently charging, Sir.")
        assertEquals(LiveVoiceSessionState.JARVIS_SPEAKING, sessionMgr.sessionState.value)
        sessionMgr.notifyJarvisFinishedSpeaking()
        assertEquals(LiveVoiceSessionState.RESTARTING, sessionMgr.sessionState.value)

        // Turn 3: User speaks "Open settings"
        sessionMgr.notifyJarvisStartedSpeaking("Opening settings console now, Sir.")
        assertEquals(LiveVoiceSessionState.JARVIS_SPEAKING, sessionMgr.sessionState.value)
        sessionMgr.notifyJarvisFinishedSpeaking()
        assertEquals(LiveVoiceSessionState.RESTARTING, sessionMgr.sessionState.value)

        // Turn 4: User speaks "Tell me a joke"
        sessionMgr.notifyJarvisStartedSpeaking("Why do programmers prefer dark mode? Because light attracts bugs, Sir.")
        assertEquals(LiveVoiceSessionState.JARVIS_SPEAKING, sessionMgr.sessionState.value)

        // Turn 5: User interrupts JARVIS while speaking (Barge-in!)
        sessionMgr.notifyBargeInTriggered()
        assertEquals(LiveVoiceSessionState.RESTARTING, sessionMgr.sessionState.value)
        assertTrue("Session must remain active on barge-in", sessionMgr.isLiveSessionActive.value)

        // Turn 6: Speak immediately after interruption
        sessionMgr.notifyJarvisStartedSpeaking("Directive acknowledged immediately, Sir.")
        assertEquals(LiveVoiceSessionState.JARVIS_SPEAKING, sessionMgr.sessionState.value)
        sessionMgr.notifyJarvisFinishedSpeaking()
        assertEquals(LiveVoiceSessionState.RESTARTING, sessionMgr.sessionState.value)

        // Turn 7: Multiple subsequent turns continue indefinitely
        for (turn in 7..12) {
            sessionMgr.notifyJarvisStartedSpeaking("Turn $turn response delivered.")
            assertEquals(LiveVoiceSessionState.JARVIS_SPEAKING, sessionMgr.sessionState.value)
            sessionMgr.notifyJarvisFinishedSpeaking()
            assertEquals(LiveVoiceSessionState.RESTARTING, sessionMgr.sessionState.value)
            assertTrue("Session must remain active across all turns", sessionMgr.isLiveSessionActive.value)
        }

        // Verify mic mute behavior
        sessionMgr.toggleMicMute()
        assertTrue(sessionMgr.isMicMuted.value)
        sessionMgr.toggleMicMute()
        assertFalse(sessionMgr.isMicMuted.value)

        // Only explicit endSession() may terminate the live session
        sessionMgr.endSession()
        assertFalse(sessionMgr.isLiveSessionActive.value)
        assertEquals(LiveVoiceSessionState.OFF, sessionMgr.sessionState.value)
    }

    @Test
    fun testSilenceTimeoutDoesNotTerminateLiveSession() {
        val context = RuntimeEnvironment.getApplication()
        val sessionMgr = LiveVoiceSessionManager

        sessionMgr.startSession(context)
        assertTrue(sessionMgr.isLiveSessionActive.value)

        // Simulate SpeechRecognizer silence timeouts (ERROR_SPEECH_TIMEOUT / ERROR_NO_MATCH)
        sessionMgr.scheduleRestart(50L)
        assertTrue("Silence must not terminate session", sessionMgr.isLiveSessionActive.value)

        // Clean up
        sessionMgr.endSession()
        assertFalse(sessionMgr.isLiveSessionActive.value)
    }
}
