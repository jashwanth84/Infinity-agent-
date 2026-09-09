package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val language: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_files")
data class ProjectFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val path: String,
    val content: String,
    val language: String,
    val realUri: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val modelUsed: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val attachedFileName: String? = null,
    val attachedFileContent: String? = null,
    val reasoningContent: String? = null,
    val hasDiff: Boolean = false,
    val diffFilePath: String? = null,
    val diffProposedContent: String? = null
)

@Entity(tableName = "agent_runs")
data class AgentRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskPrompt: String,
    val mode: String, // "AGENT" or "MULTI_AGENT"
    val status: String, // "RUNNING", "COMPLETED", "FAILED"
    val stepResultsJson: String,
    val finalOutput: String,
    val timestamp: Long = System.currentTimeMillis()
)
