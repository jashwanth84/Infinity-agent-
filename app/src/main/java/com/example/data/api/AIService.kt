package com.example.data.api

import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.AIModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class AIStreamChunk(
    val textChunk: String,
    val reasoningChunk: String? = null,
    val isFinished: Boolean = false
)

data class MessagePayload(
    val role: String,
    val text: String,
    val imageBase64: String? = null
)

class AIService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://integrate.api.nvidia.com/v1/chat/completions"

    private fun getApiKeyForModel(modelType: AIModelType): String {
        return when (modelType) {
            AIModelType.VISION_STUDIO -> BuildConfig.NV_KEY_KIMI
            AIModelType.ULTRA_PRO -> BuildConfig.NV_KEY_DEEPSEEK_PRO
            AIModelType.FLASH_TURBO -> BuildConfig.NV_KEY_DEEPSEEK_FLASH
            AIModelType.ARCHITECT -> BuildConfig.NV_KEY_MUSE
            AIModelType.CODE_FORGE -> BuildConfig.NV_KEY_LAGUNA
            AIModelType.GEMINI_PRO -> BuildConfig.GEMINI_API_KEY.ifEmpty { BuildConfig.NV_KEY_DEEPSEEK_PRO }
        }
    }

    fun streamChatCompletion(
        modelType: AIModelType,
        messages: List<MessagePayload>,
        systemPrompt: String? = null
    ): Flow<AIStreamChunk> = flow {
        val nvKey = getApiKeyForModel(modelType)
        val geminiKey = BuildConfig.GEMINI_API_KEY

        // Check if we should use Gemini API directly or if NV key is missing/default
        val shouldUseGemini = modelType == AIModelType.GEMINI_PRO || 
            ((nvKey.isBlank() || nvKey.startsWith("DEFAULT_")) && geminiKey.isNotBlank() && !geminiKey.startsWith("DEFAULT_"))

        if (shouldUseGemini && geminiKey.isNotBlank() && !geminiKey.startsWith("DEFAULT_")) {
            if (modelType != AIModelType.GEMINI_PRO && (nvKey.isBlank() || nvKey.startsWith("DEFAULT_"))) {
                emit(AIStreamChunk(textChunk = "*(Using Gemini 2.5 Flash as ${modelType.displayName} key is unconfigured)*\n\n"))
            }
            streamGemini(geminiKey, messages, systemPrompt).collect { emit(it) }
            return@flow
        }

        // Check if NV key is missing
        if (nvKey.isBlank() || nvKey.startsWith("DEFAULT_")) {
            emit(
                AIStreamChunk(
                    textChunk = "⚠️ **API Key Required**\n\nNo valid API key found for **${modelType.displayName}**.\n\nPlease configure your `GEMINI_API_KEY` or `NV_KEY_*` credentials in the **Secrets** panel in AI Studio.",
                    isFinished = true
                )
            )
            return@flow
        }

        val requestJson = JSONObject()
        val messagesArray = JSONArray()

        if (!systemPrompt.isNullOrBlank()) {
            val sysObj = JSONObject()
            sysObj.put("role", "system")
            sysObj.put("content", systemPrompt)
            messagesArray.put(sysObj)
        }

        for (msg in messages) {
            val msgObj = JSONObject()
            msgObj.put("role", msg.role)

            if (msg.imageBase64 != null && modelType.supportsVision) {
                val contentArray = JSONArray()
                val textObj = JSONObject().apply {
                    put("type", "text")
                    put("text", msg.text)
                }
                val imageObj = JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,${msg.imageBase64}")
                    })
                }
                contentArray.put(textObj)
                contentArray.put(imageObj)
                msgObj.put("content", contentArray)
            } else {
                msgObj.put("content", msg.text)
            }
            messagesArray.put(msgObj)
        }

        requestJson.put("model", modelType.modelId)
        requestJson.put("messages", messagesArray)
        requestJson.put("stream", true)
        requestJson.put("temperature", 0.7)
        requestJson.put("max_tokens", 8192)

        if (modelType == AIModelType.FLASH_TURBO) {
            val extraBody = JSONObject().apply {
                put("chat_template_kwargs", JSONObject().apply {
                    put("thinking", true)
                    put("reasoning_effort", "high")
                })
            }
            requestJson.put("extra_body", extraBody)
        } else if (modelType == AIModelType.ULTRA_PRO) {
            val extraBody = JSONObject().apply {
                put("chat_template_kwargs", JSONObject().apply {
                    put("thinking", true)
                })
            }
            requestJson.put("extra_body", extraBody)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $nvKey")
            .addHeader("Accept", "text/event-stream")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        var hasEmittedContent = false

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                val errorBody = response.body?.string()?.take(500) ?: response.message
                emit(
                    AIStreamChunk(
                        textChunk = "⚠️ **API Error (HTTP ${response.code})**: $errorBody\n\nPlease check your API credentials in the Secrets panel.",
                        isFinished = true
                    )
                )
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data: ")) {
                    val data = trimmed.substring(6).trim()
                    if (data == "[DONE]") {
                        emit(AIStreamChunk("", isFinished = true))
                        break
                    }
                    try {
                        val chunkObj = JSONObject(data)
                        val choices = chunkObj.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val delta = choice.optJSONObject("delta")
                            if (delta != null) {
                                val content = delta.optString("content", "")
                                val reasoning = when {
                                    delta.has("reasoning_content") -> delta.optString("reasoning_content")
                                    delta.has("reasoning") -> delta.optString("reasoning")
                                    else -> null
                                }
                                if (content.isNotEmpty() || reasoning != null) {
                                    hasEmittedContent = true
                                    emit(AIStreamChunk(textChunk = content, reasoningChunk = reasoning))
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore malformed intermediate chunks
                    }
                }
                line = reader.readLine()
            }
            if (!hasEmittedContent) {
                emit(AIStreamChunk(textChunk = "⚠️ Stream completed without response content.", isFinished = true))
            }
        } catch (e: Exception) {
            emit(AIStreamChunk(textChunk = "⚠️ Network Connection Error: ${e.localizedMessage ?: e.message}", isFinished = true))
        }
    }.flowOn(Dispatchers.IO)

    private fun streamGemini(
        apiKey: String,
        messages: List<MessagePayload>,
        systemPrompt: String?
    ): Flow<AIStreamChunk> = flow {
        val geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=$apiKey"
        val requestJson = JSONObject()

        if (!systemPrompt.isNullOrBlank()) {
            val sysObj = JSONObject()
            val sysParts = JSONArray().apply {
                put(JSONObject().apply { put("text", systemPrompt) })
            }
            sysObj.put("parts", sysParts)
            requestJson.put("systemInstruction", sysObj)
        }

        val contentsArray = JSONArray()
        for (msg in messages) {
            val contentObj = JSONObject()
            contentObj.put("role", if (msg.role == "assistant" || msg.role == "model") "model" else "user")
            val partsArray = JSONArray()

            if (msg.text.isNotEmpty()) {
                partsArray.put(JSONObject().apply { put("text", msg.text) })
            }

            if (msg.imageBase64 != null) {
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", msg.imageBase64)
                }
                partsArray.put(JSONObject().apply { put("inlineData", inlineData) })
            }

            if (partsArray.length() > 0) {
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }
        }
        requestJson.put("contents", contentsArray)

        val genConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 8192)
        }
        requestJson.put("generationConfig", genConfig)

        val request = Request.Builder()
            .url(geminiUrl)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        var hasEmittedContent = false

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                val errorBody = response.body?.string()?.take(500) ?: response.message
                emit(AIStreamChunk(textChunk = "⚠️ Gemini API Error (HTTP ${response.code}): $errorBody", isFinished = true))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data: ")) {
                    val data = trimmed.substring(6).trim()
                    try {
                        val chunkObj = JSONObject(data)
                        val candidates = chunkObj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null) {
                                for (i in 0 until parts.length()) {
                                    val part = parts.getJSONObject(i)
                                    if (part.has("text")) {
                                        val text = part.getString("text")
                                        if (text.isNotEmpty()) {
                                            hasEmittedContent = true
                                            emit(AIStreamChunk(textChunk = text))
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                line = reader.readLine()
            }
            if (!hasEmittedContent) {
                emit(AIStreamChunk(textChunk = "⚠️ Empty response received from Gemini API.", isFinished = true))
            }
        } catch (e: Exception) {
            emit(AIStreamChunk(textChunk = "⚠️ Gemini Connection Error: ${e.localizedMessage ?: e.message}", isFinished = true))
        }
    }

    suspend fun executeSinglePrompt(
        modelType: AIModelType,
        prompt: String,
        systemPrompt: String? = null
    ): String = withContext(Dispatchers.IO) {
        val nvKey = getApiKeyForModel(modelType)
        val geminiKey = BuildConfig.GEMINI_API_KEY

        val shouldUseGemini = modelType == AIModelType.GEMINI_PRO || 
            ((nvKey.isBlank() || nvKey.startsWith("DEFAULT_")) && geminiKey.isNotBlank() && !geminiKey.startsWith("DEFAULT_"))

        if (shouldUseGemini && geminiKey.isNotBlank() && !geminiKey.startsWith("DEFAULT_")) {
            return@withContext executeGeminiPrompt(geminiKey, prompt, systemPrompt)
        }

        if (nvKey.isBlank() || nvKey.startsWith("DEFAULT_")) {
            return@withContext "⚠️ API Key Required: Please configure NV_KEY or GEMINI_API_KEY in the Secrets panel."
        }

        val requestJson = JSONObject()
        val messagesArray = JSONArray()

        if (!systemPrompt.isNullOrBlank()) {
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        requestJson.put("model", modelType.modelId)
        requestJson.put("messages", messagesArray)
        requestJson.put("stream", false)
        requestJson.put("temperature", 0.7)
        requestJson.put("max_tokens", 4096)

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $nvKey")
            .addHeader("Accept", "application/json")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val json = JSONObject(response.body!!.string())
                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) {
                    val msg = choices.getJSONObject(0).getJSONObject("message")
                    return@withContext msg.optString("content", "")
                }
            } else {
                val err = response.body?.string()?.take(300) ?: response.message
                return@withContext "⚠️ API Error (HTTP ${response.code}): $err"
            }
        } catch (e: Exception) {
            return@withContext "⚠️ Network Error: ${e.localizedMessage ?: e.message}"
        }
        return@withContext "⚠️ No response returned from API."
    }

    private fun executeGeminiPrompt(
        apiKey: String,
        prompt: String,
        systemPrompt: String?
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val requestJson = JSONObject()

        if (!systemPrompt.isNullOrBlank()) {
            val sysObj = JSONObject()
            val sysParts = JSONArray().apply {
                put(JSONObject().apply { put("text", systemPrompt) })
            }
            sysObj.put("parts", sysParts)
            requestJson.put("systemInstruction", sysObj)
        }

        val contentsArray = JSONArray()
        val contentObj = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
            })
        }
        contentsArray.put(contentObj)
        requestJson.put("contents", contentsArray)

        val genConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 4096)
        }
        requestJson.put("generationConfig", genConfig)

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val json = JSONObject(response.body!!.string())
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "")
                    }
                }
                "⚠️ Empty candidate in Gemini response."
            } else {
                val err = response.body?.string()?.take(300) ?: response.message
                "⚠️ Gemini API Error (HTTP ${response.code}): $err"
            }
        } catch (e: Exception) {
            "⚠️ Gemini Connection Error: ${e.localizedMessage ?: e.message}"
        }
    }
}
