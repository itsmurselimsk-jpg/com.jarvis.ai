package com.example.jarvis.trainer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.training.BatchTrainingItemType
import com.example.jarvis.training.PromptBatchTrainingEngine
import com.example.jarvis.training.TrainerRepository
import com.example.jarvis.training.TrainingCategories
import com.example.jarvis.training.TrainingEngine
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
class PromptBatchTrainingTest {

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
        TrainingEngine.applyNewTrainingBundle(com.example.jarvis.training.TrainingBundle(version = 1), persist = true)
    }

    @Test
    fun testParseUserTrainingPrompt_extractsDirectivesAndCategories() {
        val userPrompt = """
            JARVIS should call me naturally.
            If I speak Bangla, reply in Bangla.
            If I speak English, reply in English.
            For destructive actions always ask for confirmation.
            When I ask about battery, give the current battery percentage.
            Be concise by default.
            Do not sound robotic.
        """.trimIndent()

        val directives = PromptBatchTrainingEngine.parseTrainingPrompt(userPrompt)
        assertEquals(7, directives.size)

        // Verify Language rules
        val banglaDirective = directives.firstOrNull { it.rawInstruction.contains("Bangla") }
        assertNotNull(banglaDirective)
        assertEquals(BatchTrainingItemType.LANGUAGE, banglaDirective?.type)
        assertEquals(TrainingCategories.BANGLA, banglaDirective?.category)

        val englishDirective = directives.firstOrNull { it.rawInstruction.contains("English") }
        assertNotNull(englishDirective)
        assertEquals(BatchTrainingItemType.LANGUAGE, englishDirective?.type)
        assertEquals(TrainingCategories.ENGLISH, englishDirective?.category)

        // Verify Safety rule
        val safetyDirective = directives.firstOrNull { it.rawInstruction.contains("destructive") }
        assertNotNull(safetyDirective)
        assertEquals(BatchTrainingItemType.SAFETY, safetyDirective?.type)
        assertTrue(safetyDirective!!.requiresConfirmation)

        // Verify Tool action
        val batteryDirective = directives.firstOrNull { it.rawInstruction.contains("battery") }
        assertNotNull(batteryDirective)
        assertEquals(BatchTrainingItemType.TOOL_ACTION, batteryDirective?.type)
        assertEquals("Battery", batteryDirective?.toolRequired)

        // Verify Personality rules
        val roboticDirective = directives.firstOrNull { it.rawInstruction.contains("robotic") }
        assertNotNull(roboticDirective)
        assertEquals(BatchTrainingItemType.PERSONALITY, roboticDirective?.type)

        val conciseDirective = directives.firstOrNull { it.rawInstruction.contains("concise") }
        assertNotNull(conciseDirective)
        assertEquals(BatchTrainingItemType.PERSONALITY, conciseDirective?.type)
    }

    @Test
    fun testApplyBatch_appliesAllDirectivesAsOneBatchAndLiveSyncs() {
        val trainerRepo = TrainerRepository(context)
        val initialVersion = trainerRepo.currentBundle.value.version

        val prompt = """
            If I speak Bangla, reply in Bangla.
            For destructive actions always ask for confirmation.
            When I ask about battery, give the current battery percentage.
            Be concise by default.
        """.trimIndent()

        val parsed = PromptBatchTrainingEngine.parseTrainingPrompt(prompt)
        assertEquals(4, parsed.size)

        // Apply as one batch
        val applyResult = trainerRepo.applyBatch(parsed, "Test Batch Apply")

        // Assert Result structure matches requirements
        assertTrue(applyResult.rulesCount >= 3)
        assertTrue(applyResult.safetyCount >= 1)
        assertTrue(applyResult.languageCount >= 1)
        assertTrue(applyResult.toolCount >= 1)
        assertTrue(applyResult.isSynced)
        assertTrue(applyResult.version >= initialVersion + 1)

        // Verify Main JARVIS active memory updated immediately (without app restart)
        val liveBundle = TrainingEngine.activeBundle.value
        assertEquals(applyResult.version, liveBundle.version)

        // Verify matched training works for battery
        val batteryMatch = TrainingEngine.matchRelevantTraining("battery level", "DEVICE_INFORMATION", "ENGLISH")
        assertEquals("Battery", batteryMatch.preferredTool)

        // Verify duplicate prevention: re-applying same prompt should not duplicate rules
        val repeatResult = trainerRepo.applyBatch(parsed, "Repeat Batch Apply")
        assertEquals(0, repeatResult.rulesCount) // All rules already present!
    }

    @Test
    fun testRollbackBatch_restoresPreviousVersion() {
        val trainerRepo = TrainerRepository(context)
        val v1Bundle = trainerRepo.currentBundle.value

        val prompt = "If I speak Bangla, reply in Bangla."
        val parsed = PromptBatchTrainingEngine.parseTrainingPrompt(prompt)
        val batchResult = trainerRepo.applyBatch(parsed, "V2 Batch")
        assertTrue(batchResult.version > v1Bundle.version)

        // Now Rollback using history
        val history = trainerRepo.history.value
        assertTrue(history.isNotEmpty())

        val previousEntry = history.first { it.version == batchResult.version }
        // Rollback to v1
        val rollbackEntry = com.example.jarvis.training.VersionHistoryEntry(
            version = v1Bundle.version,
            timestamp = System.currentTimeMillis(),
            itemsCount = v1Bundle.items.size,
            rulesCount = v1Bundle.rules.size,
            description = "Rollback to v1",
            bundleJson = v1Bundle.toJsonString()
        )
        val rollbackResult = trainerRepo.rollbackToVersion(rollbackEntry)
        assertTrue(rollbackResult.version > batchResult.version)
        assertEquals(v1Bundle.rules.size, trainerRepo.currentBundle.value.rules.size)
    }
}
