package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AIService
import com.example.data.api.MessagePayload
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.model.*
import com.example.data.repository.AgentRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ProjectRepository
import com.example.ui.components.AgentStepData
import com.example.ui.components.StepStatus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID

data class PendingDestructiveAction(
    val title: String,
    val description: String,
    val actionType: String,
    val targetId: Long,
    val onConfirm: () -> Unit
)

data class ToolExecutionResult(
    val tool: InternalToolType,
    val success: Boolean,
    val output: String,
    val requiresConfirmation: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val projectRepository = ProjectRepository(database.projectDao(), database.projectFileDao())
    val chatRepository = ChatRepository(database.chatDao())
    val agentRepository = AgentRepository(database.agentRunDao())
    val aiService = AIService()

    // Mode & Navigation State
    private val _currentMode = MutableStateFlow(AppMode.CHAT)
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    private val _currentWorkspaceTab = MutableStateFlow<WorkspaceTab?>(null)
    val currentWorkspaceTab: StateFlow<WorkspaceTab?> = _currentWorkspaceTab.asStateFlow()

    private val _currentModel = MutableStateFlow(AIModelType.ULTRA_PRO)
    val currentModel: StateFlow<AIModelType> = _currentModel.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Project & File State
    val allProjects: StateFlow<List<ProjectEntity>> = projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    val currentProjectFiles: StateFlow<List<ProjectFileEntity>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id != null) projectRepository.getFilesForProject(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeFile = MutableStateFlow<ProjectFileEntity?>(null)
    val activeFile: StateFlow<ProjectFileEntity?> = _activeFile.asStateFlow()

    // Chat State
    private val _sessionId = MutableStateFlow(UUID.randomUUID().toString())
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessageEntity>> = _sessionId
        .flatMapLatest { id -> chatRepository.getMessages(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chatInputText = MutableStateFlow("")
    val chatInputText: StateFlow<String> = _chatInputText.asStateFlow()

    private val _attachedFile = MutableStateFlow<AttachedFileRef?>(null)
    val attachedFile: StateFlow<AttachedFileRef?> = _attachedFile.asStateFlow()

    private val _attachedImageBase64 = MutableStateFlow<String?>(null)
    val attachedImageBase64: StateFlow<String?> = _attachedImageBase64.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<String?>(null)
    val attachedImageUri: StateFlow<String?> = _attachedImageUri.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeDiff = MutableStateFlow<FileDiff?>(null)
    val activeDiff: StateFlow<FileDiff?> = _activeDiff.asStateFlow()

    // Safety & Destructive Confirmation State
    private val _pendingDestructiveAction = MutableStateFlow<PendingDestructiveAction?>(null)
    val pendingDestructiveAction: StateFlow<PendingDestructiveAction?> = _pendingDestructiveAction.asStateFlow()

    private val _lastToolResult = MutableStateFlow<ToolExecutionResult?>(null)
    val lastToolResult: StateFlow<ToolExecutionResult?> = _lastToolResult.asStateFlow()

    // Coder IDE State
    private val _selectedLanguage = MutableStateFlow(SupportedLanguage.JAVA)
    val selectedLanguage: StateFlow<SupportedLanguage> = _selectedLanguage.asStateFlow()

    private val _editorCode = MutableStateFlow(SupportedLanguage.JAVA.defaultSample)
    val editorCode: StateFlow<String> = _editorCode.asStateFlow()

    private val _coderOutput = MutableStateFlow("")
    val coderOutput: StateFlow<String> = _coderOutput.asStateFlow()

    private val _isCoderExecuting = MutableStateFlow(false)
    val isCoderExecuting: StateFlow<Boolean> = _isCoderExecuting.asStateFlow()

    // Autonomous Agent State (Analyze → Plan → Code → Review → Debug → Apply)
    private val _agentTaskPrompt = MutableStateFlow("")
    val agentTaskPrompt: StateFlow<String> = _agentTaskPrompt.asStateFlow()

    private val _agentSteps = MutableStateFlow<List<AgentStepData>>(emptyList())
    val agentSteps: StateFlow<List<AgentStepData>> = _agentSteps.asStateFlow()

    private val _currentAgentStepIndex = MutableStateFlow(-1)
    val currentAgentStepIndex: StateFlow<Int> = _currentAgentStepIndex.asStateFlow()

    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning: StateFlow<Boolean> = _isAgentRunning.asStateFlow()

    // Multi-Agent Swarm State (Planner → Coder → Reviewer → Debugger → Finalizer)
    private val _multiAgentPrompt = MutableStateFlow("")
    val multiAgentPrompt: StateFlow<String> = _multiAgentPrompt.asStateFlow()

    private val _multiAgentSteps = MutableStateFlow<List<AgentStepData>>(emptyList())
    val multiAgentSteps: StateFlow<List<AgentStepData>> = _multiAgentSteps.asStateFlow()

    private val _finalMultiAgentResult = MutableStateFlow<String?>(null)
    val finalMultiAgentResult: StateFlow<String?> = _finalMultiAgentResult.asStateFlow()

    private val _isMultiAgentRunning = MutableStateFlow(false)
    val isMultiAgentRunning: StateFlow<Boolean> = _isMultiAgentRunning.asStateFlow()

    private var currentChatJob: Job? = null
    private var currentAgentJob: Job? = null

    init {
        viewModelScope.launch {
            projectRepository.ensureDefaultProjects()
            val projects = projectRepository.allProjects.first()
            if (projects.isNotEmpty() && _selectedProjectId.value == null) {
                _selectedProjectId.value = projects.first().id
            }
        }
    }

    fun setMode(mode: AppMode) {
        _currentMode.value = mode
        _currentWorkspaceTab.value = null
    }

    fun setWorkspaceTab(tab: WorkspaceTab) {
        _currentWorkspaceTab.value = tab
    }

    fun setModel(model: AIModelType) {
        _currentModel.value = model
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun selectProject(projectId: Long) {
        _selectedProjectId.value = projectId
    }

    fun setActiveFile(file: ProjectFileEntity?) {
        _activeFile.value = file
        if (file != null) {
            _editorCode.value = file.content
            val lang = SupportedLanguage.values().find {
                it.extension.equals(file.language, ignoreCase = true) ||
                it.displayName.equals(file.language, ignoreCase = true)
            } ?: SupportedLanguage.JAVA
            _selectedLanguage.value = lang
        }
    }

    fun setChatInput(text: String) {
        _chatInputText.value = text
    }

    fun attachFile(file: AttachedFileRef) {
        _attachedFile.value = file
    }

    fun clearAttachment() {
        _attachedFile.value = null
    }

    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null && bytes.isNotEmpty()) {
                    val imagesDir = java.io.File(context.cacheDir, "attached_images").apply { mkdirs() }
                    val imageFile = java.io.File(imagesDir, "img_${System.currentTimeMillis()}.jpg")
                    imageFile.writeBytes(bytes)
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    _attachedImageBase64.value = base64
                    _attachedImageUri.value = imageFile.absolutePath
                    _currentModel.value = AIModelType.VISION_STUDIO
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to attach image", e)
            }
        }
    }

    fun clearImage() {
        _attachedImageBase64.value = null
        _attachedImageUri.value = null
    }

    fun updateChatInputText(text: String) {
        _chatInputText.value = text
    }

    fun sendMessage() {
        val text = _chatInputText.value.trim()
        if (text.isEmpty() || _isGenerating.value) return

        val session = _sessionId.value
        val model = _currentModel.value
        val attached = _attachedFile.value
        val imgBase64 = _attachedImageBase64.value
        val imgUri = _attachedImageUri.value

        _chatInputText.value = ""
        _attachedFile.value = null
        _attachedImageBase64.value = null
        _attachedImageUri.value = null

        viewModelScope.launch {
            // Auto-resolve any @filename reference from active project if not explicitly attached
            var resolvedAttached = attached
            if (resolvedAttached == null) {
                val mentionMatch = Regex("@([a-zA-Z0-9_.-]+)").find(text)
                if (mentionMatch != null) {
                    val refName = mentionMatch.groupValues[1]
                    val matchedFile = currentProjectFiles.value.find { 
                        it.name.equals(refName, ignoreCase = true) || it.path.endsWith(refName, ignoreCase = true) 
                    }
                    if (matchedFile != null) {
                        resolvedAttached = AttachedFileRef(
                            name = matchedFile.name,
                            path = matchedFile.path,
                            content = matchedFile.content,
                            realUri = matchedFile.realUri
                        )
                    }
                }
            }

            // Save User Message
            val userPromptWithFile = if (resolvedAttached != null) {
                """Context File: ${resolvedAttached.path}
```${resolvedAttached.name}
${resolvedAttached.content}
```

$text"""
            } else text

            chatRepository.insertMessage(
                ChatMessageEntity(
                    sessionId = session,
                    role = "user",
                    content = text,
                    modelUsed = model.displayName,
                    imageUri = imgUri,
                    attachedFileName = resolvedAttached?.name,
                    attachedFileContent = resolvedAttached?.content
                )
            )

            // Assistant Placeholder
            val assistantMsgId = chatRepository.insertMessage(
                ChatMessageEntity(
                    sessionId = session,
                    role = "assistant",
                    content = "",
                    modelUsed = model.displayName
                )
            )

            _isGenerating.value = true

            val systemPrompt = "You are Infinity Agent, a world-class AI coding assistant. You write clean, production-ready code with minimal fluff. Always highlight key code in markdown blocks."

            val history = chatRepository.getMessages(session).first().takeLast(6)
            val payloads = history.map { msg ->
                val resolvedBase64 = when {
                    msg.imageUri == imgUri && imgBase64 != null -> imgBase64
                    msg.imageUri != null -> {
                        try {
                            val f = java.io.File(msg.imageUri)
                            if (f.exists()) Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                    else -> null
                }
                val payloadText = if (msg.role == "user" && msg.id == assistantMsgId - 1 && resolvedAttached != null) {
                    userPromptWithFile
                } else {
                    msg.content
                }
                MessagePayload(
                    role = if (msg.role == "user") "user" else "assistant",
                    text = payloadText,
                    imageBase64 = resolvedBase64
                )
            }

            currentChatJob = launch {
                var fullResponse = ""
                var reasoning = ""
                try {
                    aiService.streamChatCompletion(model, payloads, systemPrompt).collect { chunk ->
                        if (chunk.reasoningChunk != null) {
                            reasoning += chunk.reasoningChunk
                        }
                        if (chunk.textChunk.isNotEmpty()) {
                            fullResponse += chunk.textChunk
                            chatRepository.updateMessageContent(assistantMsgId, fullResponse)
                        }
                    }
                    // Check if response contains code diff suggestion for attached/referenced file
                    if (resolvedAttached != null && fullResponse.contains("```")) {
                        extractCodeBlock(fullResponse)?.let { newCode ->
                            _activeDiff.value = FileDiff(
                                filePath = resolvedAttached.path,
                                originalContent = resolvedAttached.content,
                                proposedContent = newCode,
                                changeDescription = "AI suggested modifications for ${resolvedAttached.name}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    chatRepository.updateMessageContent(assistantMsgId, fullResponse.ifEmpty { "Unable to generate a response. Please try again." })
                } finally {
                    _isGenerating.value = false
                }
            }
        }
    }

    fun retryLastMessage() {
        if (_isGenerating.value) return
        viewModelScope.launch {
            val session = _sessionId.value
            val messages = chatRepository.getMessages(session).first()
            val lastUserMsg = messages.lastOrNull { it.role == "user" } ?: return@launch
            val lastAssistantMsg = messages.lastOrNull { it.role == "assistant" }
            if (lastAssistantMsg != null && lastAssistantMsg.id > lastUserMsg.id) {
                chatRepository.deleteMessage(lastAssistantMsg.id)
            }
            _chatInputText.value = lastUserMsg.content
            if (lastUserMsg.attachedFileName != null && lastUserMsg.attachedFileContent != null) {
                _attachedFile.value = AttachedFileRef(
                    name = lastUserMsg.attachedFileName,
                    path = lastUserMsg.attachedFileName,
                    content = lastUserMsg.attachedFileContent
                )
            }
            if (lastUserMsg.imageUri != null) {
                _attachedImageUri.value = lastUserMsg.imageUri
            }
            sendMessage()
        }
    }

    fun regenerateLastResponse() {
        retryLastMessage()
    }

    fun stopGenerating() {
        currentChatJob?.cancel()
        _isGenerating.value = false
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearSession(_sessionId.value)
        }
    }

    fun setEditorCode(code: String) {
        _editorCode.value = code
    }

    fun setLanguage(lang: SupportedLanguage) {
        _selectedLanguage.value = lang
        _editorCode.value = lang.defaultSample
    }

    fun executeCoderAction(action: CodeAction) {
        val code = _editorCode.value
        val lang = _selectedLanguage.value
        _isCoderExecuting.value = true
        _coderOutput.value = "Executing ${action.label} on ${lang.displayName} source..."

        viewModelScope.launch {
            val prompt = "${action.promptPrefix} (${lang.displayName}):\n\n```${lang.extension}\n$code\n```"
            val result = aiService.executeSinglePrompt(
                modelType = _currentModel.value,
                prompt = prompt,
                systemPrompt = "You are an expert ${lang.displayName} compiler engineer and software architect. Provide direct solutions with clean code."
            )
            _coderOutput.value = result
            _isCoderExecuting.value = false
        }
    }

    suspend fun executeInternalTool(
        tool: InternalToolType,
        params: Map<String, String>,
        isConfirmed: Boolean = false
    ): ToolExecutionResult {
        val projId = _selectedProjectId.value ?: 1L
        val result = when (tool) {
            InternalToolType.LIST_FILES -> {
                val files = currentProjectFiles.value
                val summary = files.joinToString("\n") { "• ${it.path} (${it.language}, ${it.content.length} chars)" }
                ToolExecutionResult(tool, true, if (summary.isEmpty()) "No files found in workspace." else "Files in project:\n$summary")
            }
            InternalToolType.READ_FILE -> {
                val targetNameOrPath = params["path"] ?: params["name"] ?: ""
                val file = currentProjectFiles.value.find { it.path == targetNameOrPath || it.name == targetNameOrPath || it.path.endsWith(targetNameOrPath) }
                if (file != null) {
                    ToolExecutionResult(tool, true, "Content of ${file.path}:\n\n${file.content}")
                } else {
                    ToolExecutionResult(tool, false, "File not found: $targetNameOrPath")
                }
            }
            InternalToolType.SEARCH_FILES -> {
                val query = params["query"] ?: ""
                val matches = projectRepository.searchFiles(projId, query)
                val summary = matches.joinToString("\n") { "• ${it.name} (${it.path})" }
                ToolExecutionResult(tool, true, if (summary.isEmpty()) "No matches found for \"$query\"" else "Found ${matches.size} matches:\n$summary")
            }
            InternalToolType.CREATE_FILE -> {
                val name = params["name"] ?: "NewFile.txt"
                val path = params["path"] ?: name
                val content = params["content"] ?: ""
                val lang = params["language"] ?: "Java"
                val id = projectRepository.createFile(projId, name, path, content, lang)
                ToolExecutionResult(tool, true, "Created file $name at $path")
            }
            InternalToolType.EDIT_FILE -> {
                val targetPath = params["path"] ?: params["name"] ?: ""
                val file = currentProjectFiles.value.find { it.path == targetPath || it.name == targetPath || it.path.endsWith(targetPath) }
                val newContent = params["content"] ?: ""
                if (file != null) {
                    projectRepository.updateFileContent(file.id, newContent)
                    if (file.realUri != null) {
                        try {
                            val ctx = getApplication<Application>()
                            ctx.contentResolver.openOutputStream(Uri.parse(file.realUri), "wt")?.use { out ->
                                out.write(newContent.toByteArray(Charsets.UTF_8))
                            }
                        } catch (_: Exception) {}
                    }
                    ToolExecutionResult(tool, true, "Successfully updated file ${file.name}")
                } else {
                    ToolExecutionResult(tool, false, "File not found to edit: $targetPath")
                }
            }
            InternalToolType.RENAME_FILE -> {
                val targetPath = params["path"] ?: params["name"] ?: ""
                val newName = params["newName"] ?: ""
                val newPath = params["newPath"] ?: newName
                val file = currentProjectFiles.value.find { it.path == targetPath || it.name == targetPath }
                if (file != null && newName.isNotBlank()) {
                    projectRepository.renameFile(file.id, newName, newPath)
                    ToolExecutionResult(tool, true, "Renamed ${file.name} to $newName")
                } else {
                    ToolExecutionResult(tool, false, "Could not rename $targetPath")
                }
            }
            InternalToolType.CREATE_FOLDER -> {
                val folderPath = params["path"] ?: params["folder"] ?: "new_folder/"
                val name = folderPath.trimEnd('/').substringAfterLast('/') + "/"
                projectRepository.createFile(projId, name, folderPath, "// Directory placeholder", "folder")
                ToolExecutionResult(tool, true, "Created directory $folderPath")
            }
            InternalToolType.DELETE_FILE -> {
                val targetPath = params["path"] ?: params["name"] ?: ""
                val file = currentProjectFiles.value.find { it.path == targetPath || it.name == targetPath }
                if (file != null) {
                    if (!isConfirmed) {
                        ToolExecutionResult(
                            tool = tool,
                            success = false,
                            output = "Destructive operation blocked: Confirmation required to delete ${file.name}.",
                            requiresConfirmation = true
                        )
                    } else {
                        projectRepository.deleteFile(file.id)
                        ToolExecutionResult(tool, true, "Successfully deleted file ${file.name}")
                    }
                } else {
                    ToolExecutionResult(tool, false, "File not found: $targetPath")
                }
            }
        }
        _lastToolResult.value = result
        return result
    }

    fun requestDeleteFileWithConfirmation(fileId: Long, fileName: String) {
        _pendingDestructiveAction.value = PendingDestructiveAction(
            title = "Confirm File Deletion",
            description = "Are you sure you want to permanently delete \"$fileName\"? This cannot be undone.",
            actionType = "DELETE_FILE",
            targetId = fileId,
            onConfirm = {
                deleteFile(fileId)
                _pendingDestructiveAction.value = null
            }
        )
    }

    fun cancelDestructiveAction() {
        _pendingDestructiveAction.value = null
    }

    fun confirmDestructiveAction() {
        _pendingDestructiveAction.value?.onConfirm?.invoke()
        _pendingDestructiveAction.value = null
    }

    fun startAutonomousAgent(task: String) {
        if (task.isBlank() || _isAgentRunning.value) return
        _agentTaskPrompt.value = task
        _isAgentRunning.value = true

        // 8-stage real tool-based workflow:
        // Analyze → Plan → Inspect Files → Code → Review → Debug → Diff → Apply
        val steps = listOf(
            AgentStepData("step_1", 1, "Analyze", "Analyzer", "Infinity Ultra", Icons.Default.Psychology),
            AgentStepData("step_2", 2, "Plan", "Architect", "Infinity Architect", Icons.Default.AccountTree),
            AgentStepData("step_3", 3, "Inspect Files", "Inspector", "Infinity Forge", Icons.Default.FolderOpen, toolBadge = "list files"),
            AgentStepData("step_4", 4, "Code", "Coder", "Infinity Flash", Icons.Default.Code, toolBadge = "create file"),
            AgentStepData("step_5", 5, "Review", "Reviewer", "Infinity Ultra", Icons.AutoMirrored.Filled.FactCheck),
            AgentStepData("step_6", 6, "Debug", "Debugger", "Infinity Forge", Icons.Default.BugReport),
            AgentStepData("step_7", 7, "Diff", "Diff Engine", "Infinity Vision", Icons.Default.Build, toolBadge = "diff"),
            AgentStepData("step_8", 8, "Apply", "Apply Engine", "Infinity Workspace", Icons.Default.CheckCircle, toolBadge = "apply")
        )
        _agentSteps.value = steps

        currentAgentJob = viewModelScope.launch {
            var accumulatedArtifact = ""
            var targetFilePath = "src/Engine.cpp"
            var originalFileContent = ""
            var proposedFileContent = ""

            for (i in steps.indices) {
                _currentAgentStepIndex.value = i
                val step = steps[i]
                step.status = StepStatus.RUNNING

                when (i) {
                    0 -> {
                        // 1. Analyze
                        val prompt = "Analyze software task requirements and architectural constraints for: $task"
                        val result = aiService.executeSinglePrompt(AIModelType.ULTRA_PRO, prompt)
                        step.outputText = result
                        accumulatedArtifact += "\n[Analysis]:\n$result\n"
                    }
                    1 -> {
                        // 2. Plan
                        val prompt = "Create a detailed technical plan and tool execution roadmap for:\n$accumulatedArtifact"
                        val result = aiService.executeSinglePrompt(AIModelType.ARCHITECT, prompt)
                        step.outputText = result
                        accumulatedArtifact += "\n[Technical Plan]:\n$result\n"
                    }
                    2 -> {
                        // 3. Inspect Files (Executes internal tool: list files)
                        val toolRes = executeInternalTool(InternalToolType.LIST_FILES, emptyMap())
                        val prompt = "Based on current workspace files:\n${toolRes.output}\n\nDetermine which files need to be created or modified for:\n$task"
                        val result = aiService.executeSinglePrompt(AIModelType.CODE_FORGE, prompt)
                        step.outputText = "${toolRes.output}\n\n$result"
                        accumulatedArtifact += "\n[Workspace Inspection]:\n${step.outputText}\n"
                    }
                    3 -> {
                        // 4. Code
                        val prompt = "Write full, high-performance production code implementing the planned features. Specify the target filename (e.g. src/Solution.java):\n$accumulatedArtifact"
                        val result = aiService.executeSinglePrompt(AIModelType.FLASH_TURBO, prompt)
                        val code = extractCodeBlock(result) ?: result
                        step.outputText = result
                        step.codeResult = code
                        proposedFileContent = code
                        accumulatedArtifact += "\n[Code Implementation]:\n$code\n"
                    }
                    4 -> {
                        // 5. Review
                        val prompt = "Review this code implementation for security, concurrency issues, memory leaks, and performance:\n$proposedFileContent"
                        val result = aiService.executeSinglePrompt(AIModelType.ULTRA_PRO, prompt)
                        step.outputText = result
                    }
                    5 -> {
                        // 6. Debug
                        val prompt = "Analyze boundary conditions, null safety, and write assertion edge tests for:\n$proposedFileContent"
                        val result = aiService.executeSinglePrompt(AIModelType.CODE_FORGE, prompt)
                        step.outputText = result
                    }
                    6 -> {
                        // 7. Diff
                        val existingFile = currentProjectFiles.value.firstOrNull()
                        targetFilePath = existingFile?.path ?: "src/Solution.java"
                        originalFileContent = existingFile?.content ?: "// Empty baseline\n"
                        val prompt = "Generate a concise summary of changes between Original and Proposed code for $targetFilePath:\nOriginal:\n$originalFileContent\nProposed:\n$proposedFileContent"
                        val result = aiService.executeSinglePrompt(AIModelType.VISION_STUDIO, prompt)
                        step.outputText = "Diff Generated for $targetFilePath:\n\n$result"
                        val diff = FileDiff(
                            filePath = targetFilePath,
                            originalContent = originalFileContent,
                            proposedContent = proposedFileContent,
                            changeDescription = "Autonomous Agent changes for: $task"
                        )
                        _activeDiff.value = diff
                    }
                    7 -> {
                        // 8. Apply
                        step.outputText = "Diff ready for approval. Click 'Apply' in the diff modal to write changes directly to the project and document, or 'Reject' to discard."
                        step.codeResult = proposedFileContent
                    }
                }

                step.status = StepStatus.COMPLETED
                _agentSteps.value = ArrayList(steps)
            }
            _isAgentRunning.value = false
        }
    }

    fun startMultiAgentSwarm(prompt: String) {
        if (prompt.isBlank() || _isMultiAgentRunning.value) return
        _multiAgentPrompt.value = prompt
        _isMultiAgentRunning.value = true
        _finalMultiAgentResult.value = null

        val swarmSteps = listOf(
            AgentStepData("ma_1", 1, "Requirements & Schema", "Planner Agent", "Infinity Architect", Icons.Default.Lightbulb),
            AgentStepData("ma_2", 2, "Core Implementation", "Coder Agent", "Infinity Flash", Icons.Default.Terminal),
            AgentStepData("ma_3", 3, "Security & Performance", "Reviewer Agent", "Infinity Ultra", Icons.Default.Security),
            AgentStepData("ma_4", 4, "Edge Cases & Tests", "Debugger Agent", "Infinity Forge", Icons.Default.Troubleshoot),
            AgentStepData("ma_5", 5, "Final Synthesis", "Finalizer Agent", "Infinity Ultra", Icons.Default.DoneAll)
        )
        _multiAgentSteps.value = swarmSteps

        viewModelScope.launch {
            var accumulatedContext = "User Prompt: $prompt\n"

            for (i in swarmSteps.indices) {
                val step = swarmSteps[i]
                step.status = StepStatus.RUNNING

                val stepPrompt = when (i) {
                    0 -> "As the Planner Agent, design the system blueprint and API interfaces for:\n$prompt"
                    1 -> "As the Coder Agent, implement clean code following this blueprint:\n$accumulatedContext"
                    2 -> "As the Reviewer Agent, rigorously audit this code:\n$accumulatedContext"
                    3 -> "As the Debugger Agent, test boundary failures and harden edge cases:\n$accumulatedContext"
                    else -> "As the Finalizer Agent, synthesize all contributions into ONE single pristine final result with final code:\n$accumulatedContext"
                }

                val model = when (i) {
                    0 -> AIModelType.ARCHITECT
                    1 -> AIModelType.FLASH_TURBO
                    2 -> AIModelType.ULTRA_PRO
                    3 -> AIModelType.CODE_FORGE
                    else -> AIModelType.ULTRA_PRO
                }

                val output = aiService.executeSinglePrompt(model, stepPrompt)
                step.outputText = output
                step.codeResult = extractCodeBlock(output)
                step.status = StepStatus.COMPLETED
                accumulatedContext += "\n[${step.agentRole} Output]:\n$output\n"
                _multiAgentSteps.value = ArrayList(swarmSteps)

                if (i == swarmSteps.size - 1) {
                    _finalMultiAgentResult.value = output
                }
            }
            _isMultiAgentRunning.value = false
        }
    }

    fun applyDiffToProject(diff: FileDiff) {
        viewModelScope.launch {
            val projectFiles = currentProjectFiles.value
            val target = projectFiles.find { it.path == diff.filePath || it.name == diff.filePath || it.path.endsWith(diff.filePath) }
            if (target != null) {
                projectRepository.updateFileContent(target.id, diff.proposedContent)
                // Write change to the actual selected document/file on device SAF if linked
                if (target.realUri != null) {
                    try {
                        val context = getApplication<Application>()
                        context.contentResolver.openOutputStream(Uri.parse(target.realUri), "wt")?.use { out ->
                            out.write(diff.proposedContent.toByteArray(Charsets.UTF_8))
                        }
                    } catch (_: Exception) {
                    }
                }
                if (_activeFile.value?.id == target.id) {
                    _activeFile.value = target.copy(content = diff.proposedContent)
                }
            } else {
                val selectedId = _selectedProjectId.value ?: 1L
                val name = diff.filePath.substringAfterLast("/")
                val fileId = projectRepository.createFile(
                    projectId = selectedId,
                    name = name,
                    path = diff.filePath,
                    content = diff.proposedContent,
                    language = when {
                        name.endsWith(".java") -> "Java"
                        name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".h") -> "C++"
                        name.endsWith(".xml") -> "XML"
                        name.endsWith(".py") -> "Python"
                        name.endsWith(".js") -> "JavaScript"
                        name.endsWith(".ts") -> "TypeScript"
                        name.endsWith(".html") -> "HTML"
                        name.endsWith(".css") -> "CSS"
                        name.endsWith(".sql") -> "SQL"
                        name.endsWith(".json") -> "JSON"
                        else -> "Kotlin"
                    }
                )
                val newFile = projectRepository.getFileById(fileId)
                if (newFile != null) {
                    _activeFile.value = newFile
                }
            }
            _activeDiff.value = null
        }
    }

    fun dismissDiff() {
        _activeDiff.value = null
    }

    fun createNewProject(name: String, description: String, language: String) {
        viewModelScope.launch {
            val newId = projectRepository.createProject(name, description, language)
            _selectedProjectId.value = newId
            projectRepository.createFile(
                projectId = newId,
                name = "Main.kt",
                path = "src/Main.kt",
                content = "// New Project $name\nfun main() {\n    println(\"Infinity Agent\")\n}",
                language = "Kotlin"
            )
        }
    }

    fun createNewFile(name: String, path: String, content: String, language: String) {
        viewModelScope.launch {
            val projId = _selectedProjectId.value ?: return@launch
            projectRepository.createFile(projId, name, path, content, language)
        }
    }

    fun deleteFile(fileId: Long) {
        viewModelScope.launch {
            projectRepository.deleteFile(fileId)
            if (_activeFile.value?.id == fileId) {
                _activeFile.value = null
            }
        }
    }

    fun renameFile(fileId: Long, newName: String, newPath: String) {
        viewModelScope.launch {
            projectRepository.renameFile(fileId, newName, newPath)
        }
    }

    fun saveActiveFileContent(content: String) {
        viewModelScope.launch {
            val file = _activeFile.value ?: return@launch
            projectRepository.updateFileContent(file.id, content)
            // Write directly to actual physical document if selected from storage
            if (file.realUri != null) {
                try {
                    val context = getApplication<Application>()
                    context.contentResolver.openOutputStream(Uri.parse(file.realUri), "wt")?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    }
                } catch (_: Exception) {
                }
            }
            _activeFile.value = file.copy(content = content)
        }
    }

    fun importLocalFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "imported_file.txt"
                val projId = _selectedProjectId.value ?: 1L
                val fileId = projectRepository.createFile(
                    projectId = projId,
                    name = fileName,
                    path = "imported/$fileName",
                    content = content,
                    language = fileName.substringAfterLast(".", "txt"),
                    realUri = uri.toString()
                )
                val newFile = projectRepository.getFileById(fileId)
                setActiveFile(newFile)
            } catch (_: Exception) {
            }
        }
    }

    fun importDocumentTree(treeUri: Uri) {
        viewModelScope.launch {
            try {
                val projId = _selectedProjectId.value ?: 1L
                val folderName = treeUri.lastPathSegment?.substringAfterLast(":")?.substringAfterLast("/") ?: "MountedFolder"
                val fileId = projectRepository.createFile(
                    projectId = projId,
                    name = "$folderName/",
                    path = "tree/$folderName/",
                    content = "// Mounted Folder from Storage Access Framework\n// Path: $treeUri",
                    language = "folder",
                    realUri = treeUri.toString()
                )
                val newFile = projectRepository.getFileById(fileId)
                setActiveFile(newFile)
            } catch (_: Exception) {
            }
        }
    }

    private fun extractCodeBlock(markdown: String): String? {
        val startTag = "```"
        val startIdx = markdown.indexOf(startTag)
        if (startIdx == -1) return null
        val codeStart = markdown.indexOf("\n", startIdx)
        if (codeStart == -1) return null
        val endIdx = markdown.indexOf(startTag, codeStart)
        if (endIdx == -1) return null
        return markdown.substring(codeStart + 1, endIdx).trim()
    }
}
