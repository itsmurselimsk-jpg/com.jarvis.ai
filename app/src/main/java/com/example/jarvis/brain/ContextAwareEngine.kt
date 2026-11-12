package com.example.jarvis.brain

import com.example.jarvis.model.DeviceTelemetry
import com.example.jarvis.personality.LanguageStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Context-Aware Conversation Engine inspired by modern agent architectures ("Build Your Own Agent/LLM").
 * 
 * Coordinates:
 * 1. Multi-turn dialogue history tracking (Role, Content, Timestamp, Topic).
 * 2. Real-time temporal & environmental grounding (Time of day, Day of week, Battery, Connectivity).
 * 3. Cross-turn coreference resolution (Resolving "it", "that", "yeh", "seita", "uske baare mein").
 * 4. Structured prompt context synthesis for both Cloud and Local AI providers.
 */
class ContextAwareEngine(
    private val maxHistoryTurns: Int = 8
) {
    data class DialogueTurn(
        val role: String, // "User" or "JARVIS"
        val text: String,
        val timestamp: Long = System.currentTimeMillis(),
        val topicHint: String? = null
    )

    private val turns = mutableListOf<DialogueTurn>()
    private var lastDiscussedSubject: String? = null

    @Synchronized
    fun recordTurn(userText: String, jarvisText: String) {
        val topic = extractTopicHint(userText)
        if (!topic.isNullOrBlank()) {
            lastDiscussedSubject = topic
        }

        turns.add(DialogueTurn("User", userText, topicHint = topic))
        turns.add(DialogueTurn("JARVIS", jarvisText, topicHint = topic))

        // Retain sliding window within maxHistoryTurns
        while (turns.size > maxHistoryTurns * 2) {
            turns.removeAt(0)
        }
    }

    @Synchronized
    fun getRecentTurns(): List<DialogueTurn> = turns.toList()

    @Synchronized
    fun clearHistory() {
        turns.clear()
        lastDiscussedSubject = null
    }

    /**
     * Resolves pronouns and coreference references across conversation turns.
     * Supports English, Hindi/Hinglish, and Bengali/Banglish pronouns.
     */
    fun resolveCoreference(query: String): String {
        val subject = lastDiscussedSubject ?: return query
        val lower = query.trim().lowercase(Locale.ROOT)

        val pronounPatterns = listOf(
            Regex("""\b(it|its|that|this|they|them)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(yeh|woh|uske|iska|uski|isko|use)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(seita|ota|etar|otay|tar|take|or)\b""", RegexOption.IGNORE_CASE)
        )

        val hasPronoun = pronounPatterns.any { it.containsMatchIn(lower) }
        val isFollowUp = lower.startsWith("aur ") || lower.startsWith("tarpor ") || 
                         lower.startsWith("and ") || lower.startsWith("what about ") ||
                         lower.contains("aur batao") || lower.contains("aro bolo")

        return if (hasPronoun || isFollowUp) {
            "$query (Context: Referring to '$subject')"
        } else {
            query
        }
    }

    /**
     * Formats comprehensive temporal & environmental grounding string.
     */
    fun getTemporalAndSituationalContext(telemetry: DeviceTelemetry? = null): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        val hour24 = SimpleDateFormat("HH", Locale.getDefault()).format(now).toIntOrNull() ?: 12

        val timePeriod = when (hour24) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }

        val sb = StringBuilder()
        sb.appendLine("=== SITUATIONAL & TEMPORAL CONTEXT ===")
        sb.appendLine("Current Time: ${timeFormat.format(now)} ($timePeriod)")
        sb.appendLine("Current Date: ${dateFormat.format(now)}")

        if (telemetry != null) {
            val chargingStatus = if (telemetry.isCharging) "Charging" else "On Battery"
            sb.appendLine("Device Status: Battery ${telemetry.batteryPercent}% ($chargingStatus)")
            sb.appendLine("Connectivity: Network ${telemetry.networkType}")
            sb.appendLine("Audio State: Volume ${telemetry.volumePercent}%")
        }
        sb.appendLine("======================================")
        return sb.toString().trimEnd()
    }

    /**
     * Formats multi-turn dialogue history into a bounded, clean prompt section.
     */
    fun formatConversationHistoryForPrompt(): String {
        val recent = getRecentTurns()
        if (recent.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("=== CONVERSATION HISTORY (Recent Turns) ===")
        for (turn in recent) {
            sb.appendLine("${turn.role}: ${turn.text.replace("\n", " ").take(300)}")
        }
        lastDiscussedSubject?.let {
            sb.appendLine("Active Topic/Subject: $it")
        }
        sb.appendLine("===========================================")
        return sb.toString().trimEnd()
    }

    /**
     * Heuristically detects the primary noun or topic from user queries.
     */
    private fun extractTopicHint(text: String): String? {
        val clean = text.trim()
        val lower = clean.lowercase(Locale.ROOT)

        val prefixes = listOf(
            "what is ", "what are ", "who is ", "who was ", "tell me about ",
            "explain ", "how does ", "define ", "kya hai ", "kisko kehte hain ",
            "ki obostha ", "kemon ", "ki bolte ", "kothay "
        )

        for (p in prefixes) {
            if (lower.startsWith(p)) {
                val candidate = clean.substring(p.length).trim().trimEnd('?', '.', '!')
                if (candidate.length in 2..40) return candidate
            }
        }

        // Check for capitalized proper nouns
        val words = clean.split(Regex("\\s+"))
        val properNouns = words.filter { it.length > 2 && it[0].isUpperCase() && !isCommonSentenceStarter(it) }
        if (properNouns.isNotEmpty()) {
            return properNouns.joinToString(" ")
        }

        return null
    }

    private fun isCommonSentenceStarter(word: String): Boolean {
        return word.equals("What", true) || word.equals("Who", true) ||
               word.equals("Where", true) || word.equals("When", true) ||
               word.equals("Why", true) || word.equals("How", true) ||
               word.equals("Tell", true) || word.equals("Please", true) ||
               word.equals("Ami", true) || word.equals("Main", true) ||
               word.equals("Can", true) || word.equals("Could", true)
    }
}
