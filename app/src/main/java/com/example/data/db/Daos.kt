package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int
}

@Dao
interface ProjectFileDao {
    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY path ASC")
    fun getFilesForProject(projectId: Long): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getFileById(id: Long): ProjectFileEntity?

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND path = :path LIMIT 1")
    suspend fun getFileByPath(projectId: Long, path: String): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity): Long

    @Update
    suspend fun updateFile(file: ProjectFileEntity)

    @Query("UPDATE project_files SET content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFileContent(id: Long, content: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE project_files SET name = :newName, path = :newPath WHERE id = :id")
    suspend fun renameFile(id: Long, newName: String, newPath: String)

    @Query("DELETE FROM project_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteFilesForProject(projectId: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET content = :content WHERE id = :id")
    suspend fun updateMessageContent(id: Long, content: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Query("SELECT DISTINCT sessionId FROM chat_messages ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<String>>
}

@Dao
interface AgentRunDao {
    @Query("SELECT * FROM agent_runs ORDER BY timestamp DESC")
    fun getAllRuns(): Flow<List<AgentRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: AgentRunEntity): Long

    @Update
    suspend fun updateRun(run: AgentRunEntity)

    @Query("DELETE FROM agent_runs WHERE id = :id")
    suspend fun deleteRunById(id: Long)
}
