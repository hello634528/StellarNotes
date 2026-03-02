package com.hellostar.stellarnotes.ui.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ChatApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private class Acc(var id: String = "", var name: String = "", val args: StringBuilder = StringBuilder())

    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val urlInput = baseUrl.trim()
            if (urlInput.isBlank()) return@withContext Result.failure(Exception("Base URL 为空"))
            if (apiKey.isBlank()) return@withContext Result.failure(Exception("API Key 为空"))

            val normalizedBase = if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) {
                urlInput
            } else {
                "https://$urlInput"
            }
            val url = normalizedBase.trimEnd('/') + "/v1/models"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string()?.take(200) ?: ""
                return@withContext Result.failure(Exception("HTTP ${response.code} $err"))
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return@withContext Result.failure(Exception("空响应"))

            // 兼容多种返回格式，避免解析异常导致闪退
            val models = mutableListOf<String>()
            runCatching {
                val root = json.parseToJsonElement(body).jsonObject
                // OpenAI: { data:[{id:"..."}] }
                root["data"]?.jsonArray?.forEach { item ->
                    item.jsonObject["id"]?.jsonPrimitive?.contentOrNull?.let { if (it.isNotBlank()) models.add(it) }
                }
                // Some providers: { models:["a","b"] } or { models:[{id:"..."}] }
                root["models"]?.jsonArray?.forEach { item ->
                    val v = runCatching { item.jsonPrimitive.contentOrNull }.getOrNull()
                    if (!v.isNullOrBlank()) models.add(v)
                    runCatching { item.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.getOrNull()?.let { if (!it.isNullOrBlank()) models.add(it) }
                }
            }

            // fallback regex
            if (models.isEmpty()) {
                val regex = Regex("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                regex.findAll(body).forEach { m ->
                    m.groupValues.getOrNull(1)?.let { id -> if (id.isNotBlank()) models.add(id) }
                }
            }

            val cleaned = models.distinct().sorted()
            if (cleaned.isEmpty()) Result.failure(Exception("未解析到任何模型")) else Result.success(cleaned)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "获取模型失败"))
        }
    }

    suspend fun streamChat(
        baseUrl: String, apiKey: String, model: String,
        messages: List<ChatMessage>, tools: List<Tool>? = null,
        onThinking: (String) -> Unit, onContent: (String) -> Unit
    ): StreamResult = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl.trimEnd('/') + "/v1/chat/completions"
            val req = ChatRequest(model = model, messages = messages, tools = if (tools.isNullOrEmpty()) null else tools)
            val body = json.encodeToString(req)
            val request = Request.Builder().url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext StreamResult.Error("HTTP ${response.code}: ${response.body?.string()?.take(300) ?: "Unknown"}")
            val reader = response.body?.charStream()?.buffered() ?: return@withContext StreamResult.Error("Empty response")
            val tcMap = mutableMapOf<Int, Acc>()
            reader.useLines { lines ->
                for (line in lines) {
                    if (!line.startsWith("data: ")) continue
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val chunk = json.decodeFromString<StreamChunk>(data)
                        for (choice in chunk.choices) {
                            choice.delta?.reasoningContent?.let { onThinking(it) }
                            choice.delta?.content?.let { onContent(it) }
                            choice.delta?.toolCalls?.forEach { tc ->
                                val a = tcMap.getOrPut(tc.index) { Acc() }
                                tc.id?.let { a.id = it }; tc.function?.name?.let { a.name = it }; tc.function?.arguments?.let { a.args.append(it) }
                            }
                            if (choice.finishReason == "tool_calls") return@withContext StreamResult.ToolCalls(tcMap.values.map { AccumulatedToolCall(it.id, it.name, it.args.toString()) })
                        }
                    } catch (_: Exception) {}
                }
            }
            StreamResult.Done
        } catch (e: Exception) { StreamResult.Error(e.message ?: "Unknown error") }
    }
}
