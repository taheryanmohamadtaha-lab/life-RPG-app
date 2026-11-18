package com.liferpg.adventure.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.domain.Digits
import com.liferpg.adventure.domain.Rarity
import com.liferpg.adventure.ui.theme.LocalLang
import com.liferpg.adventure.ui.theme.LocalRpg
import kotlin.math.sin
import kotlin.random.Random

/** Localised number (Persian digits when the UI is Persian). */
@Composable
fun num(n: Int): String = Digits.localize(n, LocalLang.current)

@Composable
fun num(n: Long): String = Digits.localize(n, LocalLang.current)

@Composable
fun numText(s: String): String = Digits.localize(s, LocalLang.current)

fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> Color(0xFFB0B8CC)
    Rarity.UNCOMMON -> Color(0xFF4ADE80)
    Rarity.RARE -> Color(0xFF39A0FF)
    Rarity.EPIC -> Color(0xFFB36BFF)
    Rarity.LEGENDARY -> Color(0xFFFFB02E)
    Rarity.MYTHIC -> Color(0xFFFF5C93)
}

fun colorOf(argb: Long): Color = Color(argb.toInt())

// ----------------------------------------------------------------------------------------------- background

/** Dark-fantasy gradient with a gently twinkling starfield. */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = LocalRpg.current
    val stars = remember { List(60) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    val inf = rememberInfiniteTransition(label = "stars")
    val t by inf.animateFloat(0f, 6.2831f, infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.bgTop, c.bg, c.bg)))) {
        if (c.isDark) {
            Canvas(Modifier.fillMaxSize()) {
                for ((x, y, ph) in stars) {
                    val a = 0.25f + 0.5f * (0.5f + 0.5f * sin(t + ph * 6f))
                    drawCircle(Color.White.copy(alpha = a), radius = 1.2f + ph * 1.4f, center = Offset(x * size.width, y * size.height))
                }
            }
        }
        content()
    }
}

// ----------------------------------------------------------------------------------------------- cards & buttons

@Composable
fun RpgCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalRpg.current
    val shape = RoundedCornerShape(18.dp)
    val border = Brush.linearGradient(listOf((accent ?: c.magic).copy(alpha = 0.55f), c.border))
    val base = modifier
        .clip(shape)
        .background(Brush.verticalGradient(listOf(c.panelHi, c.panel)))
        .border(1.dp, border, shape)
    val m = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(m.padding(padding), content = content)
}

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalRpg.current.magic,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.72f))))
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = LocalRpg.current.textDim, enabled: Boolean = true) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(shape)
            .border(1.dp, color.copy(alpha = 0.7f), shape)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

/** Small pill used for rewards (coins, XP, gems...). */
@Composable
fun RewardChip(emoji: String, text: String, color: Color = LocalRpg.current.gold, modifier: Modifier = Modifier) {
    val c = LocalRpg.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(c.track)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(emoji, fontSize = 12.sp)
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) { Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val c = LocalRpg.current
    Row(modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(c.gold, c.purple))))
        Spacer(Modifier.width(8.dp))
        Text(text, color = c.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
    }
}

// ----------------------------------------------------------------------------------------------- progress

@Composable
fun XpBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 10.dp, colors: List<Color>? = null) {
    val c = LocalRpg.current
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800), label = "bar")
    val inf = rememberInfiniteTransition(label = "shine")
    val shine by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "s")
    val fill = colors ?: listOf(c.purple, c.magic)
    Box(modifier.height(height).clip(RoundedCornerShape(50)).background(c.track)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(anim).clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(fill)),
        )
        Canvas(Modifier.matchParentSize()) {
            val w = size.width * anim
            if (w > 8f) {
                val x = shine * (w + 80f) - 40f
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent), startX = x - 30f, endX = x + 30f),
                    size = Size(w, size.height),
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------- screens

/** Header + scrolling content used by secondary screens. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalRpg.current
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.panelHi).border(1.dp, c.border, CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = c.text) }
            } else Spacer(Modifier.size(40.dp))
            Text(
                title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            if (action != null) Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { action() } else Spacer(Modifier.size(40.dp))
        }
        val body = if (scroll) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier.weight(1f)
        Column(body.fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun EmptyHint(emoji: String, text: String, modifier: Modifier = Modifier) {
    val c = LocalRpg.current
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(emoji, fontSize = 42.sp)
        Text(text, color = c.textDim, textAlign = TextAlign.Center, fontSize = 14.sp)
    }
}

// ----------------------------------------------------------------------------------------------- confetti

private class Particle(val x: Float, val vx: Float, val vy: Float, val rot: Float, val spin: Float, val size: Float, val color: Color)

@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, colors: List<Color>, count: Int = 80, durationMs: Int = 2400) {
    val particles = remember {
        val r = Random(System.nanoTime())
        List(count) {
            Particle(
                x = 0.5f + (r.nextFloat() - 0.5f) * 0.2f, vx = (r.nextFloat() - 0.5f) * 1.3f, vy = -0.5f - r.nextFloat() * 0.9f,
                rot = r.nextFloat() * 360f, spin = (r.nextFloat() - 0.5f) * 900f, size = 6f + r.nextFloat() * 8f, color = colors[r.nextInt(colors.size)],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        val fade = if (p > 0.75f) (1f - p) / 0.25f else 1f
        for (pt in particles) {
            val px = (pt.x + pt.vx * p) * size.width
            val py = (0.45f + pt.vy * p * 0.6f + 1.4f * p * p) * size.height
            rotate(pt.rot + pt.spin * p, pivot = Offset(px, py)) {
                drawRect(pt.color.copy(alpha = fade.coerceIn(0f, 1f)), topLeft = Offset(px - pt.size / 2, py - pt.size / 3), size = Size(pt.size, pt.size * 0.6f))
            }
        }
    }
}
