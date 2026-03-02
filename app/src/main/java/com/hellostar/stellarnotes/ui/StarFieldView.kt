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
    val layer: Int
)

fun computeStarPosition(note: Note, all: List<Note>): StarPos {
    val sorted = all.sortedWith(
        compareByDescending<Note> { it.starred }.thenBy { it.createdAt }
    )
    val idx = sorted.indexOfFirst { it.id == note.id }.coerceAtLeast(0)

    // Use note id as stable seed for random offset
    val rng = java.util.Random(note.id * 31L + 7L)
    val jitterX = (rng.nextFloat() - 0.5f) * 80f
    val jitterY = (rng.nextFloat() - 0.5f) * 80f

    val phi = (1 + sqrt(5.0)) / 2
    val angleRad = idx * phi * Math.PI * 0.62
    // Much wider spacing: starred ~120+, normal ~220+
    val dist = if (note.starred) 120.0 + idx * 28.0 else 220.0 + idx * 35.0

    return StarPos(
        x = (cos(angleRad) * dist).toFloat() + jitterX,
        y = (sin(angleRad) * dist * 0.72).toFloat() + jitterY,
        z = (idx * 3f).coerceAtMost(350f)
    )
}

@Composable
fun StarFieldView(
    notes: List<Note>,
    tilt: Tilt,
    camX: Float,
    camY: Float,
    camZoom: Float,
    onPanZoom: (dx: Float, dy: Float, zoom: Float) -> Unit,
    onResetCamera: () -> Unit,
    onTapNote: (Note) -> Unit
) {
    val bgStars = remember {
        val rng = java.util.Random(42L)
        List(250) {
            BgStar(
                x = rng.nextFloat(), y = rng.nextFloat(),
                size = rng.nextFloat() * 2.2f + 0.3f,
                baseAlpha = rng.nextFloat() * 0.5f + 0.15f,
                speed = rng.nextFloat() * 2f + 0.5f,
                phase = rng.nextFloat() * 6.28f,
                layer = rng.nextInt(3)
            )
        }
    }

    val inf = rememberInfiniteTransition(label = "field")
    val time by inf.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    val shootT by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "shoot"
    )
    val pulse by inf.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )

    val labelPaint = remember {
        NativePaint().apply {
            isAntiAlias = true
            setShadowLayer(6f, 0f, 0f, android.graphics.Color.parseColor("#88000000"))
        }
    }

    val bg = Brush.verticalGradient(listOf(
        Color(0xFF02040A), Color(0xFF040A1A), Color(0xFF08142A),
        Color(0xFF0A1E48), Color(0xFF061022)
    ))

    val currentCamX by rememberUpdatedState(camX)
    val currentCamY by rememberUpdatedState(camY)
    val currentCamZoom by rememberUpdatedState(camZoom)
    val currentTilt by rememberUpdatedState(tilt)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(notes) {
                detectTapGestures(
                    onDoubleTap = { onResetCamera() },
                    onTap = { tap ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val tx = currentTilt.x * 80f
                        val ty = currentTilt.y * 80f
                        var best: Note? = null
                        var bestD = Float.MAX_VALUE
                        for (n in notes) {
                            val p = computeStarPosition(n, notes)
                            val dep = 1f / (1f + p.z.absoluteValue / 220f)
                            val sx = cx + (p.x + currentCamX + tx) * dep * currentCamZoom
                            val sy = cy + (p.y + currentCamY + ty) * dep * currentCamZoom
                            val r = 8f * dep * (if (n.starred) 1.5f else 1f) * currentCamZoom
                            val hitRadius = (r * 4f).coerceAtLeast(90f)
                            val dx = tap.x - sx
                            val dy = tap.y - sy
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist < hitRadius && dist < bestD) {
                                bestD = dist
                                best = n
                            }
                        }
                        best?.let(onTapNote)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onPanZoom(pan.x, pan.y, zoom)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val tx = tilt.x * 80f
            val ty = tilt.y * 80f

            // Background twinkling stars
            for (s in bgStars) {
                val px = when (s.layer) { 0 -> 0.05f; 1 -> 0.15f; else -> 0.3f }
                val vx = s.x * w + (camX * px + tx * px) * camZoom
                val vy = s.y * h + (camY * px + ty * px) * camZoom
                val bx = (vx % w + w) % w
                val by = (vy % h + h) % h
                val twinkle = sin(time * s.speed + s.phase) * 0.5f + 0.5f
                val a = s.baseAlpha * (0.25f + twinkle * 0.75f)
                val c = when (s.layer) {
                    0 -> Color(0xFF90A4CE); 1 -> Color(0xFFB4C8F0); else -> Color(0xFFE6F0FF)
                }
                val r = s.size * (0.8f + camZoom * 0.2f)
                if (r > 1.5f) {
                    drawCircle(c.copy(alpha = a * 0.15f), r * 5f, Offset(bx, by))
                    drawCircle(c.copy(alpha = a * 0.3f), r * 2.5f, Offset(bx, by))
                }
                drawCircle(c.copy(alpha = a), r, Offset(bx, by))
            }

            // Nebula (radial gradient)
            val drawNebula = { color: Color, radius: Float, center: Offset ->
                if (radius > 0) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius),
                        radius = radius, center = center
                    )
                }
            }
            val n1x = cx + (w * 0.2f - cx + camX * 0.2f + tx * 6f) * camZoom
            val n1y = cy + (h * 0.15f - cy + camY * 0.2f + ty * 6f) * camZoom
            drawNebula(Color(0x2216245A), 350f * camZoom, Offset(n1x, n1y))
            val n2x = cx + (w * 0.78f - cx + camX * 0.2f + tx * 4f) * camZoom
            val n2y = cy + (h * 0.5f - cy + camY * 0.2f + ty * 4f) * camZoom
            drawNebula(Color(0x1A2B1A5B), 300f * camZoom, Offset(n2x, n2y))

            // Galaxy core (subtle)
            val coreX = cx + (camX + tx) * camZoom
            val coreY = cy + (camY + ty) * camZoom
            drawNebula(Color(0x11FFD86B), 180f * camZoom, Offset(coreX, coreY))
            drawNebula(Color(0x189FD4FF), 80f * camZoom, Offset(coreX, coreY))

            // Shooting star
            if (shootT < 0.2f) {
                val t = shootT / 0.2f
                val sx = w * 0.82f; val sy = h * 0.06f
                val ex = w * 0.12f; val ey = h * 0.32f
                val curX = sx + (ex - sx) * t
                val curY = sy + (ey - sy) * t
                val dirX = ex - sx; val dirY = ey - sy
                val len = sqrt(dirX * dirX + dirY * dirY)
                val tailX = curX - dirX / len * 120f
                val tailY = curY - dirY / len * 120f
                val alpha = (1f - t) * 0.9f
                drawLine(Color.White.copy(alpha = alpha * 0.4f), Offset(tailX, tailY), Offset(curX, curY), 3f)
                drawCircle(Color.White.copy(alpha = alpha), 3f, Offset(curX, curY))
            }

            // Note stars
            for (note in notes.reversed()) {
                val pos = computeStarPosition(note, notes)
                val depth = 1f / (1f + pos.z.absoluteValue / 220f)
                val sx = cx + (pos.x + camX + tx) * depth * camZoom
                val sy = cy + (pos.y + camY + ty) * depth * camZoom
                val r = 8f * depth * (if (note.starred) 1.4f else 1f) * camZoom

                if (sx < -100 || sx > w + 100 || sy < -100 || sy > h + 100) continue

                val col = if (note.starred) Color(0xFFFFD86B) else Color(0xFFD7E7FF)
                val glowPulse = if (note.starred) 1f + sin(pulse + note.id.toFloat()) * 0.2f else 1f

                drawNebula(col.copy(alpha = 0.12f * glowPulse), r * 6f, Offset(sx, sy))
                drawNebula(col.copy(alpha = 0.25f), r * 3f, Offset(sx, sy))
                drawCircle(col, r, Offset(sx, sy))
                drawCircle(Color.White.copy(alpha = 0.7f), r * 0.4f, Offset(sx, sy))

                if (note.starred) {
                    val rl = r * 4.5f
                    drawLine(col.copy(alpha = 0.25f), Offset(sx - rl, sy), Offset(sx + rl, sy), 1.5f * camZoom)
                    drawLine(col.copy(alpha = 0.25f), Offset(sx, sy - rl), Offset(sx, sy + rl), 1.5f * camZoom)
                }

                val title = note.title.take(8)
                val la = (depth * 255).toInt().coerceIn(60, 255)
                labelPaint.color = if (note.starred) android.graphics.Color.argb(la, 255, 216, 107)
                                   else android.graphics.Color.argb(la, 215, 231, 255)
                labelPaint.textSize = (24f * depth * camZoom).coerceIn(12f, 45f)
                drawContext.canvas.nativeCanvas.drawText(
                    title, sx + r + 10f * camZoom, sy + labelPaint.textSize * 0.35f, labelPaint
                )
            }
        }
    }
}
