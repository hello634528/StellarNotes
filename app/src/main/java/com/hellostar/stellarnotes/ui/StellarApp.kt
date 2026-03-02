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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hellostar.stellarnotes.data.Note
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Deep = Color(0xFF040A1E)
private val Gold = Color(0xFFFFD86B)
private val Blue = Color(0xFF5A9DFF)
private val CardBg = Color(0xF00A1530)

enum class AppTab { Galaxy, List, Stats }

@Composable
fun StellarApp(viewModel: StellarViewModel, appPrefs: AppPreferences) {
    val hasSeen by appPrefs.hasSeenIntro.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    if (!hasSeen) {
        IntroScreen(onDismiss = { scope.launch { appPrefs.setIntroSeen() } })
    } else {
        MainScreen(viewModel = viewModel)
    }
}

@Composable
private fun IntroScreen(onDismiss: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Deep) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
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
                    color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    text = "NebulaNotes",
                    color = Blue, fontSize = 16.sp, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "\u6bcf\u4e00\u6761\u7b14\u8bb0\uff0c\u90fd\u662f\u5b87\u5b99\u4e2d\u7684\u4e00\u9897\u661f",
                    color = Color(0xCCFFFFFF), fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                val features = listOf(
                    "\uD83D\uDC46  \u5355\u6307\u6ed1\u52a8\u6f2b\u6e38\u661f\u7a7a",
                    "\uD83D\uDD0D  \u53cc\u6307\u7f29\u653e\u89c6\u89d2",
                    "\u2b50  \u661f\u6807\u7b14\u8bb0\u53d1\u5149\u66f4\u4eae",
                    "\uD83D\uDCF1  \u6643\u52a8\u624b\u673a\u661f\u7a7a\u6f02\u79fb"
                )
                for (f in features) {
                    Text(
                        text = f,
                        color = Color(0xAAFFFFFF), fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "\u70b9\u51fb\u4efb\u610f\u4f4d\u7f6e\u8fdb\u5165",
                    color = Color(0x77FFFFFF), fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MainScreen(viewModel: StellarViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(AppTab.Galaxy) }
    var activeNote by remember { mutableStateOf<Note?>(null) }
    var editing by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var full by remember { mutableStateOf(false) }
    val tilt = rememberTiltState()
    val camX = remember { Animatable(0f) }
    val camY = remember { Animatable(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    val focusNote: (Note) -> Unit = { note ->
        val pos = computeStarPosition(note, state.notes)
        scope.launch { camX.animateTo(-pos.x, spring(stiffness = Spring.StiffnessLow)) }
        scope.launch { camY.animateTo(-pos.y, spring(stiffness = Spring.StiffnessLow)) }
        zoom = 1.6f
        activeNote = note
        editing = false
        full = false
        sheet = true
    }

    LaunchedEffect(state.focusedNoteId) {
        val id = state.focusedNoteId ?: return@LaunchedEffect
        val note = state.notes.firstOrNull { it.id == id } ?: return@LaunchedEffect
        tab = AppTab.Galaxy
        focusNote(note)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Deep) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Star field always visible as background
            StarFieldView(
                notes = state.notes,
                tilt = tilt,
                camX = camX.value,
                camY = camY.value,
                camZoom = zoom,
                onPanZoom = { dx, dy, z ->
                    scope.launch { camX.snapTo(camX.value + dx / zoom) }
                    scope.launch { camY.snapTo(camY.value + dy / zoom) }
                    zoom = (zoom * z).coerceIn(0.15f, 5f)
                },
                onResetCamera = {
                    scope.launch { camX.animateTo(0f) }
                    scope.launch { camY.animateTo(0f) }
                    zoom = 1f
                },
                onTapNote = { note ->
                    if (tab == AppTab.Galaxy) focusNote(note)
                }
            )

            // Empty state hint
            if (state.notes.isEmpty() && tab == AppTab.Galaxy && !sheet) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\u2728", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "\u70b9\u51fb + \u521b\u5efa\u7b2c\u4e00\u9897\u661f",
                            color = Color(0x88FFFFFF), fontSize = 16.sp
                        )
                    }
                }
            }

            // List tab overlay
            AnimatedVisibility(
                visible = tab == AppTab.List,
                enter = fadeIn(), exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xDD040A1E))
                ) {
                    NoteListPage(
                        notes = state.notes,
                        onNoteClick = { note ->
                            activeNote = note
                            editing = false
                            full = false
                            sheet = true
                        }
                    )
                }
            }

            // Stats tab overlay
            AnimatedVisibility(
                visible = tab == AppTab.Stats,
                enter = fadeIn(), exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xDD040A1E))
                ) {
                    StatsPage(notes = state.notes)
                }
            }

            // Search bar (galaxy mode only)
            AnimatedVisibility(
                visible = tab == AppTab.Galaxy && !sheet,
                enter = fadeIn(), exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 48.dp)
                ) {
                    SearchBox(
                        query = state.query,
                        results = state.searchResults,
                        onQueryChange = { viewModel.onQueryChange(it) },
                        onSelect = { note -> viewModel.focusOn(note.id) }
                    )
                }
            }

            // FAB
            AnimatedVisibility(
                visible = !sheet,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 24.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        activeNote = null
                        editing = true
                        full = false
                        sheet = true
                    },
                    containerColor = Blue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }

            // Bottom dock
            AnimatedVisibility(
                visible = !sheet,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Dock(currentTab = tab, onTabSelected = { tab = it })
            }

            // Bottom sheet
            AnimatedVisibility(
                visible = sheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val hm = if (full) Modifier.fillMaxSize()
                         else Modifier.fillMaxWidth().height(420.dp)
                val shape = if (full) RoundedCornerShape(0.dp)
                            else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                Card(
                    modifier = hm.animateContentSize(),
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    if (editing) {
                        EditorSheet(
                            note = activeNote,
                            isFullScreen = full,
                            onToggleFullScreen = { full = !full },
                            onDismiss = { sheet = false },
                            onSave = { old, t, c, s ->
                                viewModel.addOrUpdate(old, t, c, s)
                                sheet = false
                            },
                            onToggleStar = { viewModel.toggleStar(it) },
                            onDelete = {
                                viewModel.deleteNote(it)
                                sheet = false
                            }
                        )
                    } else {
                        ReaderSheet(
                            note = activeNote!!,
                            isFullScreen = full,
                            onToggleFullScreen = { full = !full },
                            onDismiss = { sheet = false },
                            onEdit = { editing = true },
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

// ==================== Search ====================

@Composable
private fun SearchBox(
    query: String,
    results: List<SearchResult>,
    onQueryChange: (String) -> Unit,
    onSelect: (Note) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x55050C18))
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Blue),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "\u641c\u7d22\u7b14\u8bb0...",
                        color = Color(0x44FFFFFF), fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )
        if (query.isNotBlank() && results.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(results.take(6), key = { it.note.id }) { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(r.note) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (r.note.starred) {
                            Icon(
                                Icons.Default.Star, null,
                                tint = Gold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = r.note.title,
                                color = if (r.note.starred) Gold else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp
                            )
                            if (r.note.content.isNotBlank()) {
                                Text(
                                    text = r.note.content.replace('\n', ' '),
                                    color = Color(0x66FFFFFF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x18FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(r.normalizedScore)
                                    .height(3.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Bottom Dock ====================

@Composable
private fun Dock(currentTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xDD050C18))
            .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        DockItem(Icons.Default.Explore, "\u661f\u7a7a", currentTab == AppTab.Galaxy) { onTabSelected(AppTab.Galaxy) }
        DockItem(Icons.Default.FormatListBulleted, "\u5217\u8868", currentTab == AppTab.List) { onTabSelected(AppTab.List) }
        DockItem(Icons.Default.Info, "\u6863\u6848", currentTab == AppTab.Stats) { onTabSelected(AppTab.Stats) }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Blue else Color(0x66FFFFFF),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (selected) Blue else Color(0x66FFFFFF),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ==================== Note List Page ====================

@Composable
private fun NoteListPage(notes: List<Note>, onNoteClick: (Note) -> Unit) {
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            "\u6240\u6709\u7b14\u8bb0",
            color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold
        )
        Text(
            "${notes.size} \u9897\u661f\u8fb0",
            color = Color(0x66FFFFFF), fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "\u8fd8\u6ca1\u6709\u7b14\u8bb0\uff0c\u53bb\u521b\u5efa\u4e00\u9897\u661f\u5427",
                    color = Color(0x55FFFFFF), fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = notes.sortedByDescending { it.updatedAt },
                    key = { it.id }
                ) { n ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0C1628))
                            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(14.dp))
                            .clickable { onNoteClick(n) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (n.starred) Gold else Color(0xFF4A6080))
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = n.title,
                                color = Color.White, fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = n.content.ifBlank { "\u65e0\u5185\u5bb9" },
                                color = Color(0x77FFFFFF), fontSize = 13.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Row {
                                Text(
                                    text = fmt.format(Date(n.updatedAt)),
                                    color = Color(0x44FFFFFF), fontSize = 11.sp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "${n.content.length} \u5b57",
                                    color = Color(0x44FFFFFF), fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Stats Page ====================

@Composable
private fun StatsPage(notes: List<Note>) {
    val totalWords = notes.sumOf { it.content.length }
    val starred = notes.count { it.starred }
    val avgWords = if (notes.isEmpty()) 0 else totalWords / notes.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "\u661f\u9645\u6863\u6848",
            color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(36.dp))
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color(0x0D5A9DFF))
                .border(1.dp, Color(0x155A9DFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${notes.size}",
                    color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    "\u661f\u8fb0",
                    color = Blue, fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(36.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("\u2b50", "$starred", "\u661f\u6807")
            StatItem("\u270d\ufe0f", "$totalWords", "\u603b\u5b57\u6570")
            StatItem("\ud83d\udcc4", "$avgWords", "\u5e73\u5747\u5b57\u6570")
        }
    }
}

@Composable
private fun StatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 28.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color(0x88FFFFFF), fontSize = 12.sp
        )
    }
}

// ==================== Reader Sheet ====================

@Composable
private fun ReaderSheet(
    note: Note,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleStar: (Note) -> Unit
) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        text = fmt.format(Date(note.createdAt)),
                        color = Color(0x55FFFFFF), fontSize = 11.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${note.content.length} \u5b57",
                        color = Color(0x55FFFFFF), fontSize = 11.sp
                    )
                }
            }
            IconButton(onClick = { onToggleStar(note) }) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (note.starred) Gold else Color(0x33FFFFFF)
                )
            }
            IconButton(onClick = onToggleFullScreen) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = note.content.ifBlank { "\u8fd8\u6ca1\u6709\u5185\u5bb9..." },
            color = Color(0xDDFFFFFF), fontSize = 16.sp, lineHeight = 26.sp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FloatingActionButton(
                onClick = onEdit,
                containerColor = Blue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, null)
            }
        }
    }
}

// ==================== Editor Sheet ====================

@Composable
private fun EditorSheet(
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
                text = if (note == null) "\u521b\u9020\u65b0\u661f" else "\u7f16\u8f91",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (note != null) {
                IconButton(onClick = { onToggleStar(note) }) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (note.starred) Gold else Color(0x33FFFFFF)
                    )
                }
                IconButton(onClick = { onDelete(note) }) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B))
                }
            }
            IconButton(onClick = onToggleFullScreen) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x08FFFFFF), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(Blue),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text(
                            "\u6807\u9898",
                            color = Color(0x33FFFFFF), fontSize = 20.sp
                        )
                    }
                    inner()
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0x08FFFFFF), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    color = Color.White, fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                cursorBrush = SolidColor(Blue),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { inner ->
                    if (content.isEmpty()) {
                        Text(
                            "\u5f00\u59cb\u8bb0\u5f55...",
                            color = Color(0x33FFFFFF), fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${content.length} \u5b57",
                color = Color(0x44FFFFFF), fontSize = 12.sp
            )
            Row {
                TextButton(onClick = onDismiss) {
                    Text("\u53d6\u6d88", color = Color(0x66FFFFFF))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { onSave(note, title, content, note?.starred ?: false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("\u4fdd\u5b58", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
