package com.example.jarvis.intelligence

import com.example.jarvis.document.DocumentModel
import com.example.jarvis.document.DocumentRagEngine
import com.example.jarvis.document.DocumentSection
import com.example.jarvis.document.DocumentType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentRagEngineTest {

    @Test
    fun testSlidingWindowChunkingAndSectionPreservation() {
        val longText = buildString {
            appendLine("Chapter 1: Artificial Intelligence fundamentals and history.")
            repeat(30) {
                append("Deep neural networks and transformer architectures revolutionized cognitive computation. ")
            }
            appendLine("\nChapter 2: Robotics and autonomous control systems.")
            repeat(30) {
                append("Feedback control loops, inverse kinematics, and edge teleoperation systems. ")
            }
        }

        val document = DocumentModel(
            fileName = "ai_robotics_textbook.txt",
            mimeType = "text/plain",
            documentType = DocumentType.TXT,
            sizeBytes = longText.length.toLong(),
            extractedText = longText
        )

        val chunks = DocumentRagEngine.chunkDocument(document)
        assertTrue(chunks.size > 2)
        assertTrue(chunks.all { it.text.isNotBlank() })
    }

    @Test
    fun testBM25PassageRetrievalAndCitation() {
        val sec1 = DocumentSection("Financials", "Revenue in Q3 reached $45.2M with a gross margin of 68%. Net income was $12.1M.")
        val sec2 = DocumentSection("Operations", "Supply chain logistics optimized across 14 distribution centers in Europe and Asia.")
        val sec3 = DocumentSection("Executive Team", "Tony Stark continues as Chief Technology Officer and Chief Architect.")

        val document = DocumentModel(
            fileName = "annual_report.pdf",
            mimeType = "application/pdf",
            documentType = DocumentType.PDF,
            sizeBytes = 4096,
            extractedText = "${sec1.content}\n${sec2.content}\n${sec3.content}",
            sections = listOf(sec1, sec2, sec3)
        )

        // Query financial revenue
        val revenueResults = DocumentRagEngine.retrieveRelevantPassages(document, "What was the revenue and gross margin in Q3?")
        assertFalse(revenueResults.isEmpty())
        assertTrue(revenueResults.first().chunk.text.contains("Revenue in Q3"))
        assertTrue(revenueResults.first().chunk.sectionTitle == "Financials")

        // Query executive
        val execResults = DocumentRagEngine.retrieveRelevantPassages(document, "Who is the Chief Technology Officer?")
        assertFalse(execResults.isEmpty())
        assertTrue(execResults.first().chunk.text.contains("Tony Stark"))
        assertTrue(execResults.first().chunk.sectionTitle == "Executive Team")

        // Format RAG context for AgentBrain
        val formattedRag = DocumentRagEngine.formatRagContextForBrain(document, "What was Q3 revenue?")
        assertTrue(formattedRag.contains("RAG RETRIEVED DOCUMENT PASSAGES"))
        assertTrue(formattedRag.contains("Financials"))
        assertTrue(formattedRag.contains("Revenue in Q3"))

        // Direct answer extraction for autonomous mode
        val directAnswer = DocumentRagEngine.generateExtractedAnswer(document, "Who is Chief Technology Officer?")
        assertNotNull(directAnswer)
        assertTrue(directAnswer!!.contains("Tony Stark"))
    }
}
