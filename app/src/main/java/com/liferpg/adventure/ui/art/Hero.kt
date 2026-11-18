package com.liferpg.adventure.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.liferpg.adventure.domain.AvatarDef
import com.liferpg.adventure.domain.ItemDef
import com.liferpg.adventure.domain.Rarity
import com.liferpg.adventure.ui.components.rarityColor
import kotlin.math.cos
import kotlin.math.sin

/** What the hero is wearing. Items change the picture: weapon shape/colour, armor, helmet and amulet. */
data class HeroLook(
    val avatar: AvatarDef,
    /** 0 = novice ... 3 = mythic (level 50+): grows a larger cape, pauldrons, halo and aura. */
    val tier: Int,
    val weapon: ItemDef? = null,
    val armor: ItemDef? = null,
    val helmet: ItemDef? = null,
    val accessory: ItemDef? = null,
)

private fun mix(a: Color, b: Color, f: Float) = Color(
    a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, 1f,
)

private fun glowOf(r: Rarity, fallback: Color) = if (r.ordinal >= Rarity.EPIC.ordinal) rarityColor(r) else fallback

/** Full-body gender-neutral fantasy hero, drawn with vectors so it works fully offline. */
fun DrawScope.drawHero(look: HeroLook, t: Float) {
    val u = size.height
    val cx = size.width / 2f
    fun p(x: Float, y: Float) = Offset(cx + x * u, y * u)
    val av = look.avatar
    val hair = Color(av.hair.toInt())
    val cloak = Color(av.cloak.toInt())
    val eye = Color(av.eye.toInt())
    val steel = Color(0xFF2A2F4A)
    val armorCol = look.armor?.let { rarityColor(it.rarity) }
    val tier = look.tier.coerceIn(0, 3)
    val bob = sin(t) * 0.004f
    val auraColor = armorCol ?: eye

    // ---- aura & magic circle
    drawCircle(Brush.radialGradient(listOf(auraColor.copy(alpha = 0.20f + 0.08f * tier), Color.Transparent), center = p(0f, 0.5f), radius = 0.5f * u), 0.5f * u, p(0f, 0.5f))
    drawOval(auraColor.copy(alpha = 0.55f), topLeft = p(-0.34f, 0.885f), size = Size(0.68f * u, 0.095f * u), style = Stroke(2.5f))
    drawOval(auraColor.copy(alpha = 0.30f), topLeft = p(-0.26f, 0.896f), size = Size(0.52f * u, 0.073f * u), style = Stroke(1.5f))
    for (i in 0 until 12) {
        val a = i * 0.5236f + t * 0.3f
        drawCircle(auraColor.copy(alpha = 0.7f), 2.2f, p(0.30f * cos(a), 0.932f + 0.040f * sin(a)))
    }

    // ---- halo for the mythic tier
    if (tier >= 3) drawOval(Color(0xFFFFD66B).copy(alpha = 0.85f), topLeft = p(-0.10f, 0.045f + bob), size = Size(0.2f * u, 0.045f * u), style = Stroke(3f))

    // ---- cape
    val flare = 0.22f + 0.035f * tier
    val sway = sin(t * 1.3f) * 0.01f
    val cape = Path().apply {
        moveTo(p(-0.12f, 0.29f + bob).x, p(-0.12f, 0.29f + bob).y)
        cubicTo(p(-flare - 0.04f, 0.5f).x, p(-flare - 0.04f, 0.5f).y, p(-flare - 0.06f + sway, 0.75f).x, p(-flare - 0.06f, 0.75f).y, p(-flare, 0.89f).x, p(-flare, 0.89f).y)
        if (tier >= 2) {
            for (i in 0..5) {
                val x = -flare + (2 * flare) * (i / 5f)
                lineTo(p(x, 0.89f - (if (i % 2 == 0) 0.0f else 0.045f)).x, p(x, 0.89f - (if (i % 2 == 0) 0.0f else 0.045f)).y)
            }
        } else lineTo(p(flare, 0.89f).x, p(flare, 0.89f).y)
        cubicTo(p(flare + 0.06f + sway, 0.75f).x, p(flare + 0.06f, 0.75f).y, p(flare + 0.04f, 0.5f).x, p(flare + 0.04f, 0.5f).y, p(0.12f, 0.29f + bob).x, p(0.12f, 0.29f + bob).y)
        close()
    }
    drawPath(cape, Brush.verticalGradient(listOf(mix(cloak, Color.White, 0.1f), cloak, mix(cloak, Color.Black, 0.55f)), startY = 0.29f * u, endY = 0.89f * u))
    drawPath(cape, auraColor.copy(alpha = 0.35f), style = Stroke(1.6f))

    // ---- legs & boots
    for (side in listOf(-1f, 1f)) {
        drawRoundRect(steel, topLeft = p(side * 0.055f - 0.034f, 0.60f), size = Size(0.068f * u, 0.27f * u), cornerRadius = CornerRadius(8f, 8f))
        drawRoundRect(mix(steel, Color.Black, 0.35f), topLeft = p(side * 0.055f - 0.042f, 0.82f), size = Size(0.085f * u, 0.065f * u), cornerRadius = CornerRadius(8f, 8f))
        drawLine(auraColor.copy(alpha = 0.5f), p(side * 0.055f, 0.62f), p(side * 0.055f, 0.80f), 1.5f)
    }

    // ---- torso
    val torsoBase = armorCol?.let { mix(steel, it, 0.28f) } ?: mix(cloak, Color.White, 0.18f)
    val torso = Path().apply {
        moveTo(p(-0.14f, 0.30f + bob).x, p(-0.14f, 0.30f + bob).y)
        lineTo(p(0.14f, 0.30f + bob).x, p(0.14f, 0.30f + bob).y)
        lineTo(p(0.095f, 0.62f).x, p(0.095f, 0.62f).y)
        lineTo(p(-0.095f, 0.62f).x, p(-0.095f, 0.62f).y)
        close()
    }
    drawPath(torso, Brush.verticalGradient(listOf(mix(torsoBase, Color.White, 0.15f), torsoBase, mix(torsoBase, Color.Black, 0.4f)), startY = 0.30f * u, endY = 0.62f * u))
    drawPath(torso, (armorCol ?: eye).copy(alpha = 0.7f), style = Stroke(1.8f))
    drawRect(mix(steel, Color.Black, 0.3f), topLeft = p(-0.10f, 0.575f), size = Size(0.2f * u, 0.03f * u))
    // chest emblem
    val em = Path().apply {
        moveTo(p(0f, 0.37f).x, p(0f, 0.37f).y); lineTo(p(0.03f, 0.41f).x, p(0.03f, 0.41f).y)
        lineTo(p(0f, 0.46f).x, p(0f, 0.46f).y); lineTo(p(-0.03f, 0.41f).x, p(-0.03f, 0.41f).y); close()
    }
    drawPath(em, eye.copy(alpha = 0.85f + 0.15f * sin(t * 2f)))

    // ---- pauldrons
    if (look.armor != null || tier >= 1) for (side in listOf(-1f, 1f)) {
        drawCircle(mix(steel, armorCol ?: cloak, 0.35f), 0.048f * u, p(side * 0.15f, 0.31f + bob))
        drawCircle((armorCol ?: eye).copy(alpha = 0.8f), 0.048f * u, p(side * 0.15f, 0.31f + bob), style = Stroke(1.6f))
        if (tier >= 2) {
            val sp = Path().apply {
                moveTo(p(side * 0.13f, 0.27f).x, p(side * 0.13f, 0.27f).y)
                lineTo(p(side * 0.17f, 0.20f).x, p(side * 0.17f, 0.20f).y)
                lineTo(p(side * 0.19f, 0.29f).x, p(side * 0.19f, 0.29f).y); close()
            }
            drawPath(sp, (armorCol ?: eye).copy(alpha = 0.9f))
        }
    }

    // ---- arms
    val skin = Color(0xFFEFC9A6)
    val handR = p(0.19f, 0.52f)
    drawLine(mix(steel, cloak, 0.3f), p(-0.15f, 0.33f), p(-0.17f, 0.54f), 0.05f * u, StrokeCap.Round)
    drawLine(mix(steel, cloak, 0.3f), p(0.15f, 0.33f), handR, 0.05f * u, StrokeCap.Round)
    drawCircle(skin, 0.022f * u, p(-0.17f, 0.55f))
    drawCircle(skin, 0.022f * u, handR)

    // ---- weapon
    val w = look.weapon
    if (w != null) {
        val c = rarityColor(w.rarity)
        val glow = glowOf(w.rarity, c)
        val id = w.id
        val top = when {
            id.contains("staff") -> 0.10f
            id.contains("spear") -> 0.04f
            else -> 0.24f
        }
        if (w.rarity.ordinal >= Rarity.EPIC.ordinal) {
            drawLine(glow.copy(alpha = 0.35f + 0.15f * sin(t * 3f)), p(0.19f, 0.58f), p(0.19f, top), 0.045f * u, StrokeCap.Round)
        }
        when {
            id.contains("staff") -> {
                drawLine(mix(Color(0xFF6B4B2A), c, 0.25f), p(0.19f, 0.70f), p(0.19f, top + 0.03f), 0.014f * u, StrokeCap.Round)
                drawCircle(c, 0.032f * u, p(0.19f, top))
                drawCircle(Color.White.copy(alpha = 0.7f), 0.012f * u, p(0.185f, top - 0.008f))
            }
            id.contains("spear") -> {
                drawLine(mix(Color(0xFF6B4B2A), c, 0.2f), p(0.19f, 0.72f), p(0.19f, top + 0.06f), 0.012f * u, StrokeCap.Round)
                val tip = Path().apply {
                    moveTo(p(0.19f, top).x, p(0.19f, top).y); lineTo(p(0.215f, top + 0.07f).x, p(0.215f, top + 0.07f).y)
                    lineTo(p(0.165f, top + 0.07f).x, p(0.165f, top + 0.07f).y); close()
                }
                drawPath(tip, c)
            }
            else -> {
                val blade = Path().apply {
                    moveTo(p(0.19f, top).x, p(0.19f, top).y); lineTo(p(0.21f, top + 0.06f).x, p(0.21f, top + 0.06f).y)
                    lineTo(p(0.205f, 0.50f).x, p(0.205f, 0.50f).y); lineTo(p(0.175f, 0.50f).x, p(0.175f, 0.50f).y)
                    lineTo(p(0.17f, top + 0.06f).x, p(0.17f, top + 0.06f).y); close()
                }
                drawPath(blade, Brush.horizontalGradient(listOf(mix(c, Color.White, 0.5f), c), startX = p(0.17f, 0f).x, endX = p(0.21f, 0f).x))
                drawLine(Color(0xFF8A6A3A), p(0.145f, 0.505f), p(0.235f, 0.505f), 0.016f * u, StrokeCap.Round)
                drawLine(Color(0xFF5A4326), p(0.19f, 0.51f), p(0.19f, 0.58f), 0.014f * u, StrokeCap.Round)
            }
        }
    }

    // ---- amulet
    look.accessory?.let { a ->
        val c = rarityColor(a.rarity)
        drawLine(c.copy(alpha = 0.7f), p(-0.04f, 0.30f), p(0f, 0.355f), 1.4f)
        drawLine(c.copy(alpha = 0.7f), p(0.04f, 0.30f), p(0f, 0.355f), 1.4f)
        drawCircle(c, 0.014f * u, p(0f, 0.36f))
        drawCircle(c.copy(alpha = 0.3f + 0.2f * sin(t * 2f)), 0.03f * u, p(0f, 0.36f))
    }

    // ---- hair (behind head for long style)
    val hc = p(0f, 0.19f + bob)
    val hr = 0.072f * u
    val style = av.id % 3
    if (style == 1) {
        val longHair = Path().apply {
            moveTo(hc.x - hr * 1.05f, hc.y); lineTo(hc.x - hr * 1.2f, hc.y + 0.17f * u)
            lineTo(hc.x + hr * 1.2f, hc.y + 0.17f * u); lineTo(hc.x + hr * 1.05f, hc.y); close()
        }
        drawPath(longHair, mix(hair, Color.Black, 0.15f))
    }
    // ---- head
    drawCircle(skin, hr, hc)
    drawCircle(Color.Black.copy(alpha = 0.08f), hr, Offset(hc.x + hr * 0.2f, hc.y + hr * 0.2f), style = Stroke(hr * 0.4f))
    when (style) {
        0 -> {
            val h = Path().apply {
                moveTo(hc.x - hr * 1.05f, hc.y + hr * 0.1f)
                for (i in 0..5) {
                    val x = hc.x - hr * 1.05f + (hr * 2.1f) * (i / 5f)
                    val y = hc.y - hr * (if (i % 2 == 0) 1.0f else 1.55f)
                    lineTo(x, y)
                }
                lineTo(hc.x + hr * 1.05f, hc.y + hr * 0.1f); lineTo(hc.x + hr * 0.5f, hc.y - hr * 0.35f)
                lineTo(hc.x - hr * 0.5f, hc.y - hr * 0.35f); close()
            }
            drawPath(h, hair)
        }
        1 -> {
            val h = Path().apply {
                moveTo(hc.x - hr * 1.08f, hc.y + hr * 0.3f)
                quadraticBezierTo(hc.x - hr * 1.1f, hc.y - hr * 1.3f, hc.x, hc.y - hr * 1.2f)
                quadraticBezierTo(hc.x + hr * 1.1f, hc.y - hr * 1.3f, hc.x + hr * 1.08f, hc.y + hr * 0.3f)
                lineTo(hc.x + hr * 0.4f, hc.y - hr * 0.3f); lineTo(hc.x - hr * 0.4f, hc.y - hr * 0.3f); close()
            }
            drawPath(h, hair)
        }
        else -> {
            val h = Path().apply {
                moveTo(hc.x - hr * 1.05f, hc.y + hr * 0.1f)
                quadraticBezierTo(hc.x - hr * 1.05f, hc.y - hr * 1.25f, hc.x, hc.y - hr * 1.2f)
                quadraticBezierTo(hc.x + hr * 1.05f, hc.y - hr * 1.25f, hc.x + hr * 1.05f, hc.y + hr * 0.1f)
                quadraticBezierTo(hc.x + hr * 0.3f, hc.y - hr * 0.5f, hc.x - hr * 0.2f, hc.y - hr * 0.4f)
                quadraticBezierTo(hc.x - hr * 0.7f, hc.y - hr * 0.2f, hc.x - hr * 1.05f, hc.y + hr * 0.1f); close()
            }
            drawPath(h, hair)
        }
    }
    // ---- face
    for (side in listOf(-1f, 1f)) {
        drawOval(eye, topLeft = Offset(hc.x + side * hr * 0.42f - hr * 0.13f, hc.y - hr * 0.02f), size = Size(hr * 0.26f, hr * 0.34f))
        drawOval(Color.White.copy(alpha = 0.9f), topLeft = Offset(hc.x + side * hr * 0.42f - hr * 0.06f, hc.y + hr * 0.02f), size = Size(hr * 0.08f, hr * 0.1f))
    }
    drawArc(Color(0xFF8B5A4A), 20f, 140f, false, topLeft = Offset(hc.x - hr * 0.2f, hc.y + hr * 0.32f), size = Size(hr * 0.4f, hr * 0.22f), style = Stroke(1.6f, cap = StrokeCap.Round))

    // ---- helmet
    look.helmet?.let { hm ->
        val c = rarityColor(hm.rarity)
        val metal = mix(steel, c, 0.45f)
        val cap = Path().apply {
            moveTo(hc.x - hr * 1.12f, hc.y + hr * 0.35f)
            quadraticBezierTo(hc.x - hr * 1.15f, hc.y - hr * 1.45f, hc.x, hc.y - hr * 1.35f)
            quadraticBezierTo(hc.x + hr * 1.15f, hc.y - hr * 1.45f, hc.x + hr * 1.12f, hc.y + hr * 0.35f)
            lineTo(hc.x + hr * 0.8f, hc.y - hr * 0.25f); lineTo(hc.x - hr * 0.8f, hc.y - hr * 0.25f); close()
        }
        drawPath(cap, metal)
        drawPath(cap, c, style = Stroke(1.6f))
        drawLine(c, Offset(hc.x, hc.y - hr * 1.35f), Offset(hc.x, hc.y - hr * 1.95f), 0.012f * u, StrokeCap.Round)
        drawCircle(c, 0.011f * u, Offset(hc.x, hc.y - hr * 1.98f))
    }

    // ---- rune particles (higher tiers)
    for (i in 0 until tier * 3) {
        val a = t * 0.8f + i * 2.1f
        drawCircle(auraColor.copy(alpha = 0.7f), 2.5f, p(0.30f * cos(a), 0.55f + 0.30f * sin(a * 0.7f + i)))
    }
}

@Composable
fun HeroView(look: HeroLook, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "hero")
    val t by inf.animateFloat(0f, 6.2831855f, infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Canvas(modifier) { drawHero(look, t) }
}

/** Round portrait used in headers and lists. */
@Composable
fun HeroPortrait(look: HeroLook, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val av = look.avatar
        val r = size.minDimension / 2f
        drawCircle(Brush.verticalGradient(listOf(Color(av.cloak.toInt()).copy(alpha = 0.9f), Color(0xFF0B1026))), r)
        drawCircle(Color(0xFFEFC9A6), r * 0.42f, Offset(size.width / 2, size.height * 0.5f))
        val hair = Color(av.hair.toInt())
        val path = Path().apply {
            moveTo(size.width / 2 - r * 0.46f, size.height * 0.5f)
            quadraticBezierTo(size.width / 2 - r * 0.5f, size.height * 0.06f, size.width / 2, size.height * 0.08f)
            quadraticBezierTo(size.width / 2 + r * 0.5f, size.height * 0.06f, size.width / 2 + r * 0.46f, size.height * 0.5f)
            lineTo(size.width / 2 + r * 0.2f, size.height * 0.34f)
            lineTo(size.width / 2 - r * 0.2f, size.height * 0.34f)
            close()
        }
        drawPath(path, hair)
        val eye = Color(av.eye.toInt())
        drawCircle(eye, r * 0.06f, Offset(size.width / 2 - r * 0.17f, size.height * 0.5f))
        drawCircle(eye, r * 0.06f, Offset(size.width / 2 + r * 0.17f, size.height * 0.5f))
        drawCircle(Color(0xFFFFC857), r, style = Stroke(r * 0.07f))
    }
}
