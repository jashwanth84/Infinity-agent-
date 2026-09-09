package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.model.AIModelType
import com.example.data.model.AttachedFileRef
import com.example.data.model.FileDiff
import com.example.ui.MainViewModel
import com.example.ui.components.FloatingChatComposer
import com.example.ui.components.SyntaxHighlightedCodeBlock
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onOpenModelSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val chatInputText by viewModel.chatInputText.collectAsState()
    val attachedFile by viewModel.attachedFile.collectAsState()
    val attachedImageUri by viewModel.attachedImageUri.collectAsState()
    val projectFiles by viewModel.currentProjectFiles.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showFileSelectorDialog by remember { mutableStateOf(false) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }

    // Android Storage Access Framework launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importLocalFile(it) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.attachImage(it) }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Model Selector Pill Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenModelSelector() },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentCyanBright)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentModel.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Model",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.clearChatHistory() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Message List or Empty State
            if (messages.isEmpty()) {
                ChatEmptyState(
                    onSelectSuggestion = { suggestion ->
                        viewModel.setChatInput(suggestion)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageItem(
                            message = message,
                            onApplyDiff = { diff ->
                                viewModel.applyDiffToProject(diff)
                            },
                            onPreviewImage = { uri ->
                                previewImageUri = uri
                            }
                        )
                    }
                }
            }
        }

        // Floating Chat Composer
        FloatingChatComposer(
            text = chatInputText,
            onTextChange = { viewModel.setChatInput(it) },
            onSend = { viewModel.sendMessage() },
            isGenerating = isGenerating,
            onStopGenerating = { viewModel.stopGenerating() },
            onTriggerFilePicker = { showFileSelectorDialog = true },
            onTriggerImagePicker = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            attachedFile = attachedFile,
            onRemoveAttachment = { viewModel.clearAttachment() },
            attachedImageUri = attachedImageUri,
            onRemoveImage = { viewModel.clearImage() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    // Full screen image preview dialog
    previewImageUri?.let { imgUri ->
        Dialog(
            onDismissRequest = { previewImageUri = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { previewImageUri = null }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(imgUri).takeIf { it.exists() } ?: imgUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Full picture preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { previewImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 28.dp, end = 12.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // "@" File Attachment Selector Dialog
    if (showFileSelectorDialog) {
        FileAttachmentDialog(
            projectFiles = projectFiles,
            onSelectFile = { file ->
                viewModel.attachFile(
                    AttachedFileRef(
                        name = file.name,
                        path = file.path,
                        content = file.content
                    )
                )
                showFileSelectorDialog = false
            },
            onPickLocalFile = {
                showFileSelectorDialog = false
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = { showFileSelectorDialog = false }
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onApplyDiff: (FileDiff) -> Unit,
    onPreviewImage: (String) -> Unit = {}
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Author Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            Text(
                text = if (isUser) "You" else message.modelUsed,
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) AccentCyanBright else AccentPurple,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Message Content Card
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .border(
                    1.dp,
                    if (isUser) AccentCyan.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Image preview if attached
                if (message.imageUri != null) {
                    val context = LocalContext.current
                    var imageLoadFailed by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onPreviewImage(message.imageUri) },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(message.imageUri).takeIf { it.exists() } ?: message.imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Attached Image (Tap to zoom)",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 240.dp),
                            contentScale = ContentScale.Crop,
                            onError = { imageLoadFailed = true },
                            onSuccess = { imageLoadFailed = false }
                        )

                        if (imageLoadFailed) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    tint = AccentRose,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Image preview unavailable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentRose
                                )
                            }
                        } else {
                            // Subtle zoom badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Zoom",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Zoom",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Attached File Header
                if (message.attachedFileName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentCyanBright.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = AccentCyanBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.attachedFileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyanBright,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Render text / code blocks
                RenderMarkdownContent(
                    content = message.content,
                    onApplyCode = { code ->
                        if (message.attachedFileName != null && message.attachedFileContent != null) {
                            onApplyDiff(
                                FileDiff(
                                    filePath = message.attachedFileName,
                                    originalContent = message.attachedFileContent,
                                    proposedContent = code,
                                    changeDescription = "Applied from AI suggestion"
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RenderMarkdownContent(
    content: String,
    onApplyCode: (String) -> Unit
) {
    val parts = remember(content) { parseMarkdownBlocks(content) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (part in parts) {
            when (part) {
                is ContentPart.Text -> {
                    Text(
                        text = part.text,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is ContentPart.Code -> {
                    SyntaxHighlightedCodeBlock(
                        code = part.code,
                        language = part.language.ifEmpty { "Kotlin" },
                        onApplyDiff = { onApplyCode(part.code) }
                    )
                }
            }
        }
    }
}

sealed class ContentPart {
    data class Text(val text: String) : ContentPart()
    data class Code(val language: String, val code: String) : ContentPart()
}

fun parseMarkdownBlocks(text: String): List<ContentPart> {
    val result = mutableListOf<ContentPart>()
    var cursor = 0
    val startTag = "```"

    while (cursor < text.length) {
        val startIdx = text.indexOf(startTag, cursor)
        if (startIdx == -1) {
            val remaining = text.substring(cursor).trim()
            if (remaining.isNotEmpty()) result.add(ContentPart.Text(remaining))
            break
        }

        if (startIdx > cursor) {
            val textBefore = text.substring(cursor, startIdx).trim()
            if (textBefore.isNotEmpty()) result.add(ContentPart.Text(textBefore))
        }

        val newlineIdx = text.indexOf("\n", startIdx + 3)
        if (newlineIdx == -1) {
            val remaining = text.substring(startIdx)
            result.add(ContentPart.Text(remaining))
            break
        }

        val lang = text.substring(startIdx + 3, newlineIdx).trim()
        val endIdx = text.indexOf(startTag, newlineIdx)

        if (endIdx == -1) {
            val code = text.substring(newlineIdx + 1)
            result.add(ContentPart.Code(lang, code))
            break
        } else {
            val code = text.substring(newlineIdx + 1, endIdx).trimEnd()
            result.add(ContentPart.Code(lang, code))
            cursor = endIdx + 3
        }
    }

    return result
}

@Composable
fun ChatEmptyState(
    onSelectSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(AccentCyanBright.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AllInclusive,
                contentDescription = null,
                tint = AccentCyanBright,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Infinity Agent",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Autonomous AI Coding & Multi-Model Intelligence",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "QUICK START ACTIONS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        val suggestions = listOf(
            "Write a C++ native JNI bridge for matrix operations",
            "Generate an Android XML ConstraintLayout with glass styling",
            "Explain and optimize Java memory allocations in this loop",
            "Refactor this Kotlin coroutine flow for error boundaries"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (suggestion in suggestions) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectSuggestion(suggestion) }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = AccentCyanBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileAttachmentDialog(
    projectFiles: List<ProjectFileEntity>,
    onSelectFile: (ProjectFileEntity) -> Unit,
    onPickLocalFile: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Attach File (@)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option to pick from Phone Storage
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onPickLocalFile)
                        .border(1.dp, AccentCyanBright.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    color = AccentCyanBright.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = AccentCyanBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Open Device File Picker",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentCyanBright
                            )
                            Text(
                                text = "Browse storage for code files & assets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PROJECT FILES",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(projectFiles) { file ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectFile(file) },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = AccentIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = file.path,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
