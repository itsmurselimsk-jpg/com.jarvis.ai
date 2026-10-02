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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.jarvis.training.BehaviorRule
import com.example.jarvis.training.ConversationTraining
import com.example.jarvis.training.ConversationTurn
import com.example.jarvis.training.TrainingCategories
import com.example.jarvis.training.TrainingItem
import com.example.jarvis.training.VersionHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BehaviorsTab(
    items: List<TrainingItem>,
    searchQuery: String,
    selectedCategory: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (TrainingItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onToggleItem: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by pattern, response, rule, category...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TrainerCyan) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrainerCyan,
                    unfocusedBorderColor = TrainerCardBorder,
                    focusedTextColor = TrainerTextPrimary,
                    unfocusedTextColor = TrainerTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_training")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            val categoryChips = listOf("All") + TrainingCategories.ALL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("row_category_filters"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categoryChips.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) TrainerCyan else TrainerCardSurface)
                            .border(1.dp, if (isSelected) TrainerCyan else TrainerCardBorder, RoundedCornerShape(16.dp))
                            .clickable { onCategoryChange(cat) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else TrainerTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TRAINED BEHAVIOR PATTERNS (${items.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TrainerTextSecondary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No training patterns match current filters.", color = TrainerTextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        TrainingItemCard(
                            item = item,
                            onEdit = { onEditItem(item) },
                            onDelete = { onDeleteItem(item.id) },
                            onToggle = { onToggleItem(item.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Pattern
        FloatingActionButton(
            onClick = onAddItem,
            containerColor = TrainerCyan,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_training")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Training Pattern")
        }
    }
}

@Composable
fun TrainingItemCard(
    item: TrainingItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_training_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.enabled) TrainerCardSurface else TrainerCardSurface.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (item.enabled) TrainerCardBorder else Color.DarkGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Category badge, tone badge, Priority, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TrainerCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = item.category.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TrainerCyan)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TrainerElectricBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = item.tone.displayName, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TrainerElectricBlue)
                    }

                    if (item.priority > 2) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TrainerAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "P${item.priority}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TrainerAmber)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = item.enabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = TrainerCyan, checkedTrackColor = TrainerCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("switch_item_enabled_${item.id}")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = TrainerCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Trigger Input
            Text(
                text = "USER: \"${item.userInput}\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TrainerTextPrimary
            )

            // Tool & Confirmation tags if specified
            if (item.toolRequired != null || item.confirmationRequired) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.toolRequired != null) {
                        Text(
                            text = "⚡ Requires Tool: ${item.toolRequired}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TrainerCyan
                        )
                    }
                    if (item.confirmationRequired) {
                        Text(
                            text = "🛡️ Requires User Authorization",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TrainerAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Preferred positive responses
            if (item.goodResponses.isNotEmpty()) {
                item.goodResponses.take(2).forEach { resp ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TrainerGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = resp, color = TrainerTextPrimary, fontSize = 12.sp)
                    }
                }
            }

            // Prohibited bad responses
            if (item.badResponses.isNotEmpty()) {
                item.badResponses.take(1).forEach { resp ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TrainerRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = resp, color = TrainerTextSecondary, fontSize = 11.sp)
                    }
                }
            }

            // Behavior rules summary
            if (item.behaviorRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                item.behaviorRules.take(1).forEach { rule ->
                    Text(
                        text = "Rule: $rule",
                        color = TrainerCyan,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun MultiTurnTab(
    trainings: List<ConversationTraining>,
    onAddConversation: (ConversationTraining) -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MULTI-TURN CONVERSATIONS",
                        color = TrainerTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Teach complete conversational context & transitions",
                        color = TrainerTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_new_multiturn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD DIALOGUE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (trainings.isEmpty()) {
                // Show default seed multi-turn example
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TrainerCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TrainerCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Example Multi-Turn: Natural Daily Banter → Battery", color = TrainerCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        DialogueBubble("User", "Hi")
                        DialogueBubble("JARVIS", "Haan bhai, bol 😄")
                        DialogueBubble("User", "Kya kar raha hai?")
                        DialogueBubble("JARVIS", "Bas yahin hoon 😄 Bol, kya scene hai?")
                        DialogueBubble("User", "Battery kitni hai?")
                        DialogueBubble("JARVIS", "Ek sec, battery check karta hoon. [Uses Battery Tool]")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Metadata: CASUAL_CONVERSATION → DEVICE_INFORMATION | Tool: Battery | Confirmation: No", fontSize = 10.sp, color = TrainerTextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trainings, key = { it.id }) { conv ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = TrainerCardSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = conv.title, color = TrainerCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    IconButton(onClick = { onDeleteConversation(conv.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                conv.turns.forEach { turn ->
                                    DialogueBubble(turn.role.replaceFirstChar { it.uppercase() }, turn.text)
                                }
                                if (conv.expectedBehavior.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Expected: ${conv.expectedBehavior}", fontSize = 11.sp, color = TrainerGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMultiTurnDialog(
            onDismiss = { showAddDialog = false },
            onSave = {
                onAddConversation(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DialogueBubble(role: String, text: String) {
    val isUser = role.equals("User", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isUser) Color(0xFF1E293B) else Color(0xFF0F3B4A))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                Text(text = role.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isUser) TrainerElectricBlue else TrainerCyan)
                Text(text = text, fontSize = 12.sp, color = TrainerTextPrimary)
            }
        }
    }
}

@Composable
fun AddMultiTurnDialog(
    onDismiss: () -> Unit,
    onSave: (ConversationTraining) -> Unit
) {
    var title by remember { mutableStateOf("New Multi-Turn Dialogue") }
    var expectedBehavior by remember { mutableStateOf("Natural transition from greeting to device action.") }
    var user1 by remember { mutableStateOf("Hi") }
    var jarvis1 by remember { mutableStateOf("Haan bhai, bol 😄") }
    var user2 by remember { mutableStateOf("Kya kar raha hai?") }
    var jarvis2 by remember { mutableStateOf("Bas yahin hoon 😄 Bol, kya scene hai?") }
    var user3 by remember { mutableStateOf("Battery kitni hai?") }
    var jarvis3 by remember { mutableStateOf("Ek sec, battery check karta hoon.") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TrainerSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerCyan.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "TEACH MULTI-TURN DIALOGUE", color = TrainerCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Dialogue Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = user1,
                    onValueChange = { user1 = it },
                    label = { Text("Turn 1 - User") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = jarvis1,
                    onValueChange = { jarvis1 = it },
                    label = { Text("Turn 1 - JARVIS") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = user2,
                    onValueChange = { user2 = it },
                    label = { Text("Turn 2 - User") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = jarvis2,
                    onValueChange = { jarvis2 = it },
                    label = { Text("Turn 2 - JARVIS") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = user3,
                    onValueChange = { user3 = it },
                    label = { Text("Turn 3 - User") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = jarvis3,
                    onValueChange = { jarvis3 = it },
                    label = { Text("Turn 3 - JARVIS") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = expectedBehavior,
                    onValueChange = { expectedBehavior = it },
                    label = { Text("Expected Pattern / Behavior Annotation") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = TrainerTextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val turns = listOf(
                                ConversationTurn(role = "user", text = user1.trim()),
                                ConversationTurn(role = "jarvis", text = jarvis1.trim()),
                                ConversationTurn(role = "user", text = user2.trim()),
                                ConversationTurn(role = "jarvis", text = jarvis2.trim()),
                                ConversationTurn(role = "user", text = user3.trim()),
                                ConversationTurn(role = "jarvis", text = jarvis3.trim(), toolUsed = "Battery")
                            )
                            onSave(
                                ConversationTraining(
                                    title = title.trim(),
                                    turns = turns,
                                    expectedBehavior = expectedBehavior.trim(),
                                    toolRequired = "Battery"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrainerCyan, contentColor = Color.Black)
                    ) {
                        Text("SAVE MULTI-TURN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RulesTab(
    rules: List<BehaviorRule>,
    onAddRule: (BehaviorRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onToggleRule: (String) -> Unit
) {
    var newRuleText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TrainingCategories.GREETING) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "GLOBAL & CATEGORICAL BEHAVIOR RULES",
            color = TrainerTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Hard constraints and tone principles governing JARVIS replies",
            color = TrainerTextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick add rule input
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TrainerCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = newRuleText,
                    onValueChange = { newRuleText = it },
                    placeholder = { Text("e.g. 'Never answer with a guessed battery percentage.'", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_quick_rule"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrainerCyan,
                        focusedTextColor = TrainerTextPrimary,
                        unfocusedTextColor = TrainerTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Category: $selectedCategory", fontSize = 11.sp, color = TrainerCyan)
                    Button(
                        onClick = {
                            if (newRuleText.isNotBlank()) {
                                onAddRule(
                                    BehaviorRule(
                                        rule = newRuleText.trim(),
                                        category = selectedCategory,
                                        priority = 2
                                    )
                                )
                                newRuleText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrainerCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_save_rule")
                    ) {
                        Text("ADD RULE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rules, key = { it.id }) { rule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (rule.enabled) TrainerCardSurface else TrainerCardSurface.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (rule.enabled) TrainerCardBorder else Color.DarkGray.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TrainerCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = rule.category, fontSize = 9.sp, color = TrainerCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = rule.rule, fontSize = 13.sp, color = TrainerTextPrimary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { onToggleRule(rule.id) },
                                colors = SwitchDefaults.colors(checkedThumbColor = TrainerCyan, checkedTrackColor = TrainerCyan.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("switch_rule_enabled_${rule.id}")
                            )
                            IconButton(onClick = { onDeleteRule(rule.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    history: List<VersionHistoryEntry>,
    currentVersion: Int,
    onRollback: (Int) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "TRAINING VERSION HISTORY & ROLLBACK",
            color = TrainerTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Full snapshot audit trail. Revert to any prior training version instantly.",
            color = TrainerTextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No previous version snapshots saved yet.", color = TrainerTextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.version }) { entry ->
                    val isCurrent = entry.version == currentVersion
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TrainerCardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) TrainerGreen else TrainerCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Version v${entry.version}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) TrainerGreen else TrainerCyan
                                    )
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(TrainerGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = "CURRENT ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TrainerGreen)
                                        }
                                    }
                                }

                                if (!isCurrent) {
                                    Button(
                                        onClick = { onRollback(entry.version) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TrainerAmber, contentColor = Color.Black),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("btn_rollback_${entry.version}")
                                    ) {
                                        Text("ROLLBACK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = entry.description, fontSize = 12.sp, color = TrainerTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = "Items: ${entry.itemsCount}", fontSize = 11.sp, color = TrainerTextSecondary)
                                Text(text = "Rules: ${entry.rulesCount}", fontSize = 11.sp, color = TrainerTextSecondary)
                                Text(text = dateFormat.format(Date(entry.timestamp)), fontSize = 11.sp, color = TrainerTextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncDataTab(
    isConnected: Boolean,
    onPingJarvis: () -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean
) {
    var exportPreview by remember { mutableStateOf("") }
    var importInput by remember { mutableStateOf("") }
    var showImportSuccess by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "LIVE SYNC IPC & DATA BACKUP",
            color = TrainerTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Manage secure Android IPC communication and JSON portability",
            color = TrainerTextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // IPC Diagnostic Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TrainerCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "INTER-APP COMMUNICATION STATUS", color = TrainerCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isConnected) TrainerGreen else TrainerAmber))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Active Bound Service & ContentProvider Link" else "ContentProvider Persistent Link (Service Unbound)",
                        color = TrainerTextPrimary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Authority: com.jarvis.ai.training.provider", fontSize = 10.sp, color = TrainerTextSecondary, fontFamily = FontFamily.Monospace)
                Text(text = "Broadcast: com.jarvis.ai.ACTION_TRAINING_APPLIED", fontSize = 10.sp, color = TrainerTextSecondary, fontFamily = FontFamily.Monospace)

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onPingJarvis,
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerElectricBlue, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_ping_jarvis")
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PING JARVIS IPC LINK", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Export JSON Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TrainerCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "EXPORT TRAINING BUNDLE (JSON)", color = TrainerCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { exportPreview = onExportJson() },
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_generate_export_json")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GENERATE JSON EXPORT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                if (exportPreview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportPreview,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TrainerTextPrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Import JSON Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TrainerCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "IMPORT TRAINING BUNDLE (JSON)", color = TrainerCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = importInput,
                    onValueChange = { importInput = it },
                    placeholder = { Text("Paste valid TrainingBundle JSON here...", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("input_import_json"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TrainerTextPrimary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val success = onImportJson(importInput)
                        showImportSuccess = success
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrainerGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_import_json")
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("IMPORT & VALIDATE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                if (showImportSuccess != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (showImportSuccess == true) "Import Successful! Tap SAVE & APPLY to sync to JARVIS." else "Import Failed: Invalid JSON or security constraint violation.",
                        color = if (showImportSuccess == true) TrainerGreen else TrainerRed,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
