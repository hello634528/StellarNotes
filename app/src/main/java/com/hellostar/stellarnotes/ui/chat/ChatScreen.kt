package com.hellostar.stellarnotes.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hellostar.stellarnotes.data.Note
import kotlinx.coroutines.launch

private val Deep = Color(0xFF040A1E)
private val Blue = Color(0xFF5A9DFF)
private val UserBubble = Color(0xFF1A3A6A)
private val AiBubble = Color(0xFF0E1E38)

@Composable
fun ChatScreen(
    chatVm: ChatViewModel,
    notes: List<Note>,
    baseUrl: String,
    apiKey: String,
    model: String,
    enableThinking: Boolean,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (chatVm.conversations.isEmpty()) chatVm.newConversation()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF080F22)) {
                DrawerContent(
                    conversations = chatVm.conversations,
                    currentId = chatVm.currentConvId.value,
                    onSelect = { chatVm.selectConversation(it); scope.launch { drawerState.close() } },
                    onNew = { chatVm.newConversation(); scope.launch { drawerState.close() } },
                    onDelete = { chatVm.deleteConversation(it) },
                    onSettings = { scope.launch { drawerState.close() }; onOpenSettings() }
                )
            }
        }
    ) {
        Column(Modifier.fillMaxSize().background(Deep)) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF060D20))
                    .padding(top = 40.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = Color.White) }
                Text(
                    chatVm.currentConversation?.name ?: "AI \u5bf9\u8bdd",
                    color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (apiKey.isBlank()) {
                    Text(
                        "\u672a\u914d\u7f6e", color = Color(0xFFFF6B6B), fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33FF6B6B))
                            .padding(horizontal = 8.dp, vertical = 2.dp).clickable { onOpenSettings() }
                    )
                }
            }

            val conv = chatVm.currentConversation
            val listState = rememberLazyListState()
            val count = (conv?.messages?.size ?: 0) + (if (chatVm.isStreaming.value) 1 else 0)
            LaunchedEffect(count) { if (count > 0) listState.animateScrollToItem(count - 1) }

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(), state = listState,
                contentPadding = PaddingValues(12.dp, 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (conv != null) {
                    conv.contextNote?.let { note ->
                        item {
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A1530)).padding(12.dp)) {
                                Column {
                                    Text("\uD83D\uDCCB \u7b14\u8bb0\u4e0a\u4e0b\u6587", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(note.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    if (note.content.isNotBlank()) Text(
                                        note.content.take(100) + if (note.content.length > 100) "..." else "",
                                        color = Color(0x88FFFFFF), fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    items(conv.messages) { msg -> Bubble(msg) }
                    if (chatVm.isStreaming.value) {
                        item { StreamBubble(chatVm.streamThinking.value, chatVm.streamContent.value) }
                    }
                }
                if (conv == null || (conv.messages.isEmpty() && !chatVm.isStreaming.value)) {
                    item {
                        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("\u2728", fontSize = 40.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("\u5f00\u59cb\u5bf9\u8bdd", color = Color(0x66FFFFFF), fontSize = 16.sp)
                                if (apiKey.isBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("\u8bf7\u5148\u5728\u8bbe\u7f6e\u4e2d\u914d\u7f6e API", color = Color(0xFFFF6B6B), fontSize = 13.sp,
                                        modifier = Modifier.clickable { onOpenSettings() })
                                }
                            }
                        }
                    }
                }
            }

            InputBar(
                enabled = !chatVm.isStreaming.value && apiKey.isNotBlank(),
                onSend = { chatVm.sendMessage(it, baseUrl, apiKey, model, enableThinking) }
            )
        }
    }
}

@Composable
private fun Bubble(msg: UiMessage) {
    val isUser = msg.role == "user"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        if (msg.thinking.isNotBlank() && !isUser) {
            var exp by remember { mutableStateOf(false) }
            Column(
                Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A1225)).clickable { exp = !exp }.padding(10.dp)
            ) {
                Text(
                    if (exp) "\uD83D\uDCA1 \u601d\u8003\u8fc7\u7a0b \u25B2" else "\uD83D\uDCA1 \u601d\u8003\u8fc7\u7a0b \u25BC",
                    color = Color(0xFF7A9FCC), fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
                AnimatedVisibility(exp, enter = expandVertically(), exit = shrinkVertically()) {
                    Text(msg.thinking, color = Color(0x99FFFFFF), fontSize = 13.sp, fontStyle = FontStyle.Italic, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Box(
            Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(16.dp))
                .background(if (isUser) UserBubble else AiBubble)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) { Text(msg.content, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp) }
    }
}

@Composable
private fun StreamBubble(thinking: String, content: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        if (thinking.isNotBlank()) {
            Box(Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0A1225)).padding(10.dp)) {
                Column {
                    Text("\uD83D\uDCA1 \u601d\u8003\u4e2d...", color = Color(0xFF7A9FCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(thinking, color = Color(0x99FFFFFF), fontSize = 13.sp, fontStyle = FontStyle.Italic, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (content.isNotBlank()) {
            Box(Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(16.dp)).background(AiBubble).padding(14.dp, 10.dp)) {
                Text(content, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
            }
        } else {
            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(AiBubble).padding(14.dp, 10.dp)) {
                Text("\u2588", color = Blue, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun InputBar(enabled: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF060D20))
            .padding(horizontal = 12.dp, vertical = 10.dp).padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF0E1830)).padding(16.dp, 12.dp)) {
            BasicTextField(
                text, { text = it }, textStyle = TextStyle(Color.White, 15.sp),
                cursorBrush = SolidColor(Blue), modifier = Modifier.fillMaxWidth(),
                decorationBox = { i -> if (text.isEmpty()) Text("\u8f93\u5165\u6d88\u606f...", color = Color(0x44FFFFFF), fontSize = 15.sp); i() }
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(if (enabled && text.isNotBlank()) Blue else Color(0xFF1A2540))
                .clickable(enabled = enabled && text.isNotBlank()) { onSend(text); text = "" },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun DrawerContent(
    conversations: List<Conversation>, currentId: Long?,
    onSelect: (Long) -> Unit, onNew: () -> Unit, onDelete: (Long) -> Unit, onSettings: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(top = 48.dp, start = 12.dp, end = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("\u5bf9\u8bdd\u5217\u8868", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, null, tint = Color(0x88FFFFFF)) }
                IconButton(onClick = onNew) { Icon(Icons.Default.Add, null, tint = Blue) }
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            items(conversations, key = { it.id }) { c ->
                val cur = c.id == currentId
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(if (cur) Color(0xFF152040) else Color.Transparent)
                        .clickable { onSelect(c.id) }.padding(12.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = if (cur) Blue else Color.White, fontSize = 14.sp, fontWeight = if (cur) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                        Text("${c.messages.size} \u6761\u6d88\u606f", color = Color(0x55FFFFFF), fontSize = 11.sp)
                    }
                    IconButton(onClick = { onDelete(c.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color(0x44FFFFFF), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
