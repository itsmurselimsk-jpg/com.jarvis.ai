package com.example.jarvis.trainer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.intent.ConversationIntent
import com.example.jarvis.personality.JarvisPersonality
import com.example.jarvis.personality.LanguageStyle
import com.example.jarvis.training.BehaviorRule
import com.example.jarvis.training.ConversationTraining
import com.example.jarvis.training.ConversationTurn
import com.example.jarvis.training.TrainerRepository
import com.example.jarvis.training.TrainingCategories
import com.example.jarvis.training.TrainingEngine
import com.example.jarvis.training.TrainingItem
import com.example.jarvis.training.TrainingTone
import com.example.jarvis.training.TrainingValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisTrainerIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("jarvis_training_active_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("jarvis_trainer_repository_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        org.robolectric.Robolectric.setupContentProvider(
            com.example.jarvis.training.JarvisTrainingContentProvider::class.java,
            com.example.jarvis.training.JarvisTrainingContentProvider.AUTHORITY
        )
        TrainingEngine.initialize(context)
    }

    @Test
    fun testExactScenario_teachFriendlyGreetingAndVerifyWithoutRestart() {
        // 1. Open JARVIS - verify baseline engine initialized
        val initialVersion = TrainingEngine.activeBundle.value.version

        // 2. Open JARVIS Trainer
        val trainerRepo = TrainerRepository(context)

        // 3. Create: User: "Hi", Bad: "System initialized.", Good: "Haan bhai, bol 😄", Rule: "Greetings should be natural and friendly."
        val greetingItem = TrainingItem(
            userInput = "Hi",
            category = TrainingCategories.GREETING,
            intent = "GREETING",
            goodResponses = listOf("Haan bhai, bol 😄"),
            badResponses = listOf("System initialized. How may I assist you?"),
            behaviorRules = listOf("Greetings should be natural and friendly."),
            tone = TrainingTone.FRIENDLY
        )
        trainerRepo.addItem(greetingItem)

        // 4. Press SAVE & APPLY
        val syncResult = trainerRepo.saveAndApply("User taught friendly greeting")
        assertTrue(syncResult.version > initialVersion)

        // 5. Confirm JARVIS receives the new training version
        val jarvisActiveBundle = TrainingEngine.activeBundle.value
        assertEquals(syncResult.version, jarvisActiveBundle.version)

        // 6 & 7. Without rebuilding or restarting JARVIS, send: "Hello"
        // Verify that JARVIS uses the newly applied friendly greeting behavior
        val response = JarvisPersonality.generateConversationalResponse(
            userInput = "Hello",
            intent = ConversationIntent.GREETING,
            languageStyle = LanguageStyle.HINGLISH
        )

        // Must NOT use the robotic response
        assertFalse(response.contains("System initialized"))
        // Must use the learned friendly pattern
        assertTrue(response.contains("😄") || response.contains("Hey") || response.contains("bhai") || response.contains("bol"))

        // Also test exact match "Hi"
        val hiResponse = JarvisPersonality.generateConversationalResponse(
            userInput = "Hi",
            intent = ConversationIntent.GREETING,
            languageStyle = LanguageStyle.HINGLISH
        )
        assertEquals("Haan bhai, bol 😄", hiResponse)
    }

    @Test
    fun testToolBehaviorAndConfirmationTraining() {
        val trainerRepo = TrainerRepository(context)

        // Train: "Battery kitni hai?" -> Expected Tool: Battery. Do not guess.
        val batteryItem = TrainingItem(
            userInput = "Battery kitni hai?",
            category = TrainingCategories.BATTERY,
            intent = "DEVICE_INFORMATION",
            goodResponses = listOf("Ek sec, battery check karta hoon."),
            badResponses = listOf("Aapki battery 85% hai."),
            behaviorRules = listOf("Always invoke the Battery tool. Never guess battery percentage."),
            toolRequired = "Battery",
            tone = TrainingTone.HELPFUL
        )
        trainerRepo.addItem(batteryItem)

        // Train: "Delete this file" -> Expected: User confirmation
        val deleteItem = TrainingItem(
            userInput = "Delete this file",
            category = TrainingCategories.FILE_OPERATION,
            intent = "DEVICE_ACTION",
            goodResponses = listOf("Please confirm file deletion."),
            behaviorRules = listOf("Require explicit user confirmation."),
            confirmationRequired = true,
            tone = TrainingTone.WARNING
        )
        trainerRepo.addItem(deleteItem)

        trainerRepo.saveAndApply("Added tool and safety rules")

        // Verify battery training matching
        val batteryMatch = TrainingEngine.matchRelevantTraining("Battery kitni hai?", "DEVICE_INFORMATION", "HINGLISH")
        assertEquals("Battery", batteryMatch.preferredTool)
        assertFalse(batteryMatch.requiresConfirmation)

        // Verify delete training matching
        val deleteMatch = TrainingEngine.matchRelevantTraining("Delete this file", "DEVICE_ACTION", "ENGLISH")
        assertTrue(deleteMatch.requiresConfirmation)
    }

    @Test
    fun testMultiTurnConversationTraining() {
        val trainerRepo = TrainerRepository(context)

        val turns = listOf(
            ConversationTurn(role = "user", text = "Hi"),
            ConversationTurn(role = "jarvis", text = "Haan bhai, bol 😄"),
            ConversationTurn(role = "user", text = "Kya kar raha hai?"),
            ConversationTurn(role = "jarvis", text = "Bas yahin hoon 😄 Bol, kya scene hai?"),
            ConversationTurn(role = "user", text = "Battery kitni hai?"),
            ConversationTurn(role = "jarvis", text = "Ek sec, battery check karta hoon.", toolUsed = "Battery")
        )

        val conv = ConversationTraining(
            title = "Banter to Battery Flow",
            turns = turns,
            expectedBehavior = "Natural transition from greeting to battery query.",
            intent = "CASUAL_CONVERSATION",
            toolRequired = "Battery"
        )

        trainerRepo.addConversationTraining(conv)
        val syncResult = trainerRepo.saveAndApply("Added multi-turn dialogue")

        val active = TrainingEngine.activeBundle.value
        assertEquals(syncResult.version, active.version)
        val matchedConv = active.conversationTrainings.firstOrNull { it.title == "Banter to Battery Flow" }
        assertNotNull(matchedConv)
        assertEquals(6, matchedConv!!.turns.size)
    }

    @Test
    fun testPersistenceAndRestartRecovery() {
        val trainerRepo = TrainerRepository(context)
        val customRule = BehaviorRule(rule = "Always be polite to developers", category = TrainingCategories.HELP)
        trainerRepo.addRule(customRule)
        val syncResult = trainerRepo.saveAndApply("Added developer rule")

        // Simulate app closing and cold restarting
        TrainingEngine.loadPersistedTraining()
        val reloadedBundle = TrainingEngine.activeBundle.value

        assertEquals(syncResult.version, reloadedBundle.version)
        assertTrue(reloadedBundle.rules.any { it.rule == "Always be polite to developers" })
    }

    @Test
    fun testRollbackFunctionality() {
        val trainerRepo = TrainerRepository(context)

        // Apply version A
        trainerRepo.addRule(BehaviorRule(rule = "Rule Alpha", category = TrainingCategories.GREETING))
        val vA = trainerRepo.saveAndApply("Version Alpha").version

        // Apply version B
        trainerRepo.addRule(BehaviorRule(rule = "Rule Beta", category = TrainingCategories.GREETING))
        val vB = trainerRepo.saveAndApply("Version Beta").version
        assertTrue(vB > vA)

        // Roll back to version A entry
        val historyEntryA = trainerRepo.history.value.first { it.version == vA }
        val rollbackResult = trainerRepo.rollbackToVersion(historyEntryA)

        assertTrue(rollbackResult.version > vB)
        val active = TrainingEngine.activeBundle.value
        // Active bundle must contain Rule Alpha and not Rule Beta
        assertTrue(active.rules.any { it.rule == "Rule Alpha" })
        assertFalse(active.rules.any { it.rule == "Rule Beta" })
    }

    @Test
    fun testSecurityValidationRejectsArbitraryExecution() {
        val maliciousItem = TrainingItem(
            userInput = "rm -rf /data/data/com.jarvis.ai; su -c reboot",
            category = TrainingCategories.SECURITY
        )
        val result = TrainingValidation.validateItem(maliciousItem)
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("Security violation"))
    }

    @Test
    fun testJsonExportAndImport() {
        val trainerRepo = TrainerRepository(context)
        val exportedJson = trainerRepo.exportToJson()
        assertTrue(exportedJson.contains("trainingVersion"))

        val newTrainerRepo = TrainerRepository(context)
        val imported = newTrainerRepo.importFromJson(exportedJson)
        assertTrue(imported)
        assertEquals(trainerRepo.currentBundle.value.version, newTrainerRepo.currentBundle.value.version)
    }
}
