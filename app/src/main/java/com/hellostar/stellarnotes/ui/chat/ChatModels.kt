package com.hellostar.stellarnotes.ui.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class StreamChunk(
    val choices: List<StreamChoice> = emptyList()
)

@Serializable
data class StreamChoice(
    val delta: StreamDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class StreamDelta(
    val content: String? = null,
    val role: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null
)
