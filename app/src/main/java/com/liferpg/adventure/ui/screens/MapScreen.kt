package com.liferpg.adventure.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.RegionDef
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.Tag
import com.liferpg.adventure.ui.components.XpBar
import com.liferpg.adventure.ui.components.colorOf
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class RegionState(val def: RegionDef, val unlocked: Boolean, val count: Int, val completions: Int)

private fun regionStates(ui: GameUi): List<RegionState> {
    val total = ui.completions.size
    return Catalog.regions.map { r ->
        val count = r.categoryIds.sumOf { ui.categoryCounts[it] ?: 0 }
        RegionState(r, ui.player.level >= r.unlockLevel && total >= r.unlockCompletions, count, total)
    }
}

@Composable
fun WorldMapScreen(ui: GameUi, onEnter: (RegionDef) -> Unit, onNewQuest: (RegionDef) -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    val states = regionStates(ui)
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = states.firstOrNull { it.def.id == selectedId } ?: states.first { it.unlocked }
    val stars = remember { List(70) { Triple(Random.nextFloat(), Random.nextFloat() * 0.45f, Random.nextFloat()) } }
    val inf = rememberInfiniteTransition(label = "map")
    val pulse by inf.animateFloat(0f, 6.2831855f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "p")

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("نقشهٔ جهان", "World map"), color = c.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            tr("هر سرزمین یک حوزهٔ رشد در زندگی واقعی توست. با سطح بالاتر و ماموریت‌های بیشتر، سرزمین‌های تازه باز می‌شوند.", "Every land is a real-life growth area. Level up and finish quests to unlock new lands."),
            color = c.textDim, fontSize = 12.sp,
        )
        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(0.85f).clip(RoundedCornerShape(22.dp)).border(1.dp, c.border, RoundedCornerShape(22.dp)),
        ) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1246), Color(0xFF2A1E5E), Color(0xFF123A4A), Color(0xFF0E2A1E))))
                for ((x, y, ph) in stars) drawCircle(Color.White.copy(alpha = 0.3f + 0.4f * (0.5f + 0.5f * sin(pulse + ph * 6f))), 1.3f + ph * 1.2f, Offset(x * w, y * h))
                // moon
                drawCircle(Brush.radialGradient(listOf(Color(0x66FFF3C4), Color.Transparent), Offset(w * 0.82f, h * 0.09f), w * 0.2f), w * 0.2f, Offset(w * 0.82f, h * 0.09f))
                drawCircle(Color(0xFFFFF3C4), w * 0.05f, Offset(w * 0.82f, h * 0.09f))
                // mountains (back, front)
                fun ridge(base: Float, amp: Float, color: Color, seed: Int) {
                    val r = Random(seed)
                    val p = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, h * base)
                        var x = 0f
                        while (x < w) {
                            x += w * 0.09f
                            lineTo(x - w * 0.045f, h * (base - amp * (0.4f + r.nextFloat() * 0.6f)))
                            lineTo(x, h * base)
                        }
                        lineTo(w, h); close()
                    }
                    drawPath(p, color)
                }
                ridge(0.40f, 0.12f, Color(0xFF241A55), 3)
                ridge(0.55f, 0.10f, Color(0xFF1B2F5E), 8)
                ridge(0.78f, 0.06f, Color(0xFF0F3B33), 5)
                // river
                val river = Path().apply { moveTo(w * 0.52f, h * 0.30f); cubicTo(w * 0.40f, h * 0.5f, w * 0.65f, h * 0.65f, w * 0.50f, h) }
                drawPath(river, Color(0x5539A0FF), style = Stroke(w * 0.035f, cap = StrokeCap.Round))
                // trees around the forest
                val rnd = Random(21)
                repeat(26) {
                    val tx = w * (0.08f + rnd.nextFloat() * 0.34f); val ty = h * (0.74f + rnd.nextFloat() * 0.2f)
                    val tree = Path().apply { moveTo(tx, ty - 16f); lineTo(tx - 9f, ty + 6f); lineTo(tx + 9f, ty + 6f); close() }
                    drawPath(tree, Color(0xFF1E7A5A).copy(alpha = 0.8f))
                }
                // roads between regions (unlock order)
                val ordered = states.sortedBy { it.def.unlockLevel }
                for (i in 0 until ordered.size - 1) {
                    val a = ordered[i]; val b = ordered[i + 1]
                    val pa = Offset(a.def.x * w, a.def.y * h); val pb = Offset(b.def.x * w, b.def.y * h)
                    val mid = Offset((pa.x + pb.x) / 2 + (if (i % 2 == 0) 0.12f else -0.12f) * w, (pa.y + pb.y) / 2)
                    val path = Path().apply { moveTo(pa.x, pa.y); quadraticBezierTo(mid.x, mid.y, pb.x, pb.y) }
                    val open = a.unlocked && b.unlocked
                    if (open) drawPath(path, Color(0x55FFC857), style = Stroke(10f, cap = StrokeCap.Round))
                    drawPath(path, if (open) Color(0xFFFFC857) else Color(0x66AAB4E0), style = Stroke(3f, cap = StrokeCap.Round, pathEffect = if (open) null else PathEffect.dashPathEffect(floatArrayOf(14f, 12f))))
                }
            }
            val nodePx = with(LocalDensity.current) { 58.dp.toPx() }
            for (st in states) {
                val r = st.def
                val col = colorOf(r.color)
                val isSel = st.def.id == selected.def.id
                Column(
                    Modifier.absoluteOffset { IntOffset((r.x * w - nodePx / 2).roundToInt(), (r.y * h - nodePx / 2).roundToInt()) }
                        .clickable { selectedId = r.id },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape)
                            .background(if (st.unlocked) Brush.radialGradient(listOf(col.copy(alpha = 0.55f), Color(0xFF0B1026))) else Brush.radialGradient(listOf(Color(0xFF3A3F5C), Color(0xFF14172B))))
                            .border(if (isSel) 3.dp else 2.dp, if (st.unlocked) (if (isSel) Color(0xFFFFC857) else col) else Color(0xFF5A6080), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (st.unlocked) r.emoji else "🔒", fontSize = 26.sp) }
                    Text(
                        if (lang == "fa") r.fa else r.en, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.background(Color(0x99000000), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }

        // ---- selected location card
        val r = selected.def
        val col = colorOf(r.color)
        RpgCard(accent = col) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.emoji, fontSize = 32.sp)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (lang == "fa") r.fa else r.en, color = c.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(r.categoryIds.mapNotNull { ui.category(it) }.joinToString(" · ") { it.emoji + " " + it.name(lang) }, color = c.textDim, fontSize = 12.sp)
                    }
                    if (selected.unlocked) Tag(if (selected.count >= r.masteryTarget) "👑 " + tr("تسلط", "Mastered") else tr("باز", "Open"), c.emerald) else Tag("🔒 " + tr("قفل", "Locked"), c.textDim)
                }
                Text(if (lang == "fa") r.descFa else r.descEn, color = c.text, fontSize = 13.sp)
                if (selected.unlocked) {
                    XpBar((selected.count.toFloat() / r.masteryTarget).coerceIn(0f, 1f), Modifier.fillMaxWidth(), colors = listOf(col.copy(alpha = 0.7f), col))
                    Text(tr("پیشرفت تسلط: ${num(selected.count.coerceAtMost(r.masteryTarget))} از ${num(r.masteryTarget)} ماموریت", "Mastery: ${selected.count.coerceAtMost(r.masteryTarget)} of ${r.masteryTarget} quests"), color = c.textDim, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        GlowButton(tr("ورود به ماموریت‌ها", "Enter quests"), { onEnter(r) }, Modifier.weight(1f), color = col)
                        OutlineButton(tr("ماموریت جدید", "New quest"), { onNewQuest(r) }, Modifier.weight(1f))
                    }
                } else {
                    val lvlOk = ui.player.level >= r.unlockLevel
                    val cntOk = selected.completions >= r.unlockCompletions
                    Text((if (lvlOk) "✅ " else "⬜ ") + tr("سطح ${num(r.unlockLevel)} (اکنون ${num(ui.player.level)})", "Level ${r.unlockLevel} (now ${ui.player.level})"), color = c.text, fontSize = 13.sp)
                    Text((if (cntOk) "✅ " else "⬜ ") + tr("${num(r.unlockCompletions)} ماموریت کامل‌شده (اکنون ${num(selected.completions)})", "${r.unlockCompletions} completed quests (now ${selected.completions})"), color = c.text, fontSize = 13.sp)
                    Text(tr("با ماموریت‌های روزمره این سرزمین به‌زودی باز می‌شود.", "Everyday quests will open this land soon."), color = c.textDim, fontSize = 12.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RewardChip("🗺️", tr("${num(states.count { it.unlocked })} از ${num(states.size)} سرزمین باز است", "${states.count { it.unlocked }} of ${states.size} lands open"), c.gold)
        }
        Spacer(Modifier.height(16.dp))
    }
}
