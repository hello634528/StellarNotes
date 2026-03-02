package com.hellostar.stellarnotes.ui.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7,
    val tools: List<Tool>? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallRef>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null
)

@Serializable
data class Tool(val type: String = "function", val function: FunctionDef)
@Serializable
data class FunctionDef(val name: String, val description: String, val parameters: FunctionParams)
@Serializable
data class FunctionParams(val type: String = "object", val properties: Map<String, ParamProp>, val required: List<String> = emptyList())
@Serializable
data class ParamProp(val type: String, val description: String = "")
@Serializable
data class ToolCallRef(val id: String, val type: String = "function", val function: ToolCallFn)
@Serializable
data class ToolCallFn(val name: String, val arguments: String)

@Serializable
data class StreamChunk(val choices: List<StreamChoice> = emptyList())
@Serializable
data class StreamChoice(val delta: StreamDelta? = null, @SerialName("finish_reason") val finishReason: String? = null)
@Serializable
data class StreamDelta(
    val content: String? = null,
    val role: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    @SerialName("tool_calls") val toolCalls: List<StreamToolCallDelta>? = null
)
@Serializable
data class StreamToolCallDelta(val index: Int = 0, val id: String? = null, val type: String? = null, val function: StreamFnDelta? = null)
@Serializable
data class StreamFnDelta(val name: String? = null, val arguments: String? = null)

data class AccumulatedToolCall(val id: String, val name: String, val arguments: String)

sealed class StreamResult {
    object Done : StreamResult()
    data class ToolCalls(val calls: List<AccumulatedToolCall>) : StreamResult()
    data class Error(val message: String) : StreamResult()
}
