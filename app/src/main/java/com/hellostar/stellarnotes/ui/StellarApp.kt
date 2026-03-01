package com.hellostar.stellarnotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hellostar.stellarnotes.data.Note
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StellarApp(viewModel: StellarViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val tilt = rememberTiltState()

    val cameraX = remember { Animatable(0f) }
    val cameraY = remember { Animatable(0f) }

    LaunchedEffect(state.focusedNoteId, state.notes) {
        val n = state.notes.firstOrNull { it.id == state.focusedNoteId } ?: return@LaunchedEffect
        val p = toStellarPosition(n, state.notes)
        cameraX.animateTo(-p.x, spring(stiffness = Spring.StiffnessLow))
        cameraY.animateTo(-p.y, spring(stiffness = Spring.StiffnessLow))
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF040A1E)) {
        Box {
            StarField(state.notes, tilt, cameraX.value, cameraY.value) {
                editing = it
                showEditor = true
            }

            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                SearchPanel(state.query, state.searchResults, viewModel::onQueryChange) { viewModel.focusOn(it.id) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { editing = null; showEditor = true }) { Text("新建") }
                }
            }

            AnimatedVisibility(showEditor, Modifier.align(Alignment.BottomCenter)) {
                EditorSheet(
                    note = editing,
                    onClose = { showEditor = false },
                    onSave = { old, t, c, p, s -> viewModel.addOrUpdate(old, t, c, p, s); showEditor = false },
                    onPinToggle = viewModel::togglePin,
                    onStarToggle = viewModel::toggleStar,
                    onDelete = { viewModel.delete(it); showEditor = false }
                )
            }
        }
    }
}

@Composable
private fun SearchPanel(query: String, results: List<SearchResult>, onQueryChange: (String) -> Unit, onSelect: (Note) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x55213A78))
    ) {
        Column(Modifier.padding(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("搜索星群") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (query.isNotBlank()) {
                LazyColumn(Modifier.height(150.dp)) {
                    items(results.take(8), key = { it.note.id }) { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(r.note) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (r.note.starred) "⭐ ${r.note.title}" else r.note.title,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text("%.1f".format(r.score), color = Color(0xFF8AC6FF))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StarField(notes: List<Note>, tilt: Tilt, cameraX: Float, cameraY: Float, onClickNote: (Note) -> Unit) {
    val bg = Brush.verticalGradient(listOf(Color(0xFF030712), Color(0xFF071739), Color(0xFF0A1E48)))
    Box(Modifier.fillMaxSize().background(bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            notes.forEach { n ->
                val p = toStellarPosition(n, notes)
                val depth = 1f / (1f + p.z.absoluteValue / 220f)
                val x = c.x + (p.x + cameraX + tilt.x * 80f) * depth
                val y = c.y + (p.y + cameraY + tilt.y * 80f) * depth
                val r = (if (n.pinned) 11f else 8f) * depth * if (n.starred) 1.15f else 1f
                val col = when {
                    n.starred -> Color(0xFFFFD86B)
                    n.pinned -> Color(0xFF9FD4FF)
                    else -> Color(0xFFD7E7FF)
                }
                drawCircle(col.copy(alpha = 0.2f), r * 2.3f, Offset(x, y))
                drawCircle(col, r, Offset(x, y))
            }
        }

        Box(Modifier.matchParentSize().clickable {
            val target = notes.minByOrNull {
                val p = toStellarPosition(it, notes)
                (p.x + cameraX + tilt.x * 80f).absoluteValue + (p.y + cameraY + tilt.y * 80f).absoluteValue
            }
            target?.let(onClickNote)
        })
    }
}

data class StarPos(val x: Float, val y: Float, val z: Float)

private fun toStellarPosition(note: Note, all: List<Note>): StarPos {
    val sorted = all.sortedWith(compareByDescending<Note> { it.pinned }.thenBy { it.createdAt })
    val idx = sorted.indexOfFirst { it.id == note.id }.coerceAtLeast(0)
    val ring = (idx / 10) + 1
    val angle = (idx % 10) * 36f
    val rad = Math.toRadians(angle.toDouble())
    val d = if (note.pinned) 70f else 120f + ring * 36f
    val x = (cos(rad) * d).toFloat()
    val y = (sin(rad) * d * 0.72f).toFloat()
    val z = ring * 32f - if (note.pinned) 20f else 0f
    return StarPos(x, y, z)
}

@Composable
private fun EditorSheet(
    note: Note?,
    onClose: () -> Unit,
    onSave: (Note?, String, String, Boolean, Boolean) -> Unit,
    onPinToggle: (Note) -> Unit,
    onStarToggle: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    var title by remember(note?.id) { mutableStateOf(note?.title ?: "") }
    var content by remember(note?.id) { mutableStateOf(note?.content ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth().height(340.dp),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE0D1C42))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (note == null) "新建星际笔记" else "编辑笔记", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (note != null) {
                    IconButton(onClick = { onPinToggle(note) }) { Icon(Icons.Default.PushPin, null, tint = Color(0xFFAED7FF)) }
                    IconButton(onClick = { onStarToggle(note) }) { Icon(if (note.starred) Icons.Default.Star else Icons.Outlined.StarBorder, null, tint = if (note.starred) Color(0xFFFFD86B) else Color.White) }
                    IconButton(onClick = { onDelete(note) }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF8A8A)) }
                }
            }
            OutlinedTextField(title, { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(content, { content = it }, label = { Text("内容") }, modifier = Modifier.fillMaxWidth().weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) { Text("取消") }
                Button(onClick = { onSave(note, title, content, note?.pinned ?: false, note?.starred ?: false) }, modifier = Modifier.width(120.dp)) { Text("保存") }
            }
        }
    }
}
