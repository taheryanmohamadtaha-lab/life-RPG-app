package com.liferpg.adventure.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.liferpg.adventure.domain.PetDef
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun lighten(c: Color, f: Float) = Color(
    red = c.red + (1f - c.red) * f, green = c.green + (1f - c.green) * f, blue = c.blue + (1f - c.blue) * f, alpha = c.alpha,
)

private fun darken(c: Color, f: Float) = Color(red = c.red * (1f - f), green = c.green * (1f - f), blue = c.blue * (1f - f), alpha = c.alpha)

/** Four-pointed sparkle. */
private fun DrawScope.sparkle(center: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticBezierTo(center.x + r * 0.12f, center.y - r * 0.12f, center.x + r, center.y)
        quadraticBezierTo(center.x + r * 0.12f, center.y + r * 0.12f, center.x, center.y + r)
        quadraticBezierTo(center.x - r * 0.12f, center.y + r * 0.12f, center.x - r, center.y)
        quadraticBezierTo(center.x - r * 0.12f, center.y - r * 0.12f, center.x, center.y - r)
        close()
    }
    drawPath(p, color)
}

/**
 * Original vector dragon companion, three species x three growth stages.
 * [stage]: 1 = hatchling, 2 = young dragon, 3 = guardian dragon. [t] is an animation phase in radians.
 */
fun DrawScope.drawDragon(pet: PetDef, stage: Int, t: Float) {
    val s = min(size.width, size.height)
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    val st = stage.coerceIn(1, 3)
    val body = Color(pet.body.toInt())
    val belly = Color(pet.belly.toInt())
    val accent = Color(pet.accent.toInt())
    val glow = Color(pet.glow.toInt())
    val bob = sin(t) * 0.012f * s
    val flap = sin(t * 2f) * 0.025f

    translate(ox, oy + bob) {
        fun o(x: Float, y: Float) = Offset(x * s, y * s)
        fun sz(w: Float, h: Float) = Size(w * s, h * s)

        // ---- aura
        val auraR = when (st) { 1 -> 0.38f; 2 -> 0.46f; else -> 0.54f }
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.38f), Color.Transparent), center = o(0.5f, 0.55f), radius = auraR * s), radius = auraR * s, center = o(0.5f, 0.55f))

        // ---- geometry per stage
        val headC = when (st) { 1 -> o(0.5f, 0.40f); 2 -> o(0.5f, 0.34f); else -> o(0.5f, 0.27f) }
        val headR = when (st) { 1 -> 0.21f; 2 -> 0.18f; else -> 0.15f } * s
        val bodyCy = when (st) { 1 -> 0.68f; 2 -> 0.65f; else -> 0.63f }
        val bodyRx = when (st) { 1 -> 0.17f; 2 -> 0.20f; else -> 0.22f }
        val bodyRy = when (st) { 1 -> 0.19f; 2 -> 0.23f; else -> 0.25f }

        // ---- tail (behind the body)
        val tail = Path().apply {
            moveTo(0.58f * s, (bodyCy + 0.1f) * s)
            when (st) {
                1 -> cubicTo(0.80f * s, 0.90f * s, 0.92f * s, 0.74f * s, 0.84f * s, 0.62f * s)
                2 -> cubicTo(0.86f * s, 0.95f * s, 0.98f * s, 0.74f * s, 0.88f * s, 0.55f * s)
                else -> cubicTo(0.88f * s, 1.0f * s, 1.02f * s, 0.70f * s, 0.88f * s, 0.46f * s)
            }
        }
        drawPath(tail, darken(body, 0.15f), style = Stroke(width = (0.075f + 0.012f * st) * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(tail, body, style = Stroke(width = (0.055f + 0.012f * st) * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val tip = when (st) { 1 -> o(0.84f, 0.62f); 2 -> o(0.88f, 0.55f); else -> o(0.88f, 0.46f) }
        when (pet.id) {
            2 -> { // ember: flame
                val fl = Path().apply {
                    moveTo(tip.x - 0.03f * s, tip.y)
                    quadraticBezierTo(tip.x - 0.04f * s, tip.y - 0.07f * s, tip.x, tip.y - (0.11f + 0.01f * sin(t * 3f)) * s)
                    quadraticBezierTo(tip.x + 0.04f * s, tip.y - 0.07f * s, tip.x + 0.03f * s, tip.y)
                    close()
                }
                drawPath(fl, accent); drawCircle(Color(0xFFFF6A00), radius = 0.02f * s, center = Offset(tip.x, tip.y - 0.015f * s))
            }
            1 -> { // crystal cluster
                for (i in -1..1) {
                    val c = Path().apply {
                        moveTo(tip.x + i * 0.03f * s - 0.012f * s, tip.y)
                        lineTo(tip.x + i * 0.035f * s, tip.y - (0.07f + 0.02f * (1 - kotlin.math.abs(i))) * s)
                        lineTo(tip.x + i * 0.03f * s + 0.012f * s, tip.y)
                        close()
                    }
                    drawPath(c, accent)
                }
            }
            else -> { // aqua: tail fin
                val fin = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(tip.x - 0.05f * s, tip.y - 0.08f * s)
                    lineTo(tip.x + 0.01f * s, tip.y - 0.05f * s)
                    lineTo(tip.x + 0.05f * s, tip.y - 0.08f * s)
                    close()
                }
                drawPath(fin, accent)
            }
        }

        // ---- wings
        val span = when (st) { 1 -> 0.26f; 2 -> 0.40f; else -> 0.48f }
        val topY = when (st) { 1 -> 0.52f; 2 -> 0.38f; else -> 0.20f } - flap
        val drop = when (st) { 1 -> 0.14f; 2 -> 0.22f; else -> 0.30f }
        for (side in listOf(-1f, 1f)) {
            val sx = 0.5f + side * 0.13f
            val tx = 0.5f + side * (0.13f + span)
            val w = Path().apply {
                moveTo(sx * s, (bodyCy - 0.06f) * s)
                cubicTo((sx + side * span * 0.15f) * s, (topY + 0.02f) * s, (tx - side * span * 0.25f) * s, (topY - 0.03f) * s, tx * s, topY * s)
                quadraticBezierTo((tx - side * span * 0.02f) * s, (topY + drop * 0.55f) * s, (tx - side * span * 0.32f) * s, (topY + drop * 0.5f) * s)
                quadraticBezierTo((tx - side * span * 0.42f) * s, (topY + drop * 0.85f) * s, (tx - side * span * 0.62f) * s, (topY + drop * 0.78f) * s)
                quadraticBezierTo((sx + side * span * 0.2f) * s, (bodyCy + 0.02f) * s, sx * s, (bodyCy + 0.02f) * s)
                close()
            }
            drawPath(w, Brush.verticalGradient(listOf(accent.copy(alpha = 0.95f), lighten(accent, 0.35f).copy(alpha = 0.8f)), startY = topY * s, endY = (topY + drop) * s))
            drawPath(w, darken(body, 0.1f), style = Stroke(width = 0.012f * s, join = StrokeJoin.Round))
        }

        // ---- body
        drawOval(
            Brush.verticalGradient(listOf(lighten(body, 0.12f), body, darken(body, 0.18f)), startY = (bodyCy - bodyRy) * s, endY = (bodyCy + bodyRy) * s),
            topLeft = o(0.5f - bodyRx, bodyCy - bodyRy), size = sz(bodyRx * 2, bodyRy * 2),
        )
        drawOval(
            Brush.verticalGradient(listOf(lighten(belly, 0.25f), belly)),
            topLeft = o(0.5f - bodyRx * 0.62f, bodyCy - bodyRy * 0.55f), size = sz(bodyRx * 1.24f, bodyRy * 1.5f),
        )
        if (st >= 2) for (i in 0..2) { // belly plates
            val y = bodyCy - bodyRy * 0.2f + i * bodyRy * 0.38f
            drawArc(darken(belly, 0.12f), 20f, 140f, false, topLeft = o(0.5f - bodyRx * 0.45f, y - 0.03f), size = sz(bodyRx * 0.9f, 0.06f), style = Stroke(0.008f * s, cap = StrokeCap.Round))
        }
        // arms
        for (side in listOf(-1f, 1f)) {
            drawOval(darken(body, 0.05f), topLeft = o(0.5f + side * (bodyRx * 0.95f) - 0.035f, bodyCy - 0.03f), size = sz(0.07f, 0.11f))
        }
        // feet
        for (side in listOf(-1f, 1f)) {
            val fx = 0.5f + side * bodyRx * 0.5f
            drawOval(darken(body, 0.12f), topLeft = o(fx - 0.06f, bodyCy + bodyRy - 0.04f), size = sz(0.12f, 0.06f))
            for (k in -1..1) drawCircle(lighten(belly, 0.4f), 0.008f * s, o(fx + k * 0.03f, bodyCy + bodyRy + 0.012f))
        }
        // back spikes
        if (st >= 2) for (side in listOf(-1f, 1f)) for (i in 0..2) {
            val yy = bodyCy - bodyRy * 0.5f + i * bodyRy * 0.45f
            val xx = 0.5f + side * (bodyRx * (0.92f - 0.05f * i))
            val sp = Path().apply {
                moveTo(xx * s, (yy - 0.02f) * s); lineTo((xx + side * 0.035f) * s, yy * s); lineTo(xx * s, (yy + 0.025f) * s); close()
            }
            drawPath(sp, accent)
        }

        // ---- neck (stage 3 elegant guardian)
        if (st == 3) drawOval(body, topLeft = o(0.5f - 0.07f, 0.34f), size = sz(0.14f, 0.22f))

        // ---- ears / fins (species)
        when (pet.id) {
            0 -> for (side in listOf(-1f, 1f)) {
                val ear = Path().apply {
                    moveTo(headC.x + side * headR * 0.85f, headC.y - headR * 0.1f)
                    lineTo(headC.x + side * headR * (1.55f + 0.1f * st), headC.y - headR * (0.75f + 0.15f * sin(t * 2f)))
                    lineTo(headC.x + side * headR * 0.9f, headC.y - headR * 0.6f)
                    close()
                }
                drawPath(ear, darken(body, 0.05f)); drawPath(ear, accent.copy(alpha = 0.5f))
            }
            1 -> for (side in listOf(-1f, 1f)) {
                val ear = Path().apply {
                    moveTo(headC.x + side * headR * 0.8f, headC.y - headR * 0.2f)
                    lineTo(headC.x + side * headR * 1.5f, headC.y - headR * 0.9f)
                    lineTo(headC.x + side * headR * 1.0f, headC.y - headR * 0.9f)
                    close()
                }
                drawPath(ear, lighten(body, 0.1f)); drawPath(ear, accent.copy(alpha = 0.55f))
            }
            else -> Unit
        }

        // ---- head
        drawCircle(
            Brush.radialGradient(listOf(lighten(body, 0.2f), body, darken(body, 0.15f)), center = Offset(headC.x - headR * 0.3f, headC.y - headR * 0.4f), radius = headR * 1.5f),
            radius = headR, center = headC,
        )
        // snout
        drawOval(belly, topLeft = Offset(headC.x - headR * 0.5f, headC.y + headR * 0.15f), size = Size(headR, headR * 0.7f))
        drawCircle(darken(belly, 0.3f), headR * 0.05f, Offset(headC.x - headR * 0.14f, headC.y + headR * 0.42f))
        drawCircle(darken(belly, 0.3f), headR * 0.05f, Offset(headC.x + headR * 0.14f, headC.y + headR * 0.42f))
        drawArc(darken(belly, 0.45f), 15f, 150f, false, topLeft = Offset(headC.x - headR * 0.18f, headC.y + headR * 0.45f), size = Size(headR * 0.36f, headR * 0.25f), style = Stroke(headR * 0.05f, cap = StrokeCap.Round))
        // cheeks
        for (side in listOf(-1f, 1f)) drawCircle(accent.copy(alpha = 0.45f), headR * 0.13f, Offset(headC.x + side * headR * 0.62f, headC.y + headR * 0.28f))
        // eyes
        val blink = if (sin(t * 0.5f + 1f) > 0.985f) 0.15f else 1f
        for (side in listOf(-1f, 1f)) {
            val ec = Offset(headC.x + side * headR * 0.4f, headC.y - headR * 0.05f)
            drawOval(Color(0xFF10142B), topLeft = Offset(ec.x - headR * 0.16f, ec.y - headR * 0.2f * blink), size = Size(headR * 0.32f, headR * 0.4f * blink))
            if (blink > 0.5f) {
                drawCircle(Color.White, headR * 0.07f, Offset(ec.x - headR * 0.04f, ec.y - headR * 0.08f))
                drawCircle(Color.White.copy(alpha = 0.8f), headR * 0.035f, Offset(ec.x + headR * 0.05f, ec.y + headR * 0.07f))
            }
        }
        // horns / forehead gem / crest
        val hornH = when (st) { 1 -> 0.07f; 2 -> 0.10f; else -> 0.14f } * s
        for (side in listOf(-1f, 1f)) {
            val bx = headC.x + side * headR * 0.45f
            val by = headC.y - headR * 0.88f
            val horn = Path().apply {
                moveTo(bx - 0.022f * s, by + 0.01f * s)
                quadraticBezierTo(bx + side * 0.01f * s, by - hornH * 0.5f, bx + side * 0.03f * s, by - hornH)
                quadraticBezierTo(bx + 0.025f * s, by - hornH * 0.3f, bx + 0.022f * s, by + 0.01f * s)
                close()
            }
            drawPath(horn, accent)
        }
        if (pet.id == 1) {
            val g = Path().apply {
                moveTo(headC.x, headC.y - headR * 0.72f); lineTo(headC.x + headR * 0.1f, headC.y - headR * 0.55f)
                lineTo(headC.x, headC.y - headR * 0.38f); lineTo(headC.x - headR * 0.1f, headC.y - headR * 0.55f); close()
            }
            drawPath(g, accent); drawPath(g, Color.White.copy(alpha = 0.3f), style = Stroke(1.5f))
        }
        if (st == 3) for (i in -2..2) { // crystal / flame crest behind the head
            val cx = headC.x + i * headR * 0.28f
            val h = headR * (0.55f - 0.08f * kotlin.math.abs(i))
            val cr = Path().apply { moveTo(cx - headR * 0.07f, headC.y - headR * 0.92f); lineTo(cx, headC.y - headR * 0.92f - h); lineTo(cx + headR * 0.07f, headC.y - headR * 0.92f); close() }
            drawPath(cr, lighten(accent, 0.2f))
        }

        // ---- sparkles
        for (i in 0 until (st + 2)) {
            val a = t + i * 1.7f
            sparkle(o(0.5f + 0.4f * cos(a * 0.7f + i), 0.5f + 0.32f * sin(a * 0.9f + i * 2f)), (0.014f + 0.006f * sin(a * 2f)) * s + 0.008f * s, lighten(glow, 0.5f).copy(alpha = 0.85f))
        }
    }
}

@Composable
fun DragonView(pet: PetDef, stage: Int, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "dragon")
    val t by inf.animateFloat(0f, 6.2831855f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Canvas(modifier) { drawDragon(pet, stage, t) }
}

/** A glowing arched alcove that shows one evolution stage (locked stages are dimmed). */
@Composable
fun DragonPod(pet: PetDef, stage: Int, unlocked: Boolean, modifier: Modifier = Modifier) {
    val glow = Color(pet.glow.toInt())
    Box(modifier.aspectRatio(0.72f)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val arch = Path().apply {
                moveTo(w * 0.1f, h * 0.88f)
                lineTo(w * 0.1f, h * 0.32f)
                cubicTo(w * 0.1f, h * 0.04f, w * 0.9f, h * 0.04f, w * 0.9f, h * 0.32f)
                lineTo(w * 0.9f, h * 0.88f)
            }
            drawPath(arch, glow.copy(alpha = if (unlocked) 0.75f else 0.25f), style = Stroke(width = 3f, cap = StrokeCap.Round))
            drawOval(glow.copy(alpha = if (unlocked) 0.5f else 0.15f), topLeft = Offset(w * 0.06f, h * 0.84f), size = Size(w * 0.88f, h * 0.1f), style = Stroke(2.5f))
            drawOval(Brush.radialGradient(listOf(glow.copy(alpha = if (unlocked) 0.45f else 0.1f), Color.Transparent), center = Offset(w / 2, h * 0.88f), radius = w * 0.5f), topLeft = Offset(w * 0.06f, h * 0.8f), size = Size(w * 0.88f, h * 0.16f))
        }
        DragonView(pet, stage, Modifier.fillMaxSize().alpha(if (unlocked) 1f else 0.28f))
    }
}
