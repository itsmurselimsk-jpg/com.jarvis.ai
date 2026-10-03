package com.example.trainer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.training.BatchAnalysisSummary
import com.example.jarvis.training.BatchApplyResult
import com.example.jarvis.training.BatchTrainingItemType
import com.example.jarvis.training.BehaviorRule
import com.example.jarvis.training.ConversationTraining
import com.example.jarvis.training.ParsedBatchDirective
import com.example.jarvis.training.PromptBatchTrainingEngine
import com.example.jarvis.training.SyncResult
import com.example.jarvis.training.SyncState
import com.example.jarvis.training.TrainerRepository
import com.example.jarvis.training.TrainingBundle
import com.example.jarvis.training.TrainingCategories
import com.example.jarvis.training.TrainingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TrainerTab(val title: String) {
    BATCH_PROMPT("Batch Train"),
    BEHAVIORS("Behaviors"),
    RULES("Rules"),
    MULTI_TURN("Multi-Turn"),
    HISTORY("History"),
    SYNC_DATA("Sync & Data")
}

class TrainerViewModel(application: Application) : AndroidViewModel(application) {

    val repository = TrainerRepository(application)

    private val _selectedTab = MutableStateFlow(TrainerTab.BATCH_PROMPT)
    val selectedTab: StateFlow<TrainerTab> = _selectedTab.asStateFlow()

    // BATCH PROMPT TRAINING STATE
    private val _batchPromptText = MutableStateFlow(PromptBatchTrainingEngine.DEFAULT_TEMPLATE)
    val batchPromptText: StateFlow<String> = _batchPromptText.asStateFlow()

    private val _isAnalyzingPrompt = MutableStateFlow(false)
    val isAnalyzingPrompt: StateFlow<Boolean> = _isAnalyzingPrompt.asStateFlow()

    private val _isReviewMode = MutableStateFlow(false)
    val isReviewMode: StateFlow<Boolean> = _isReviewMode.asStateFlow()

    private val _parsedDirectives = MutableStateFlow<List<ParsedBatchDirective>>(emptyList())
    val parsedDirectives: StateFlow<List<ParsedBatchDirective>> = _parsedDirectives.asStateFlow()

    private val _batchSummary = MutableStateFlow(BatchAnalysisSummary())
    val batchSummary: StateFlow<BatchAnalysisSummary> = _batchSummary.asStateFlow()

    private val _batchApplyResult = MutableStateFlow<BatchApplyResult?>(null)
    val batchApplyResult: StateFlow<BatchApplyResult?> = _batchApplyResult.asStateFlow()

    private val _reviewCategoryFilter = MutableStateFlow("All")
    val reviewCategoryFilter: StateFlow<String> = _reviewCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val currentBundle = repository.currentBundle
    val history = repository.history

    val isConnected = repository.ipcClient.isConnected
    val acknowledgedVersion = repository.ipcClient.acknowledgedVersion
    val syncState = repository.ipcClient.syncState

    private val _syncMessage = MutableStateFlow<String?>("Ready to train JARVIS")
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    // Filtered items list
    val filteredItems: StateFlow<List<TrainingItem>> = combine(
        currentBundle,
        _searchQuery,
        _selectedCategory
    ) { bundle, query, category ->
        bundle.items.filter { item ->
            val matchesCategory = (category == "All") || item.category.equals(category, ignoreCase = true)
            val matchesQuery = if (query.isBlank()) true else {
                item.userInput.contains(query, ignoreCase = true) ||
                        item.goodResponses.any { it.contains(query, ignoreCase = true) } ||
                        item.badResponses.any { it.contains(query, ignoreCase = true) } ||
                        item.behaviorRules.any { it.contains(query, ignoreCase = true) } ||
                        item.category.contains(query, ignoreCase = true)
            }
            matchesCategory && matchesQuery
        }.sortedByDescending { it.priority }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: TrainerTab) {
        _selectedTab.value = tab
    }

    // --- BATCH PROMPT TRAINING METHODS ---

    fun setBatchPromptText(text: String) {
        _batchPromptText.value = text
    }

    fun loadBatchTemplate(template: String) {
        _batchPromptText.value = template
        _isReviewMode.value = false
    }

    fun analyzeBatchPrompt() {
        viewModelScope.launch {
            _isAnalyzingPrompt.value = true
            _syncMessage.value = "Analyzing prompt & structuring directives..."
            kotlinx.coroutines.delay(150)
            val directives = PromptBatchTrainingEngine.parseTrainingPrompt(_batchPromptText.value)
            _parsedDirectives.value = directives
            _batchSummary.value = PromptBatchTrainingEngine.summarize(directives)
            _isAnalyzingPrompt.value = false
            _isReviewMode.value = true
            _syncMessage.value = "Analyzed ${directives.size} directives. Ready for review."
        }
    }

    fun exitReviewMode() {
        _isReviewMode.value = false
    }

    fun setReviewCategoryFilter(category: String) {
        _reviewCategoryFilter.value = category
    }

    fun toggleDirectiveApproval(id: String) {
        val updated = _parsedDirectives.value.map {
            if (it.id == id) it.copy(isApproved = !it.isApproved) else it
        }
        _parsedDirectives.value = updated
        _batchSummary.value = PromptBatchTrainingEngine.summarize(updated)
    }

    fun updateDirective(directive: ParsedBatchDirective) {
        val updated = _parsedDirectives.value.map {
            if (it.id == directive.id) directive else it
        }
        _parsedDirectives.value = updated
        _batchSummary.value = PromptBatchTrainingEngine.summarize(updated)
    }

    fun deleteDirective(id: String) {
        val updated = _parsedDirectives.value.filterNot { it.id == id }
        _parsedDirectives.value = updated
        _batchSummary.value = PromptBatchTrainingEngine.summarize(updated)
    }

    fun addCustomDirective(directive: ParsedBatchDirective) {
        val updated = _parsedDirectives.value + directive
        _parsedDirectives.value = updated
        _batchSummary.value = PromptBatchTrainingEngine.summarize(updated)
    }

    fun clearBatchPrompt() {
        _batchPromptText.value = ""
        _isReviewMode.value = false
        _parsedDirectives.value = emptyList()
    }

    fun applyPromptDirectly() {
        val text = _batchPromptText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _isApplying.value = true
            _syncMessage.value = "Compiling & applying training directly to JARVIS..."
            var directives = PromptBatchTrainingEngine.parseTrainingPrompt(text)
            if (directives.isEmpty()) {
                // If unstructured text, wrap as a behavioral training directive
                directives = listOf(
                    ParsedBatchDirective(
                        rawInstruction = text,
                        type = BatchTrainingItemType.BEHAVIOR,
                        title = "Custom User Directive",
                        ruleText = text,
                        goodResponseExample = text
                    )
                )
            }
            val result = repository.applyBatch(
                directives = directives,
                batchDescription = "Direct Batch Training"
            )
            _isApplying.value = false
            _batchPromptText.value = "" // Cleared immediately so user can type the next prompt!
            _parsedDirectives.value = emptyList()
            _isReviewMode.value = false
            _batchApplyResult.value = result
            _syncMessage.value = "✓ Training applied to JARVIS! (v${result.version} • Rules: ${result.rulesCount} • Examples: ${result.examplesCount}). Prompt cleared."
        }
    }

    fun applyBatchToJarvis() {
        viewModelScope.launch {
            _isApplying.value = true
            _syncMessage.value = "Applying training batch and live-syncing to JARVIS..."
            val result = repository.applyBatch(
                directives = _parsedDirectives.value,
                batchDescription = "Prompt Batch Training"
            )
            _isApplying.value = false
            _batchApplyResult.value = result
            _batchPromptText.value = "" // Auto-clear prompt so user can enter the next one
            _syncMessage.value = "Batch Training applied! v${result.version} (Rules: ${result.rulesCount}, Examples: ${result.examplesCount})"
        }
    }

    fun dismissApplyResult() {
        _batchApplyResult.value = null
        _isReviewMode.value = false
        _batchPromptText.value = "" // Clear prompt box ready for next input
        _parsedDirectives.value = emptyList()
    }

    // --- END BATCH METHODS ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun saveAndApply() {
        viewModelScope.launch {
            _isApplying.value = true
            _syncMessage.value = "Synchronizing training to JARVIS..."
            val result = repository.saveAndApply("Applied via Trainer UI")
            _isApplying.value = false
            _syncMessage.value = result.message
        }
    }

    fun addItem(item: TrainingItem) {
        repository.addItem(item)
    }

    fun updateItem(item: TrainingItem) {
        repository.updateItem(item)
    }

    fun deleteItem(itemId: String) {
        repository.deleteItem(itemId)
    }

    fun toggleItemEnabled(itemId: String) {
        repository.toggleItemEnabled(itemId)
    }

    fun addRule(rule: BehaviorRule) {
        repository.addRule(rule)
    }

    fun updateRule(rule: BehaviorRule) {
        repository.updateRule(rule)
    }

    fun deleteRule(ruleId: String) {
        repository.deleteRule(ruleId)
    }

    fun toggleRuleEnabled(ruleId: String) {
        repository.toggleRuleEnabled(ruleId)
    }

    fun addConversation(conv: ConversationTraining) {
        repository.addConversationTraining(conv)
    }

    fun deleteConversation(convId: String) {
        repository.deleteConversationTraining(convId)
    }

    fun rollbackToVersion(version: Int) {
        val entry = history.value.firstOrNull { it.version == version }
        if (entry != null) {
            viewModelScope.launch {
                _isApplying.value = true
                val result = repository.rollbackToVersion(entry)
                _isApplying.value = false
                _syncMessage.value = "Rolled back to v$version: ${result.message}"
            }
        }
    }

    fun undo() {
        repository.undo()
        _syncMessage.value = "Action undone"
    }

    fun canUndo(): Boolean = repository.canUndo()

    fun pingJarvis() {
        val reachable = repository.ipcClient.checkConnectionViaProvider()
        _syncMessage.value = if (reachable) "JARVIS acknowledged ping! Link active 🟢" else "JARVIS is currently offline ⚪"
    }

    fun exportJson(): String = repository.exportToJson()

    fun importJson(json: String): Boolean {
        val success = repository.importFromJson(json)
        _syncMessage.value = if (success) "Training data imported successfully" else "Import failed: invalid JSON format"
        return success
    }

    override fun onCleared() {
        super.onCleared()
        repository.ipcClient.stopServiceBinding()
    }
}
