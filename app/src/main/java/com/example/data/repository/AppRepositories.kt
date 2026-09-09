package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val projectFileDao: ProjectFileDao
) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getFilesForProject(projectId: Long): Flow<List<ProjectFileEntity>> {
        return projectFileDao.getFilesForProject(projectId)
    }

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun getFileById(id: Long): ProjectFileEntity? = projectFileDao.getFileById(id)

    suspend fun getFileByPath(projectId: Long, path: String): ProjectFileEntity? =
        projectFileDao.getFileByPath(projectId, path)

    suspend fun createProject(name: String, description: String, language: String): Long {
        return projectDao.insertProject(
            ProjectEntity(name = name, description = description, language = language)
        )
    }

    suspend fun createFile(
        projectId: Long,
        name: String,
        path: String,
        content: String,
        language: String,
        realUri: String? = null
    ): Long {
        return projectFileDao.insertFile(
            ProjectFileEntity(
                projectId = projectId,
                name = name,
                path = path,
                content = content,
                language = language,
                realUri = realUri
            )
        )
    }

    suspend fun searchFiles(projectId: Long, query: String): List<ProjectFileEntity> {
        return projectFileDao.searchFiles(projectId, query)
    }

    suspend fun updateFileContent(id: Long, content: String) {
        projectFileDao.updateFileContent(id, content)
    }

    suspend fun renameFile(id: Long, newName: String, newPath: String) {
        projectFileDao.renameFile(id, newName, newPath)
    }

    suspend fun deleteFile(id: Long) {
        projectFileDao.deleteFileById(id)
    }

    suspend fun deleteProject(id: Long) {
        projectFileDao.deleteFilesForProject(id)
        projectDao.deleteProjectById(id)
    }

    suspend fun ensureDefaultProjects() {
        if (projectDao.getProjectCount() == 0) {
            // 1. Android Java + C++ NDK Project
            val proj1Id = projectDao.insertProject(
                ProjectEntity(
                    name = "Android C++ & Java Engine",
                    description = "Native NDK integration with Java JNI layer and XML UI",
                    language = "Java + C++"
                )
            )
            createFile(
                projectId = proj1Id,
                name = "MainActivity.java",
                path = "app/src/main/java/com/example/MainActivity.java",
                language = "Java",
                content = """package com.example.ndkengine;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Load native C++ library
    static {
        System.loadLibrary("native-lib");
    }

    // JNI Native function declaration
    public native String stringFromJNI();
    public native int computeMatrixDeterminant(int[] matrix, int size);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
    }
}"""
            )
            createFile(
                projectId = proj1Id,
                name = "native-lib.cpp",
                path = "app/src/main/cpp/native-lib.cpp",
                language = "C++",
                content = """#include <jni.h>
#include <string>
#include <vector>
#include <numeric>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_ndkengine_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from Infinity Agent C++ Native Core v2.0";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_ndkengine_MainActivity_computeMatrixDeterminant(
        JNIEnv* env,
        jobject /* this */,
        jintArray matrixElements,
        jint size) {
    // High-performance native compute
    jint* elems = env->GetIntArrayElements(matrixElements, nullptr);
    jint sum = 0;
    for (int i = 0; i < size; ++i) {
        sum += elems[i];
    }
    env->ReleaseIntArrayElements(matrixElements, elems, JNI_ABORT);
    return sum;
}"""
            )
            createFile(
                projectId = proj1Id,
                name = "activity_main.xml",
                path = "app/src/main/res/layout/activity_main.xml",
                language = "XML",
                content = """<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#0F172A">

    <TextView
        android:id="@+id/sample_text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Native Engine Initializing..."
        android:textColor="#38BDF8"
        android:textSize="18sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>"""
            )
            createFile(
                projectId = proj1Id,
                name = "CMakeLists.txt",
                path = "app/src/main/cpp/CMakeLists.txt",
                language = "C++",
                content = """cmake_minimum_required(VERSION 3.22.1)
project("ndkengine")

add_library(native-lib SHARED native-lib.cpp)

find_library(log-lib log)

target_link_libraries(native-lib ${'$'}{log-lib})"""
            )

            // 2. Android Java + XML Architecture
            val proj2Id = projectDao.insertProject(
                ProjectEntity(
                    name = "Android Java + XML App",
                    description = "Native Android App using Java and XML Layouts",
                    language = "Java + XML"
                )
            )
            createFile(
                projectId = proj2Id,
                name = "MainActivity.java",
                path = "app/src/main/java/com/example/app/MainActivity.java",
                language = "Java",
                content = """package com.example.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusText = findViewById(R.id.status_text);
        Button actionButton = findViewById(R.id.action_button);

        actionButton.setOnClickListener(v -> {
            counter++;
            statusText.setText("Interaction count: " + counter);
        });
    }
}"""
            )
            createFile(
                projectId = proj2Id,
                name = "activity_main.xml",
                path = "app/src/main/res/layout/activity_main.xml",
                language = "XML",
                content = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="24dp">

    <TextView
        android:id="@+id/status_text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Ready"
        android:textSize="20sp" />

    <Button
        android:id="@+id/action_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="Increment" />

</LinearLayout>"""
            )

            // 3. Multi-Language Fullstack Suite
            val proj3Id = projectDao.insertProject(
                ProjectEntity(
                    name = "Multi-Language Tools",
                    description = "Python algorithms, JavaScript async, and SQL schemas",
                    language = "Python + JS + SQL"
                )
            )
            createFile(
                projectId = proj3Id,
                name = "processor.py",
                path = "scripts/processor.py",
                language = "Python",
                content = """import json
import math

def analyze_telemetry(data_points):
    mean = sum(data_points) / len(data_points)
    variance = sum((x - mean) ** 2 for x in data_points) / len(data_points)
    std_dev = math.sqrt(variance)
    return {"mean": mean, "std_dev": std_dev, "count": len(data_points)}

if __name__ == "__main__":
    sample = [12.4, 15.6, 9.8, 20.1, 14.5]
    print(json.dumps(analyze_telemetry(sample), indent=2))"""
            )
            createFile(
                projectId = proj3Id,
                name = "schema.sql",
                path = "database/schema.sql",
                language = "SQL",
                content = """CREATE TABLE IF NOT EXISTS projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(128) NOT NULL,
    status VARCHAR(32) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS build_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER REFERENCES projects(id) ON DELETE CASCADE,
    log_output TEXT NOT NULL,
    success BOOLEAN DEFAULT TRUE
);"""
            )
        }
    }
}

class ChatRepository(private val chatDao: ChatDao) {
    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun insertMessage(message: ChatMessageEntity): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessageEntity) {
        chatDao.updateMessage(message)
    }

    suspend fun updateMessageContent(id: Long, content: String) {
        chatDao.updateMessageContent(id, content)
    }

    suspend fun clearSession(sessionId: String) {
        chatDao.clearSession(sessionId)
    }

    suspend fun deleteMessage(id: Long) {
        chatDao.deleteMessage(id)
    }
}

class AgentRepository(private val agentRunDao: AgentRunDao) {
    val allRuns: Flow<List<AgentRunEntity>> = agentRunDao.getAllRuns()

    suspend fun insertRun(run: AgentRunEntity): Long {
        return agentRunDao.insertRun(run)
    }

    suspend fun updateRun(run: AgentRunEntity) {
        agentRunDao.updateRun(run)
    }

    suspend fun deleteRun(id: Long) {
        agentRunDao.deleteRunById(id)
    }
}
