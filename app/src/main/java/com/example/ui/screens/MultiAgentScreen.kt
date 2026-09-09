package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileDiff
import com.example.ui.MainViewModel
import com.example.ui.components.AgentStepCard
import com.example.ui.components.SyntaxHighlightedCodeBlock
import com.example.ui.theme.*

@Composable
fun MultiAgentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("Architect a zero-copy circular IPC buffer in C++ with Java JNI bindings and XML metrics layout") }
    val steps by viewModel.multiAgentSteps.collectAsState()
    val finalResult by viewModel.finalMultiAgentResult.collectAsState()
    val isRunning by viewModel.isMultiAgentRunning.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Multi-Agent Header Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, AccentPink.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                color = AccentPink.copy(alpha = 0.1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = AccentPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Multi-Model Collaborative Swarm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "5 Specialized AI Models collaborate in sequence:\nPlanner → Coder → Reviewer → Debugger → Finalizer\nSynthesizes into ONE authoritative final result.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Swarm Input Box
        item {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                label = { Text("Collaborative Objective") },
                placeholder = { Text("What should the multi-agent swarm build?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3
            )
        }

        // Trigger Button
        item {
            Button(
                onClick = { viewModel.startMultiAgentSwarm(promptInput) },
                enabled = !isRunning && promptInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPink,
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
                    Text("Swarm Collaborating...")
                } else {
                    Icon(Icons.Default.Diversity3, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Launch Multi-Agent Swarm", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Final Synthesis Card (The ONE Final Result)
        if (finalResult != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.5.dp, AccentGreen, RoundedCornerShape(18.dp)),
                    color = AccentGreen.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Final Consensus Result",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = finalResult!!,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Intermediate Collaborations
        if (steps.isNotEmpty()) {
            item {
                Text(
                    text = "AGENT COLLABORATION HANDOFFS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            items(steps) { step ->
                AgentStepCard(
                    step = step,
                    isCurrent = isRunning && step.status == com.example.ui.components.StepStatus.RUNNING,
                    onApplyCode = { code ->
                        viewModel.applyDiffToProject(
                            FileDiff(
                                filePath = "src/MultiAgentOutput.kt",
                                originalContent = "",
                                proposedContent = code,
                                changeDescription = "Applied from Multi-Agent Swarm"
                            )
                        )
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
