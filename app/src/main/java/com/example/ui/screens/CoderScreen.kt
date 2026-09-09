package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CodeAction
import com.example.data.model.SupportedLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.SyntaxHighlightedCodeBlock
import com.example.ui.theme.*

@Composable
fun CoderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val editorCode by viewModel.editorCode.collectAsState()
    val coderOutput by viewModel.coderOutput.collectAsState()
    val isExecuting by viewModel.isCoderExecuting.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()

    var showActionConfirmDialog by remember { mutableStateOf<CodeAction?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active file indicator (if any)
        if (activeFile != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentCyanBright.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = AccentCyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Editing: ${activeFile!!.path}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentCyanBright,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = { viewModel.saveActiveFileContent(editorCode) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentCyanBright)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", color = AccentCyanBright, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Language Selector Horizontal Chips
        Column {
            Text(
                text = "TARGET LANGUAGE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SupportedLanguage.values()) { lang ->
                    val isSelected = lang == selectedLanguage
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setLanguage(lang) },
                        label = {
                            Text(
                                text = lang.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyanBright.copy(alpha = 0.2f),
                            selectedLabelColor = AccentCyanBright
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) AccentCyanBright else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            selectedBorderColor = AccentCyanBright,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }

        // Code Editor Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 340.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            color = CodeEditorDarkBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Editor Mini Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B22))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedLanguage.displayName} Source Code",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B949E),
                        fontWeight = FontWeight.SemiBold
                    )

                    Row {
                        IconButton(
                            onClick = { viewModel.setEditorCode("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear code",
                                tint = Color(0xFF8B949E),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Editor Text Input
                BasicTextField(
                    value = editorCode,
                    onValueChange = { viewModel.setEditorCode(it) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFFE6EDF3),
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(AccentCyanBright),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                )
            }
        }

        // Action Buttons Row (Generate, Explain, Debug, Refactor, Review, Optimize)
        Column {
            Text(
                text = "NEURAL ACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val actions = CodeAction.values()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (action in actions) {
                    val actionColor = when (action) {
                        CodeAction.GENERATE -> AccentCyanBright
                        CodeAction.EXPLAIN -> AccentIndigo
                        CodeAction.DEBUG -> AccentRose
                        CodeAction.REFACTOR -> AccentPurple
                        CodeAction.REVIEW -> AccentGreen
                        CodeAction.OPTIMIZE -> AccentAmber
                    }

                    Button(
                        onClick = { viewModel.executeCoderAction(action) },
                        enabled = !isExecuting && editorCode.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = actionColor.copy(alpha = 0.15f),
                            contentColor = actionColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, actionColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Execution Spinner or Output Box
        if (isExecuting) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, AccentCyanBright.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AccentCyanBright,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Analyzing & optimizing ${selectedLanguage.displayName}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else if (coderOutput.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "AI ACTION RESULT",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                SyntaxHighlightedCodeBlock(
                    code = coderOutput,
                    language = selectedLanguage.displayName
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
