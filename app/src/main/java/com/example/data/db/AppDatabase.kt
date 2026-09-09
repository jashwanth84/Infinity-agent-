package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        ProjectFileEntity::class,
        ChatMessageEntity::class,
        AgentRunEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileDao(): ProjectFileDao
    abstract fun chatDao(): ChatDao
    abstract fun agentRunDao(): AgentRunDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "infinity_agent_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
