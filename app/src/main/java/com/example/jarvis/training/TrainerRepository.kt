package com.example.jarvis.training

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class VersionHistoryEntry(
    val version: Int,
    val timestamp: Long,
    val itemsCount: Int,
    val rulesCount: Int,
    val description: String,
    val bundleJson: String
)

class TrainerRepository(private val context: Context) {

    companion object {
        private const val TAG = "TrainerRepository"
        private const val PREFS_NAME = "jarvis_trainer_repository_prefs"
        private const val KEY_BUNDLE = "current_bundle_json"
        private const val KEY_HISTORY = "version_history_json"
        private const val KEY_CURRENT_VERSION = "current_version_counter"
    }

    val ipcClient = TrainingIpcClient(context)

    private val _currentBundle = MutableStateFlow(TrainingBundle(version = 1))
    val currentBundle: StateFlow<TrainingBundle> = _currentBundle.asStateFlow()

    private val _history = MutableStateFlow<List<VersionHistoryEntry>>(emptyList())
    val history: StateFlow<List<VersionHistoryEntry>> = _history.asStateFlow()

    private val _undoStack = mutableListOf<TrainingBundle>()

    init {
        loadFromStorage()
        ipcClient.startServiceBinding()
    }

    private fun loadFromStorage() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bundleJson = prefs.getString(KEY_BUNDLE, null)
        if (!bundleJson.isNullOrBlank()) {
            try {
                _currentBundle.value = TrainingBundle.fromJsonString(bundleJson)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bundle", e)
            }
        } else {
            // Seed initial training baseline if first launch
            seedInitialBaseline()
        }

        val historyJson = prefs.getString(KEY_HISTORY, null)
        if (!historyJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<VersionHistoryEntry>()
                val arr = JSONArray(historyJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        VersionHistoryEntry(
                            version = obj.getInt("version"),
                            timestamp = obj.getLong("timestamp"),
                            itemsCount = obj.getInt("itemsCount"),
                            rulesCount = obj.getInt("rulesCount"),
                            description = obj.optString("description", "Training update"),
                            bundleJson = obj.getString("bundleJson")
                        )
                    )
                }
                _history.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Error loading history", e)
            }
        }
    }

    private fun seedInitialBaseline() {
        val initialItems = listOf(
            TrainingItem(
                userInput = "Hi",
                category = TrainingCategories.GREETING,
                intent = "GREETING",
                goodResponses = listOf("Haan bhai, bol 😄", "Hello Sir! Kya instruction hai?"),
                badResponses = listOf("System initialized. How may I assist you?"),
                behaviorRules = listOf("Greetings should be natural, friendly, and non-robotic."),
                tone = TrainingTone.FRIENDLY,
                priority = 2
            ),
            TrainingItem(
                userInput = "Battery kitni hai?",
                category = TrainingCategories.BATTERY,
                intent = "DEVICE_INFORMATION",
                goodResponses = listOf("Ek sec, battery check karta hoon."),
                badResponses = listOf("Aapki battery 85% hai."),
                behaviorRules = listOf("Always invoke the Battery tool. Never guess battery percentage."),
                toolRequired = "Battery",
                tone = TrainingTone.HELPFUL,
                priority = 3
            ),
            TrainingItem(
                userInput = "Delete this file",
                category = TrainingCategories.FILE_OPERATION,
                intent = "DEVICE_ACTION",
                goodResponses = listOf("Please confirm: are you certain you want to permanently delete this file?"),
                badResponses = listOf("File deleted."),
                behaviorRules = listOf("Always request explicit user confirmation before destructive actions."),
                confirmationRequired = true,
                tone = TrainingTone.WARNING,
                priority = 4
            )
        )

        val initialRules = listOf(
            BehaviorRule(rule = "Be natural, helpful and articulate.", category = TrainingCategories.GREETING),
            BehaviorRule(rule = "Adopt Stark engineering precision without robotic stiffness.", category = TrainingCategories.CASUAL_CONVERSATION),
            BehaviorRule(rule = "Never execute destructive actions without confirmation.", category = TrainingCategories.SECURITY)
        )

        val bundle = TrainingBundle(
            version = 1,
            items = initialItems,
            rules = initialRules,
            appliedBy = "Baseline Seed"
        )
        _currentBundle.value = bundle
        saveToStorage(bundle)
    }

    private fun saveToStorage(bundle: TrainingBundle) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_BUNDLE, bundle.toJsonString())
            .putInt(KEY_CURRENT_VERSION, bundle.version)
            .apply()
    }

    private fun pushUndo() {
        if (_undoStack.size > 20) {
            _undoStack.removeAt(0)
        }
        _undoStack.add(_currentBundle.value)
    }

    fun canUndo(): Boolean = _undoStack.isNotEmpty()

    fun undo() {
        if (_undoStack.isNotEmpty()) {
            val prev = _undoStack.removeAt(_undoStack.size - 1)
            _currentBundle.value = prev
            saveToStorage(prev)
        }
    }

    fun addItem(item: TrainingItem) {
        pushUndo()
        val cur = _currentBundle.value
        val updated = cur.copy(items = cur.items + item)
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    fun updateItem(item: TrainingItem) {
        pushUndo()
        val cur = _currentBundle.value
        val updated = cur.copy(items = cur.items.map { if (it.id == item.id) item else it })
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    fun deleteItem(itemId: String) {
        pushUndo()
        val cur = _currentBundle.value
        val updated = cur.copy(items = cur.items.filterNot { it.id == itemId })
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    fun toggleItemEnabled(itemId: String) {
        val cur = _currentBundle.value
        val updated = cur.copy(items = cur.items.map {
            if (it.id == itemId) it.copy(enabled = !it.enabled) else it
        })
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    fun addRule(rule: BehaviorRule) {
        pushUndo()
        val cur = _currentBundle.value
        val updated = cur.copy(rules = cur.rules + rule)
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    fun updateRule(rule: BehaviorRule) {
        pushUndo()
        val cur = _currentBundle.value
        val updated = cur.copy(rules = cur.rules.map { if (it.id == rule.id) rule else it })
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    fun deleteRule(ruleId: String) {
        pushUndo()
        val cur = _currentBundle.value
        val updated = cur.copy(rules = cur.rules.filterNot { it.id == ruleId })
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    fun toggleRuleEnabled(ruleId: String) {
        val cur = _currentBundle.value
        val updated = cur.copy(rules = cur.rules.map {
            if (it.id == ruleId) it.copy(enabled = !it.enabled) else it
        })
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    fun addConversationTraining(conv: ConversationTraining) {
        pushUndo()
        val cur = _currentBundle.value
        val updated = cur.copy(conversationTrainings = cur.conversationTrainings + conv)
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    fun deleteConversationTraining(convId: String) {
        pushUndo()
        val cur = _currentBundle.value
        val updated = cur.copy(conversationTrainings = cur.conversationTrainings.filterNot { it.id == convId })
        _currentBundle.value = updated
        saveToStorage(updated)
    }

    /**
     * BATCH APPLY:
     * Applies all approved batch directives in one atomic operation.
     * Prevents duplicate rules, preserves existing training, records history, and delivers live sync.
     */
    fun applyBatch(
        directives: List<ParsedBatchDirective>,
        batchDescription: String = "Prompt Batch Training"
    ): BatchApplyResult {
        val approved = directives.filter { it.isApproved }
        val cur = _currentBundle.value

        val existingRules = cur.rules
        val existingItems = cur.items

        var rulesCount = 0
        var examplesCount = 0
        var personalityCount = 0
        var languageCount = 0
        var safetyCount = 0
        var toolCount = 0

        val newRulesToAdd = mutableListOf<BehaviorRule>()
        val newItemsToAdd = mutableListOf<TrainingItem>()

        val ackVer = try { TrainingEngine.getAcknowledgedVersion() } catch (e: Exception) { 1 }
        val targetVersion = maxOf(cur.version, ackVer) + 1

        for (directive in approved) {
            when (directive.type) {
                BatchTrainingItemType.PERSONALITY -> personalityCount++
                BatchTrainingItemType.LANGUAGE -> languageCount++
                BatchTrainingItemType.SAFETY -> safetyCount++
                BatchTrainingItemType.TOOL_ACTION -> toolCount++
                BatchTrainingItemType.RESPONSE_EXAMPLE -> examplesCount++
                BatchTrainingItemType.BEHAVIOR -> {}
            }

            // 1. Behavior Rule creation (check duplicates)
            val ruleText = directive.ruleText.ifBlank { directive.rawInstruction }
            if (directive.type != BatchTrainingItemType.RESPONSE_EXAMPLE &&
                !PromptBatchTrainingEngine.isDuplicateRule(existingRules + newRulesToAdd, ruleText)
            ) {
                newRulesToAdd.add(directive.toBehaviorRule(targetVersion))
                rulesCount++
            }

            // 2. Training Item creation (for triggers, tools, examples, confirmation)
            val trainingItem = directive.toTrainingItem(targetVersion)
            if (trainingItem != null) {
                val isDup = PromptBatchTrainingEngine.isDuplicateItem(existingItems + newItemsToAdd, trainingItem.userInput)
                if (!isDup) {
                    newItemsToAdd.add(trainingItem)
                    if (directive.type != BatchTrainingItemType.RESPONSE_EXAMPLE) {
                        examplesCount++
                    }
                }
            }
        }

        pushUndo()

        val updatedBundle = cur.copy(
            version = targetVersion,
            timestamp = System.currentTimeMillis(),
            rules = cur.rules + newRulesToAdd,
            items = cur.items + newItemsToAdd,
            appliedBy = "Prompt Batch Training"
        )

        _currentBundle.value = updatedBundle
        saveToStorage(updatedBundle)

        // Record history entry
        val historyDesc = "$batchDescription: +${newRulesToAdd.size} rules, +${newItemsToAdd.size} items"
        val entry = VersionHistoryEntry(
            version = targetVersion,
            timestamp = System.currentTimeMillis(),
            itemsCount = updatedBundle.items.size,
            rulesCount = updatedBundle.rules.size,
            description = historyDesc,
            bundleJson = updatedBundle.toJsonString()
        )
        val newHistory = (listOf(entry) + _history.value).take(50)
        _history.value = newHistory
        saveHistory(newHistory)

        // Live-sync to Main JARVIS App immediately
        val syncResult = ipcClient.saveAndApply(updatedBundle)
        val isSynced = syncResult.state == SyncState.SYNCED || isDirectlySyncedInSameProcess(updatedBundle)

        return BatchApplyResult(
            rulesCount = newRulesToAdd.size,
            examplesCount = newItemsToAdd.size,
            personalityCount = personalityCount,
            languageCount = languageCount,
            safetyCount = safetyCount,
            toolCount = toolCount,
            isSynced = isSynced,
            version = targetVersion,
            message = syncResult.message
        )
    }

    private fun isDirectlySyncedInSameProcess(bundle: TrainingBundle): Boolean {
        return try {
            TrainingEngine.activeBundle.value.version >= bundle.version
        } catch (_: Exception) {
            true
        }
    }

    /**
     * SAVE & APPLY:
     * Increments version, records history, persists, and delivers live sync via IPC!
     */
    fun saveAndApply(description: String = "Manual Trainer Apply"): SyncResult {
        val cur = _currentBundle.value
        val ackVer = try { TrainingEngine.getAcknowledgedVersion() } catch (e: Exception) { 1 }
        val newVersion = maxOf(cur.version, ackVer) + 1
        val updatedBundle = cur.copy(
            version = newVersion,
            timestamp = System.currentTimeMillis(),
            appliedBy = "JARVIS Trainer"
        )
        _currentBundle.value = updatedBundle
        saveToStorage(updatedBundle)

        // Record history entry
        val entry = VersionHistoryEntry(
            version = newVersion,
            timestamp = System.currentTimeMillis(),
            itemsCount = updatedBundle.items.size,
            rulesCount = updatedBundle.rules.size,
            description = description,
            bundleJson = updatedBundle.toJsonString()
        )
        val newHistory = (listOf(entry) + _history.value).take(50)
        _history.value = newHistory
        saveHistory(newHistory)

        // Deliver live sync to JARVIS!
        return ipcClient.saveAndApply(updatedBundle)
    }

    fun rollbackToVersion(historyEntry: VersionHistoryEntry): SyncResult {
        pushUndo()
        try {
            val bundle = TrainingBundle.fromJsonString(historyEntry.bundleJson)
            val newVersion = _currentBundle.value.version + 1
            val rolledBackBundle = bundle.copy(
                version = newVersion,
                timestamp = System.currentTimeMillis(),
                appliedBy = "Rollback to v${historyEntry.version}"
            )
            _currentBundle.value = rolledBackBundle
            saveToStorage(rolledBackBundle)
            return ipcClient.saveAndApply(rolledBackBundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error rolling back", e)
            return SyncResult(SyncState.FAILED, _currentBundle.value.version, "Rollback failed: ${e.message}")
        }
    }

    private fun saveHistory(historyList: List<VersionHistoryEntry>) {
        val arr = JSONArray()
        historyList.forEach {
            val obj = JSONObject()
            obj.put("version", it.version)
            obj.put("timestamp", it.timestamp)
            obj.put("itemsCount", it.itemsCount)
            obj.put("rulesCount", it.rulesCount)
            obj.put("description", it.description)
            obj.put("bundleJson", it.bundleJson)
            arr.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun exportToJson(): String {
        return _currentBundle.value.toJsonString()
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val imported = TrainingBundle.fromJsonString(jsonString)
            val validated = TrainingValidation.validateBundle(imported)
            if (!validated.isValid) return false
            pushUndo()
            _currentBundle.value = imported
            saveToStorage(imported)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import error", e)
            false
        }
    }
}
