package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.ui.MainViewModel
import com.example.ui.components.AgentStepCard
import com.example.ui.theme.*

@Composable
fun AgentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var taskInput by remember { mutableStateOf("Build a high-performance LRU Cache with concurrent eviction in C++ and Java wrapper") }
    val steps by viewModel.agentSteps.collectAsState()
    val isRunning by viewModel.isAgentRunning.collectAsState()
    val currentStepIndex by viewModel.currentAgentStepIndex.collectAsState()

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
                    text = "Executes the 6-stage autonomous software engineering loop:\nAnalyze → Plan → Code → Review → Debug → Apply",
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

        // Run Button
        Button(
            onClick = { viewModel.startAutonomousAgent(taskInput) },
            enabled = !isRunning && taskInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
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
                Text("Executing Autonomous Loop...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Autonomous Cycle", fontWeight = FontWeight.Bold)
            }
        }

        // Steps List
        if (steps.isNotEmpty()) {
            Text(
                text = "EXECUTION STAGES",
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
                                    filePath = "src/Engine.kt",
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
}
