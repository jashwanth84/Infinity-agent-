package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileDiff
import com.example.data.model.InternalToolType
import com.example.ui.MainViewModel
import com.example.ui.components.AgentStepCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AgentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var taskInput by remember { mutableStateOf("Build a high-performance LRU Cache with concurrent eviction in C++ and Java wrapper") }
    val steps by viewModel.agentSteps.collectAsState()
    val isRunning by viewModel.isAgentRunning.collectAsState()
    val currentStepIndex by viewModel.currentAgentStepIndex.collectAsState()
    val pendingDestructiveAction by viewModel.pendingDestructiveAction.collectAsState()
    val lastToolResult by viewModel.lastToolResult.collectAsState()

    var showToolsPanel by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf(InternalToolType.LIST_FILES) }
    var toolParamInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Workflow Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, AccentPurple.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
            color = AccentPurple.copy(alpha = 0.1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Autonomous Agent Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Executes the real 8-stage software engineering loop:\nAnalyze → Plan → Inspect Files → Code → Review → Debug → Diff → Apply",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Task Input Box
        OutlinedTextField(
            value = taskInput,
            onValueChange = { taskInput = it },
            label = { Text("Task Description / Goal") },
            placeholder = { Text("e.g. Implement thread-safe ring buffer in C++ and JNI interface...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                if (taskInput.isNotBlank() && !isRunning) {
                    viewModel.startAutonomousAgent(taskInput)
                }
            })
        )

        // Run & Tools Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.startAutonomousAgent(taskInput) },
                enabled = !isRunning && taskInput.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    contentColor = Color.White
                )
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Executing Cycle...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start 8-Stage Cycle", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { showToolsPanel = !showToolsPanel },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyanBright.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Internal Tools",
                    tint = AccentCyanBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showToolsPanel) "Hide Tools" else "Tools (8)",
                    color = AccentCyanBright,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Internal Tools Console (Expandable)
        AnimatedVisibility(visible = showToolsPanel) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, AccentCyanBright.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "INTERNAL FILE & WORKSPACE TOOLS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyanBright
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tool selector pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InternalToolType.values().forEach { tool ->
                            val isSelected = tool == selectedTool
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTool = tool
                                    toolParamInput = when (tool) {
                                        InternalToolType.LIST_FILES -> ""
                                        InternalToolType.READ_FILE -> "src/Main.kt"
                                        InternalToolType.SEARCH_FILES -> "class"
                                        InternalToolType.CREATE_FILE -> "src/Helper.java"
                                        InternalToolType.EDIT_FILE -> "src/Main.kt"
                                        InternalToolType.RENAME_FILE -> "src/Main.kt"
                                        InternalToolType.CREATE_FOLDER -> "src/native/"
                                        InternalToolType.DELETE_FILE -> "imported/file.txt"
                                    }
                                },
                                label = {
                                    Text(
                                        tool.toolName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (tool.isDestructive) AccentRose.copy(alpha = 0.2f) else AccentCyanBright.copy(alpha = 0.2f),
                                    selectedLabelColor = if (tool.isDestructive) AccentRose else AccentCyanBright
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tool parameter input
                    if (selectedTool != InternalToolType.LIST_FILES) {
                        OutlinedTextField(
                            value = toolParamInput,
                            onValueChange = { toolParamInput = it },
                            label = { Text("Target Path / Query / Parameter") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Execute tool button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val params = when (selectedTool) {
                                        InternalToolType.LIST_FILES -> emptyMap()
                                        InternalToolType.READ_FILE -> mapOf("path" to toolParamInput)
                                        InternalToolType.SEARCH_FILES -> mapOf("query" to toolParamInput)
                                        InternalToolType.CREATE_FILE -> mapOf("name" to toolParamInput, "content" to "// Created by tool\n")
                                        InternalToolType.EDIT_FILE -> mapOf("path" to toolParamInput, "content" to "// Updated by tool\n")
                                        InternalToolType.RENAME_FILE -> mapOf("path" to toolParamInput, "newName" to "${toolParamInput}.bak")
                                        InternalToolType.CREATE_FOLDER -> mapOf("folder" to toolParamInput)
                                        InternalToolType.DELETE_FILE -> mapOf("path" to toolParamInput)
                                    }
                                    viewModel.executeInternalTool(selectedTool, params, isConfirmed = false)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTool.isDestructive) AccentRose else AccentCyanBright,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Run '${selectedTool.toolName}'", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Output of last tool run
                    if (lastToolResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = lastToolResult!!.output,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                                color = if (lastToolResult!!.success) MaterialTheme.colorScheme.onSurface else AccentRose
                            )
                        }
                    }
                }
            }
        }

        // Steps List
        if (steps.isNotEmpty()) {
            Text(
                text = "EXECUTION STAGES (8)",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(steps) { idx, step ->
                    AgentStepCard(
                        step = step,
                        isCurrent = isRunning && currentStepIndex == idx,
                        onApplyCode = { code ->
                            viewModel.applyDiffToProject(
                                FileDiff(
                                    filePath = "src/Engine.cpp",
                                    originalContent = "",
                                    proposedContent = code,
                                    changeDescription = "Applied from Autonomous Agent"
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    // Safety Confirmation Dialog for Destructive Actions
    if (pendingDestructiveAction != null) {
        val action = pendingDestructiveAction!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelDestructiveAction() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = AccentRose,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = action.title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDestructiveAction() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRose,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelDestructiveAction() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

