package com.liferpg.adventure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.ItemSlot
import com.liferpg.adventure.domain.Stat
import com.liferpg.adventure.ui.art.DragonPod
import com.liferpg.adventure.ui.art.HeroView
import com.liferpg.adventure.ui.components.CurrencyRow
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.SectionTitle
import com.liferpg.adventure.ui.components.XpBar
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.components.rarityColor
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import kotlin.math.max

@Composable
fun HeroScreen(ui: GameUi, onInventory: () -> Unit, onSkills: () -> Unit, onAchievements: () -> Unit, onStats: () -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    val p = ui.player
    val power = Catalog.powerScore(p.level, ui.effStats)
    val stage = Catalog.petStage(p.level)
    val pet = Catalog.pet(p.petType)
    val statMax = max(30, ((ui.effStats.values.maxOrNull() ?: 1) * 5 / 4))

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name.ifBlank { tr("قهرمان", "Hero") }, color = c.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(tr("سطح ${num(p.level)} · ", "Level ${p.level} · ") + Catalog.heroTitle(p.level, lang), color = c.purple, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            CurrencyRow(p.gold, p.gems)
        }
        XpBar(ui.xp.fraction, Modifier.fillMaxWidth(), height = 10.dp)
        Text(
            if (ui.xp.maxed) tr("حداکثر سطح", "Max level") else "XP ${num(ui.xp.currentXp)} / ${num(ui.xp.xpForNext)}",
            color = c.textDim, fontSize = 12.sp,
        )

        // ---- hero stage
        Box(
            Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(c.purple.copy(alpha = 0.35f), c.panel)))
                .border(1.dp, c.purple.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            HeroView(ui.look(), Modifier.fillMaxSize().padding(8.dp))
            Box(
                Modifier.padding(bottom = 10.dp).clip(RoundedCornerShape(50)).background(c.track.copy(alpha = 0.85f))
                    .border(1.dp, c.gold.copy(alpha = 0.7f), RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 6.dp),
            ) { Text("✦ " + tr("قدرت بازی", "Power") + ": " + num(power), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        }
        Text(
            tr("این اعداد فقط آمار بازی هستند و ربطی به سنجش‌های واقعی سلامت ندارند.", "These numbers are game stats only; they are unrelated to real health measurements."),
            color = c.textDim, fontSize = 11.sp,
        )

        // ---- stats
        RpgCard(accent = c.magic) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(tr("ویژگی‌ها", "Attributes"))
                val colors = listOf(c.danger, c.magic, c.gold, c.emerald, c.purple, c.magic, c.gold)
                for ((i, s) in Stat.entries.withIndex()) {
                    val base = p.stat(s)
                    val bonus = ui.bonuses.stats[s] ?: 0
                    val col = colors[i % colors.size]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.emoji + " " + s.label(lang), color = c.text, fontSize = 13.sp, modifier = Modifier.width(96.dp))
                        XpBar(((base + bonus).toFloat() / statMax), Modifier.weight(1f), height = 9.dp, colors = listOf(col.copy(alpha = 0.7f), col))
                        Spacer(Modifier.width(8.dp))
                        Text(num(base) + if (bonus > 0) " +${num(bonus)}" else "", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(54.dp), textAlign = TextAlign.End)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (ui.bonuses.xpPercent > 0) RewardChip("✨", "+${num(ui.bonuses.xpPercent)}٪ XP", c.purple)
                    if (ui.bonuses.coinPercent > 0) RewardChip("🪙", "+${num(ui.bonuses.coinPercent)}٪", c.gold)
                    RewardChip("🌟", tr("امتیاز مهارت: ", "Skill pts: ") + num(p.skillPoints), c.emerald)
                }
            }
        }

        // ---- equipment
        RpgCard(accent = c.gold) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(tr("تجهیزات", "Equipment"), trailing = { OutlineButton(tr("کوله‌پشتی", "Inventory"), onInventory) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (slot in listOf(ItemSlot.WEAPON, ItemSlot.ARMOR, ItemSlot.HELMET, ItemSlot.ACCESSORY)) {
                        val def = Catalog.item(ui.item(slot))
                        val col = def?.let { rarityColor(it.rarity) } ?: c.border
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(c.track)
                                .border(1.5.dp, col, RoundedCornerShape(14.dp)).clickable(onClick = onInventory).padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(def?.emoji ?: slot.emoji, fontSize = 26.sp, color = if (def == null) c.textDim else c.text)
                            Text(def?.let { if (lang == "fa") it.fa else it.en } ?: slot.label(lang), color = c.textDim, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
            }
        }

        // ---- dragon companion
        RpgCard(accent = c.emerald) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(tr("اژدهای همراه: ", "Companion: ") + p.petName.ifBlank { if (lang == "fa") pet.fa else pet.en })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (st in 1..3) {
                        val unlocked = st <= stage
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            DragonPod(pet, st, unlocked, Modifier.fillMaxWidth())
                            val need = when (st) { 1 -> 1; 2 -> 10; else -> 25 }
                            Text(
                                if (unlocked) (if (st == stage) tr("اکنون", "Now") else "✓") else "🔒 " + tr("سطح ", "Lv ") + num(need),
                                color = if (st == stage) c.emerald else c.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Text(
                    if (stage < 3) tr("اژدهایت با رسیدن به سطح ${num(if (stage == 1) 10 else 25)} تکامل پیدا می‌کند. با ماموریت‌های واقعی رشدش بده!", "Your dragon evolves at level ${if (stage == 1) 10 else 25}. Grow it with real-life quests!")
                    else tr("اژدهایت به بالاترین مرحله رسیده است 🐉", "Your dragon has reached its final form 🐉"),
                    color = c.textDim, fontSize = 12.sp,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton(tr("🌳 مهارت‌ها", "🌳 Skills"), onSkills, Modifier.weight(1f))
            OutlineButton(tr("🏆 دستاوردها", "🏆 Badges"), onAchievements, Modifier.weight(1f))
            OutlineButton(tr("📊 آمار", "📊 Stats"), onStats, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
    }
}
