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

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        onThinking: (String) -> Unit,
        onContent: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl.trimEnd('/') + "/v1/chat/completions"
            val body = json.encodeToString(ChatRequest(model = model, messages = messages))
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                onError("HTTP ${response.code}: ${response.body?.string()?.take(200) ?: "Unknown error"}")
                return@withContext
            }

            val reader = response.body?.charStream()?.buffered()
                ?: run { onError("Empty response"); return@withContext }

            reader.useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val chunk = json.decodeFromString<StreamChunk>(data)
                            for (choice in chunk.choices) {
                                choice.delta?.reasoningContent?.let { onThinking(it) }
                                choice.delta?.content?.let { onContent(it) }
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}
