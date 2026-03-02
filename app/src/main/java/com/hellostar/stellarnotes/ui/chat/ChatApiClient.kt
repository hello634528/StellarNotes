package com.hellostar.stellarnotes.ui.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            val url = baseUrl.trimEnd('/') + "/v1/models"
            val request = Request.Builder().url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty"))
            val models = json.decodeFromString<ModelsResponse>(body)
            Result.success(models.data.map { it.id }.sorted())
        } catch (e: Exception) { Result.failure(e) }
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
