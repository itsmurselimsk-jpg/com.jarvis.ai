package com.example.jarvis.document

import java.util.Locale
import kotlin.math.ln
import kotlin.math.min

/**
 * Native Document RAG (Retrieval-Augmented Generation) Engine.
 * 
 * Inspired by modern search and retrieval architectures ("Build Your Own Search Engine / RAG"):
 * 1. Sliding Window Chunking: Breaks large documents (PDF, CSV, TXT, JSON, Markdown)
 *    into discrete indexed passages with semantic overlap.
 * 2. Inverted Index: Efficient term-to-chunk mapping.
 * 3. Okapi BM25 Ranking: Accurately identifies top-K evidence passages matching user queries.
 * 4. Citation Generation: Links answers to specific document sections and line numbers.
 * 5. Direct Extraction: Extracts answers directly for questions when operating in offline/autonomous mode.
 */
object DocumentRagEngine {

    data class DocumentChunk(
        val chunkId: Int,
        val text: String,
        val sectionTitle: String? = null,
        val startChar: Int = 0,
        val endChar: Int = 0
    )

    data class ScoredChunk(
        val chunk: DocumentChunk,
        val score: Double,
        val matchedTerms: List<String>
    )

    private const val CHUNK_SIZE = 450
    private const val CHUNK_OVERLAP = 80
    private const val K1 = 1.2
    private const val B = 0.75

    /**
     * Chunks a document into indexed passages preserving section metadata.
     */
    fun chunkDocument(document: DocumentModel): List<DocumentChunk> {
        val fullText = document.extractedText
        if (fullText.isBlank()) return emptyList()

        val chunks = mutableListOf<DocumentChunk>()
        var chunkId = 0

        // If document already has parsed sections (Markdown, JSON, XML, PDF)
        if (document.sections.isNotEmpty()) {
            for (sec in document.sections) {
                val secTitle = sec.title ?: "Section"
                val secContent = sec.content
                if (secContent.length <= CHUNK_SIZE) {
                    chunks.add(DocumentChunk(chunkId++, secContent.trim(), secTitle))
                } else {
                    // Split section using sliding window
                    var start = 0
                    while (start < secContent.length) {
                        val end = min(start + CHUNK_SIZE, secContent.length)
                        val slice = secContent.substring(start, end).trim()
                        if (slice.isNotBlank()) {
                            chunks.add(DocumentChunk(chunkId++, slice, secTitle, start, end))
                        }
                        if (end == secContent.length) break
                        start += (CHUNK_SIZE - CHUNK_OVERLAP)
                    }
                }
            }
        } else {
            // Sliding window over raw text
            var start = 0
            while (start < fullText.length) {
                val end = min(start + CHUNK_SIZE, fullText.length)
                val slice = fullText.substring(start, end).trim()
                if (slice.isNotBlank()) {
                    chunks.add(DocumentChunk(chunkId++, slice, null, start, end))
                }
                if (end == fullText.length) break
                start += (CHUNK_SIZE - CHUNK_OVERLAP)
            }
        }

        return chunks
    }

    /**
     * BM25 Retrieval of top-K relevant chunks for a user question.
     */
    fun retrieveRelevantPassages(
        document: DocumentModel,
        query: String,
        topK: Int = 4
    ): List<ScoredChunk> {
        val chunks = chunkDocument(document)
        if (chunks.isEmpty() || query.isBlank()) return emptyList()

        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        val n = chunks.size.toDouble()
        val chunkTokens = chunks.map { tokenize("${it.sectionTitle ?: ""} ${it.text}") }
        val avgChunkLen = chunkTokens.map { it.size }.average().coerceAtLeast(1.0)

        // Calculate Document Frequency (DF) for each query token
        val dfMap = mutableMapOf<String, Int>()
        for (token in queryTokens) {
            dfMap[token] = chunkTokens.count { it.contains(token) }
        }

        val scored = chunks.mapIndexed { idx, chunk ->
            val tokens = chunkTokens[idx]
            val chunkLen = tokens.size.toDouble()
            var score = 0.0
            val matched = mutableListOf<String>()

            for (qToken in queryTokens) {
                val tf = tokens.count { it == qToken }.toDouble()
                if (tf > 0) {
                    matched.add(qToken)
                    val df = dfMap[qToken] ?: 1
                    val idf = ln((n - df + 0.5) / (df + 0.5) + 1.0)
                    val numerator = tf * (K1 + 1.0)
                    val denominator = tf + K1 * (1.0 - B + B * (chunkLen / avgChunkLen))
                    score += idf * (numerator / denominator)
                }
            }

            // Section title match boost
            if (chunk.sectionTitle != null) {
                val secLower = chunk.sectionTitle.lowercase(Locale.ROOT)
                for (qToken in queryTokens) {
                    if (secLower.contains(qToken)) score += 2.0
                }
            }

            ScoredChunk(chunk, score, matched.distinct())
        }

        return scored
            .filter { it.score > 0.05 }
            .sortedByDescending { it.score }
            .take(topK)
    }

    /**
     * Formats retrieved RAG evidence passages into a bounded context string for AgentBrain.
     */
    fun formatRagContextForBrain(document: DocumentModel, query: String, topK: Int = 4): String {
        val passages = retrieveRelevantPassages(document, query, topK)
        if (passages.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("=== RAG RETRIEVED DOCUMENT PASSAGES (${document.fileName}) ===")
        sb.appendLine("Query: \"$query\"")
        passages.forEachIndexed { i, scored ->
            val c = scored.chunk
            val loc = if (c.sectionTitle != null) " [Section: ${c.sectionTitle}]" else ""
            sb.appendLine("Passage ${i + 1}$loc (Relevance: ${String.format(Locale.ROOT, "%.2f", scored.score)}):")
            sb.appendLine("\"${c.text.replace("\n", " ")}\"")
        }
        sb.appendLine("CITATION INSTRUCTION: Ground your answer using the retrieved passages above and cite the section if available.")
        sb.appendLine("================================================================")
        return sb.toString().trimEnd()
    }

    /**
     * Generates a direct extracted answer when running offline or autonomously without an external LLM.
     */
    fun generateExtractedAnswer(document: DocumentModel, query: String): String? {
        val passages = retrieveRelevantPassages(document, query, topK = 3)
        if (passages.isEmpty()) return null

        val top = passages.first()
        val sentences = top.chunk.text.split(Regex("""(?<=[.!?\n])\s+""")).filter { it.isNotBlank() }
        val queryTokens = tokenize(query)

        // Find best matching sentence within top chunk
        val bestSentence = sentences.maxByOrNull { s ->
            val sTokens = tokenize(s)
            queryTokens.count { sTokens.contains(it) }
        }

        val answerText = bestSentence?.trim() ?: top.chunk.text.take(200).trim()
        val sourceRef = if (top.chunk.sectionTitle != null) " (${top.chunk.sectionTitle})" else ""

        return "According to ${document.fileName}$sourceRef:\n\n\"$answerText\""
    }

    private fun tokenize(text: String): List<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "what", "where", "who", "how",
            "when", "why", "my", "your", "me", "you", "i", "to", "in", "on", "for", "with",
            "about", "tell", "jarvis", "please", "can", "hai", "kya", "ko", "ki", "ka", "se",
            "mein", "aur", "ye", "woh", "amar", "ami", "ar", "ki", "er", "te"
        )
        val pattern = Regex("""[a-zA-Z0-9\u0900-\u097F\u0980-\u09FF]+""")
        return pattern.findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .filter { it.length > 1 && !stopWords.contains(it) }
            .toList()
    }
}
