package com.hellostar.stellarnotes.ui

import android.graphics.Paint as NativePaint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.hellostar.stellarnotes.data.Note
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class StarPos(val x: Float, val y: Float, val z: Float)

private data class BgStar(
    val x: Float, val y: Float,
    val size: Float, val baseAlpha: Float,
    val speed: Float, val phase: Float,
    val layer: Int, val colorTint: Int
)

fun computeStarPosition(note: Note, all: List<Note>): StarPos {
    val seed = note.id * 127L + note.title.hashCode() * 31L + 7919L
    val rng = java.util.Random(seed)
    val angle = rng.nextDouble() * Math.PI * 2.0
    val baseRange = 600.0 + all.size * 50.0
    val dist = if (note.starred) 100.0 + rng.nextDouble() * baseRange * 0.45
              else 250.0 + rng.nextDouble() * baseRange * 1.2
    return StarPos((cos(angle) * dist).toFloat(), (sin(angle) * dist).toFloat(), rng.nextFloat() * 150f)
}

private fun DrawScope.drawGlow(color: Color, radius: Float, center: Offset) {
    if (radius > 0f) drawCircle(brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius), radius = radius, center = center)
}

@Composable
fun StarFieldView(
    notes: List<Note>, tilt: Tilt, camX: Float, camY: Float, camZoom: Float,
    onPanZoom: (dx: Float, dy: Float, zoom: Float) -> Unit, onResetCamera: () -> Unit, onTapNote: (Note) -> Unit
) {
    val bgStars = remember {
        val rng = java.util.Random(42L)
        List(500) { BgStar(rng.nextFloat(), rng.nextFloat(), rng.nextFloat() * 2.8f + 0.3f, rng.nextFloat() * 0.7f + 0.2f, rng.nextFloat() * 2.5f + 0.3f, rng.nextFloat() * 6.28f, rng.nextInt(3), rng.nextInt(6)) }
    }
    val inf = rememberInfiniteTransition(label = "f")
    val time by inf.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val shootT by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "s")
    val pulse by inf.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val labelPaint = remember { NativePaint().apply { isAntiAlias = true; setShadowLayer(10f, 0f, 2f, android.graphics.Color.parseColor("#DD000000")) } }
    val bg = Brush.verticalGradient(listOf(Color(0xFF010308), Color(0xFF030918), Color(0xFF06122C), Color(0xFF091D42), Color(0xFF050E1E)))
    val cCamX by rememberUpdatedState(camX); val cCamY by rememberUpdatedState(camY); val cCamZoom by rememberUpdatedState(camZoom); val cTilt by rememberUpdatedState(tilt)

    Box(modifier = Modifier.fillMaxSize().background(bg)
        .pointerInput(notes) {
            detectTapGestures(onDoubleTap = { onResetCamera() }, onTap = { tap ->
                val cx = size.width / 2f; val cy = size.height / 2f; val tx = cTilt.x * 80f; val ty = cTilt.y * 80f
                var best: Note? = null; var bestD = Float.MAX_VALUE
                for (n in notes) { val p = computeStarPosition(n, notes); val dep = 1f / (1f + p.z.absoluteValue / 220f)
                    val sx = cx + (p.x + cCamX + tx) * dep * cCamZoom; val sy = cy + (p.y + cCamY + ty) * dep * cCamZoom
                    val hitR = (12f * dep * cCamZoom * 3f).coerceAtLeast(70f); val dx = tap.x - sx; val dy = tap.y - sy; val dist = sqrt(dx * dx + dy * dy)
                    if (dist < hitR && dist < bestD) { bestD = dist; best = n } }
                best?.let(onTapNote) })
        }.pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> onPanZoom(pan.x, pan.y, zoom) } }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val cx = w / 2f; val cy = h / 2f; val tx = tilt.x * 80f; val ty = tilt.y * 80f
            for (s in bgStars) {
                val px = when (s.layer) { 0 -> 0.03f; 1 -> 0.10f; else -> 0.22f }
                val vx = s.x * w + (camX * px + tx * px) * camZoom; val vy = s.y * h + (camY * px + ty * px) * camZoom
                val bx = (vx % w + w) % w; val by = (vy % h + h) % h
                val twinkle = sin(time * s.speed + s.phase) * 0.5f + 0.5f; val a = (s.baseAlpha * (0.3f + twinkle * 0.7f)).coerceIn(0f, 1f)
                val c = when (s.colorTint) { 0 -> Color(0xFFAABBDD); 1 -> Color(0xFFCCDDFF); 2 -> Color(0xFFFFEECC); 3 -> Color(0xFFBBCCEE); 4 -> Color(0xFFFFDDAA); else -> Color(0xFFEEF4FF) }
                val r = s.size * (0.85f + camZoom * 0.15f)
                if (r > 1.5f) { drawCircle(c.copy(alpha = (a * 0.2f).coerceIn(0f, 1f)), r * 4f, Offset(bx, by)); drawCircle(c.copy(alpha = (a * 0.45f).coerceIn(0f, 1f)), r * 2f, Offset(bx, by)) }
                drawCircle(c.copy(alpha = a), r, Offset(bx, by))
                if (r > 2.2f) drawCircle(Color.White.copy(alpha = (a * 0.7f).coerceIn(0f, 1f)), r * 0.3f, Offset(bx, by))
            }
            drawGlow(Color(0x0C2040A0), 500f * camZoom, Offset(cx + camX * 0.1f * camZoom, cy + camY * 0.1f * camZoom))
            drawGlow(Color(0x09503080), 400f * camZoom, Offset(cx + w * 0.3f * camZoom, cy - h * 0.2f * camZoom))
            drawGlow(Color(0x07204060), 350f * camZoom, Offset(cx - w * 0.25f * camZoom, cy + h * 0.3f * camZoom))
            if (shootT < 0.15f) { val t = shootT / 0.15f; val ssx = w * 0.85f; val ssy = h * 0.04f; val ex = w * 0.1f; val ey = h * 0.35f
                val curX = ssx + (ex - ssx) * t; val curY = ssy + (ey - ssy) * t; val dX = ex - ssx; val dY = ey - ssy; val len = sqrt(dX * dX + dY * dY); val alpha = ((1f - t) * 0.85f).coerceIn(0f, 1f)
                drawLine(Color.White.copy(alpha = (alpha * 0.5f).coerceIn(0f, 1f)), Offset(curX - dX / len * 120f, curY - dY / len * 120f), Offset(curX, curY), 2.5f); drawCircle(Color.White.copy(alpha = alpha), 3f, Offset(curX, curY)) }
            val s2 = (shootT + 0.55f) % 1f
            if (s2 < 0.12f) { val t = s2 / 0.12f; val sx2 = w * 0.15f; val sy2 = h * 0.12f; val ex2 = w * 0.75f; val ey2 = h * 0.5f
                val curX = sx2 + (ex2 - sx2) * t; val curY = sy2 + (ey2 - sy2) * t; val alpha = ((1f - t) * 0.65f).coerceIn(0f, 1f); val dX = ex2 - sx2; val dY = ey2 - sy2; val len = sqrt(dX * dX + dY * dY)
                drawLine(Color(0xFFAABBFF).copy(alpha = (alpha * 0.4f).coerceIn(0f, 1f)), Offset(curX - dX / len * 90f, curY - dY / len * 90f), Offset(curX, curY), 1.8f); drawCircle(Color(0xFFAABBFF).copy(alpha = alpha), 2.5f, Offset(curX, curY)) }
            val starredNotes = notes.filter { it.starred }
            if (starredNotes.size >= 2) { val positions = starredNotes.map { n -> val p = computeStarPosition(n, notes); val dep = 1f / (1f + p.z.absoluteValue / 220f); Offset(cx + (p.x + camX + tx) * dep * camZoom, cy + (p.y + camY + ty) * dep * camZoom) }
                for (i in 0 until positions.size - 1) drawLine(Color(0xFFFFD86B).copy(alpha = 0.1f), positions[i], positions[i + 1], 1.2f * camZoom) }
            for (note in notes.reversed()) {
                val pos = computeStarPosition(note, notes); val depth = 1f / (1f + pos.z.absoluteValue / 220f)
                val sx = cx + (pos.x + camX + tx) * depth * camZoom; val sy = cy + (pos.y + camY + ty) * depth * camZoom
                if (sx < -200 || sx > w + 200 || sy < -200 || sy > h + 200) continue
                val baseR = if (note.starred) 9f else 6f; val r = baseR * depth * camZoom
                val col = if (note.starred) Color(0xFFFFD86B) else Color(0xFFB0D4FF)
                val gp = if (note.starred) 1f + sin(pulse + note.id.toFloat() * 1.7f) * 0.25f else 1f + sin(pulse + note.id.toFloat()) * 0.1f
                drawGlow(col.copy(alpha = (0.12f * gp).coerceIn(0f, 1f)), r * 14f, Offset(sx, sy)); drawGlow(col.copy(alpha = (0.22f * gp).coerceIn(0f, 1f)), r * 7f, Offset(sx, sy))
                drawCircle(col.copy(alpha = (0.22f * gp).coerceIn(0f, 1f)), r * 3.5f * gp, Offset(sx, sy), style = Stroke(width = 1.3f * camZoom))
                if (note.starred) drawCircle(col.copy(alpha = 0.12f), r * 5.5f * gp, Offset(sx, sy), style = Stroke(width = 0.9f * camZoom))
                drawCircle(col.copy(alpha = 0.5f), r * 2f, Offset(sx, sy)); drawCircle(col, r, Offset(sx, sy)); drawCircle(Color.White.copy(alpha = 0.85f), r * 0.4f, Offset(sx, sy))
                if (note.starred) { val rl = r * 8f; drawLine(col.copy(alpha = 0.22f), Offset(sx - rl, sy), Offset(sx + rl, sy), 1.2f * camZoom); drawLine(col.copy(alpha = 0.22f), Offset(sx, sy - rl), Offset(sx, sy + rl), 1.2f * camZoom)
                    val d45 = rl * 0.6f; drawLine(col.copy(alpha = 0.12f), Offset(sx - d45, sy - d45), Offset(sx + d45, sy + d45), 0.9f * camZoom); drawLine(col.copy(alpha = 0.12f), Offset(sx + d45, sy - d45), Offset(sx - d45, sy + d45), 0.9f * camZoom) }
                val title = note.title.take(10); val la = (depth * 255).toInt().coerceIn(80, 255)
                labelPaint.color = if (note.starred) android.graphics.Color.argb(la, 255, 216, 107) else android.graphics.Color.argb(la, 176, 212, 255)
                labelPaint.textSize = (24f * depth * camZoom).coerceIn(12f, 40f)
                drawContext.canvas.nativeCanvas.drawText(title, sx + r * 2.5f + 8f * camZoom, sy + labelPaint.textSize * 0.35f, labelPaint)
            }
        }
    }
}
