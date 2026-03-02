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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hellostar.stellarnotes.data.Note
import kotlinx.coroutines.launch

private val DeepBlue = Color(0xFF040A1E)
private val StarGold = Color(0xFFFFD86B)
private val ActionBlue = Color(0xFF5A9DFF)
private val CardBg = Color(0xEE0A1530)

enum class AppTab { Galaxy, List, Stats }

@Composable
fun StellarApp(viewModel: StellarViewModel) {
    var showIntro by rememberSaveable { mutableStateOf(true) }

    if (showIntro) {
        IntroScreen(onDismiss = { showIntro = false })
    } else {
        MainScreen(viewModel = viewModel)
    }
}

@Composable
private fun IntroScreen(onDismiss: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlue) {
        Box(
            modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(40.dp)
            ) {
                Text(text = "✨", fontSize = 72.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "星澜笔记",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "NebulaNotes",
                    color = ActionBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "每一条笔记，都是宇宙中的一颗星",
                    color = Color(0xCCFFFFFF),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                val features = listOf(
                    "👆  单指滑动漫游星空",
                    "🔍  双指捏合缩放视角",
                    "⭐  星标笔记汇聚星系中心",
                    "📱  轻晃手机，星空随之漂移"
                )
                for (f in features) {
                    Text(
                        text = f,
                        color = Color(0xAAFFFFFF),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "点击任意位置进入星际",
                    color = Color(0x77FFFFFF),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MainScreen(viewModel: StellarViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    var currentTab by remember { mutableStateOf(AppTab.Galaxy) }
    var activeNote by remember { mutableStateOf<Note?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    
    val tilt = rememberTiltState()
    val cameraX = remember { Animatable(0f) }
    val cameraY = remember { Animatable(0f) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.focusedNoteId) {
        val noteId = state.focusedNoteId ?: return@LaunchedEffect
        val note = state.notes.firstOrNull { it.id == noteId } ?: return@LaunchedEffect
        
        currentTab = AppTab.Galaxy
        val pos = computeStarPosition(note, state.notes)
        coroutineScope.launch { cameraX.animateTo(-pos.x, spring(stiffness = Spring.StiffnessLow)) }
        coroutineScope.launch { cameraY.animateTo(-pos.y, spring(stiffness = Spring.StiffnessLow)) }
        cameraZoom = 1.8f
        
        activeNote = note
        isEditing = false
        isFullScreen = false
        showSheet = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlue) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Background Layer: StarField View
            StarFieldView(
                notes = state.notes,
                tilt = tilt,
                camX = cameraX.value,
                camY = cameraY.value,
                camZoom = cameraZoom,
                onPanZoom = { dx, dy, zoom ->
                    coroutineScope.launch {
                        cameraX.snapTo(cameraX.value + dx / cameraZoom)
                        cameraY.snapTo(cameraY.value + dy / cameraZoom)
                    }
                    cameraZoom = (cameraZoom * zoom).coerceIn(0.2f, 4.0f)
                },
                onResetCamera = {
                    coroutineScope.launch { cameraX.animateTo(0f) }
                    coroutineScope.launch { cameraY.animateTo(0f) }
                    cameraZoom = 1f
                },
                onTapNote = { note ->
                    if (currentTab == AppTab.Galaxy) {
                        val pos = computeStarPosition(note, state.notes)
                        coroutineScope.launch { cameraX.animateTo(-pos.x, spring(stiffness = Spring.StiffnessLow)) }
                        coroutineScope.launch { cameraY.animateTo(-pos.y, spring(stiffness = Spring.StiffnessLow)) }
                        cameraZoom = 1.6f
                        
                        activeNote = note
                        isEditing = false
                        isFullScreen = false
                        showSheet = true
                    }
                }
            )

            // Foreground Layers based on Tab
            AnimatedVisibility(
                visible = currentTab == AppTab.List,
                enter = fadeIn(), exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xBB040A1E))) {
                    NoteListScreen(
                        notes = state.notes,
                        onNoteClick = { note ->
                            activeNote = note
                            isEditing = false
                            isFullScreen = false
                            showSheet = true
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = currentTab == AppTab.Stats,
                enter = fadeIn(), exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xBB040A1E))) {
                    StatsScreen(notes = state.notes)
                }
            }

            // Search Bar (Only in Galaxy mode, hidden when sheet is open)
            AnimatedVisibility(
                visible = currentTab == AppTab.Galaxy && !showSheet,
                enter = fadeIn(), exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 48.dp)
                ) {
                    CustomSearchBar(
                        query = state.query,
                        results = state.searchResults,
                        onQueryChange = { viewModel.onQueryChange(it) },
                        onSelect = { note -> viewModel.focusOn(note.id) }
                    )
                }
            }

            // FAB (Hidden when sheet is open)
            AnimatedVisibility(
                visible = !showSheet,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 24.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        activeNote = null
                        isEditing = true
                        isFullScreen = false
                        showSheet = true
                    },
                    containerColor = ActionBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New")
                }
            }

            // Bottom Navigation Dock
            AnimatedVisibility(
                visible = !showSheet,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
            ) {
                BottomDock(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }

            // Bottom Sheet (Reader or Editor)
            AnimatedVisibility(
                visible = showSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val heightModifier = if (isFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(420.dp)
                val shape = if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

                Card(
                    modifier = heightModifier.animateContentSize(),
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    if (isEditing) {
                        NoteEditorContent(
                            note = activeNote,
                            isFullScreen = isFullScreen,
                            onToggleFullScreen = { isFullScreen = !isFullScreen },
                            onDismiss = { showSheet = false },
                            onSave = { old, title, content, starred ->
                                viewModel.addOrUpdate(old, title, content, starred)
                                showSheet = false
                            },
                            onToggleStar = { viewModel.toggleStar(it) },
                            onDelete = {
                                viewModel.deleteNote(it)
                                showSheet = false
                            }
                        )
                    } else {
                        NoteReaderContent(
                            note = activeNote!!,
                            isFullScreen = isFullScreen,
                            onToggleFullScreen = { isFullScreen = !isFullScreen },
                            onDismiss = { showSheet = false },
                            onEdit = { isEditing = true },
                            onToggleStar = { 
                                viewModel.toggleStar(it)
                                activeNote = it.copy(starred = !it.starred) 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomSearchBar(
    query: String,
    results: List<SearchResult>,
    onQueryChange: (String) -> Unit,
    onSelect: (Note) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x66020815)) // Deep dark transparent
            .border(1.dp, Color(0x335A9DFF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(ActionBlue),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text("搜索星群笔记...", color = Color(0x66FFFFFF), fontSize = 16.sp)
                }
                innerTextField()
            }
        )
        
        if (query.isNotBlank() && results.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(results.take(6), key = { it.note.id }) { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(r.note) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (r.note.starred) {
                            Icon(Icons.Default.Star, null, tint = StarGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = r.note.title,
                                color = if (r.note.starred) StarGold else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 15.sp
                            )
                            if (r.note.content.isNotBlank()) {
                                Text(
                                    text = r.note.content.replace('\n', ' '),
                                    color = Color(0x88FFFFFF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(r.normalizedScore)
                                    .height(4.dp)
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomDock(currentTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xCC040A1E))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        DockItem(
            icon = Icons.Default.Explore,
            label = "星空",
            isSelected = currentTab == AppTab.Galaxy,
            onClick = { onTabSelected(AppTab.Galaxy) }
        )
        DockItem(
            icon = Icons.Default.FormatListBulleted,
            label = "列表",
            isSelected = currentTab == AppTab.List,
            onClick = { onTabSelected(AppTab.List) }
        )
        DockItem(
            icon = Icons.Default.Info,
            label = "档案",
            isSelected = currentTab == AppTab.Stats,
            onClick = { onTabSelected(AppTab.Stats) }
        )
    }
}

@Composable
private fun DockItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ActionBlue else Color(0x88FFFFFF),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = if (isSelected) ActionBlue else Color(0x88FFFFFF),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun NoteListScreen(notes: List<Note>, onNoteClick: (Note) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 16.dp, end = 16.dp)) {
        Text("所有笔记", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(notes.sortedByDescending { it.updatedAt }, key = { it.id }) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNoteClick(note) },
                    colors = CardDefaults.cardColors(containerColor = Color(0x44FFFFFF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (note.starred) {
                                Icon(Icons.Default.Star, null, tint = StarGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = note.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = note.content.ifBlank { "无内容" },
                            color = Color(0xAAFFFFFF),
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(notes: List<Note>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("星际档案", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(40.dp))
        
        Box(modifier = Modifier.size(160.dp).clip(CircleShape).background(Color(0x225A9DFF)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${notes.size}", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text("宇宙星辰", color = ActionBlue, fontSize = 14.sp)
            }
        }
        
        Spacer(Modifier.height(40.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Star, null, tint = StarGold, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text("${notes.count { it.starred }}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("金色星标", color = Color(0xAAFFFFFF), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NoteReaderContent(
    note: Note,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleStar: (Note) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(note.createdAt)),
                    color = Color(0x66FFFFFF),
                    fontSize = 12.sp
                )
            }
            
            IconButton(onClick = { onToggleStar(note) }) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Star",
                    tint = if (note.starred) StarGold else Color(0x44FFFFFF)
                )
            }
            IconButton(onClick = onToggleFullScreen) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }

        Spacer(Modifier.height(20.dp))
        
        val scrollState = rememberScrollState()
        Text(
            text = note.content.ifBlank { "开始写下你的星际灵感..." },
            color = Color(0xDDFFFFFF),
            fontSize = 16.sp,
            lineHeight = 26.sp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FloatingActionButton(
                onClick = onEdit,
                containerColor = ActionBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}

@Composable
private fun NoteEditorContent(
    note: Note?,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Note?, String, String, Boolean) -> Unit,
    onToggleStar: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (note == null) "创造新星" else "重塑星体",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (note != null) {
                IconButton(onClick = { onToggleStar(note) }) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Star",
                        tint = if (note.starred) StarGold else Color(0x44FFFFFF)
                    )
                }
                IconButton(onClick = { onDelete(note) }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF6B6B))
                }
            }
            IconButton(onClick = onToggleFullScreen) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Custom BasicTextField for seamless input
        Box(modifier = Modifier.fillMaxWidth().background(Color(0x11FFFFFF), RoundedCornerShape(12.dp)).padding(12.dp)) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(ActionBlue),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (title.isEmpty()) Text("输入标题...", color = Color(0x55FFFFFF), fontSize = 20.sp)
                    inner()
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0x11FFFFFF), RoundedCornerShape(12.dp)).padding(12.dp)) {
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp, lineHeight = 24.sp),
                cursorBrush = SolidColor(ActionBlue),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { inner ->
                    if (content.isEmpty()) Text("记录下此刻的灵感...", color = Color(0x55FFFFFF), fontSize = 16.sp)
                    inner()
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) {
                Text("丢弃", color = Color(0x88FFFFFF))
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = {
                    onSave(note, title, content, note?.starred ?: false)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ActionBlue),
                modifier = Modifier.width(120.dp).height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("注入宇宙", fontWeight = FontWeight.Bold)
            }
        }
    }
}
