package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SyntaxHighlightedCodeBlock(
    code: String,
    language: String = "Kotlin",
    modifier: Modifier = Modifier,
    onApplyDiff: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val lines = remember(code) { code.lines() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        color = CodeEditorDarkBg,
        tonalElevation = 4.dp
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentCyanBright)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = language.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFFC9D1D9)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onApplyDiff != null) {
                        IconButton(
                            onClick = onApplyDiff,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Difference,
                                contentDescription = "View Diff",
                                tint = AccentCyanBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Code Snippet", code)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (isCopied) AccentGreen else Color(0xFF8B949E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Code Content with Line Numbers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                // Gutter (Line Numbers)
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in lines.indices) {
                        Text(
                            text = "${i + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF484F58),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Code text
                Column(modifier = Modifier.padding(end = 16.dp)) {
                    for (line in lines) {
                        Text(
                            text = highlightCodeLine(line, language),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

fun highlightCodeLine(line: String, language: String): AnnotatedString {
    return buildAnnotatedString {
        val trimmed = line.trimStart()

        // Comments
        if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
            append(AnnotatedString(line, SpanStyle(color = SyntaxComment)))
            return@buildAnnotatedString
        }

        val keywords = setOf(
            "fun", "val", "var", "class", "interface", "object", "data", "override", "private", "public", "protected",
            "import", "package", "return", "if", "else", "when", "for", "while", "true", "false", "null", "suspend",
            "void", "int", "double", "float", "boolean", "char", "static", "final", "native", "extern",
            "def", "from", "async", "await", "function", "const", "let", "type", "select", "from", "where", "group", "by"
        )

        val tokens = line.split(Regex("(?<=\\s|\\b)|(?=\\s|\\b)"))
        var inString = false
        var stringChar = ' '

        for (token in tokens) {
            val lower = token.lowercase()
            when {
                token.startsWith("\"") || token.startsWith("'") -> {
                    append(AnnotatedString(token, SpanStyle(color = SyntaxString)))
                }
                token.endsWith("\"") || token.endsWith("'") -> {
                    append(AnnotatedString(token, SpanStyle(color = SyntaxString)))
                }
                keywords.contains(lower) -> {
                    append(AnnotatedString(token, SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold)))
                }
                token.all { it.isDigit() || it == '.' || it == 'x' || it == 'X' } && token.isNotEmpty() -> {
                    append(AnnotatedString(token, SpanStyle(color = SyntaxNumber)))
                }
                token.startsWith("@") -> {
                    append(AnnotatedString(token, SpanStyle(color = SyntaxAnnotation)))
                }
                token.firstOrNull()?.isUpperCase() == true && token.length > 1 -> {
                    append(AnnotatedString(token, SpanStyle(color = SyntaxType)))
                }
                token.contains("(") -> {
                    append(AnnotatedString(token, SpanStyle(color = SyntaxFunction)))
                }
                else -> {
                    append(AnnotatedString(token, SpanStyle(color = Color(0xFFE6EDF3))))
                }
            }
        }
    }
}
