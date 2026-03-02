package com.hellostar.stellarnotes.ui.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostar.stellarnotes.data.Note
import kotlinx.coroutines.launch

data class Conversation(val id: Long = System.currentTimeMillis(), var name: String = "\u65b0\u5bf9\u8bdd", val messages: MutableList<UiMessage> = mutableListOf(), var contextNote: Note? = null)
data class UiMessage(val role: String, val content: String, val thinking: String = "")

class ChatViewModel : ViewModel() {
    private val api = ChatApiClient()
    val conversations = mutableStateListOf<Conversation>()
    val currentConvId = mutableStateOf<Long?>(null)
    val isStreaming = mutableStateOf(false)
    val streamContent = mutableStateOf("")
    val streamThinking = mutableStateOf("")
    val currentConversation: Conversation? get() = conversations.find { it.id == currentConvId.value }

    fun newConversation(contextNote: Note? = null): Long { val conv = Conversation(contextNote = contextNote); conversations.add(0, conv); currentConvId.value = conv.id; return conv.id }
    fun selectConversation(id: Long) { currentConvId.value = id }
    fun renameConversation(id: Long, name: String) { conversations.find { it.id == id }?.name = name.ifBlank { "\u65b0\u5bf9\u8bdd" } }
    fun deleteConversation(id: Long) { conversations.removeAll { it.id == id }; if (currentConvId.value == id) currentConvId.value = conversations.firstOrNull()?.id }

    fun sendMessage(text: String, baseUrl: String, apiKey: String, model: String, enableThinking: Boolean) {
        val conv = currentConversation ?: return; if (text.isBlank()) return
        conv.messages.add(UiMessage(role = "user", content = text))
        if (conv.messages.size == 1 && conv.name == "\u65b0\u5bf9\u8bdd") conv.name = text.take(20)
        isStreaming.value = true; streamContent.value = ""; streamThinking.value = ""
        viewModelScope.launch {
            val apiMessages = mutableListOf<ChatMessage>()
            val sys = buildString { append("You are a helpful AI assistant in \u661f\u6f9c\u7b14\u8bb0 (NebulaNotes). Respond in the user's language.")
                conv.contextNote?.let { append("\n\n\u7528\u6237\u6b63\u5728\u67e5\u770b\u7b14\u8bb0:\n\u6807\u9898: ${it.title}\n\u5185\u5bb9: ${it.content}") }
                if (enableThinking) append("\nPlease think step by step.") }
            apiMessages.add(ChatMessage("system", sys)); for (msg in conv.messages) apiMessages.add(ChatMessage(msg.role, msg.content))
            val thinkBuf = StringBuilder(); val contentBuf = StringBuilder()
            api.streamChat(baseUrl, apiKey, model, apiMessages,
                onThinking = { thinkBuf.append(it); streamThinking.value = thinkBuf.toString() },
                onContent = { contentBuf.append(it); streamContent.value = contentBuf.toString() },
                onError = { conv.messages.add(UiMessage("assistant", "\u2757 $it")); isStreaming.value = false },
                onDone = { val fc = contentBuf.toString().ifBlank { thinkBuf.toString() }; conv.messages.add(UiMessage("assistant", fc, thinkBuf.toString())); isStreaming.value = false; streamContent.value = ""; streamThinking.value = "" })
        }
    }
}
