package com.example.trainer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.training.BehaviorRule
import com.example.jarvis.training.ConversationTraining
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
    BEHAVIORS("Behaviors"),
    MULTI_TURN("Multi-Turn"),
    RULES("Rules"),
    HISTORY("History"),
    SYNC_DATA("Sync & Data")
}

class TrainerViewModel(application: Application) : AndroidViewModel(application) {

    val repository = TrainerRepository(application)

    private val _selectedTab = MutableStateFlow(TrainerTab.BEHAVIORS)
    val selectedTab: StateFlow<TrainerTab> = _selectedTab.asStateFlow()

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
