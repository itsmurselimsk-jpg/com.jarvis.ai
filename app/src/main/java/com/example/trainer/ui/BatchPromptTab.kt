package com.example.trainer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.training.BatchAnalysisSummary
import com.example.jarvis.training.BatchApplyResult
import com.example.jarvis.training.BatchTrainingItemType
import com.example.jarvis.training.ParsedBatchDirective
import com.example.jarvis.training.PromptBatchTrainingEngine

@Composable
fun BatchPromptTab(
    promptText: String,
    isAnalyzing: Boolean,
    isReviewMode: Boolean,
    directives: List<ParsedBatchDirective>,
    summary: BatchAnalysisSummary,
    applyResult: BatchApplyResult?,
    categoryFilter: String,
    onPromptChange: (String) -> Unit,
    onLoadTemplate: (String) -> Unit,
    onAnalyzePrompt: () -> Unit,
    onApplyDirectly: () -> Unit = {},
    onExitReview: () -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onToggleApproval: (String) -> Unit,
    onUpdateDirective: (ParsedBatchDirective) -> Unit,
    onDeleteDirective: (String) -> Unit,
    onApplyBatch: () -> Unit,
    onDismissApplyResult: () -> Unit,
    onViewHistory: () -> Unit
) {
    var directiveToEdit by remember { mutableStateOf<ParsedBatchDirective?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isReviewMode) {
            // STEP 1: PROMPT INPUT EDITOR
            PromptEditorView(
                promptText = promptText,
                isAnalyzing = isAnalyzing,
                onPromptChange = onPromptChange,
                onLoadTemplate = onLoadTemplate,
                onAnalyze = onAnalyzePrompt,
                onApplyDirectly = onApplyDirectly
            )
        } else {
            // STEP 2: REVIEW DETECTED ITEMS BEFORE APPLYING
            BatchReviewView(
                directives = directives,
                summary = summary,
                categoryFilter = categoryFilter,
                onExitReview = onExitReview,
                onCategoryFilterChange = onCategoryFilterChange,
                onToggleApproval = onToggleApproval,
                onEditDirective = { directiveToEdit = it },
                onDeleteDirective = onDeleteDirective,
                onApplyBatch = onApplyBatch
            )
        }

        // Edit Individual Directive Dialog
        directiveToEdit?.let { directive ->
            EditDirectiveDialog(
                directive = directive,
                onDismiss = { directiveToEdit = null },
                onSave = { updated ->
                    onUpdateDirective(updated)
                    directiveToEdit = null
                }
            )
        }

        // STEP 3: BATCH APPLIED RESULT DIALOG (Exact specifications from user)
        applyResult?.let { result ->
            BatchAppliedResultDialog(
                result = result,
                onDismiss = onDismissApplyResult,
                onViewHistory = {
                    onDismissApplyResult()
                    onViewHistory()
                }
            )
        }
    }
}

@Composable
private fun PromptEditorView(
    promptText: String,
    isAnalyzing: Boolean,
    onPromptChange: (String) -> Unit,
    onLoadTemplate: (String) -> Unit,
    onAnalyze: () -> Unit,
    onApplyDirectly: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Architecture & Transparency Banner (Requirement 14)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TrainerSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = TrainerCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "JARVIS BEHAVIORAL BATCH TRAINING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TrainerCyan
                    )
                }
                Text(
                    text = "Structured Behavioral Steering & Few-Shot Constraint Engine\n" +
                            "Enter instructions in plain English or Bangla. The engine compiles your prompt into structured rules, tool triggers, and runtime safety constraints.",
                    fontSize = 11.sp,
                    color = TrainerTextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // Template Quick Fill Bar
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "QUICK TEMPLATES:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TrainerTextSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemplateChip("Complete Assistant") {
                    onLoadTemplate(PromptBatchTrainingEngine.DEFAULT_TEMPLATE)
                }
                TemplateChip("Bengali & English") {
                    onLoadTemplate(PromptBatchTrainingEngine.BILINGUAL_BANGLA_TEMPLATE)
                }
                TemplateChip("Tony Stark Persona") {
                    onLoadTemplate(PromptBatchTrainingEngine.STARK_PERSONALITY_TEMPLATE)
                }
                TemplateChip("Strict Safety") {
                    onLoadTemplate(PromptBatchTrainingEngine.STRICT_SAFETY_TEMPLATE)
                }
                TemplateChip("Clear Box", isAccent = false) {
                    onLoadTemplate("")
                }
            }
        }

        // Large Training Prompt Editor (Requirement 1 & 2)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LARGE TRAINING PROMPT:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TrainerTextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${promptText.lines().count { it.isNotBlank() }} directives  •  ${promptText.length} chars",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TrainerTextDim
                    )
                    if (promptText.isNotBlank()) {
                        Surface(
                            onClick = { onPromptChange("") },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2E1A1A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerRed.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("btn_clear_prompt")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TrainerRed, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CLEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TrainerRed)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = promptText,
                onValueChange = onPromptChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 520.dp)
                    .testTag("batch_prompt_input"),
                minLines = 10,
                maxLines = 50,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrainerCyan,
                    unfocusedBorderColor = TrainerBorder,
                    focusedContainerColor = TrainerBackground,
                    unfocusedContainerColor = TrainerBackground,
                    focusedTextColor = TrainerTextPrimary,
                    unfocusedTextColor = TrainerTextPrimary
                ),
                placeholder = {
                    Text(
                        text = "Enter your training instructions here (large batch prompts supported)...\n\nExamples:\n" +
                                "• Mujhe Boss bolo (address me as Boss)\n" +
                                "• Bhai mat bolo\n" +
                                "• When I say 'Hi', reply: 'Hello Boss! Sab ready hai.'\n" +
                                "• Jab main puchu 'battery', give the real battery percentage\n" +
                                "• If I speak Bangla, reply in pure Bengali\n" +
                                "• Destructive actions always require confirmation\n" +
                                "• Keep responses concise, direct and polite",
                        fontSize = 12.sp,
                        color = TrainerTextDim,
                        lineHeight = 18.sp
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            )
        }

        // Direct Apply Button (1-Click Apply & Auto-Clear Prompt)
        Button(
            onClick = onApplyDirectly,
            enabled = promptText.isNotBlank() && !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("apply_directly_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrainerGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("APPLYING TO JARVIS...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("APPLY DIRECTLY TO JARVIS (AUTO-CLEAR)", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Analyze & Review First Action Button
        OutlinedButton(
            onClick = onAnalyze,
            enabled = promptText.isNotBlank() && !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("analyze_training_button"),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TrainerCyan
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerCyan.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("ANALYZE & REVIEW FIRST", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun TemplateChip(label: String, isAccent: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isAccent) Color(0xFF10283E) else TrainerSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAccent) TrainerCyan.copy(alpha = 0.5f) else TrainerBorder)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isAccent) TrainerCyan else TrainerTextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchReviewView(
    directives: List<ParsedBatchDirective>,
    summary: BatchAnalysisSummary,
    categoryFilter: String,
    onExitReview: () -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onToggleApproval: (String) -> Unit,
    onEditDirective: (ParsedBatchDirective) -> Unit,
    onDeleteDirective: (String) -> Unit,
    onApplyBatch: () -> Unit
) {
    val filteredList = remember(directives, categoryFilter) {
        if (categoryFilter == "All") directives else {
            directives.filter { it.type.name.equals(categoryFilter, ignoreCase = true) }
        }
    }

    val approvedCount = directives.count { it.isApproved }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Navigation Bar inside Review
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onExitReview() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TrainerCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Prompt", fontSize = 12.sp, color = TrainerCyan, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "$approvedCount of ${directives.size} Approved",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TrainerGreen
            )
        }

        // Summary Metric Badges (Requirement 5)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TrainerSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "STRUCTURED DIRECTIVES BREAKDOWN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TrainerTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricBadge("Rules", summary.rulesCount, TrainerCyan)
                    MetricBadge("Behaviors", summary.totalDirectives - summary.personalityCount - summary.languageCount - summary.safetyCount - summary.toolCount, TrainerCyan)
                    MetricBadge("Personality", summary.personalityCount, Color(0xFFBD93F9))
                    MetricBadge("Language", summary.languageCount, TrainerGreen)
                    MetricBadge("Safety", summary.safetyCount, TrainerAmber)
                    MetricBadge("Tools", summary.toolCount, Color(0xFFFFB86C))
                    MetricBadge("Examples", summary.examplesCount, Color(0xFF8BE9FD))
                }
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                Pair("All", "All (${directives.size})"),
                Pair("BEHAVIOR", "Behaviors"),
                Pair("PERSONALITY", "Personality (${summary.personalityCount})"),
                Pair("LANGUAGE", "Language (${summary.languageCount})"),
                Pair("SAFETY", "Safety (${summary.safetyCount})"),
                Pair("TOOL_ACTION", "Tools (${summary.toolCount})"),
                Pair("RESPONSE_EXAMPLE", "Examples (${summary.examplesCount})")
            )
            filters.forEach { (key, label) ->
                val isSelected = categoryFilter.equals(key, ignoreCase = true)
                Surface(
                    onClick = { onCategoryFilterChange(key) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) TrainerCyan else TrainerSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) TrainerCyan else TrainerBorder)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else TrainerTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Directives Review List (Requirement 5 & 6)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.id }) { directive ->
                DirectiveReviewCard(
                    directive = directive,
                    onToggleApproval = { onToggleApproval(directive.id) },
                    onEdit = { onEditDirective(directive) },
                    onDelete = { onDeleteDirective(directive.id) }
                )
            }
        }

        // Sticky "Apply to JARVIS" Action Button (Requirement 7 & 8)
        Button(
            onClick = onApplyBatch,
            enabled = approvedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_to_jarvis_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrainerGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RocketLaunch,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "APPLY TO JARVIS ($approvedCount APPROVED)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MetricBadge(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, fontSize = 10.sp, color = color)
            Text(text = count.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun DirectiveReviewCard(
    directive: ParsedBatchDirective,
    onToggleApproval: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (!directive.isApproved) TrainerBorderSubtle else when (directive.type) {
        BatchTrainingItemType.SAFETY -> TrainerAmber
        BatchTrainingItemType.LANGUAGE -> TrainerGreen
        BatchTrainingItemType.PERSONALITY -> Color(0xFFBD93F9)
        BatchTrainingItemType.TOOL_ACTION -> Color(0xFFFFB86C)
        else -> TrainerCyan
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("directive_card_${directive.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (directive.isApproved) TrainerSurface else TrainerBackground
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type & Category badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeBadge(directive.type)
                    Text(
                        text = directive.category,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TrainerTextDim
                    )
                }

                // Switch and action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp).testTag("edit_directive_${directive.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TrainerCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).testTag("delete_directive_${directive.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5555),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Switch(
                        checked = directive.isApproved,
                        onCheckedChange = { onToggleApproval() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = TrainerGreen,
                            uncheckedThumbColor = TrainerTextDim,
                            uncheckedTrackColor = TrainerSurface
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Rule / Instruction text
            Text(
                text = directive.ruleText.ifBlank { directive.rawInstruction },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (directive.isApproved) TrainerTextPrimary else TrainerTextDim
            )

            // Optional tags: Tool, Confirmation, Example
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                directive.toolRequired?.let { tool ->
                    TagChip(text = "Tool: $tool", color = Color(0xFFFFB86C))
                }
                if (directive.requiresConfirmation) {
                    TagChip(text = "Requires Confirmation", color = TrainerAmber)
                }
                if (!directive.userInputExample.isNullOrBlank()) {
                    TagChip(text = "Trigger: \"${directive.userInputExample}\"", color = Color(0xFF8BE9FD))
                }
            }

            if (!directive.goodResponseExample.isNullOrBlank()) {
                Text(
                    text = "Response: ${directive.goodResponseExample}",
                    fontSize = 11.sp,
                    color = TrainerTextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(type: BatchTrainingItemType) {
    val (label, color) = when (type) {
        BatchTrainingItemType.SAFETY -> Pair("SAFETY", TrainerAmber)
        BatchTrainingItemType.LANGUAGE -> Pair("LANGUAGE", TrainerGreen)
        BatchTrainingItemType.PERSONALITY -> Pair("PERSONALITY", Color(0xFFBD93F9))
        BatchTrainingItemType.TOOL_ACTION -> Pair("TOOL", Color(0xFFFFB86C))
        BatchTrainingItemType.RESPONSE_EXAMPLE -> Pair("EXAMPLE", Color(0xFF8BE9FD))
        BatchTrainingItemType.BEHAVIOR -> Pair("BEHAVIOR", TrainerCyan)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TagChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF0F1A28)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EditDirectiveDialog(
    directive: ParsedBatchDirective,
    onDismiss: () -> Unit,
    onSave: (ParsedBatchDirective) -> Unit
) {
    var editedRule by remember { mutableStateOf(directive.ruleText.ifBlank { directive.rawInstruction }) }
    var editedResponse by remember { mutableStateOf(directive.goodResponseExample ?: "") }
    var editedConfirmation by remember { mutableStateOf(directive.requiresConfirmation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Directive",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TrainerCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editedRule,
                    onValueChange = { editedRule = it },
                    label = { Text("Rule / Instruction", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrainerCyan,
                        unfocusedBorderColor = TrainerBorder
                    )
                )

                OutlinedTextField(
                    value = editedResponse,
                    onValueChange = { editedResponse = it },
                    label = { Text("Example Response (optional)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrainerCyan,
                        unfocusedBorderColor = TrainerBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Require User Confirmation", fontSize = 12.sp, color = TrainerTextPrimary)
                    Switch(
                        checked = editedConfirmation,
                        onCheckedChange = { editedConfirmation = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        directive.copy(
                            ruleText = editedRule,
                            goodResponseExample = editedResponse.ifBlank { null },
                            requiresConfirmation = editedConfirmation
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrainerCyan, contentColor = Color.Black)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TrainerTextDim)
            }
        },
        containerColor = TrainerSurface
    )
}

/**
 * EXACT RESULT DIALOG (Requirement 11)
 *
 * Training Batch Applied
 * Rules: 42
 * Examples: 18
 * Personality: 7
 * Language: 5
 * Safety: 4
 * Synced: YES
 */
@Composable
fun BatchAppliedResultDialog(
    result: BatchApplyResult,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TrainerGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Training Batch Applied",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TrainerGreen,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TrainerBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TrainerBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResultRow("Rules:", result.rulesCount.toString())
                        ResultRow("Examples:", result.examplesCount.toString())
                        ResultRow("Personality:", result.personalityCount.toString())
                        ResultRow("Language:", result.languageCount.toString())
                        ResultRow("Safety:", result.safetyCount.toString())
                        if (result.toolCount > 0) {
                            ResultRow("Tools / Actions:", result.toolCount.toString())
                        }
                        HorizontalDivider(color = TrainerBorderSubtle, thickness = 1.dp)
                        ResultRow(
                            label = "Synced:",
                            value = if (result.isSynced) "YES" else "PENDING",
                            valueColor = if (result.isSynced) TrainerGreen else TrainerAmber
                        )
                        ResultRow(
                            label = "Version:",
                            value = "v${result.version}",
                            valueColor = TrainerCyan
                        )
                    }
                }

                Text(
                    text = "All approved directives have been compiled into JARVIS's persistent memory and synced across IPC. Main JARVIS is using this training immediately.",
                    fontSize = 10.sp,
                    color = TrainerTextSecondary,
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TrainerCyan, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DONE / ADD ANOTHER PROMPT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("VIEW TRAINING HISTORY", color = TrainerCyan, fontSize = 11.sp)
            }
        },
        containerColor = TrainerSurface
    )
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    valueColor: Color = TrainerTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = TrainerTextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}
