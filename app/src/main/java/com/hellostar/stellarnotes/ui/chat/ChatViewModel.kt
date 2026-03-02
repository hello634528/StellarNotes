package com.hellostar.stellarnotes.ui.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostar.stellarnotes.data.Note
import kotlinx.coroutines.launch

data class Conversation(
    val id: Long = System.currentTimeMillis(),
    var name: String = "\u65b0\u5bf9\u8bdd",
    val messages: MutableList<UiMessage> = mutableListOf(),
    var contextNote: Note? = null
)

data class UiMessage(
    val role: String,
    val content: String,
    val thinking: String = ""
)

class ChatViewModel : ViewModel() {
    private val api = ChatApiClient()

    val conversations = mutableStateListOf<Conversation>()
    val currentConvId = mutableStateOf<Long?>(null)
    val isStreaming = mutableStateOf(false)
    val streamContent = mutableStateOf("")
    val streamThinking = mutableStateOf("")

    val currentConversation: Conversation?
        get() = conversations.find { it.id == currentConvId.value }

    fun newConversation(contextNote: Note? = null): Long {
        val conv = Conversation(contextNote = contextNote)
        conversations.add(0, conv)
        currentConvId.value = conv.id
        return conv.id
    }

    fun selectConversation(id: Long) {
        currentConvId.value = id
    }

    fun deleteConversation(id: Long) {
        conversations.removeAll { it.id == id }
        if (currentConvId.value == id) {
            currentConvId.value = conversations.firstOrNull()?.id
        }
    }

    fun sendMessage(
        text: String, baseUrl: String, apiKey: String, model: String, enableThinking: Boolean
    ) {
        val conv = currentConversation ?: return
        if (text.isBlank()) return
        conv.messages.add(UiMessage(role = "user", content = text))
        if (conv.messages.size == 1 && conv.name == "\u65b0\u5bf9\u8bdd") conv.name = text.take(20)
        isStreaming.value = true
        streamContent.value = ""
        streamThinking.value = ""

        viewModelScope.launch {
            val apiMessages = mutableListOf<ChatMessage>()
            val systemPrompt = buildString {
                append("You are a helpful AI assistant in NebulaNotes.")
                conv.contextNote?.let { n ->
                    append("\n\nUser's note context:\nTitle: ${n.title}\nContent: ${n.content}")
                }
                if (enableThinking) append("\nPlease think step by step.")
            }
            apiMessages.add(ChatMessage("system", systemPrompt))
            for (msg in conv.messages) apiMessages.add(ChatMessage(msg.role, msg.content))

            val thinkBuf = StringBuilder()
            val contentBuf = StringBuilder()

            api.streamChat(
                baseUrl = baseUrl, apiKey = apiKey, model = model, messages = apiMessages,
                onThinking = { thinkBuf.append(it); streamThinking.value = thinkBuf.toString() },
                onContent = { contentBuf.append(it); streamContent.value = contentBuf.toString() },
                onError = { conv.messages.add(UiMessage("assistant", "\u2757 $it")); isStreaming.value = false },
                onDone = {
                    val final0 = contentBuf.toString().ifBlank { thinkBuf.toString() }
                    conv.messages.add(UiMessage("assistant", final0, thinkBuf.toString()))
                    isStreaming.value = false; streamContent.value = ""; streamThinking.value = ""
                }
            )
        }
    }
}
