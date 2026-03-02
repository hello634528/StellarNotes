package com.hellostar.stellarnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hellostar.stellarnotes.data.Note
import com.hellostar.stellarnotes.ui.chat.ChatScreen
import com.hellostar.stellarnotes.ui.chat.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Deep = Color(0xFF040A1E)
private val Gold = Color(0xFFFFD86B)
private val Blue = Color(0xFF5A9DFF)
private val CardBg = Color(0xF00A1530)

enum class AppTab { Galaxy, List, Stats }
enum class Screen { Main, Chat, Settings }

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
        IntroScreen { scope.launch { appPrefs.setIntroSeen() } }
    } else when (screen) {
        Screen.Main -> MainScreen(viewModel,
            onOpenChat = { note -> if (note != null) chatVm.newConversation(note); screen = Screen.Chat },
            onOpenSettings = { screen = Screen.Settings })
        Screen.Chat -> ChatScreen(chatVm, emptyList(), apiBaseUrl, apiKey, modelName, enableThinking,
            onOpenSettings = { screen = Screen.Settings }, onBack = { screen = Screen.Main })
        Screen.Settings -> SettingsScreen(appPrefs) { screen = Screen.Main }
    }
}

@Composable
private fun IntroScreen(onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Deep) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
                Text("\u2728", fontSize = 72.sp)
                Spacer(Modifier.height(16.dp))
                Text("\u661f\u6f9c\u7b14\u8bb0", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text("NebulaNotes", color = Blue, fontSize = 16.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(40.dp))
                Text("\u6bcf\u4e00\u6761\u7b14\u8bb0\uff0c\u90fd\u662f\u5b87\u5b99\u4e2d\u7684\u4e00\u9897\u661f", color = Color(0xCCFFFFFF), fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                for (f in listOf("\uD83D\uDC46  \u5355\u6307\u6ed1\u52a8\u6f2b\u6e38\u661f\u7a7a", "\uD83D\uDD0D  \u53cc\u6307\u7f29\u653e\u89c6\u89d2", "\u2b50  \u661f\u6807\u7b14\u8bb0\u53d1\u5149\u66f4\u4eae", "\uD83E\uDD16  AI \u5bf9\u8bdd\u7406\u89e3\u7b14\u8bb0"))
                    Text(f, color = Color(0xAAFFFFFF), fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(Modifier.height(48.dp))
                Text("\u70b9\u51fb\u4efb\u610f\u4f4d\u7f6e\u8fdb\u5165", color = Color(0x77FFFFFF), fontSize = 13.sp)
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
    val fc = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue, unfocusedBorderColor = Color(0x33FFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Blue, focusedLabelColor = Blue, unfocusedLabelColor = Color(0x88FFFFFF))

    Surface(Modifier.fillMaxSize(), color = Deep) {
        Column(Modifier.fillMaxSize().padding(top = 48.dp, start = 20.dp, end = 20.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.Close, null, tint = Color.White) }
                Text("\u8bbe\u7f6e", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            Text("AI \u5bf9\u8bdd\u914d\u7f6e", color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("\u652f\u6301 OpenAI \u517c\u5bb9\u683c\u5f0f\u7684 API", color = Color(0x66FFFFFF), fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(url, { url = it }, label = { Text("Base URL") }, placeholder = { Text("https://api.openai.com", color = Color(0x33FFFFFF)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fc)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(key, { key = it }, label = { Text("API Key") }, placeholder = { Text("sk-...", color = Color(0x33FFFFFF)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = fc)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(model, { model = it }, label = { Text("\u6a21\u578b\u540d\u79f0") }, placeholder = { Text("gpt-4o / deepseek-r1", color = Color(0x33FFFFFF)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fc)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("\u542f\u7528\u601d\u8003", color = Color.White, fontSize = 15.sp); Text("\u652f\u6301 reasoning_content", color = Color(0x66FFFFFF), fontSize = 11.sp) }
                Switch(thinking, { thinking = it }, colors = SwitchDefaults.colors(checkedThumbColor = Blue, checkedTrackColor = Color(0xFF1A3060)))
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = { scope.launch { appPrefs.saveApiSettings(url, key, model, thinking) }; onBack() },
                colors = ButtonDefaults.buttonColors(containerColor = Blue), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp)
            ) { Text("\u4fdd\u5b58", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MainScreen(vm: StellarViewModel, onOpenChat: (Note?) -> Unit, onOpenSettings: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(AppTab.Galaxy) }
    var activeNote by remember { mutableStateOf<Note?>(null) }
    var editing by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var full by remember { mutableStateOf(false) }
    val tilt = rememberTiltState()
    val camX = remember { Animatable(0f) }; val camY = remember { Animatable(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    val focusNote: (Note) -> Unit = { note ->
        val pos = computeStarPosition(note, state.notes)
        scope.launch { camX.animateTo(-pos.x, spring(stiffness = Spring.StiffnessLow)) }
        scope.launch { camY.animateTo(-pos.y, spring(stiffness = Spring.StiffnessLow)) }
        zoom = 1.6f; activeNote = note; editing = false; full = false; sheet = true
    }

    LaunchedEffect(state.focusedNoteId) {
        val id = state.focusedNoteId ?: return@LaunchedEffect
        val note = state.notes.firstOrNull { it.id == id } ?: return@LaunchedEffect
        tab = AppTab.Galaxy; focusNote(note)
    }

    Surface(Modifier.fillMaxSize(), color = Deep) {
        Box(Modifier.fillMaxSize()) {
            StarFieldView(state.notes, tilt, camX.value, camY.value, zoom,
                onPanZoom = { dx, dy, z -> scope.launch { camX.snapTo(camX.value + dx / zoom) }; scope.launch { camY.snapTo(camY.value + dy / zoom) }; zoom = (zoom * z).coerceIn(0.15f, 5f) },
                onResetCamera = { scope.launch { camX.animateTo(0f) }; scope.launch { camY.animateTo(0f) }; zoom = 1f },
                onTapNote = { if (tab == AppTab.Galaxy) focusNote(it) })

            if (state.notes.isEmpty() && tab == AppTab.Galaxy && !sheet) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\u2728", fontSize = 48.sp); Spacer(Modifier.height(12.dp))
                        Text("\u70b9\u51fb + \u521b\u5efa\u7b2c\u4e00\u9897\u661f", color = Color(0x88FFFFFF), fontSize = 16.sp)
                    }
                }
            }

            AnimatedVisibility(tab == AppTab.List, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize().background(Color(0xDD040A1E))) { NoteListPage(state.notes) { activeNote = it; editing = false; full = false; sheet = true } }
            }
            AnimatedVisibility(tab == AppTab.Stats, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize().background(Color(0xDD040A1E))) { StatsPage(state.notes) }
            }
            AnimatedVisibility(tab == AppTab.Galaxy && !sheet, enter = fadeIn(), exit = fadeOut()) {
                Column(Modifier.fillMaxWidth().padding(16.dp, 48.dp, 16.dp, 0.dp)) {
                    SearchBox(state.query, state.searchResults, { vm.onQueryChange(it) }, { vm.focusOn(it.id) })
                }
            }

            AnimatedVisibility(!sheet, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 24.dp)) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FloatingActionButton(onClick = { onOpenChat(null) }, containerColor = Color(0xFF2A4A7A), contentColor = Color.White, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(20.dp))
                    }
                    FloatingActionButton(onClick = { activeNote = null; editing = true; full = false; sheet = true }, containerColor = Blue, contentColor = Color.White) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
            AnimatedVisibility(!sheet, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                Dock(tab) { tab = it }
            }

            AnimatedVisibility(sheet, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
                val hm = if (full) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(420.dp)
                val shape = if (full) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                Card(hm.animateContentSize(), shape = shape, colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    if (editing) EditorSheet(activeNote, full, { full = !full }, { sheet = false },
                        { o, t, c, s -> vm.addOrUpdate(o, t, c, s); sheet = false }, { vm.toggleStar(it) }, { vm.deleteNote(it); sheet = false })
                    else ReaderSheet(activeNote!!, full, { full = !full }, { sheet = false }, { editing = true },
                        { vm.toggleStar(it); activeNote = it.copy(starred = !it.starred) }, { onOpenChat(it) })
                }
            }
        }
    }
}

@Composable
private fun SearchBox(q: String, res: List<SearchResult>, onChange: (String) -> Unit, onSelect: (Note) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x55050C18)).border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(18.dp)).padding(14.dp)) {
        BasicTextField(q, onChange, textStyle = TextStyle(Color.White, 16.sp), cursorBrush = SolidColor(Blue), singleLine = true, modifier = Modifier.fillMaxWidth(),
            decorationBox = { i -> if (q.isEmpty()) Text("\u641c\u7d22\u7b14\u8bb0...", color = Color(0x44FFFFFF), fontSize = 16.sp); i() })
        if (q.isNotBlank() && res.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.height(200.dp)) {
                items(res.take(6), key = { it.note.id }) { r ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onSelect(r.note) }.padding(10.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (r.note.starred) { Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(r.note.title, color = if (r.note.starred) Gold else Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            if (r.note.content.isNotBlank()) Text(r.note.content.replace('\n', ' '), color = Color(0x66FFFFFF), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x18FFFFFF))) {
                            Box(Modifier.fillMaxWidth(r.normalizedScore).height(3.dp).background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)))))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dock(tab: AppTab, onTab: (AppTab) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xDD050C18)).border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(22.dp)).padding(14.dp, 6.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        DI(Icons.Default.Explore, "\u661f\u7a7a", tab == AppTab.Galaxy) { onTab(AppTab.Galaxy) }
        DI(Icons.Default.FormatListBulleted, "\u5217\u8868", tab == AppTab.List) { onTab(AppTab.List) }
        DI(Icons.Default.Info, "\u6863\u6848", tab == AppTab.Stats) { onTab(AppTab.Stats) }
    }
}
@Composable
private fun DI(icon: ImageVector, label: String, sel: Boolean, onClick: () -> Unit) {
    Column(Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(4.dp)) {
        Icon(icon, label, tint = if (sel) Blue else Color(0x66FFFFFF), modifier = Modifier.size(22.dp))
        Text(label, color = if (sel) Blue else Color(0x66FFFFFF), fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun NoteListPage(notes: List<Note>, onClick: (Note) -> Unit) {
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(top = 48.dp, start = 16.dp, end = 16.dp)) {
        Text("\u6240\u6709\u7b14\u8bb0", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("${notes.size} \u9897\u661f\u8fb0", color = Color(0x66FFFFFF), fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        if (notes.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("\u8fd8\u6ca1\u6709\u7b14\u8bb0", color = Color(0x55FFFFFF)) }
        else LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes.sortedByDescending { it.updatedAt }, key = { it.id }) { n ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF0C1628)).border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(14.dp)).clickable { onClick(n) }.padding(16.dp, 14.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(if (n.starred) Gold else Color(0xFF4A6080)))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(n.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(n.content.ifBlank { "\u65e0\u5185\u5bb9" }, color = Color(0x77FFFFFF), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                        Spacer(Modifier.height(6.dp))
                        Row { Text(fmt.format(Date(n.updatedAt)), color = Color(0x44FFFFFF), fontSize = 11.sp); Spacer(Modifier.width(12.dp)); Text("${n.content.length} \u5b57", color = Color(0x44FFFFFF), fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPage(notes: List<Note>) {
    val total = notes.sumOf { it.content.length }; val starred = notes.count { it.starred }; val avg = if (notes.isEmpty()) 0 else total / notes.size
    Column(Modifier.fillMaxSize().padding(top = 48.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("\u661f\u9645\u6863\u6848", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(36.dp))
        Box(Modifier.size(140.dp).clip(CircleShape).background(Color(0x0D5A9DFF)).border(1.dp, Color(0x155A9DFF), CircleShape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${notes.size}", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold); Text("\u661f\u8fb0", color = Blue, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(36.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { SI("\u2b50", "$starred", "\u661f\u6807"); SI("\u270d\ufe0f", "$total", "\u603b\u5b57\u6570"); SI("\ud83d\udcc4", "$avg", "\u5e73\u5747") }
    }
}
@Composable
private fun SI(emoji: String, v: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(emoji, fontSize = 28.sp); Spacer(Modifier.height(6.dp)); Text(v, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(label, color = Color(0x88FFFFFF), fontSize = 12.sp) }
}

@Composable
private fun ReaderSheet(note: Note, full: Boolean, toggleFull: () -> Unit, dismiss: () -> Unit, edit: () -> Unit, toggleStar: (Note) -> Unit, onAskAi: (Note) -> Unit) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(note.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row { Text(fmt.format(Date(note.createdAt)), color = Color(0x55FFFFFF), fontSize = 11.sp); Spacer(Modifier.width(12.dp)); Text("${note.content.length} \u5b57", color = Color(0x55FFFFFF), fontSize = 11.sp) }
            }
            IconButton(onClick = { toggleStar(note) }) { Icon(Icons.Filled.Star, null, tint = if (note.starred) Gold else Color(0x33FFFFFF)) }
            IconButton(onClick = toggleFull) { Icon(if (full) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, null, tint = Color.White) }
            IconButton(onClick = dismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
        }
        Spacer(Modifier.height(16.dp))
        Text(note.content.ifBlank { "\u8fd8\u6ca1\u6709\u5185\u5bb9..." }, color = Color(0xDDFFFFFF), fontSize = 16.sp, lineHeight = 26.sp, modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.weight(1f))
            FloatingActionButton(onClick = { onAskAi(note) }, containerColor = Color(0xFF2A4A7A), contentColor = Color.White, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Chat, null, modifier = Modifier.size(20.dp)) }
            FloatingActionButton(onClick = edit, containerColor = Blue, contentColor = Color.White) { Icon(Icons.Default.Edit, null) }
        }
    }
}

@Composable
private fun EditorSheet(note: Note?, full: Boolean, toggleFull: () -> Unit, dismiss: () -> Unit, save: (Note?, String, String, Boolean) -> Unit, toggleStar: (Note) -> Unit, delete: (Note) -> Unit) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (note == null) "\u521b\u9020\u65b0\u661f" else "\u7f16\u8f91", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (note != null) {
                IconButton(onClick = { toggleStar(note) }) { Icon(Icons.Filled.Star, null, tint = if (note.starred) Gold else Color(0x33FFFFFF)) }
                IconButton(onClick = { delete(note) }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B)) }
            }
            IconButton(onClick = toggleFull) { Icon(if (full) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, null, tint = Color.White) }
            IconButton(onClick = dismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().background(Color(0x08FFFFFF), RoundedCornerShape(10.dp)).padding(12.dp)) {
            BasicTextField(title, { title = it }, textStyle = TextStyle(Color.White, 20.sp, fontWeight = FontWeight.Bold), cursorBrush = SolidColor(Blue), singleLine = true, modifier = Modifier.fillMaxWidth(),
                decorationBox = { i -> if (title.isEmpty()) Text("\u6807\u9898", color = Color(0x33FFFFFF), fontSize = 20.sp); i() })
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0x08FFFFFF), RoundedCornerShape(10.dp)).padding(12.dp)) {
            BasicTextField(content, { content = it }, textStyle = TextStyle(Color.White, 16.sp, lineHeight = 24.sp), cursorBrush = SolidColor(Blue), modifier = Modifier.fillMaxSize(),
                decorationBox = { i -> if (content.isEmpty()) Text("\u5f00\u59cb\u8bb0\u5f55...", color = Color(0x33FFFFFF), fontSize = 16.sp); i() })
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${content.length} \u5b57", color = Color(0x44FFFFFF), fontSize = 12.sp)
            Row {
                TextButton(onClick = dismiss) { Text("\u53d6\u6d88", color = Color(0x66FFFFFF)) }
                Spacer(Modifier.width(12.dp))
                Button(onClick = { save(note, title, content, note?.starred ?: false) }, colors = ButtonDefaults.buttonColors(containerColor = Blue), modifier = Modifier.height(44.dp), shape = RoundedCornerShape(22.dp)) { Text("\u4fdd\u5b58", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
