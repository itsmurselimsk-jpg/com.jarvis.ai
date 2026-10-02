package com.example.trainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jarvis.training.TrainingItem
import com.example.trainer.TrainerTab
import com.example.trainer.TrainerViewModel

@Composable
fun JarvisTrainerApp(
    viewModel: TrainerViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val currentBundle by viewModel.currentBundle.collectAsState()
    val acknowledgedVersion by viewModel.acknowledgedVersion.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val isApplying by viewModel.isApplying.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val history by viewModel.history.collectAsState()

    // Batch Prompt State
    val batchPromptText by viewModel.batchPromptText.collectAsState()
    val isAnalyzingPrompt by viewModel.isAnalyzingPrompt.collectAsState()
    val isReviewMode by viewModel.isReviewMode.collectAsState()
    val parsedDirectives by viewModel.parsedDirectives.collectAsState()
    val batchSummary by viewModel.batchSummary.collectAsState()
    val batchApplyResult by viewModel.batchApplyResult.collectAsState()
    val reviewCategoryFilter by viewModel.reviewCategoryFilter.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<TrainingItem?>(null) }

    JarvisTrainerTheme {
        Scaffold(
            topBar = {
                TrainerTopBar(
                    isConnected = isConnected,
                    trainerVersion = currentBundle.version,
                    jarvisVersion = acknowledgedVersion,
                    syncState = syncState,
                    syncMessage = syncMessage,
                    isApplying = isApplying,
                    canUndo = viewModel.canUndo(),
                    onSaveAndApply = { viewModel.saveAndApply() },
                    onUndo = { viewModel.undo() }
                )
            },
            containerColor = TrainerBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Navigation Strip
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = TrainerSurface,
                    contentColor = TrainerCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = TrainerCyan
                        )
                    }
                ) {
                    TrainerTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == tab) TrainerCyan else TrainerTextSecondary
                                )
                            },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }

                // Tab Content
                when (selectedTab) {
                    TrainerTab.BATCH_PROMPT -> {
                        BatchPromptTab(
                            promptText = batchPromptText,
                            isAnalyzing = isAnalyzingPrompt,
                            isReviewMode = isReviewMode,
                            directives = parsedDirectives,
                            summary = batchSummary,
                            applyResult = batchApplyResult,
                            categoryFilter = reviewCategoryFilter,
                            onPromptChange = { viewModel.setBatchPromptText(it) },
                            onLoadTemplate = { viewModel.loadBatchTemplate(it) },
                            onAnalyzePrompt = { viewModel.analyzeBatchPrompt() },
                            onExitReview = { viewModel.exitReviewMode() },
                            onCategoryFilterChange = { viewModel.setReviewCategoryFilter(it) },
                            onToggleApproval = { viewModel.toggleDirectiveApproval(it) },
                            onUpdateDirective = { viewModel.updateDirective(it) },
                            onDeleteDirective = { viewModel.deleteDirective(it) },
                            onApplyBatch = { viewModel.applyBatchToJarvis() },
                            onDismissApplyResult = { viewModel.dismissApplyResult() },
                            onViewHistory = { viewModel.setTab(TrainerTab.HISTORY) }
                        )
                    }

                    TrainerTab.BEHAVIORS -> {
                        BehaviorsTab(
                            items = filteredItems,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onCategoryChange = { viewModel.setSelectedCategory(it) },
                            onAddItem = {
                                itemToEdit = null
                                showAddEditDialog = true
                            },
                            onEditItem = { item ->
                                itemToEdit = item
                                showAddEditDialog = true
                            },
                            onDeleteItem = { viewModel.deleteItem(it) },
                            onToggleItem = { viewModel.toggleItemEnabled(it) }
                        )
                    }

                    TrainerTab.MULTI_TURN -> {
                        MultiTurnTab(
                            trainings = currentBundle.conversationTrainings,
                            onAddConversation = { viewModel.addConversation(it) },
                            onDeleteConversation = { viewModel.deleteConversation(it) }
                        )
                    }

                    TrainerTab.RULES -> {
                        RulesTab(
                            rules = currentBundle.rules,
                            onAddRule = { viewModel.addRule(it) },
                            onDeleteRule = { viewModel.deleteRule(it) },
                            onToggleRule = { viewModel.toggleRuleEnabled(it) }
                        )
                    }

                    TrainerTab.HISTORY -> {
                        HistoryTab(
                            history = history,
                            currentVersion = currentBundle.version,
                            onRollback = { viewModel.rollbackToVersion(it) }
                        )
                    }

                    TrainerTab.SYNC_DATA -> {
                        SyncDataTab(
                            isConnected = isConnected,
                            onPingJarvis = { viewModel.pingJarvis() },
                            onExportJson = { viewModel.exportJson() },
                            onImportJson = { viewModel.importJson(it) }
                        )
                    }
                }
            }
        }

        if (showAddEditDialog) {
            AddEditTrainingItemDialog(
                itemToEdit = itemToEdit,
                onDismiss = { showAddEditDialog = false },
                onSave = { savedItem ->
                    if (itemToEdit == null) {
                        viewModel.addItem(savedItem)
                    } else {
                        viewModel.updateItem(savedItem)
                    }
                    showAddEditDialog = false
                }
            )
        }
    }
}
