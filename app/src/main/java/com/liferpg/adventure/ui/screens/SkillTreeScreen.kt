package com.liferpg.adventure.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.SkillDef
import com.liferpg.adventure.domain.SkillRules
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.ScreenScaffold
import com.liferpg.adventure.ui.components.Tag
import com.liferpg.adventure.ui.components.colorOf
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SkillTreeScreen(ui: GameUi, vm: GameViewModel, onBack: () -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { Catalog.skill(it) }
    val cats = Catalog.categories

    ScreenScaffold(tr("درخت مهارت‌ها", "Skill tree"), onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RewardChip("🌟", tr("امتیاز مهارت: ", "Skill points: ") + num(ui.player.skillPoints), c.emerald)
            RewardChip("📈", tr("خرج‌شده: ", "Spent: ") + num(SkillRules.pointsSpent(ui.ranks)), c.gold)
        }
        Text(tr("با هر سطح جدید یک امتیاز مهارت می‌گیری. مهارت‌ها پاداش XP و ویژگی‌های بازی را بالا می‌برند.", "You earn one skill point per level. Skills raise XP bonuses and in-game attributes."), color = c.textDim, fontSize = 12.sp)

        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(0.95f).clip(RoundedCornerShape(22.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF2A1D66), Color(0xFF0B1026))))
                .border(1.dp, c.border, RoundedCornerShape(22.dp)),
        ) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val nodePx = with(LocalDensity.current) { 46.dp.toPx() }
            val cx = w / 2f
            val cy = h / 2f
            val rMax = min(w, h) / 2f - nodePx * 0.8f
            fun pos(branch: Int, tier: Int): Offset {
                val a = -PI / 2 + branch * 2 * PI / cats.size
                val r = rMax * when (tier) { 0 -> 0f; 1 -> 0.40f; 2 -> 0.70f; else -> 1.0f }
                return Offset(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat())
            }
            Canvas(Modifier.matchParentSize()) {
                for ((i, cat) in cats.withIndex()) {
                    val col = Color(cat.color.toInt())
                    var prev = pos(i, 0)
                    for (t in 1..3) {
                        val def = Catalog.skill("${cat.id}_$t") ?: continue
                        val next = pos(i, t)
                        val on = (ui.ranks[def.parentId ?: ""] ?: 0) >= def.parentRank
                        if (on) drawLine(col.copy(alpha = 0.35f), prev, next, 9f, StrokeCap.Round)
                        drawLine(if (on) col else Color(0xFF4A5278), prev, next, 3f, StrokeCap.Round)
                        prev = next
                    }
                }
                drawCircle(Brush.radialGradient(listOf(Color(0x66FFC857), Color.Transparent), Offset(cx, cy), nodePx * 1.4f), nodePx * 1.4f, Offset(cx, cy))
            }
            // hub
            Box(
                Modifier.absoluteOffset { IntOffset((cx - nodePx * 0.6f).roundToInt(), (cy - nodePx * 0.6f).roundToInt()) }
                    .size(((nodePx * 1.2f) / LocalDensity.current.density).dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(c.gold, Color(0xFF7A4F00)))).border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(num(ui.player.level), color = Color(0xFF241A00), fontWeight = FontWeight.Bold, fontSize = 15.sp) }

            for ((i, cat) in cats.withIndex()) {
                val col = colorOf(cat.color)
                for (t in 1..3) {
                    val def = Catalog.skill("${cat.id}_$t") ?: continue
                    val rank = ui.ranks[def.id] ?: 0
                    val open = (ui.ranks[def.parentId ?: ""] ?: 0) >= def.parentRank
                    val p = pos(i, t)
                    val isSel = selectedId == def.id
                    Box(
                        Modifier.absoluteOffset { IntOffset((p.x - nodePx / 2).roundToInt(), (p.y - nodePx / 2).roundToInt()) }
                            .size(46.dp).clip(CircleShape)
                            .background(
                                when {
                                    rank > 0 -> Brush.radialGradient(listOf(col.copy(alpha = 0.9f), Color(0xFF12183A)))
                                    open -> Brush.radialGradient(listOf(col.copy(alpha = 0.35f), Color(0xFF12183A)))
                                    else -> Brush.radialGradient(listOf(Color(0xFF30365A), Color(0xFF12183A)))
                                },
                            )
                            .border(if (isSel) 3.dp else 2.dp, if (isSel) c.gold else if (rank > 0 || open) col else Color(0xFF4A5278), CircleShape)
                            .clickable { selectedId = def.id },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (rank == 0 && !open) "🔒" else def.emoji, fontSize = 20.sp) }
                }
            }
        }

        // legend
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tag("● " + tr("باز شده", "Unlocked"), c.emerald)
            Tag("● " + tr("قابل ارتقا", "Available"), c.gold)
            Tag("● " + tr("قفل", "Locked"), c.textDim)
        }

        if (selected == null) {
            RpgCard { Text(tr("روی یک مهارت بزن تا جزئیات و پیش‌نیازها را ببینی. هر شاخه یک حوزهٔ زندگی است.", "Tap a skill to see details and prerequisites. Each branch is a life area."), color = c.textDim, fontSize = 13.sp) }
        } else {
            SkillDetail(ui, vm, selected)
        }
    }
}

@Composable
private fun SkillDetail(ui: GameUi, vm: GameViewModel, def: SkillDef) {
    val c = LocalRpg.current
    val lang = ui.lang
    val cat = Catalog.category(def.categoryId)
    val col = cat?.let { colorOf(it.color) } ?: c.magic
    val rank = ui.ranks[def.id] ?: 0
    val check = SkillRules.check(def, ui.ranks, ui.player.skillPoints)
    RpgCard(accent = col) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(def.emoji, fontSize = 30.sp)
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (lang == "fa") def.fa else def.en, color = c.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text((cat?.let { it.emoji + " " + (if (lang == "fa") it.fa else it.en) } ?: "") + " · " + tr("ردهٔ ${num(def.tier)}", "Tier ${def.tier}"), color = c.textDim, fontSize = 12.sp)
                }
                Tag(tr("رتبه ", "Rank ") + num(rank) + "/" + num(SkillDef.MAX_RANK), if (rank > 0) c.emerald else c.textDim)
            }
            Text(if (lang == "fa") def.descFa else def.descEn, color = c.text, fontSize = 13.sp)
            val stat = def.bonusStat()
            Text(
                "✨ " + tr("هر رتبه: +${num(def.xpPercentPerRank)}٪ XP برای ماموریت‌های این دسته", "Per rank: +${def.xpPercentPerRank}% XP for quests in this category") +
                    (if (stat != null) "\n" + stat.emoji + " " + tr("و +۱ ${stat.fa}", "and +1 ${stat.en}") else ""),
                color = c.emerald, fontSize = 12.sp,
            )
            val parent = def.parentId?.let { Catalog.skill(it) }
            if (parent != null) {
                val ok = (ui.ranks[parent.id] ?: 0) >= def.parentRank
                Text((if (ok) "✅ " else "⬜ ") + tr("پیش‌نیاز: ${parent.fa} رتبهٔ ${num(def.parentRank)}", "Requires: ${parent.en} rank ${def.parentRank}"), color = c.text, fontSize = 12.sp)
            }
            when (check) {
                is SkillRules.Check.Ok -> GlowButton(tr("ارتقا (هزینه: ${num(check.cost)} امتیاز)", "Upgrade (cost: ${check.cost} pts)"), { vm.upgradeSkill(def.id) }, Modifier.fillMaxWidth(), color = c.emerald)
                is SkillRules.Check.MaxRank -> Text("👑 " + tr("به حداکثر رتبه رسیده است", "Max rank reached"), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                is SkillRules.Check.NeedPrerequisite -> Text("🔒 " + tr("ابتدا پیش‌نیاز را باز کن", "Unlock the prerequisite first"), color = c.textDim, fontSize = 13.sp)
                is SkillRules.Check.NotEnoughPoints -> Text("🌟 " + tr("امتیاز کافی نیست؛ ${num(check.cost)} امتیاز لازم است. با سطح گرفتن امتیاز می‌گیری.", "Not enough points; ${check.cost} needed. Level up to earn more."), color = c.textDim, fontSize = 13.sp)
            }
        }
    }
}
