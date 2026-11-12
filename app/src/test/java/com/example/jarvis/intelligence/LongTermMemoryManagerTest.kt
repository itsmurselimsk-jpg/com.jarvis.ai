package com.example.jarvis.intelligence

import com.example.jarvis.memory.LongTermMemoryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class LongTermMemoryManagerTest {

    private lateinit var memoryManager: LongTermMemoryManager

    @Before
    fun setUp() {
        memoryManager = LongTermMemoryManager()
    }

    @Test
    fun testMultilingualFactExtraction() {
        // English
        val englishFacts = memoryManager.extractFactsFromUtterance("My name is Tony Stark and I live in Malibu")
        assertTrue(englishFacts.any { it.title == "User Name" && it.content.contains("Tony Stark") })
        assertTrue(englishFacts.any { it.title == "User Location" && it.content.contains("Malibu") })

        // Hindi
        val hindiFacts = memoryManager.extractFactsFromUtterance("Mera naam Rohit Verma hai aur main Mumbai mein rehta hoon")
        assertTrue(hindiFacts.any { it.title == "User Name" && it.content.contains("Rohit Verma") })
        assertTrue(hindiFacts.any { it.title == "User Location" && it.content.contains("Mumbai") })

        // Bengali
        val bengaliFacts = memoryManager.extractFactsFromUtterance("Amar naam Sourav Ganguly ami Kolkata te thaki")
        assertTrue(bengaliFacts.any { it.title == "User Name" && it.content.contains("Sourav Ganguly") })
        assertTrue(bengaliFacts.any { it.title == "User Location" && it.content.contains("Kolkata") })

        // Explicit Directives
        val directive = memoryManager.extractFactsFromUtterance("Remember that flight to Tokyo is on Monday morning")
        assertTrue(directive.any { it.title == "User Note" && it.content.contains("flight to Tokyo is on Monday morning") })
    }

    @Test
    fun testBM25SemanticRetrievalAndRanking() {
        val memories = listOf(
            com.example.jarvis.model.MemoryItem(
                id = UUID.randomUUID().toString(),
                title = "User Location",
                content = "User lives in Kolkata and works as a Software Engineer",
                category = "Location",
                timestamp = System.currentTimeMillis()
            ),
            com.example.jarvis.model.MemoryItem(
                id = UUID.randomUUID().toString(),
                title = "Favorite Foods",
                content = "User loves pizza, pasta and iced espresso",
                category = "Preferences",
                timestamp = System.currentTimeMillis()
            ),
            com.example.jarvis.model.MemoryItem(
                id = UUID.randomUUID().toString(),
                title = "Emergency Contact",
                content = "Doctor contact phone number is +91-9876543210",
                category = "Contact",
                timestamp = System.currentTimeMillis()
            )
        )

        // Query location
        val locationResults = memoryManager.searchMemoriesBM25("Where does the user live in Kolkata?", memories)
        assertFalse(locationResults.isEmpty())
        assertEquals("User Location", locationResults.first().memory.title)

        // Query food
        val foodResults = memoryManager.searchMemoriesBM25("What are the user's favorite foods?", memories)
        assertFalse(foodResults.isEmpty())
        assertEquals("Favorite Foods", foodResults.first().memory.title)

        // Context formatting
        val formatted = memoryManager.formatMemoriesForContext(locationResults)
        assertTrue(formatted.contains("RELEVANT LONG-TERM MEMORIES"))
        assertTrue(formatted.contains("Kolkata"))
    }

    @Test
    fun testUnicodeTokenizationForBengaliAndHindi() {
        val bengaliTokens = memoryManager.tokenize("আমার নাম অনির্বাণ চক্রবর্তী")
        assertTrue(bengaliTokens.contains("অনির্বাণ"))
        assertTrue(bengaliTokens.contains("চক্রবর্তী"))

        val hindiTokens = memoryManager.tokenize("मेरा नाम विकास शर्मा है")
        assertTrue(hindiTokens.contains("विकास"))
        assertTrue(hindiTokens.contains("शर्मा"))
    }
}
