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
    val rng = java.util.Random(note.id * 73L + 17L)
    val range = 800f + all.size * 30f
    val x = (rng.nextFloat() - 0.5f) * range * 2f
    val y = (rng.nextFloat() - 0.5f) * range * 2f
    val z = rng.nextFloat() * 200f
    val factor = if (note.starred) 0.35f else 1f
    return StarPos(x * factor, y * factor, z)
}

private fun DrawScope.drawGlow(color: Color, radius: Float, center: Offset) {
    if (radius > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
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
        List(300) {
            BgStar(
                x = rng.nextFloat(), y = rng.nextFloat(),
                size = rng.nextFloat() * 1.6f + 0.2f,
                baseAlpha = rng.nextFloat() * 0.35f + 0.08f,
                speed = rng.nextFloat() * 2f + 0.5f,
                phase = rng.nextFloat() * 6.28f,
                layer = rng.nextInt(3)
            )
        }
    }

    val inf = rememberInfiniteTransition(label = "f")
    val time by inf.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    val shootT by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "s"
    )
    val pulse by inf.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "p"
    )

    val labelPaint = remember {
        NativePaint().apply {
            isAntiAlias = true
            setShadowLayer(8f, 0f, 2f, android.graphics.Color.parseColor("#CC000000"))
        }
    }

    val bg = Brush.verticalGradient(
        listOf(
            Color(0xFF010308), Color(0xFF030918), Color(0xFF06122C),
            Color(0xFF091D42), Color(0xFF050E1E)
        )
    )

    val cCamX by rememberUpdatedState(camX)
    val cCamY by rememberUpdatedState(camY)
    val cCamZoom by rememberUpdatedState(camZoom)
    val cTilt by rememberUpdatedState(tilt)

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
                        val tx = cTilt.x * 80f
                        val ty = cTilt.y * 80f
                        var best: Note? = null
                        var bestD = Float.MAX_VALUE
                        for (n in notes) {
                            val p = computeStarPosition(n, notes)
                            val dep = 1f / (1f + p.z.absoluteValue / 220f)
                            val sx = cx + (p.x + cCamX + tx) * dep * cCamZoom
                            val sy = cy + (p.y + cCamY + ty) * dep * cCamZoom
                            val hitR = (12f * dep * cCamZoom * 3f).coerceAtLeast(70f)
                            val dx = tap.x - sx
                            val dy = tap.y - sy
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist < hitR && dist < bestD) {
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

            // === Background twinkling stars (tiny, dim) ===
            for (s in bgStars) {
                val px = when (s.layer) { 0 -> 0.04f; 1 -> 0.12f; else -> 0.25f }
                val vx = s.x * w + (camX * px + tx * px) * camZoom
                val vy = s.y * h + (camY * px + ty * px) * camZoom
                val bx = (vx % w + w) % w
                val by = (vy % h + h) % h
                val twinkle = sin(time * s.speed + s.phase) * 0.5f + 0.5f
                val a = s.baseAlpha * (0.2f + twinkle * 0.8f)
                val c = when (s.layer) {
                    0 -> Color(0xFF7088B0)
                    1 -> Color(0xFF9AB0DA)
                    else -> Color(0xFFCCD8F0)
                }
                val r = s.size * (0.8f + camZoom * 0.2f)
                if (r > 1.2f) {
                    drawCircle(c.copy(alpha = (a * 0.15f).coerceIn(0f, 1f)), r * 3f, Offset(bx, by))
                }
                drawCircle(c.copy(alpha = a.coerceIn(0f, 1f)), r, Offset(bx, by))
            }

            // === Subtle nebula wisps ===
            drawGlow(Color(0x0A1830A0), 400f * camZoom, Offset(cx + camX * 0.15f * camZoom, cy + camY * 0.15f * camZoom))
            drawGlow(Color(0x08401860), 350f * camZoom, Offset(cx + (w * 0.3f + camX * 0.1f) * camZoom, cy + (-h * 0.2f + camY * 0.1f) * camZoom))

            // === Shooting star 1 ===
            if (shootT < 0.15f) {
                val t = shootT / 0.15f
                val ssx = w * 0.85f; val ssy = h * 0.04f
                val ex = w * 0.1f; val ey = h * 0.35f
                val curX = ssx + (ex - ssx) * t
                val curY = ssy + (ey - ssy) * t
                val dX = ex - ssx; val dY = ey - ssy
                val len = sqrt(dX * dX + dY * dY)
                val tailX = curX - dX / len * 100f
                val tailY = curY - dY / len * 100f
                val alpha = ((1f - t) * 0.8f).coerceIn(0f, 1f)
                drawLine(Color.White.copy(alpha = (alpha * 0.5f).coerceIn(0f, 1f)), Offset(tailX, tailY), Offset(curX, curY), 2f)
                drawCircle(Color.White.copy(alpha = alpha), 2.5f, Offset(curX, curY))
            }
            // === Shooting star 2 ===
            val s2 = (shootT + 0.6f) % 1f
            if (s2 < 0.12f) {
                val t = s2 / 0.12f
                val sx2 = w * 0.2f; val sy2 = h * 0.15f
                val ex2 = w * 0.7f; val ey2 = h * 0.55f
                val curX = sx2 + (ex2 - sx2) * t
                val curY = sy2 + (ey2 - sy2) * t
                val alpha = ((1f - t) * 0.6f).coerceIn(0f, 1f)
                val dX = ex2 - sx2; val dY = ey2 - sy2
                val len = sqrt(dX * dX + dY * dY)
                drawLine(
                    Color(0xFFAABBFF).copy(alpha = (alpha * 0.4f).coerceIn(0f, 1f)),
                    Offset(curX - dX / len * 80f, curY - dY / len * 80f),
                    Offset(curX, curY), 1.5f
                )
                drawCircle(Color(0xFFAABBFF).copy(alpha = alpha), 2f, Offset(curX, curY))
            }

            // === Constellation lines between starred notes ===
            val starredNotes = notes.filter { it.starred }
            if (starredNotes.size >= 2) {
                val positions = starredNotes.map { n ->
                    val p = computeStarPosition(n, notes)
                    val dep = 1f / (1f + p.z.absoluteValue / 220f)
                    Offset(
                        cx + (p.x + camX + tx) * dep * camZoom,
                        cy + (p.y + camY + ty) * dep * camZoom
                    )
                }
                for (i in 0 until positions.size - 1) {
                    drawLine(
                        Color(0xFFFFD86B).copy(alpha = 0.08f),
                        positions[i], positions[i + 1], 1f * camZoom
                    )
                }
            }

            // === Note stars (big, glowing, with ring) ===
            for (note in notes.reversed()) {
                val pos = computeStarPosition(note, notes)
                val depth = 1f / (1f + pos.z.absoluteValue / 220f)
                val sx = cx + (pos.x + camX + tx) * depth * camZoom
                val sy = cy + (pos.y + camY + ty) * depth * camZoom
                if (sx < -150 || sx > w + 150 || sy < -150 || sy > h + 150) continue

                val baseR = if (note.starred) 7f else 5f
                val r = baseR * depth * camZoom
                val col = if (note.starred) Color(0xFFFFD86B) else Color(0xFFB0D4FF)
                val gp = if (note.starred) {
                    1f + sin(pulse + note.id.toFloat() * 1.7f) * 0.25f
                } else {
                    1f + sin(pulse + note.id.toFloat()) * 0.1f
                }

                // Outer glow
                drawGlow(col.copy(alpha = (0.08f * gp).coerceIn(0f, 1f)), r * 10f, Offset(sx, sy))
                drawGlow(col.copy(alpha = 0.18f), r * 5f, Offset(sx, sy))

                // Animated ring
                val ringR = r * 3.5f * gp
                drawCircle(
                    col.copy(alpha = (0.2f * gp).coerceIn(0f, 1f)),
                    ringR, Offset(sx, sy),
                    style = Stroke(width = 1.2f * camZoom)
                )
                if (note.starred) {
                    val ringR2 = r * 5f * gp
                    drawCircle(
                        col.copy(alpha = 0.1f),
                        ringR2, Offset(sx, sy),
                        style = Stroke(width = 0.8f * camZoom)
                    )
                }

                // Core
                drawCircle(col.copy(alpha = 0.5f), r * 1.8f, Offset(sx, sy))
                drawCircle(col, r, Offset(sx, sy))
                drawCircle(Color.White.copy(alpha = 0.8f), r * 0.35f, Offset(sx, sy))

                // Cross rays for starred
                if (note.starred) {
                    val rl = r * 6f
                    drawLine(col.copy(alpha = 0.2f), Offset(sx - rl, sy), Offset(sx + rl, sy), 1f * camZoom)
                    drawLine(col.copy(alpha = 0.2f), Offset(sx, sy - rl), Offset(sx, sy + rl), 1f * camZoom)
                }

                // Label
                val title = note.title.take(10)
                val la = (depth * 255).toInt().coerceIn(80, 255)
                labelPaint.color = if (note.starred) {
                    android.graphics.Color.argb(la, 255, 216, 107)
                } else {
                    android.graphics.Color.argb(la, 176, 212, 255)
                }
                labelPaint.textSize = (22f * depth * camZoom).coerceIn(11f, 38f)
                drawContext.canvas.nativeCanvas.drawText(
                    title,
                    sx + r * 2.5f + 6f * camZoom,
                    sy + labelPaint.textSize * 0.35f,
                    labelPaint
                )
            }
        }
    }
}
