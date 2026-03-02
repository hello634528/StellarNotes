package com.hellostar.stellarnotes.ui.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostar.stellarnotes.data.Note
import com.hellostar.stellarnotes.data.NoteRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Conversation(
    val id: Long = System.currentTimeMillis(),
    var name: String = "\u65b0\u5bf9\u8bdd",
    val messages: MutableList<UiMessage> = mutableListOf(),
    val contextNotes: MutableList<Note> = mutableListOf()
)

data class UiMessage(
    val role: String,
    val content: String,
    val thinking: String = "",
    val toolAction: String = ""
)

class ChatViewModel : ViewModel() {
    private val api = ChatApiClient()
    private val pJson = Json { ignoreUnknownKeys = true }

    var repo: NoteRepository? = null

    val conversations = mutableStateListOf<Conversation>()
    val currentConvId = mutableStateOf<Long?>(null)
    val isStreaming = mutableStateOf(false)
    val streamContent = mutableStateOf("")
    val streamThinking = mutableStateOf("")

    val currentConversation: Conversation?
        get() = conversations.find { it.id == currentConvId.value }

    fun newConversation(contextNote: Note? = null): Long {
        val conv = Conversation()
        if (contextNote != null) conv.contextNotes.add(contextNote)
        conversations.add(0, conv)
        currentConvId.value = conv.id
        return conv.id
    }

    fun addContextNote(note: Note) {
        val conv = currentConversation ?: return
        if (conv.contextNotes.none { it.id == note.id }) conv.contextNotes.add(note)
    }

    fun removeContextNote(noteId: Long) {
        currentConversation?.contextNotes?.removeAll { it.id == noteId }
    }

    fun selectConversation(id: Long) { currentConvId.value = id }
    fun deleteConversation(id: Long) { conversations.removeAll { it.id == id }; if (currentConvId.value == id) currentConvId.value = conversations.firstOrNull()?.id }

    private val tools = listOf(
        Tool(function = FunctionDef(
            name = "create_note",
            description = "\u521b\u5efa\u4e00\u7bc7\u65b0\u7b14\u8bb0",
            parameters = FunctionParams(
                properties = mapOf(
                    "title" to ParamProp("string", "\u7b14\u8bb0\u6807\u9898"),
                    "content" to ParamProp("string", "\u7b14\u8bb0\u5185\u5bb9")
                ),
                required = listOf("title", "content")
            )
        )),
        Tool(function = FunctionDef(
            name = "edit_note",
            description = "\u4fee\u6539/\u6da6\u8272\u4e00\u7bc7\u5df2\u6709\u7b14\u8bb0\u7684\u6807\u9898\u6216\u5185\u5bb9",
            parameters = FunctionParams(
                properties = mapOf(
                    "note_id" to ParamProp("number", "\u8981\u4fee\u6539\u7684\u7b14\u8bb0ID"),
                    "title" to ParamProp("string", "\u65b0\u6807\u9898\uff0c\u4e0d\u6539\u5219\u7559\u7a7a"),
                    "content" to ParamProp("string", "\u65b0\u5185\u5bb9\uff0c\u4e0d\u6539\u5219\u7559\u7a7a")
                ),
                required = listOf("note_id")
            )
        ))
    )

    private fun buildSystemPrompt(conv: Conversation): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (EEEE)", Locale.getDefault())
        val noteFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return buildString {
            append("You are a helpful AI assistant in \u661f\u6f9c\u7b14\u8bb0 (NebulaNotes). Respond in the user's language.\n")
            append("\u5f53\u524d\u65f6\u95f4: ${fmt.format(Date())}\n\n")
            if (conv.contextNotes.isNotEmpty()) {
                append("=== \u7528\u6237\u9644\u52a0\u7684\u7b14\u8bb0\u4e0a\u4e0b\u6587 (${conv.contextNotes.size}\u7bc7) ===\n")
                for (note in conv.contextNotes) {
                    append("--- \u7b14\u8bb0 #${note.id} ---\n")
                    append("\u6807\u9898: ${note.title}\n")
                    append("\u521b\u5efa: ${noteFmt.format(Date(note.createdAt))}\n")
                    append("\u66f4\u65b0: ${noteFmt.format(Date(note.updatedAt))}\n")
                    append("\u661f\u6807: ${if (note.starred) "\u2b50 \u662f" else "\u5426"}\n")
                    append("\u5185\u5bb9:\n${note.content}\n\n")
                }
                append("=== \u7b14\u8bb0\u4e0a\u4e0b\u6587\u7ed3\u675f ===\n\n")
            }
            append("\u4f60\u53ef\u4ee5\u4f7f\u7528\u5de5\u5177\u6765\u5e2e\u7528\u6237\u521b\u5efa\u65b0\u7b14\u8bb0\u6216\u4fee\u6539\u5df2\u6709\u7b14\u8bb0\u3002\u53ea\u5728\u7528\u6237\u660e\u786e\u8981\u6c42\u65f6\u624d\u8c03\u7528\u5de5\u5177\u3002\n")
        }
    }

    fun sendMessage(text: String, baseUrl: String, apiKey: String, model: String, enableThinking: Boolean) {
        val conv = currentConversation ?: return
        if (text.isBlank()) return
        conv.messages.add(UiMessage(role = "user", content = text))
        if (conv.messages.count { it.role == "user" } == 1 && conv.name == "\u65b0\u5bf9\u8bdd") conv.name = text.take(20)
        doStream(conv, baseUrl, apiKey, model)
    }

    private fun doStream(conv: Conversation, baseUrl: String, apiKey: String, model: String) {
        isStreaming.value = true; streamContent.value = ""; streamThinking.value = ""
        viewModelScope.launch {
            val apiMessages = mutableListOf<ChatMessage>()
            apiMessages.add(ChatMessage("system", buildSystemPrompt(conv)))
            for (msg in conv.messages) {
                when (msg.role) {
                    "user" -> apiMessages.add(ChatMessage("user", msg.content))
                    "assistant" -> apiMessages.add(ChatMessage("assistant", msg.content))
                    "tool_result" -> {} // skip UI-only messages
                }
            }

            val thinkBuf = StringBuilder(); val contentBuf = StringBuilder()
            val result = api.streamChat(
                baseUrl, apiKey, model, apiMessages, tools,
                onThinking = { thinkBuf.append(it); streamThinking.value = thinkBuf.toString() },
                onContent = { contentBuf.append(it); streamContent.value = contentBuf.toString() }
            )

            when (result) {
                is StreamResult.Done -> {
                    val fc = contentBuf.toString().ifBlank { thinkBuf.toString() }
                    conv.messages.add(UiMessage("assistant", fc, thinkBuf.toString()))
                    isStreaming.value = false; streamContent.value = ""; streamThinking.value = ""
                }
                is StreamResult.Error -> {
                    conv.messages.add(UiMessage("assistant", "\u2757 ${result.message}"))
                    isStreaming.value = false; streamContent.value = ""; streamThinking.value = ""
                }
                is StreamResult.ToolCalls -> {
                    // Show what AI is doing
                    if (contentBuf.isNotBlank()) {
                        conv.messages.add(UiMessage("assistant", contentBuf.toString(), thinkBuf.toString()))
                    }
                    // Execute tools
                    for (tc in result.calls) {
                        val toolResult = executeTool(tc)
                        conv.messages.add(UiMessage("tool_result", toolResult, toolAction = "\uD83D\uDEE0 ${tc.name}"))
                    }
                    // Build follow-up messages including tool results
                    val followUp = mutableListOf<ChatMessage>()
                    followUp.add(ChatMessage("system", buildSystemPrompt(conv)))
                    for (msg in conv.messages) {
                        when (msg.role) {
                            "user" -> followUp.add(ChatMessage("user", msg.content))
                            "assistant" -> followUp.add(ChatMessage("assistant", msg.content))
                            "tool_result" -> followUp.add(ChatMessage("user", "[\u5de5\u5177\u6267\u884c\u7ed3\u679c] ${msg.content}"))
                        }
                    }
                    followUp.add(ChatMessage("user", "\u5de5\u5177\u5df2\u6267\u884c\u5b8c\u6bd5\uff0c\u8bf7\u7528\u81ea\u7136\u8bed\u8a00\u544a\u77e5\u7528\u6237\u7ed3\u679c\u3002"))
                    // Re-stream for final response
                    streamContent.value = ""; streamThinking.value = ""
                    val thinkBuf2 = StringBuilder(); val contentBuf2 = StringBuilder()
                    val result2 = api.streamChat(
                        baseUrl, apiKey, model, followUp, null,
                        onThinking = { thinkBuf2.append(it); streamThinking.value = thinkBuf2.toString() },
                        onContent = { contentBuf2.append(it); streamContent.value = contentBuf2.toString() }
                    )
                    val finalContent = when (result2) {
                        is StreamResult.Done -> contentBuf2.toString().ifBlank { "\u2705 \u5de5\u5177\u5df2\u6267\u884c" }
                        is StreamResult.Error -> "\u2757 ${result2.message}"
                        else -> contentBuf2.toString().ifBlank { "\u2705 \u5b8c\u6210" }
                    }
                    conv.messages.add(UiMessage("assistant", finalContent, thinkBuf2.toString()))
                    isStreaming.value = false; streamContent.value = ""; streamThinking.value = ""
                }
            }
        }
    }

    private suspend fun executeTool(tc: AccumulatedToolCall): String {
        val r = repo ?: return "\u2757 \u7b14\u8bb0\u4ed3\u5e93\u672a\u521d\u59cb\u5316"
        return try {
            val args = pJson.parseToJsonElement(tc.arguments).jsonObject
            when (tc.name) {
                "create_note" -> {
                    val title = args["title"]?.jsonPrimitive?.content ?: "\u65e0\u6807\u9898"
                    val content = args["content"]?.jsonPrimitive?.content ?: ""
                    val now = System.currentTimeMillis()
                    val id = r.create(Note(title = title, content = content, createdAt = now, updatedAt = now))
                    "\u2705 \u5df2\u521b\u5efa\u7b14\u8bb0 \"$title\" (ID: $id)"
                }
                "edit_note" -> {
                    val noteId = args["note_id"]?.jsonPrimitive?.content?.toLongOrNull() ?: return "\u2757 \u65e0\u6548\u7684\u7b14\u8bb0ID"
                    val existing = r.getById(noteId) ?: return "\u2757 \u627e\u4e0d\u5230 ID=$noteId \u7684\u7b14\u8bb0"
                    val newTitle = args["title"]?.jsonPrimitive?.content
                    val newContent = args["content"]?.jsonPrimitive?.content
                    val updated = existing.copy(
                        title = newTitle ?: existing.title,
                        content = newContent ?: existing.content,
                        updatedAt = System.currentTimeMillis()
                    )
                    r.upsert(updated)
                    // Also update context if this note is in context
                    currentConversation?.contextNotes?.let { list ->
                        val idx = list.indexOfFirst { it.id == noteId }
                        if (idx >= 0) list[idx] = updated
                    }
                    "\u2705 \u5df2\u4fee\u6539\u7b14\u8bb0 \"${updated.title}\" (ID: $noteId)"
                }
                else -> "\u2757 \u672a\u77e5\u5de5\u5177: ${tc.name}"
            }
        } catch (e: Exception) {
            "\u2757 \u5de5\u5177\u6267\u884c\u5931\u8d25: ${e.message}"
        }
    }
}
