package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttachedFileRef
import com.example.ui.theme.*

@Composable
fun FloatingChatComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
    onStopGenerating: () -> Unit,
    onTriggerFilePicker: () -> Unit,
    onTriggerImagePicker: () -> Unit,
    attachedFile: AttachedFileRef?,
    onRemoveAttachment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = AccentCyan.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(26.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Attached File Pill (if any)
            AnimatedVisibility(visible = attachedFile != null) {
                attachedFile?.let { file ->
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp, start = 8.dp, end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentCyanBright.copy(alpha = 0.15f))
                            .border(1.dp, AccentCyanBright.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = AccentCyanBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentCyanBright,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove file",
                            tint = AccentCyanBright,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRemoveAttachment() }
                        )
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // @ Mention File Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onTriggerFilePicker()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "@",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyanBright
                        )
                    }
                }

                // Image Upload Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onTriggerImagePicker()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Screenshot or Diagram",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text Input Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Ask anything or type @ to link files...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { newText ->
                            onTextChange(newText)
                            // If user typed "@" as the last character, trigger file picker!
                            if (newText.endsWith("@")) {
                                onTriggerFilePicker()
                            }
                        },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(AccentCyanBright),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (text.isNotBlank() && !isGenerating) onSend()
                        }),
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action Button (Send or Stop)
                if (isGenerating) {
                    IconButton(
                        onClick = onStopGenerating,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentRose)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop generation",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onSend()
                            }
                        },
                        enabled = text.isNotBlank(),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (text.isNotBlank()) AccentCyanBright else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = if (text.isNotBlank()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
