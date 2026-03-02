package com.hellostar.stellarnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hellostar.stellarnotes.data.Note
import com.hellostar.stellarnotes.ui.chat.ChatScreen
import com.hellostar.stellarnotes.ui.chat.ChatViewModel
import kotlinx.coroutines.launch

enum class AppTab { Galaxy, List, Stats }
enum class Screen { Main, Chat, Settings }

private val Deep = Color(0xFF040A1E)
private val Blue = Color(0xFF5A9DFF)
private val Gold = Color(0xFFFFD86B)

@Composable
fun StellarApp(viewModel: StellarViewModel, appPrefs: AppPreferences, chatVm: ChatViewModel) {
    val hasSeen by appPrefs.hasSeenIntro.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.Main) }

    val apiBaseUrl by appPrefs.apiBaseUrl.collectAsState(initial = "")
    val apiKey by appPrefs.apiKey.collectAsState(initial = "")
    val modelName by appPrefs.modelName.collectAsState(initial = "")
    val enableThinking by appPrefs.enableThinking.collectAsState(initial = true)

    if (!hasSeen) {
        Surface(Modifier.fillMaxSize(), color = Deep) {
            Box(Modifier.fillMaxSize().clickable { scope.launch { appPrefs.setIntroSeen() } }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NebulaNotes", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("Tap to enter", color = Color(0x88FFFFFF))
                }
            }
        }
        return
    }

    when (screen) {
        Screen.Main -> MainScreen(
            vm = viewModel,
            onOpenChat = { note ->
                if (note != null) chatVm.newConversation(note) else if (chatVm.conversations.isEmpty()) chatVm.newConversation()
                screen = Screen.Chat
            },
            onOpenSettings = { screen = Screen.Settings }
        )

        Screen.Chat -> ChatScreen(
            chatVm = chatVm,
            notes = emptyList(),
            baseUrl = apiBaseUrl,
            apiKey = apiKey,
            model = modelName,
            enableThinking = enableThinking,
            onOpenSettings = { screen = Screen.Settings },
            onBack = { screen = Screen.Main }
        )

        Screen.Settings -> SettingsScreen(appPrefs = appPrefs, onBack = { screen = Screen.Main })
    }
}

@Composable
private fun MainScreen(vm: StellarViewModel, onOpenChat: (Note?) -> Unit, onOpenSettings: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(AppTab.Galaxy) }

    var selected by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Note?>(null) }

    var draftTitle by remember { mutableStateOf("") }
    var draftContent by remember { mutableStateOf("") }
    var draftStar by remember { mutableStateOf(false) }

    val tilt = rememberTiltState()
    var camX by remember { mutableFloatStateOf(0f) }
    var camY by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }

    Surface(Modifier.fillMaxSize(), color = Deep) {
        Box(Modifier.fillMaxSize()) {
            if (tab == AppTab.Galaxy) {
                StarFieldView(
                    notes = state.notes,
                    tilt = tilt,
                    camX = camX,
                    camY = camY,
                    camZoom = zoom,
                    onPanZoom = { dx, dy, z ->
                        camX += dx / zoom
                        camY += dy / zoom
                        zoom = (zoom * z).coerceIn(0.2f, 5f)
                    },
                    onResetCamera = {
                        camX = 0f
                        camY = 0f
                        zoom = 1f
                    },
                    onTapNote = { selected = it }
                )
            } else if (tab == AppTab.List) {
                NoteList(state.notes) { selected = it }
            } else {
                StatsPage(state.notes)
            }

            SearchBar(state.query, vm::onQueryChange)

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TabChip("Galaxy", tab == AppTab.Galaxy) { tab = AppTab.Galaxy }
                TabChip("List", tab == AppTab.List) { tab = AppTab.List }
                TabChip("Stats", tab == AppTab.Stats) { tab = AppTab.Stats }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingActionButton(onClick = { onOpenChat(null) }, containerColor = Color(0xFF2A4A7A)) {
                    Icon(Icons.Default.Chat, contentDescription = "chat", tint = Color.White)
                }
                FloatingActionButton(onClick = {
                    editing = null
                    draftTitle = ""
                    draftContent = ""
                    draftStar = false
                    showEditor = true
                }, containerColor = Blue) {
                    Icon(Icons.Default.Add, contentDescription = "add", tint = Color.White)
                }
            }

            if (selected != null) {
                NotePanel(
                    note = selected!!,
                    onClose = { selected = null },
                    onEdit = {
                        editing = selected
                        draftTitle = selected!!.title
                        draftContent = selected!!.content
                        draftStar = selected!!.starred
                        showEditor = true
                    },
                    onDelete = {
                        vm.deleteNote(selected!!)
                        selected = null
                    },
                    onToggleStar = {
                        vm.toggleStar(selected!!)
                        selected = selected!!.copy(starred = !selected!!.starred)
                    },
                    onAskAi = { onOpenChat(selected) }
                )
            }

            if (showEditor) {
                EditorDialog(
                    title = draftTitle,
                    content = draftContent,
                    starred = draftStar,
                    onTitle = { draftTitle = it },
                    onContent = { draftContent = it },
                    onStar = { draftStar = it },
                    onCancel = { showEditor = false },
                    onSave = {
                        vm.addOrUpdate(editing, draftTitle, draftContent, draftStar)
                        showEditor = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 42.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0x660B1328))) {
            BasicTextField(
                value = query,
                onValueChange = onChange,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Blue),
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search notes...", color = Color(0x66FFFFFF))
                    inner()
                }
            )
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xAA1B2A4A) else Color(0x66081422))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            val icon = when (text) {
                "Galaxy" -> Icons.Default.Explore
                "List" -> Icons.Default.FormatListBulleted
                else -> Icons.Default.Info
            }
            Icon(icon, contentDescription = text, tint = if (selected) Blue else Color(0x99FFFFFF), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = if (selected) Blue else Color(0x99FFFFFF), fontSize = 12.sp)
        }
    }
}

@Composable
private fun NoteList(notes: List<Note>, onClick: (Note) -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xCC040A1E)).padding(top = 90.dp, start = 12.dp, end = 12.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes.sortedByDescending { it.updatedAt }) { n ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onClick(n) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1530))) {
                    Column(Modifier.padding(12.dp)) {
                        Text(n.title, color = if (n.starred) Gold else Color.White, fontWeight = FontWeight.SemiBold)
                        if (n.content.isNotBlank()) Text(n.content, color = Color(0xAAFFFFFF), maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPage(notes: List<Note>) {
    val starred = notes.count { it.starred }
    val totalChars = notes.sumOf { it.content.length }
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xCC040A1E)).padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Total notes: ${notes.size}", color = Color.White, fontSize = 22.sp)
        Spacer(Modifier.height(10.dp))
        Text("Starred: $starred", color = Gold)
        Spacer(Modifier.height(10.dp))
        Text("Characters: $totalChars", color = Color(0xCCFFFFFF))
    }
}

@Composable
private fun NotePanel(
    note: Note,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStar: () -> Unit,
    onAskAi: () -> Unit
) {
    Card(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE0C1733))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleStar) { Icon(Icons.Default.Star, null, tint = if (note.starred) Gold else Color(0x55FFFFFF)) }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
            Text(note.content.ifBlank { "No content" }, color = Color(0xCCFFFFFF), modifier = Modifier.height(120.dp).verticalScroll(rememberScrollState()))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAskAi, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A4A7A))) { Text("Ask AI") }
                Button(onClick = onEdit, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A2A2A))) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun BoxScope.EditorDialog(
    title: String,
    content: String,
    starred: Boolean,
    onTitle: (String) -> Unit,
    onContent: (String) -> Unit,
    onStar: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111E3F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Edit note", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitle,
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Blue,
                        unfocusedLabelColor = Color(0x88FFFFFF)
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = onContent,
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Blue,
                        unfocusedLabelColor = Color(0x88FFFFFF)
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Starred", color = Color.White, modifier = Modifier.weight(1f))
                    Switch(checked = starred, onCheckedChange = onStar, colors = SwitchDefaults.colors(checkedThumbColor = Blue))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color(0x552A3A5A))) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(appPrefs: AppPreferences, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sUrl by appPrefs.apiBaseUrl.collectAsState(initial = "")
    val sKey by appPrefs.apiKey.collectAsState(initial = "")
    val sModel by appPrefs.modelName.collectAsState(initial = "")
    val sThink by appPrefs.enableThinking.collectAsState(initial = true)

    var url by remember(sUrl) { mutableStateOf(sUrl) }
    var key by remember(sKey) { mutableStateOf(sKey) }
    var model by remember(sModel) { mutableStateOf(sModel) }
    var thinking by remember(sThink) { mutableStateOf(sThink) }

    val fc = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Blue,
        unfocusedBorderColor = Color(0x33FFFFFF),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = Blue,
        focusedLabelColor = Blue,
        unfocusedLabelColor = Color(0x88FFFFFF)
    )

    Surface(Modifier.fillMaxSize(), color = Deep) {
        Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.Close, null, tint = Color.White) }
                Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(url, { url = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fc)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(key, { key = it }, label = { Text("API Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = fc)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fc)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Enable thinking", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = thinking, onCheckedChange = { thinking = it }, colors = SwitchDefaults.colors(checkedThumbColor = Blue))
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    scope.launch { appPrefs.saveApiSettings(url, key, model, thinking) }
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
    }
}
