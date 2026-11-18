package com.liferpg.adventure.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.domain.AchievementMetrics
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.PeriodKeys
import com.liferpg.adventure.ui.components.ChipRow
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.RpgDialog
import com.liferpg.adventure.ui.components.RpgTextField
import com.liferpg.adventure.ui.components.ScreenScaffold
import com.liferpg.adventure.ui.components.SectionTitle
import com.liferpg.adventure.ui.components.Stepper
import com.liferpg.adventure.ui.components.Tag
import com.liferpg.adventure.ui.components.XpBar
import com.liferpg.adventure.ui.components.colorOf
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel

fun achievementMetrics(ui: GameUi): AchievementMetrics {
    val range = PeriodKeys.monthRange(ui.today)
    return AchievementMetrics(
        total = ui.completions.size, bestStreak = ui.player.bestStreak, level = ui.player.level,
        monthCount = ui.completions.count { it.epochDay in range },
        hardPlus = ui.completions.count { it.difficulty == "HARD" || it.difficulty == "EPIC" || it.difficulty == "LEGENDARY" },
        perCategory = ui.categoryCounts,
    )
}

@Composable
fun AchievementsScreen(ui: GameUi, vm: GameViewModel, onBack: () -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    val m = achievementMetrics(ui)
    var creating by remember { mutableStateOf(false) }
    val unlockedCount = Catalog.achievements.count { it.id in ui.unlocks } + ui.customAch.count { "custom_${it.id}" in ui.unlocks }
    val totalCount = Catalog.achievements.size + ui.customAch.size

    ScreenScaffold(tr("دستاوردها", "Achievements"), onBack) {
        RpgCard(accent = c.gold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", fontSize = 34.sp)
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("${num(unlockedCount)} از ${num(totalCount)} دستاورد", "$unlockedCount of $totalCount unlocked"), color = c.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    XpBar(if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount, Modifier.fillMaxWidth(), colors = listOf(c.gold, c.purple))
                }
            }
        }
        for (a in Catalog.achievements) {
            val unlocked = a.id in ui.unlocks
            val value = a.value(m).coerceAtMost(a.target)
            RpgCard(Modifier.alpha(if (unlocked) 1f else 0.82f), accent = if (unlocked) c.gold else c.border) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (unlocked) a.emoji else "🔒", fontSize = 30.sp)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (lang == "fa") a.fa else a.en, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(if (lang == "fa") a.descFa else a.descEn, color = c.textDim, fontSize = 12.sp)
                    }
                    if (unlocked) Tag("✓", c.emerald)
                }
                Spacer(Modifier.size(6.dp))
                XpBar(value.toFloat() / a.target, Modifier.fillMaxWidth(), height = 8.dp, colors = listOf(c.gold.copy(alpha = 0.7f), c.gold))
                Spacer(Modifier.size(4.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${num(value)} / ${num(a.target)}", color = c.textDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (a.gold > 0) RewardChip("🪙", num(a.gold), c.gold)
                    Spacer(Modifier.size(4.dp))
                    if (a.gems > 0) RewardChip("💎", num(a.gems), c.magic)
                }
            }
        }
        SectionTitle(tr("دستاوردهای سفارشی", "Custom achievements"), trailing = { GlowButton(tr("＋ جدید", "＋ New"), { creating = true }, color = c.emerald) })
        if (ui.customAch.isEmpty()) Text(tr("هدف شخصی خودت را به‌عنوان دستاورد تعریف کن.", "Turn your own goal into an achievement."), color = c.textDim, fontSize = 12.sp)
        for (ca in ui.customAch) {
            val unlocked = "custom_${ca.id}" in ui.unlocks
            val value = (if (ca.categoryId.isEmpty()) ui.completions.size else (ui.categoryCounts[ca.categoryId] ?: 0)).coerceAtMost(ca.target)
            RpgCard(accent = if (unlocked) c.gold else c.border) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (unlocked) "🏵️" else "🔒", fontSize = 28.sp)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ca.title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (ca.description.isNotBlank()) Text(ca.description, color = c.textDim, fontSize = 12.sp)
                    }
                    OutlineButton(tr("حذف", "Delete"), { vm.deleteCustomAchievement(ca.id) }, color = c.danger)
                }
                Spacer(Modifier.size(6.dp))
                XpBar(value.toFloat() / ca.target, Modifier.fillMaxWidth(), height = 8.dp, colors = listOf(c.gold.copy(alpha = 0.7f), c.gold))
                Text("${num(value)} / ${num(ca.target)}  ·  🪙 ${num(ca.rewardGold)}", color = c.textDim, fontSize = 12.sp)
            }
        }
    }
    if (creating) CustomAchievementDialog(ui, vm) { creating = false }
}

@Composable
private fun CustomAchievementDialog(ui: GameUi, vm: GameViewModel, onClose: () -> Unit) {
    val c = LocalRpg.current
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(10) }
    var gold by remember { mutableStateOf(50) }
    var error by remember { mutableStateOf(false) }
    RpgDialog(onClose, tr("دستاورد سفارشی 🏵️", "Custom achievement 🏵️")) {
        RpgTextField(title, { title = it }, tr("عنوان", "Title"))
        RpgTextField(desc, { desc = it }, tr("توضیح (اختیاری)", "Description (optional)"))
        Text(tr("شمارش ماموریت‌های این دسته", "Count quests of this category"), color = c.textDim, fontSize = 12.sp)
        ChipRow(listOf("") + ui.categories.map { it.id }, cat, label = { id -> if (id.isEmpty()) tr("همه", "Any") else ui.category(id)?.let { it.emoji + " " + it.name(ui.lang) } ?: id }, onSelect = { cat = it }, accent = { id -> ui.category(id)?.let { colorOf(it.color) } })
        Stepper(tr("هدف (تعداد)", "Target (count)"), num(target), { target = (target - 5).coerceAtLeast(1) }, { target = (target + 5).coerceAtMost(1000) })
        Stepper(tr("پاداش سکه", "Gold reward"), num(gold), { gold = (gold - 10).coerceAtLeast(0) }, { gold = (gold + 10).coerceAtMost(1000) })
        if (error) Text("⚠ " + tr("عنوان را درست وارد کن.", "Please enter a valid title."), color = c.danger, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GlowButton(tr("ذخیره", "Save"), { vm.addCustomAchievement(title, desc, cat, target, gold) { ok -> if (ok) onClose() else error = true } }, Modifier.weight(1f), color = c.emerald)
            OutlineButton(tr("انصراف", "Cancel"), onClose, Modifier.weight(1f))
        }
    }
}
