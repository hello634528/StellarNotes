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
        List(200) {
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
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "t"
    )
    val shootT by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shoot"
    )
    val pulse by inf.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse"
    )

    val labelPaint = remember {
        NativePaint().apply {
            isAntiAlias = true
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
        }
    }

    val bg = Brush.verticalGradient(
        listOf(
            Color(0xFF010510),
            Color(0xFF031025),
            Color(0xFF061838),
            Color(0xFF0A1E48),
            Color(0xFF081530)
        )
    )

    // Capture latest state for pointer input without restarting the gesture
    val currentCamX by rememberUpdatedState(camX)
    val currentCamY by rememberUpdatedState(camY)
    val currentCamZoom by rememberUpdatedState(camZoom)
    val currentTilt by rememberUpdatedState(tilt)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(notes) { // Only restart if notes list changes
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
                            val r = (if (n.pinned) 12f else 7f) * dep * (if (n.starred) 1.3f else 1f) * currentCamZoom
                            // Generous touch target (at least 80px radius)
                            val hitRadius = (r * 4f).coerceAtLeast(80f)
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

            // ===== Layer 1: Background twinkling stars (with parallax) =====
            for (s in bgStars) {
                val px = when (s.layer) { 0 -> 0.05f; 1 -> 0.15f; else -> 0.3f }
                val vx = s.x * w + (camX * px + tx * px) * camZoom
                val vy = s.y * h + (camY * px + ty * px) * camZoom
                val bx = (vx % w + w) % w
                val by = (vy % h + h) % h
                val twinkle = sin(time * s.speed + s.phase) * 0.5f + 0.5f
                val a = s.baseAlpha * (0.25f + twinkle * 0.75f)
                val c = when (s.layer) {
                    0 -> Color(0xFFA0B0D0)
                    1 -> Color(0xFFBBCCEE)
                    else -> Color(0xFFDDE8FF)
                }
                val r = s.size * (0.8f + camZoom * 0.2f) // Slight zoom effect on bg
                if (r > 1.5f) {
                    drawCircle(c.copy(alpha = a * 0.1f), r * 4f, Offset(bx, by))
                    drawCircle(c.copy(alpha = a * 0.25f), r * 2f, Offset(bx, by))
                }
                drawCircle(c.copy(alpha = a), r, Offset(bx, by))
            }

            // ===== Layer 2: Nebula glow patches (with parallax) =====
            val n1x = cx + (w * 0.2f - cx + camX * 0.2f + tx * 6f) * camZoom
            val n1y = cy + (h * 0.15f - cy + camY * 0.2f + ty * 6f) * camZoom
            drawCircle(Color(0xFF1A2B6B).copy(alpha = 0.06f), 240f * camZoom, Offset(n1x, n1y))

            val n2x = cx + (w * 0.78f - cx + camX * 0.2f + tx * 4f) * camZoom
            val n2y = cy + (h * 0.5f - cy + camY * 0.2f + ty * 4f) * camZoom
            drawCircle(Color(0xFF2B1A5B).copy(alpha = 0.05f), 200f * camZoom, Offset(n2x, n2y))

            val n3x = cx + (w * 0.45f - cx + camX * 0.2f + tx * 5f) * camZoom
            val n3y = cy + (h * 0.82f - cy + camY * 0.2f + ty * 5f) * camZoom
            drawCircle(Color(0xFF0A2A5A).copy(alpha = 0.06f), 280f * camZoom, Offset(n3x, n3y))

            // ===== Layer 3: Shooting star =====
            if (shootT < 0.2f) {
                val t = shootT / 0.2f
                val sx = w * 0.82f; val sy = h * 0.06f
                val ex = w * 0.12f; val ey = h * 0.32f
                val curX = sx + (ex - sx) * t
                val curY = sy + (ey - sy) * t
                val dirX = ex - sx; val dirY = ey - sy
                val len = sqrt(dirX * dirX + dirY * dirY)
                val tailL = 90f
                val tailX = curX - dirX / len * tailL
                val tailY = curY - dirY / len * tailL
                val alpha = (1f - t) * 0.8f
                drawLine(Color.White.copy(alpha = alpha * 0.3f),
                    Offset(tailX, tailY), Offset(curX, curY), 2f)
                drawCircle(Color.White.copy(alpha = alpha), 2.5f, Offset(curX, curY))
            }

            // ===== Layer 4: Note stars =====
            for (note in notes) {
                val pos = computeStarPosition(note, notes)
                val depth = 1f / (1f + pos.z.absoluteValue / 220f)
                val sx = cx + (pos.x + camX + tx) * depth * camZoom
                val sy = cy + (pos.y + camY + ty) * depth * camZoom
                val baseR = if (note.pinned) 13f else 8f
                val mul = if (note.starred) 1.35f else 1f
                val r = baseR * depth * mul * camZoom

                // Don't draw if completely off-screen (optimization)
                if (sx < -100 || sx > w + 100 || sy < -100 || sy > h + 100) continue

                val col = when {
                    note.starred -> Color(0xFFFFD86B)
                    note.pinned -> Color(0xFF9FD4FF)
                    else -> Color(0xFFD7E7FF)
                }

                // Pulsing glow for pinned/starred
                val glowPulse = if (note.pinned || note.starred) {
                    1f + sin(pulse + note.id.toFloat()) * 0.15f
                } else 1f

                // Multi-layer glow halo
                drawCircle(col.copy(alpha = 0.03f), r * 7f * glowPulse, Offset(sx, sy))
                drawCircle(col.copy(alpha = 0.06f), r * 4.5f * glowPulse, Offset(sx, sy))
                drawCircle(col.copy(alpha = 0.14f), r * 2.5f, Offset(sx, sy))
                drawCircle(col.copy(alpha = 0.35f), r * 1.5f, Offset(sx, sy))
                // Core
                drawCircle(col, r, Offset(sx, sy))
                // Bright center
                drawCircle(Color.White.copy(alpha = 0.55f), r * 0.35f, Offset(sx, sy))

                // Cross rays for starred notes
                if (note.starred) {
                    val rl = r * 4f
                    drawLine(col.copy(alpha = 0.15f), Offset(sx - rl, sy), Offset(sx + rl, sy), 1f * camZoom)
                    drawLine(col.copy(alpha = 0.15f), Offset(sx, sy - rl), Offset(sx, sy + rl), 1f * camZoom)
                    val d45 = rl * 0.7f
                    drawLine(col.copy(alpha = 0.08f), Offset(sx - d45, sy - d45), Offset(sx + d45, sy + d45), 0.8f * camZoom)
                    drawLine(col.copy(alpha = 0.08f), Offset(sx + d45, sy - d45), Offset(sx - d45, sy + d45), 0.8f * camZoom)
                }

                // Pin indicator ring
                if (note.pinned && !note.starred) {
                    drawCircle(col.copy(alpha = 0.25f), r * 2f, Offset(sx, sy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * camZoom))
                }

                // Title label
                val title = note.title.take(6)
                val la = (depth * 255).toInt().coerceIn(50, 255)
                labelPaint.color = when {
                    note.starred -> android.graphics.Color.argb(la, 255, 216, 107)
                    note.pinned -> android.graphics.Color.argb(la, 159, 212, 255)
                    else -> android.graphics.Color.argb(la, 190, 210, 240)
                }
                labelPaint.textSize = (22f * depth * camZoom).coerceIn(10f, 40f)
                drawContext.canvas.nativeCanvas.drawText(
                    title, sx + r + 8f * camZoom, sy + labelPaint.textSize * 0.35f, labelPaint
                )
            }
        }
    }
}
