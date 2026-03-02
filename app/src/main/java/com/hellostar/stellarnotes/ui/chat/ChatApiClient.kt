package com.hellostar.stellarnotes.ui.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class ChatApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private class Acc(var id: String = "", var name: String = "", val args: StringBuilder = StringBuilder())

    private fun readBodyLimited(body: ResponseBody, maxBytes: Int): String? {
        body.byteStream().use { input ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return null
                out.write(buf, 0, read)
            }
            return out.toString(Charsets.UTF_8.name())
        }
    }

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

            val request = try {
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("URL 无效: $url"))
            }

            client.newCall(request).execute().use { response ->
                val rb = response.body

                if (!response.isSuccessful) {
                    val err = rb?.let { readBodyLimited(it, 64 * 1024) }?.take(260) ?: ""
                    return@withContext Result.failure(Exception("HTTP ${response.code} $err"))
                }

                if (rb == null) return@withContext Result.failure(Exception("空响应"))
                val len = rb.contentLength()
                if (len > 2_000_000) return@withContext Result.failure(Exception("响应过大，无法解析模型列表"))

                val body = readBodyLimited(rb, 2_000_000)
                    ?: return@withContext Result.failure(Exception("响应过大，无法解析模型列表"))

                if (body.isBlank()) return@withContext Result.failure(Exception("空响应"))

                // 稳健策略：仅提取 "id" 字段，避免 provider 返回结构不一致导致崩溃
                val models = linkedSetOf<String>()
                val idRegex = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
                idRegex.findAll(body).forEach { m ->
                    m.groupValues.getOrNull(1)
                        ?.takeIf { it.isNotBlank() && it.length <= 80 }
                        ?.let(models::add)
                }

                // 保留较大上限，支持 130+ 模型提供商
                val cleaned = models.toList().sorted().take(500)
                return@withContext if (cleaned.isEmpty()) {
                    Result.failure(Exception("未解析到任何模型"))
                } else {
                    Result.success(cleaned)
                }
            }
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "获取模型失败"))
        }
    }

    suspend fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        tools: List<Tool>? = null,
        onThinking: (String) -> Unit,
        onContent: (String) -> Unit
    ): StreamResult = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl.trimEnd('/') + "/v1/chat/completions"
            val req = ChatRequest(model = model, messages = messages, tools = if (tools.isNullOrEmpty()) null else tools)
            val body = json.encodeToString(req)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext StreamResult.Error("HTTP ${response.code}: ${response.body?.string()?.take(300) ?: "Unknown"}")
            }

            val reader = response.body?.charStream()?.buffered()
                ?: return@withContext StreamResult.Error("Empty response")

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
                                tc.id?.let { a.id = it }
                                tc.function?.name?.let { a.name = it }
                                tc.function?.arguments?.let { a.args.append(it) }
                            }
                            if (choice.finishReason == "tool_calls") {
                                return@withContext StreamResult.ToolCalls(
                                    tcMap.values.map { AccumulatedToolCall(it.id, it.name, it.args.toString()) }
                                )
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            StreamResult.Done
        } catch (e: Exception) {
            StreamResult.Error(e.message ?: "Unknown error")
        }
    }
}
