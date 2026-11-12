package com.example.jarvis.memory

import com.example.jarvis.model.MemoryItem
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.storage.db.MemoryEntity
import java.util.Locale
import kotlin.math.ln
import kotlin.math.max

/**
 * Intelligent Long-Term Memory Manager using Room Database Architecture.
 * 
 * Features:
 * 1. Automatic background semantic extraction of facts, preferences, and user profile
 *    across English, Hindi/Hinglish, and Bengali/Banglish.
 * 2. Deduplicated automated persistence directly to Room (`memories` table).
 * 3. Okapi BM25 semantic information retrieval with TF-IDF, length normalization,
 *    and recency decay.
 * 4. Multilingual tokenization supporting Latin, Bengali (বাংলা), and Devanagari (हिंदी).
 */
class LongTermMemoryManager(
    private val repository: JarvisRepository? = null
) {
    data class MemoryFact(
        val title: String,
        val content: String,
        val category: String,
        val tags: String = ""
    )

    data class ScoredMemory(
        val memory: MemoryItem,
        val score: Double
    )

    /**
     * Inspects a conversational utterance, automatically extracts permanent user facts/preferences,
     * and persists them to the Room database if not already present.
     */
    suspend fun extractAndPersistMemories(userInput: String): List<MemoryFact> {
        val facts = extractFactsFromUtterance(userInput)
        if (facts.isEmpty()) return emptyList()

        val repo = repository ?: return facts
        val existingMemories = repo.memories.value
        val newlyInserted = mutableListOf<MemoryFact>()

        for (fact in facts) {
            val isDuplicate = existingMemories.any { existing ->
                val titleOverlap = calculateTokenOverlap(existing.title, fact.title)
                val contentOverlap = calculateTokenOverlap(existing.content, fact.content)
                titleOverlap > 0.7 || contentOverlap > 0.75
            }

            if (!isDuplicate) {
                repo.addMemory(fact.title, fact.content, fact.category)
                newlyInserted.add(fact)
            }
        }

        return newlyInserted
    }

    /**
     * Multilingual extraction patterns for user profile, preferences, and facts.
     */
    fun extractFactsFromUtterance(input: String): List<MemoryFact> {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val extracted = mutableListOf<MemoryFact>()

        // 1. Name & Identity
        extractRegex(input, """(?i)\b(my name is|i am called|call me)\s+([A-Za-z\u0900-\u097F\u0980-\u09FF\s]{2,30})\b""")?.let {
            extracted.add(MemoryFact("User Name", "User's name is ${it.trim()}", "Profile", "identity,name"))
        }
        extractRegex(input, """(?i)\b(mera naam|mujhe)\s+([A-Za-z\u0900-\u097F\s]{2,30})\s+(hai|kehte hain)\b""")?.let {
            extracted.add(MemoryFact("User Name", "User's name is ${it.trim()}", "Profile", "identity,name,hindi"))
        }
        extractRegex(input, """(?i)\b(amar naam|amake)\s+([A-Za-z\u0980-\u09FF\s]{2,30})\s*(bole|daken|)\b""")?.let {
            extracted.add(MemoryFact("User Name", "User's name is ${it.trim()}", "Profile", "identity,name,bengali"))
        }

        // 2. Residence & Location
        extractRegex(input, """(?i)\b(i live in|i stay in|i am from|my home is in)\s+([A-Za-z\u0900-\u097F\u0980-\u09FF\s,]{2,40})\b""")?.let {
            extracted.add(MemoryFact("User Location", "User lives in ${it.trim()}", "Location", "location,home"))
        }
        extractRegex(input, """(?i)\b(main|mera ghar)\s+([A-Za-z\u0900-\u097F\s,]{2,40})\s+(mein rehta hoon|rehta hu|se hoon)\b""")?.let {
            extracted.add(MemoryFact("User Location", "User lives in ${it.trim()}", "Location", "location,home,hindi"))
        }
        extractRegex(input, """(?i)\b(ami|amar bari)\s+([A-Za-z\u0980-\u09FF\s,]{2,40})\s*(te thaki|e thaki|te bas kori)\b""")?.let {
            extracted.add(MemoryFact("User Location", "User lives in ${it.trim()}", "Location", "location,home,bengali"))
        }

        // 3. User Preferences & Favorites
        extractRegex(input, """(?i)\b(my favorite\s+([A-Za-z\s]{2,20})\s+is)\s+([A-Za-z0-9\s]{2,30})\b""")?.let {
            extracted.add(MemoryFact("User Preference", it.trim(), "Preferences", "favorite,likes"))
        }
        extractRegex(input, """(?i)\b(mera favorite\s+([A-Za-z\s]{2,20})\s+hai)\s+([A-Za-z0-9\u0900-\u097F\s]{2,30})\b""")?.let {
            extracted.add(MemoryFact("User Preference", it.trim(), "Preferences", "favorite,hindi"))
        }
        extractRegex(input, """(?i)\b(amar pochhonder\s+([A-Za-z\s]{2,20})\s+holo)\s+([A-Za-z0-9\u0980-\u09FF\s]{2,30})\b""")?.let {
            extracted.add(MemoryFact("User Preference", it.trim(), "Preferences", "favorite,bengali"))
        }

        // 4. Explicit Memory Directives ("Remember that ...")
        if (lower.startsWith("remember that ") || lower.startsWith("note that ") || lower.startsWith("keep in mind that ")) {
            val note = trimmed.substringAfter("that ").trim()
            if (note.length > 5) {
                extracted.add(MemoryFact("User Note", note, "UserDirective", "explicit,directive"))
            }
        }
        if (lower.startsWith("yaad rakhna ki ") || lower.startsWith("yaad rakhna ")) {
            val note = trimmed.substringAfter("yaad rakhna ").removePrefix("ki ").trim()
            if (note.length > 5) {
                extracted.add(MemoryFact("User Note", note, "UserDirective", "explicit,directive,hindi"))
            }
        }
        if (lower.startsWith("mone rakhis je ") || lower.startsWith("mone rakhben je ") || lower.startsWith("mone rekho ")) {
            val note = trimmed.substringAfter("je ").ifBlank { trimmed.substringAfter("rekho ") }.trim()
            if (note.length > 5) {
                extracted.add(MemoryFact("User Note", note, "UserDirective", "explicit,directive,bengali"))
            }
        }

        // 5. Contact & Personal Details
        val phoneMatch = Regex("""(?i)\b(phone number|mobile|contact|call me at)\s+(?:is\s+)?(\+?[0-9\-\s]{10,15})\b""").find(input)
        if (phoneMatch != null) {
            val num = phoneMatch.groupValues[2].trim()
            extracted.add(MemoryFact("Phone Number", "User's contact phone number is $num", "Contact", "phone,contact"))
        }

        val emailMatch = Regex("""(?i)\b(email|mail address)\s+(?:is\s+)?([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})\b""").find(input)
        if (emailMatch != null) {
            val email = emailMatch.groupValues[2].trim()
            extracted.add(MemoryFact("Email Address", "User's email is $email", "Contact", "email,contact"))
        }

        return extracted
    }

    /**
     * Okapi BM25 Ranking with TF-IDF, Document Length Normalization, and Recency Decay for MemoryItem.
     */
    fun searchMemoriesBM25(query: String, memories: List<MemoryItem>, topK: Int = 4): List<ScoredMemory> {
        if (memories.isEmpty() || query.isBlank()) return emptyList()

        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        val n = memories.size.toDouble()
        val docTokens = memories.map { mem ->
            val text = "${mem.title} ${mem.content} ${mem.category}"
            tokenize(text)
        }

        val avgDocLen = docTokens.map { it.size }.average().coerceAtLeast(1.0)
        val k1 = 1.5
        val b = 0.75

        // Calculate Document Frequency (DF) for each query token
        val dfMap = mutableMapOf<String, Int>()
        for (qToken in queryTokens) {
            val count = docTokens.count { tokens -> tokens.contains(qToken) }
            dfMap[qToken] = count
        }

        val scored = memories.mapIndexed { idx, mem ->
            val tokens = docTokens[idx]
            val docLen = tokens.size.toDouble()
            var bm25Score = 0.0

            for (qToken in queryTokens) {
                val tf = tokens.count { it == qToken }.toDouble()
                if (tf > 0) {
                    val df = dfMap[qToken] ?: 1
                    val idf = ln((n - df + 0.5) / (df + 0.5) + 1.0)
                    val numerator = tf * (k1 + 1.0)
                    val denominator = tf + k1 * (1.0 - b + b * (docLen / avgDocLen))
                    bm25Score += idf * (numerator / denominator)
                }
            }

            // Title exact/partial boost
            val lowerTitle = mem.title.lowercase(Locale.ROOT)
            val lowerQuery = query.lowercase(Locale.ROOT)
            if (lowerTitle.contains(lowerQuery) || lowerQuery.contains(lowerTitle)) {
                bm25Score += 4.0
            }

            // Recency decay factor (half-life of 30 days)
            val ageDays = max(0.0, (System.currentTimeMillis() - mem.timestamp) / (1000.0 * 60 * 60 * 24))
            val recencyMultiplier = Math.exp(-0.023 * (ageDays / 30.0))
            val finalScore = bm25Score * recencyMultiplier

            ScoredMemory(mem, finalScore)
        }

        return scored
            .filter { it.score > 0.1 }
            .sortedByDescending { it.score }
            .take(topK)
    }

    /**
     * Formats retrieved memories into a clean prompt context section.
     */
    fun formatMemoriesForContext(scoredMemories: List<ScoredMemory>): String {
        if (scoredMemories.isEmpty()) return ""
        val sb = StringBuilder()
        sb.appendLine("=== RELEVANT LONG-TERM MEMORIES (Room Database) ===")
        for (item in scoredMemories) {
            val mem = item.memory
            sb.appendLine("• [${mem.category}] ${mem.title}: ${mem.content}")
        }
        sb.appendLine("====================================================")
        return sb.toString().trimEnd()
    }

    /**
     * Multilingual Unicode tokenization for English, Bengali, and Hindi.
     */
    fun tokenize(text: String): List<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "what", "where", "who", "how",
            "when", "why", "my", "your", "me", "you", "i", "to", "in", "on", "for", "with",
            "about", "tell", "jarvis", "please", "can", "hai", "kya", "ko", "ki", "ka",
            "se", "mein", "aur", "ye", "woh", "amar", "ami", "ar", "ki", "er", "te"
        )

        // Matches alphanumeric characters in Latin, Devanagari (\u0900-\u097F), and Bengali (\u0980-\u09FF)
        val pattern = Regex("""[a-zA-Z0-9\u0900-\u097F\u0980-\u09FF]+""")
        return pattern.findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .filter { it.length > 1 && !stopWords.contains(it) }
            .toList()
    }

    private fun extractRegex(input: String, patternStr: String): String? {
        val regex = Regex(patternStr)
        val match = regex.find(input) ?: return null
        return if (match.groupValues.size >= 3) match.groupValues[2] else match.groupValues.lastOrNull()
    }

    private fun calculateTokenOverlap(s1: String, s2: String): Double {
        val t1 = tokenize(s1).toSet()
        val t2 = tokenize(s2).toSet()
        if (t1.isEmpty() || t2.isEmpty()) return 0.0
        val intersection = t1.intersect(t2).size.toDouble()
        return intersection / max(t1.size, t2.size)
    }
}
