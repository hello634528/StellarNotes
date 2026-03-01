package com.hellostar.stellarnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hellostar.stellarnotes.data.Note
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

private val DeepBlue = Color(0xFF040A1E)
private val StarWhite = Color(0xFFD7E7FF)
private val StarGold = Color(0xFFFFD86B)
private val PinBlue = Color(0xFF9FD4FF)
private val PanelBg = Color(0x88112244)
private val CardBg = Color(0xEE0D1C42)

@Composable
fun StellarApp(viewModel: StellarViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val tilt = rememberTiltState()

    val cameraX = remember { Animatable(0f) }
    val cameraY = remember { Animatable(0f) }

    LaunchedEffect(state.focusedNoteId) {
        val noteId = state.focusedNoteId ?: return@LaunchedEffect
        val note = state.notes.firstOrNull { it.id == noteId } ?: return@LaunchedEffect
        val pos = computeStarPosition(note, state.notes)
        cameraX.animateTo(-pos.x, spring(stiffness = Spring.StiffnessLow))
        cameraY.animateTo(-pos.y, spring(stiffness = Spring.StiffnessLow))
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlue) {
        Box(modifier = Modifier.fillMaxSize()) {
            StarFieldCanvas(
                notes = state.notes,
                tilt = tilt,
                camX = cameraX.value,
                camY = cameraY.value,
                onTapNote = { note ->
                    editing = note
                    showEditor = true
                }
            )

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

            FloatingActionButton(
                onClick = {
                    editing = null
                    showEditor = true
                },
                containerColor = Color(0xFF1A3A7A),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }

            AnimatedVisibility(
                visible = showEditor,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                NoteEditorSheet(
                    note = editing,
                    onDismiss = { showEditor = false },
                    onSave = { old, title, content, pinned, starred ->
                        viewModel.addOrUpdate(old, title, content, pinned, starred)
                        showEditor = false
                    },
                    onToggleStar = { viewModel.toggleStar(it) },
                    onTogglePin = { viewModel.togglePin(it) },
                    onDelete = {
                        viewModel.deleteNote(it)
                        showEditor = false
                    }
                )
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
                label = { Text("\u641C\u7D22\u661F\u7FA4\u7B14\u8BB0", color = Color(0xAAFFFFFF)) },
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

data class StarPos(val x: Float, val y: Float, val z: Float)

private fun computeStarPosition(note: Note, all: List<Note>): StarPos {
    val sorted = all.sortedWith(
        compareByDescending<Note> { it.pinned }.thenBy { it.createdAt }
    )
    val idx = sorted.indexOfFirst { it.id == note.id }.coerceAtLeast(0)
    val ring = idx / 10 + 1
    val slot = idx % 10
    val angleDeg = slot * 36f + ring * 15f
    val rad = Math.toRadians(angleDeg.toDouble())
    val dist = if (note.pinned) 75f else 130f + ring * 40f
    return StarPos(
        x = (cos(rad) * dist).toFloat(),
        y = (sin(rad) * dist * 0.72f).toFloat(),
        z = ring * 30f - (if (note.pinned) 20f else 0f)
    )
}

@Composable
private fun StarFieldCanvas(
    notes: List<Note>,
    tilt: Tilt,
    camX: Float,
    camY: Float,
    onTapNote: (Note) -> Unit
) {
    val bg = Brush.verticalGradient(
        listOf(Color(0xFF030712), Color(0xFF071739), Color(0xFF0A1E48))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = center.x
            val cy = center.y
            val tiltX = tilt.x * 80f
            val tiltY = tilt.y * 80f

            for (note in notes) {
                val pos = computeStarPosition(note, notes)
                val depth = 1f / (1f + pos.z.absoluteValue / 220f)
                val sx = cx + (pos.x + camX + tiltX) * depth
                val sy = cy + (pos.y + camY + tiltY) * depth

                val baseRadius = if (note.pinned) 11f else 7f
                val starMul = if (note.starred) 1.2f else 1f
                val r = baseRadius * depth * starMul

                val color = when {
                    note.starred -> StarGold
                    note.pinned -> PinBlue
                    else -> StarWhite
                }

                drawCircle(color = color.copy(alpha = 0.15f), radius = r * 3f, center = Offset(sx, sy))
                drawCircle(color = color.copy(alpha = 0.3f), radius = r * 1.8f, center = Offset(sx, sy))
                drawCircle(color = color, radius = r, center = Offset(sx, sy))
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val nearest = notes.minByOrNull { n ->
                        val p = computeStarPosition(n, notes)
                        val dx = p.x + camX + tilt.x * 80f
                        val dy = p.y + camY + tilt.y * 80f
                        dx * dx + dy * dy
                    }
                    nearest?.let(onTapNote)
                }
        )
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
            .height(360.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (note == null) "\u65B0\u5EFA\u661F\u9645\u7B14\u8BB0" else "\u7F16\u8F91\u7B14\u8BB0",
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
                            imageVector = if (note.starred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (note.starred) StarGold else Color(0x88FFFFFF)
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
                label = { Text("\u5185\u5BB9", color = Color(0xAAFFFFFF)) },
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
                    Text("\u53D6\u6D88", color = Color(0xAAFFFFFF))
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
                    Text("\u4FDD\u5B58")
                }
            }
        }
    }
}
