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
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    _attachedImageBase64.value = base64
                    _attachedImageUri.value = uri.toString()
                    _currentModel.value = AIModelType.VISION_STUDIO
                }
            } catch (_: Exception) {
            }
        }
    }

    fun clearImage() {
        _attachedImageBase64.value = null
        _attachedImageUri.value = null
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
            // Save User Message
            val userPromptWithFile = if (attached != null) {
                """Context File: ${attached.path}
```${attached.name}
${attached.content}
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
                    attachedFileName = attached?.name,
                    attachedFileContent = attached?.content
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
                MessagePayload(
                    role = if (msg.role == "user") "user" else "assistant",
                    text = msg.content,
                    imageBase64 = if (msg.imageUri != null) imgBase64 else null
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
                    // Check if response contains code diff suggestion for attached file
                    if (attached != null && fullResponse.contains("```")) {
                        extractCodeBlock(fullResponse)?.let { newCode ->
                            _activeDiff.value = FileDiff(
                                filePath = attached.path,
                                originalContent = attached.content,
                                proposedContent = newCode,
                                changeDescription = "AI suggested modifications"
                            )
                        }
                    }
                } catch (e: Exception) {
                    chatRepository.updateMessageContent(assistantMsgId, fullResponse.ifEmpty { "Service response completed." })
                } finally {
                    _isGenerating.value = false
                }
            }
        }
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

    fun startAutonomousAgent(task: String) {
        if (task.isBlank() || _isAgentRunning.value) return
        _agentTaskPrompt.value = task
        _isAgentRunning.value = true

        val steps = listOf(
            AgentStepData("step_1", 1, "Analyze", "Analyzer", "Infinity Ultra", Icons.Default.Psychology),
            AgentStepData("step_2", 2, "Plan", "Architect", "Infinity Architect", Icons.Default.AccountTree),
            AgentStepData("step_3", 3, "Code", "Coder", "Infinity Flash", Icons.Default.Code),
            AgentStepData("step_4", 4, "Review", "Reviewer", "Infinity Vision", Icons.AutoMirrored.Filled.FactCheck),
            AgentStepData("step_5", 5, "Debug", "Debugger", "Infinity Forge", Icons.Default.BugReport),
            AgentStepData("step_6", 6, "Apply", "Engine", "Infinity Workspace", Icons.Default.CheckCircle)
        )
        _agentSteps.value = steps

        currentAgentJob = viewModelScope.launch {
            for (i in steps.indices) {
                _currentAgentStepIndex.value = i
                val step = steps[i]
                step.status = StepStatus.RUNNING

                val prompt = when (i) {
                    0 -> "Analyze task requirements and constraints for: $task"
                    1 -> "Create detailed architecture specifications and plan for: $task"
                    2 -> "Write production-ready code implementation for: $task"
                    3 -> "Review the code for security vulnerabilities, race conditions, and performance"
                    4 -> "Identify boundary edge cases, verify null safety and output assertions"
                    else -> "Finalize package, verify dependencies and prepare patch for: $task"
                }

                val model = when (i) {
                    0 -> AIModelType.ULTRA_PRO
                    1 -> AIModelType.ARCHITECT
                    2 -> AIModelType.FLASH_TURBO
                    3 -> AIModelType.VISION_STUDIO
                    4 -> AIModelType.CODE_FORGE
                    else -> AIModelType.FLASH_TURBO
                }

                val result = aiService.executeSinglePrompt(model, prompt)
                step.outputText = result
                step.codeResult = extractCodeBlock(result)
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
            val target = projectFiles.find { it.path == diff.filePath || it.name == diff.filePath }
            if (target != null) {
                projectRepository.updateFileContent(target.id, diff.proposedContent)
            } else {
                val selectedId = _selectedProjectId.value
                if (selectedId != null) {
                    val name = diff.filePath.substringAfterLast("/")
                    projectRepository.createFile(
                        projectId = selectedId,
                        name = name,
                        path = diff.filePath,
                        content = diff.proposedContent,
                        language = "Kotlin"
                    )
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
                    language = fileName.substringAfterLast(".", "txt")
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
