package com.example.trainer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import com.example.jarvis.training.BehaviorRule
import com.example.jarvis.training.ConversationTraining
import com.example.jarvis.training.ConversationTurn
import com.example.jarvis.training.SyncState
import com.example.jarvis.training.TrainingCategories
import com.example.jarvis.training.TrainingItem
import com.example.jarvis.training.TrainingTone
import com.example.jarvis.training.TrainingType

@Composable
fun TrainerTopBar(
    isConnected: Boolean,
    trainerVersion: Int,
    jarvisVersion: Int,
    syncState: SyncState,
    syncMessage: String?,
    isApplying: Boolean,
    canUndo: Boolean,
    onSaveAndApply: () -> Unit,
    onUndo: () -> Unit
) {
    Surface(
        color = TrainerSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JARVIS",
                            color = TrainerCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TRAINER",
                            color = TrainerTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Behavioral & Neural Training Layer",
                        color = TrainerTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canUndo) {
                        IconButton(
                            onClick = onUndo,
                            modifier = Modifier.testTag("btn_undo")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = TrainerCyan
                            )
                        }
                    }

                    Button(
                        onClick = onSaveAndApply,
                        enabled = !isApplying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrainerCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .testTag("btn_save_and_apply")
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isApplying) Icons.Default.Refresh else Icons.Default.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isApplying) "APPLYING..." else "SAVE & APPLY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status bar strip: connection + versions + sync indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TrainerCardSurface)
                        .border(1.dp, if (isConnected) TrainerGreen.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("chip_connection_status")
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) TrainerGreen else Color.LightGray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "JARVIS CONNECTED 🟢" else "JARVIS OFFLINE ⚪",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) TrainerGreen else TrainerTextSecondary
                    )
                }

                // Version comparison badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TrainerCardSurface)
                        .border(1.dp, TrainerCardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("chip_version_sync")
                ) {
                    Text(
                        text = "Trainer: v$trainerVersion",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TrainerCyan
                    )
                    Text(
                        text = " | ",
                        fontSize = 11.sp,
                        color = TrainerTextSecondary
                    )
                    Text(
                        text = "JARVIS: v$jarvisVersion",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (trainerVersion == jarvisVersion) TrainerGreen else TrainerAmber
                    )
                }

                // Sync status badge
                val (syncLabel, syncColor) = when (syncState) {
                    SyncState.SYNCED -> Pair("SYNCED ✓", TrainerGreen)
                    SyncState.PENDING -> Pair("PENDING ⏳", TrainerAmber)
                    SyncState.FAILED -> Pair("FAILED ✕", TrainerRed)
                    SyncState.IDLE -> Pair(if (trainerVersion == jarvisVersion) "ACTIVE ⚡" else "PENDING ⏳", if (trainerVersion == jarvisVersion) TrainerCyan else TrainerAmber)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(syncColor.copy(alpha = 0.15f))
                        .border(1.dp, syncColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("chip_sync_state")
                ) {
                    Text(
                        text = syncLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = syncColor
                    )
                }
            }

            if (!syncMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = syncMessage,
                    fontSize = 11.sp,
                    color = TrainerTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTrainingItemDialog(
    itemToEdit: TrainingItem? = null,
    onDismiss: () -> Unit,
    onSave: (TrainingItem) -> Unit
) {
    var userInput by remember { mutableStateOf(itemToEdit?.userInput ?: "") }
    var selectedCategory by remember { mutableStateOf(itemToEdit?.category ?: TrainingCategories.GREETING) }
    var intent by remember { mutableStateOf(itemToEdit?.intent ?: "GREETING") }
    var selectedTone by remember { mutableStateOf(itemToEdit?.tone ?: TrainingTone.FRIENDLY) }
    var toolRequired by remember { mutableStateOf(itemToEdit?.toolRequired ?: "None") }
    var confirmationRequired by remember { mutableStateOf(itemToEdit?.confirmationRequired ?: false) }
    var priority by remember { mutableFloatStateOf((itemToEdit?.priority ?: 2).toFloat()) }

    val goodResponses = remember { mutableStateListOf<String>().apply { addAll(itemToEdit?.goodResponses ?: listOf("Haan bhai, bol 😄")) } }
    val badResponses = remember { mutableStateListOf<String>().apply { addAll(itemToEdit?.badResponses ?: listOf("System initialized. How may I assist you?")) } }
    val rules = remember { mutableStateListOf<String>().apply { addAll(itemToEdit?.behaviorRules ?: listOf("Greetings should be natural and friendly.")) } }

    var newGoodText by remember { mutableStateOf("") }
    var newBadText by remember { mutableStateOf("") }
    var newRuleText by remember { mutableStateOf("") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var toneDropdownExpanded by remember { mutableStateOf(false) }
    var toolDropdownExpanded by remember { mutableStateOf(false) }

    val toolOptions = listOf("None", "Battery", "Flashlight", "Volume", "Wifi", "Bluetooth", "Brightness", "FileOperation", "AppLauncher")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TrainerSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrainerCyan.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (itemToEdit == null) "NEW TRAINING PATTERN" else "EDIT TRAINING PATTERN",
                    color = TrainerCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User Input
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("User Trigger Pattern (e.g. 'Hi', 'Battery kitni hai?')") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrainerCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = TrainerCyan,
                        focusedTextColor = TrainerTextPrimary,
                        unfocusedTextColor = TrainerTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_user_query")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category selection dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrainerCyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = TrainerTextPrimary,
                            unfocusedTextColor = TrainerTextPrimary
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        TrainingCategories.ALL.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tone & Intent row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = toneDropdownExpanded,
                        onExpandedChange = { toneDropdownExpanded = !toneDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedTone.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tone") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toneDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrainerCyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = TrainerTextPrimary,
                                unfocusedTextColor = TrainerTextPrimary
                            ),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = toneDropdownExpanded,
                            onDismissRequest = { toneDropdownExpanded = false }
                        ) {
                            TrainingTone.values().forEach { tone ->
                                DropdownMenuItem(
                                    text = { Text(tone.displayName) },
                                    onClick = {
                                        selectedTone = tone
                                        toneDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = intent,
                        onValueChange = { intent = it },
                        label = { Text("Intent") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrainerCyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = TrainerTextPrimary,
                            unfocusedTextColor = TrainerTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preferred Good Responses Section
                Text(
                    text = "PREFERRED RESPONSES (Teach the positive pattern):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrainerGreen
                )
                goodResponses.forEachIndexed { idx, resp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TrainerGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = resp, color = TrainerTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { goodResponses.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newGoodText,
                        onValueChange = { newGoodText = it },
                        placeholder = { Text("Add positive response exemplar...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_new_good_response"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrainerGreen,
                            focusedTextColor = TrainerTextPrimary,
                            unfocusedTextColor = TrainerTextPrimary
                        )
                    )
                    IconButton(
                        onClick = {
                            if (newGoodText.isNotBlank()) {
                                goodResponses.add(newGoodText.trim())
                                newGoodText = ""
                            }
                        },
                        modifier = Modifier.testTag("btn_add_good_response")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = TrainerGreen)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bad Responses Section
                Text(
                    text = "PROHIBITED / ANTI-PATTERNS (What NOT to say):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrainerRed
                )
                badResponses.forEachIndexed { idx, resp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TrainerRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = resp, color = TrainerTextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { badResponses.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newBadText,
                        onValueChange = { newBadText = it },
                        placeholder = { Text("Add prohibited cold/robotic response...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_new_bad_response"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrainerRed,
                            focusedTextColor = TrainerTextPrimary,
                            unfocusedTextColor = TrainerTextPrimary
                        )
                    )
                    IconButton(
                        onClick = {
                            if (newBadText.isNotBlank()) {
                                badResponses.add(newBadText.trim())
                                newBadText = ""
                            }
                        },
                        modifier = Modifier.testTag("btn_add_bad_response")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = TrainerRed)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Behavioral Rules Section
                Text(
                    text = "BEHAVIORAL RULES (Explicit constraints):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrainerCyan
                )
                rules.forEachIndexed { idx, rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "• $rule", color = TrainerTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { rules.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newRuleText,
                        onValueChange = { newRuleText = it },
                        placeholder = { Text("e.g. 'Greetings should be natural and friendly.'", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_new_rule"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrainerCyan,
                            focusedTextColor = TrainerTextPrimary,
                            unfocusedTextColor = TrainerTextPrimary
                        )
                    )
                    IconButton(
                        onClick = {
                            if (newRuleText.isNotBlank()) {
                                rules.add(newRuleText.trim())
                                newRuleText = ""
                            }
                        },
                        modifier = Modifier.testTag("btn_add_rule")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = TrainerCyan)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tool Required & Confirmation Required
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = toolDropdownExpanded,
                        onExpandedChange = { toolDropdownExpanded = !toolDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = toolRequired,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Required Tool") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toolDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrainerCyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = TrainerTextPrimary,
                                unfocusedTextColor = TrainerTextPrimary
                            ),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = toolDropdownExpanded,
                            onDismissRequest = { toolDropdownExpanded = false }
                        ) {
                            toolOptions.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = {
                                        toolRequired = t
                                        toolDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = confirmationRequired,
                            onCheckedChange = { confirmationRequired = it },
                            colors = CheckboxDefaults.colors(checkedColor = TrainerAmber)
                        )
                        Text(
                            text = "Require User Confirmation",
                            fontSize = 11.sp,
                            color = if (confirmationRequired) TrainerAmber else TrainerTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Priority slider
                Text(
                    text = "Training Priority: ${priority.toInt()} / 5",
                    fontSize = 12.sp,
                    color = TrainerTextPrimary
                )
                Slider(
                    value = priority,
                    onValueChange = { priority = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = TrainerCyan, activeTrackColor = TrainerCyan)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = TrainerTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newItem = (itemToEdit ?: TrainingItem()).copy(
                                userInput = userInput.trim(),
                                category = selectedCategory,
                                intent = intent.trim().ifBlank { "GENERAL" },
                                tone = selectedTone,
                                goodResponses = goodResponses.toList(),
                                badResponses = badResponses.toList(),
                                behaviorRules = rules.toList(),
                                toolRequired = if (toolRequired != "None") toolRequired else null,
                                confirmationRequired = confirmationRequired,
                                priority = priority.toInt(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(newItem)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrainerCyan, contentColor = Color.Black),
                        modifier = Modifier.testTag("btn_save_training_item")
                    ) {
                        Text("SAVE PATTERN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
