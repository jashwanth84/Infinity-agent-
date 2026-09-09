package com.example.data.model

enum class AIModelType(
    val modelId: String,
    val displayName: String,
    val badge: String,
    val description: String,
    val supportsVision: Boolean,
    val isReasoning: Boolean = false
) {
    ULTRA_PRO(
        modelId = "deepseek-ai/deepseek-v4-pro-0813",
        displayName = "Infinity Ultra",
        badge = "Reasoning Pro",
        description = "Deep architectural thinking and algorithmic reasoning",
        supportsVision = false,
        isReasoning = true
    ),
    FLASH_TURBO(
        modelId = "deepseek-ai/deepseek-v4-flash-0731",
        displayName = "Infinity Flash",
        badge = "Turbo Velocity",
        description = "Instantaneous code generation, refactors, and error diagnostics",
        supportsVision = false,
        isReasoning = true
    ),
    VISION_STUDIO(
        modelId = "moonshotai/kimi-k3",
        displayName = "Infinity Vision",
        badge = "Multimodal",
        description = "Visual comprehension, UI wireframes, and image-to-code",
        supportsVision = true
    ),
    CODE_FORGE(
        modelId = "poolside/laguna-xs-2.1",
        displayName = "Infinity Forge",
        badge = "Code Specialist",
        description = "Strict typing, performance optimization, and syntax precision",
        supportsVision = false
    ),
    ARCHITECT(
        modelId = "meta/muse-glimmer-30b",
        displayName = "Infinity Architect",
        badge = "Systems Design",
        description = "Cross-language engineering, schemas, and complex workflows",
        supportsVision = false
    )
}

enum class InternalToolType(val toolName: String, val description: String, val isDestructive: Boolean = false) {
    LIST_FILES("list files", "List all project and workspace files"),
    READ_FILE("read file", "Read contents of a specific file"),
    SEARCH_FILES("search files", "Search code patterns across files"),
    CREATE_FILE("create file", "Create a new file in workspace"),
    EDIT_FILE("edit file", "Update content of an existing file"),
    RENAME_FILE("rename file", "Rename or move a file"),
    CREATE_FOLDER("create folder", "Create a directory in project"),
    DELETE_FILE("delete file", "Permanently remove a file", isDestructive = true)
}

enum class AppMode(val title: String, val subtitle: String) {
    CHAT("Chat", "Conversational AI pair programmer"),
    CODER("Coder", "Action-driven code workspace"),
    AGENT("Agent", "Autonomous Analyze-to-Apply loop"),
    MULTI_AGENT("Multi-Agent", "Collaborative multi-model swarm")
}

enum class WorkspaceTab(val title: String) {
    PROJECTS("Projects"),
    FILES("Files"),
    HISTORY("History")
}

enum class CodeAction(val label: String, val promptPrefix: String) {
    GENERATE("Generate", "Generate clean, production-ready code for: "),
    EXPLAIN("Explain", "Explain the following code clearly, detailing its architecture and logic:\n\n"),
    DEBUG("Debug", "Identify all bugs, edge cases, and potential failures in this code and provide corrected code:\n\n"),
    REFACTOR("Refactor", "Refactor the following code for better readability, modularity, and adherence to clean architecture:\n\n"),
    REVIEW("Review", "Perform an in-depth code review focusing on security, performance, memory leaks, and idioms:\n\n"),
    OPTIMIZE("Optimize", "Optimize the following code for maximum speed, concurrency, and minimal memory usage:\n\n")
}

enum class SupportedLanguage(
    val displayName: String,
    val extension: String,
    val defaultSample: String
) {
    JAVA(
        "Java",
        "java",
        """public class MathUtils {
    public static int fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}"""
    ),
    CPP(
        "C++",
        "cpp",
        """#include <iostream>
#include <vector>
#include <numeric>

double calculateAverage(const std::vector<double>& values) {
    if (values.empty()) return 0.0;
    double sum = std::accumulate(values.begin(), values.end(), 0.0);
    return sum / values.size();
}"""
    ),
    KOTLIN(
        "Kotlin",
        "kt",
        """data class User(val id: String, val name: String)

fun processUsers(users: List<User>): Map<String, String> {
    return users.associate { it.id to it.name }
}"""
    ),
    XML(
        "XML",
        "xml",
        """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    <TextView
        android:id="@+id/titleText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Infinity Agent" />
</LinearLayout>"""
    ),
    PYTHON(
        "Python",
        "py",
        """def quick_sort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[len(arr) // 2]
    left = [x for x in arr if x < pivot]
    middle = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]
    return quick_sort(left) + middle + quick_sort(right)"""
    ),
    JAVASCRIPT(
        "JavaScript",
        "js",
        """async function fetchAndFilter(url) {
    const res = await fetch(url);
    const data = await res.json();
    return data.filter(item => item.active);
}"""
    ),
    TYPESCRIPT(
        "TypeScript",
        "ts",
        """interface AgentTask<T> {
    id: string;
    payload: T;
    timestamp: number;
}

function processTask<T>(task: AgentTask<T>): boolean {
    return task.id.length > 0;
}"""
    ),
    HTML(
        "HTML",
        "html",
        """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Infinity Agent</title>
</head>
<body>
    <h1>Welcome to Infinity Agent</h1>
</body>
</html>"""
    ),
    CSS(
        "CSS",
        "css",
        """.glass-card {
    background: rgba(255, 255, 255, 0.08);
    backdrop-filter: blur(12px);
    border-radius: 16px;
    border: 1px solid rgba(255, 255, 255, 0.15);
}"""
    ),
    SQL(
        "SQL",
        "sql",
        """SELECT u.id, u.username, COUNT(p.id) AS project_count
FROM users u
LEFT JOIN projects p ON u.id = p.user_id
GROUP BY u.id, u.username
ORDER BY project_count DESC;"""
    ),
    JSON(
        "JSON",
        "json",
        """{
  "name": "Infinity Agent",
  "version": "1.0.0",
  "capabilities": ["multi_agent", "code_review", "diff_viewer"]
}"""
    )
}

data class FileDiff(
    val filePath: String,
    val originalContent: String,
    val proposedContent: String,
    val changeDescription: String
)

data class AttachedFileRef(
    val name: String,
    val path: String,
    val content: String,
    val isLocalDevice: Boolean = false,
    val realUri: String? = null
)
