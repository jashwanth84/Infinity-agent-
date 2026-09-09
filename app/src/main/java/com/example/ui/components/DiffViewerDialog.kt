package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

sealed class DiffLine {
    data class Unchanged(val text: String, val oldNum: Int, val newNum: Int) : DiffLine()
    data class Added(val text: String, val newNum: Int) : DiffLine()
    data class Deleted(val text: String, val oldNum: Int) : DiffLine()
}

@Composable
fun DiffViewerDialog(
    filePath: String,
    oldContent: String,
    newContent: String,
    onApply: () -> Unit,
    onReject: () -> Unit
) {
    val diffLines = remember(oldContent, newContent) {
        computeSimpleDiff(oldContent, newContent)
    }

    val additions = diffLines.count { it is DiffLine.Added }
    val deletions = diffLines.count { it is DiffLine.Deleted }

    Dialog(
        onDismissRequest = onReject,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Difference,
                            contentDescription = null,
                            tint = AccentCyanBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Code Diff Review",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = filePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentGreen.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "+$additions",
                                color = AccentGreen,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentRose.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "-$deletions",
                                color = AccentRose,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Diff Lines List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(CodeEditorDarkBg)
                        .padding(vertical = 4.dp)
                ) {
                    itemsIndexed(diffLines) { _, line ->
                        DiffLineItem(line)
                    }
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject")
                    }

                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffLineItem(line: DiffLine) {
    val (bg, textColor, prefix, lineNum) = when (line) {
        is DiffLine.Added -> Quad(
            Color(0x2E10B981),
            Color(0xFF86EFAC),
            "+",
            line.newNum.toString()
        )
        is DiffLine.Deleted -> Quad(
            Color(0x33F43F5E),
            Color(0xFFFDA4AF),
            "-",
            line.oldNum.toString()
        )
        is DiffLine.Unchanged -> Quad(
            Color.Transparent,
            Color(0xFFC9D1D9),
            " ",
            line.newNum.toString()
        )
    }

    val text = when (line) {
        is DiffLine.Added -> line.text
        is DiffLine.Deleted -> line.text
        is DiffLine.Unchanged -> line.text
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = lineNum.padStart(3, ' '),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFF6E7681),
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = prefix,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.width(16.dp)
        )
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun computeSimpleDiff(oldText: String, newText: String): List<DiffLine> {
    val oldLines = oldText.lines()
    val newLines = newText.lines()
    val result = mutableListOf<DiffLine>()

    var i = 0
    var j = 0

    while (i < oldLines.size || j < newLines.size) {
        when {
            i < oldLines.size && j < newLines.size && oldLines[i] == newLines[j] -> {
                result.add(DiffLine.Unchanged(oldLines[i], i + 1, j + 1))
                i++
                j++
            }
            j < newLines.size && (i >= oldLines.size || !oldLines.contains(newLines[j])) -> {
                result.add(DiffLine.Added(newLines[j], j + 1))
                j++
            }
            i < oldLines.size -> {
                result.add(DiffLine.Deleted(oldLines[i], i + 1))
                i++
            }
        }
    }
    return result
}
