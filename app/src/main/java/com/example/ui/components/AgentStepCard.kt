package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class StepStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED
}

data class AgentStepData(
    val id: String,
    val stepIndex: Int,
    val name: String,
    val agentRole: String, // e.g., "Planner", "Coder", "Reviewer"
    val modelDisplayName: String,
    val icon: ImageVector,
    var status: StepStatus = StepStatus.IDLE,
    var toolBadge: String? = null,
    var outputText: String = "",
    var codeResult: String? = null
)

@Composable
fun AgentStepCard(
    step: AgentStepData,
    isCurrent: Boolean,
    onApplyCode: ((String) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(isCurrent || step.status == StepStatus.RUNNING) }

    val statusColor = when (step.status) {
        StepStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        StepStatus.RUNNING -> AccentCyanBright
        StepStatus.COMPLETED -> AccentGreen
        StepStatus.FAILED -> AccentRose
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) AccentCyanBright else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isCurrent) 4.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Indicator Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.5.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (step.status == StepStatus.RUNNING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AccentCyanBright,
                            strokeWidth = 2.dp
                        )
                    } else if (step.status == StepStatus.COMPLETED) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "${step.stepIndex}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = step.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentIndigo.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = step.agentRole,
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentIndigo,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (step.toolBadge != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentCyanBright.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = step.toolBadge!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentCyanBright,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Model: ${step.modelDisplayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded && step.outputText.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = step.outputText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (step.codeResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SyntaxHighlightedCodeBlock(
                            code = step.codeResult!!,
                            language = "Kotlin",
                            onApplyDiff = onApplyCode?.let { { it(step.codeResult!!) } }
                        )
                    }
                }
            }
        }
    }
}
