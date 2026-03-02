package com.hellostar.stellarnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
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
private val PinBlue = Color(0xFF9FD4FF)
private val PanelBg = Color(0x88112244)
private val CardBg = Color(0xEE0D1C42)

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
                Text(text = "\u2728", fontSize = 72.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "\u661f\u6f9c\u7b14\u8bb0",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "NebulaNotes",
                    color = PinBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "\u6bcf\u4e00\u6761\u7b14\u8bb0\uff0c\u90fd\u662f\u4f60\u7684\u4e00\u9897\u661f",
                    color = Color(0xCCFFFFFF),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                val features = listOf(
                    "\uD83D\uDC46  \u5355\u6307\u6ed1\u52a8\u6f2b\u6e38\u661f\u7a7a",
                    "\uD83D\uDD0D  \u53cc\u6307\u634f\u5408\u7f29\u653e\u89c6\u89d2",
                    "\u2b50  \u661f\u6807\u7b14\u8bb0\u53d8\u6210\u91d1\u8272\u661f\u4f53",
                    "\uD83D\uDCCC  \u7f6e\u9876\u7b14\u8bb0\u79bb\u4f60\u6700\u8fd1",
                    "\uD83D\uDCF1  \u8f7b\u6643\u624b\u673a\uff0c\u661f\u7a7a\u8ddf\u7740\u52a8"
                )
                for (f in features) {
                    Text(
                        text = f,
                        color = Color(0xAAFFFFFF),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "\u70b9\u51fb\u4efb\u610f\u4f4d\u7f6e\u8fdb\u5165\u661f\u7a7a",
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
    
    // UI states
    var activeNote by remember { mutableStateOf<Note?>(null) }
    var isEditing by remember { mutableStateOf(false) } // true: Editor, false: Reader
    var showSheet by remember { mutableStateOf(false) }
    
    // Camera states
    val tilt = rememberTiltState()
    val cameraX = remember { Animatable(0f) }
    val cameraY = remember { Animatable(0f) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    val coroutineScope = rememberCoroutineScope()

    // Focus camera on specific note
    LaunchedEffect(state.focusedNoteId) {
        val noteId = state.focusedNoteId ?: return@LaunchedEffect
        val note = state.notes.firstOrNull { it.id == noteId } ?: return@LaunchedEffect
        val pos = computeStarPosition(note, state.notes)
        coroutineScope.launch {
            cameraX.animateTo(-pos.x, spring(stiffness = Spring.StiffnessLow))
        }
        coroutineScope.launch {
            cameraY.animateTo(-pos.y, spring(stiffness = Spring.StiffnessLow))
        }
        cameraZoom = 1.8f // Zoom in when focused
        
        // Open reader mode
        activeNote = note
        isEditing = false
        showSheet = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlue) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                    // Focus camera
                    val pos = computeStarPosition(note, state.notes)
                    coroutineScope.launch { cameraX.animateTo(-pos.x, spring(stiffness = Spring.StiffnessLow)) }
                    coroutineScope.launch { cameraY.animateTo(-pos.y, spring(stiffness = Spring.StiffnessLow)) }
                    cameraZoom = 1.5f
                    
                    // Open reader
                    activeNote = note
                    isEditing = false
                    showSheet = true
                }
            )

            // Search Bar (hide when sheet is open)
            AnimatedVisibility(
                visible = !showSheet,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 40.dp)
                ) {
                    SearchBar(
                        query = state.query,
                        results = state.searchResults,
                        onQueryChange = { viewModel.onQueryChange(it) },
                        onSelect = { note -> viewModel.focusOn(note.id) }
                    )
                }
            }

            // FAB (hide when sheet is open)
            AnimatedVisibility(
                visible = !showSheet,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        activeNote = null
                        isEditing = true
                        showSheet = true
                    },
                    containerColor = Color(0xFF1A3A7A),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New")
                }
            }

            // Bottom Sheet (Reader or Editor)
            AnimatedVisibility(
                visible = showSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (isEditing) {
                    NoteEditorSheet(
                        note = activeNote,
                        onDismiss = { showSheet = false },
                        onSave = { old, title, content, pinned, starred ->
                            viewModel.addOrUpdate(old, title, content, pinned, starred)
                            showSheet = false
                        },
                        onToggleStar = { viewModel.toggleStar(it) },
                        onTogglePin = { viewModel.togglePin(it) },
                        onDelete = {
                            viewModel.deleteNote(it)
                            showSheet = false
                        }
                    )
                } else {
                    NoteReaderSheet(
                        note = activeNote!!,
                        onDismiss = { showSheet = false },
                        onEdit = { isEditing = true },
                        onToggleStar = { viewModel.toggleStar(it); activeNote = it.copy(starred = !it.starred) },
                        onTogglePin = { viewModel.togglePin(it); activeNote = it.copy(pinned = !it.pinned) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    results: List<SearchResult>,
    onQueryChange: (String) -> Unit,
    onSelect: (Note) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("\u641c\u7d22\u661f\u7fa4\u7b14\u8bb0\u2026", color = Color(0xAAFFFFFF)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinBlue,
                    unfocusedBorderColor = Color(0x55FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PinBlue
                )
            )
            if (query.isNotBlank() && results.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.height(160.dp)) {
                    items(results.take(8), key = { it.note.id }) { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(r.note) }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (r.note.starred) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = StarGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = r.note.title,
                                color = if (r.note.starred) StarGold else Color.White,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "%.0f".format(r.score),
                                color = Color(0xFF8AC6FF),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteReaderSheet(
    note: Note,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleStar: (Note) -> Unit,
    onTogglePin: (Note) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "创建于: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(note.createdAt)),
                        color = Color(0x77FFFFFF),
                        fontSize = 12.sp
                    )
                }
                
                IconButton(onClick = { onTogglePin(note) }) {
                    Text(
                        text = if (note.pinned) "\uD83D\uDCCC" else "\uD83D\uDCCD",
                        fontSize = 18.sp
                    )
                }
                IconButton(onClick = { onToggleStar(note) }) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (note.starred) StarGold else Color(0x66FFFFFF)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))
            
            // Content
            val scrollState = rememberScrollState()
            Text(
                text = note.content.ifBlank { "（空）" },
                color = Color(0xEEFFFFFF),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Edit FAB
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FloatingActionButton(
                    onClick = onEdit,
                    containerColor = PinBlue,
                    contentColor = DeepBlue
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    }
}


@Composable
private fun NoteEditorSheet(
    note: Note?,
    onDismiss: () -> Unit,
    onSave: (Note?, String, String, Boolean, Boolean) -> Unit,
    onToggleStar: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (note == null) "\u65b0\u5efa\u661f\u9645\u7b14\u8bb0" else "\u7f16\u8f91\u7b14\u8bb0",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (note != null) {
                    IconButton(onClick = { onTogglePin(note) }) {
                        Text(
                            text = if (note.pinned) "\uD83D\uDCCC" else "\uD83D\uDCCD",
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = { onToggleStar(note) }) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (note.starred) StarGold else Color(0x66FFFFFF)
                        )
                    }
                    IconButton(onClick = { onDelete(note) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF8A8A)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("\u6807\u9898", color = Color(0xAAFFFFFF)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinBlue,
                    unfocusedBorderColor = Color(0x55FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PinBlue
                )
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("\u5185\u5bb9", color = Color(0xAAFFFFFF)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinBlue,
                    unfocusedBorderColor = Color(0x55FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PinBlue
                )
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("\u53d6\u6d88", color = Color(0xAAFFFFFF))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        onSave(
                            note, title, content,
                            note?.pinned ?: false,
                            note?.starred ?: false
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A3A7A)
                    ),
                    modifier = Modifier.width(120.dp)
                ) {
                    Text("\u4fdd\u5b58")
                }
            }
        }
    }
}
