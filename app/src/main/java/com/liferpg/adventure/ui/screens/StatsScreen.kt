package com.liferpg.adventure.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.data.kindEnum
import com.liferpg.adventure.data.statusEnum
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.QuestKind
import com.liferpg.adventure.domain.QuestStatus
import com.liferpg.adventure.domain.Schedule
import com.liferpg.adventure.domain.StatsCalc
import com.liferpg.adventure.ui.components.KeyValue
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.ScreenScaffold
import com.liferpg.adventure.ui.components.SectionTitle
import com.liferpg.adventure.ui.components.XpBar
import com.liferpg.adventure.ui.components.colorOf
import com.liferpg.adventure.ui.components.fmtDay
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
private fun BarChart(values: List<Int>, highlightLast: Boolean, color: Color, modifier: Modifier = Modifier) {
    val c = LocalRpg.current
    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        val mx = max(1, values.maxOrNull() ?: 1).toFloat()
        val n = values.size
        val gap = size.width / n
        val bw = gap * 0.62f
        drawLine(c.border, Offset(0f, size.height), Offset(size.width, size.height), 2f)
        for ((i, v) in values.withIndex()) {
            val h = (v / mx) * (size.height - 6f)
            val x = i * gap + (gap - bw) / 2
            val last = highlightLast && i == n - 1
            drawRoundRect(if (last) c.gold else color.copy(alpha = if (v == 0) 0.18f else 0.9f), Offset(x, size.height - h - 1f), Size(bw, max(h, 3f)), CornerRadius(bw / 3))
        }
    }
}

@Composable
private fun LineChart(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    val c = LocalRpg.current
    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        if (values.size < 2) return@Canvas
        val mn = values.min().toFloat()
        val mx = values.max().toFloat()
        val span = max(1f, mx - mn)
        val path = Path()
        val fill = Path()
        for ((i, v) in values.withIndex()) {
            val x = i * size.width / (values.size - 1)
            val y = size.height - 8f - ((v - mn) / span) * (size.height - 16f)
            if (i == 0) { path.moveTo(x, y); fill.moveTo(x, size.height); fill.lineTo(x, y) } else { path.lineTo(x, y); fill.lineTo(x, y) }
        }
        fill.lineTo(size.width, size.height); fill.close()
        drawPath(fill, color.copy(alpha = 0.18f))
        drawPath(path, color, style = Stroke(4f, cap = StrokeCap.Round))
        drawLine(c.border, Offset(0f, size.height), Offset(size.width, size.height), 2f)
    }
}

@Composable
fun StatsScreen(ui: GameUi, onBack: () -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    val today = ui.today
    val done = ui.doneList
    val week = StatsCalc.xpPerDay(done, today, 7)
    val month = StatsCalc.xpPerDay(done, today, 30)
    val perDay = StatsCalc.countPerDay(done)
    val monthCounts = (today - 29..today).map { perDay[it] ?: 0 }
    val cap = ui.settings.rules.dailyXpCap
    val first = ui.completions.minOfOrNull { it.epochDay } ?: today
    val consistency = StatsCalc.consistency(done, ui.restDays, today, 30, minOf(first, Instant.ofEpochMilli(ui.player.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()))
    val bests = StatsCalc.bests(done, ui.player.bestStreak, ui.player.totalXp)

    // completion rate over the last 7 days
    val range = (today - 6)..today
    val doneRange = ui.completions.count { it.epochDay in range }
    val dailyQuests = ui.quests.filter { it.kindEnum() == QuestKind.DAILY && it.statusEnum() == QuestStatus.ACTIVE }
    var expected = 0
    for (d in range) expected += dailyQuests.count { Schedule.isScheduledOn(it.weekdaysMask, d) }
    val dailyIds = dailyQuests.map { it.id }.toSet()
    val nonDaily = ui.completions.count { it.epochDay in range && it.questId !in dailyIds }
    val rate = StatsCalc.completionRate(doneRange, expected, nonDaily)

    // cumulative XP (last 30 days)
    val inWindow = month.sumOf { it.second }
    var run = ui.player.totalXp - inWindow
    val cumulative = month.map { run += it.second; run }

    ScreenScaffold(tr("آمار و تحلیل", "Statistics"), onBack) {
        RpgCard(accent = c.purple) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("امروز", "Today"))
                XpBar((ui.xpToday.toFloat() / cap).coerceIn(0f, 1f), Modifier.fillMaxWidth())
                Text(tr("${num(ui.xpToday)} از ${num(cap)} XP روزانه", "${ui.xpToday} of $cap daily XP"), color = c.textDim, fontSize = 12.sp)
            }
        }
        RpgCard(accent = c.magic) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("XP هفتهٔ اخیر", "XP this week"))
                BarChart(week.map { it.second }, true, c.magic)
                Row(Modifier.fillMaxWidth()) {
                    val names = if (lang == "fa") listOf("ش", "ی", "د", "س", "چ", "پ", "ج") else listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr")
                    for ((d, _) in week) Text(names[Schedule.dayBit(d)], color = c.textDim, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
                Text(tr("مجموع: ${num(week.sumOf { it.second })} XP · میانگین روزانه: ${num(week.sumOf { it.second } / 7)}", "Total: ${week.sumOf { it.second }} XP · daily avg: ${week.sumOf { it.second } / 7}"), color = c.textDim, fontSize = 12.sp)
            }
        }
        RpgCard(accent = c.emerald) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("فعالیت ماه (۳۰ روز)", "Monthly activity (30 days)"))
                BarChart(monthCounts, true, c.emerald)
                Text(tr("تعداد ماموریت‌های هر روز", "Quests completed per day"), color = c.textDim, fontSize = 11.sp)
            }
        }
        RpgCard(accent = c.gold) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("پیشرفت XP (۳۰ روز)", "XP progression (30 days)"))
                LineChart(cumulative, c.gold)
                Text(tr("XP کل: ${num(ui.player.totalXp)} · سطح ${num(ui.player.level)}", "Total XP: ${ui.player.totalXp} · Level ${ui.player.level}"), color = c.textDim, fontSize = 12.sp)
            }
        }
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(tr("نرخ تکمیل و ثبات", "Completion & consistency"))
                Text(tr("نرخ تکمیل ماموریت‌ها (۷ روز): ${num((rate * 100).roundToInt())}٪", "Quest completion rate (7 days): ${(rate * 100).roundToInt()}%"), color = c.text, fontSize = 13.sp)
                XpBar(rate, Modifier.fillMaxWidth(), colors = listOf(c.emerald.copy(alpha = 0.7f), c.emerald))
                Text(tr("ثبات عادت‌ها (۳۰ روز، بدون روزهای استراحت): ${num((consistency * 100).roundToInt())}٪", "Habit consistency (30 days, rest days excluded): ${(consistency * 100).roundToInt()}%"), color = c.text, fontSize = 13.sp)
                XpBar(consistency, Modifier.fillMaxWidth(), colors = listOf(c.magic.copy(alpha = 0.7f), c.magic))
            }
        }
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(tr("فعال‌ترین دسته‌ها", "Most active categories"))
                val top = StatsCalc.topCategories(done, 5)
                if (top.isEmpty()) Text(tr("هنوز دیتایی نیست.", "No data yet."), color = c.textDim, fontSize = 12.sp)
                val topMax = (top.firstOrNull()?.second ?: 1).toFloat()
                for ((id, n) in top) {
                    val cat = ui.category(id)
                    Row(Modifier.fillMaxWidth()) { Text((cat?.emoji ?: "•") + " " + (cat?.name(lang) ?: id), color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text(num(n), color = c.textDim, fontSize = 13.sp) }
                    XpBar(n / topMax, Modifier.fillMaxWidth(), height = 7.dp, colors = (cat?.let { colorOf(it.color) } ?: c.purple).let { listOf(it.copy(alpha = 0.6f), it) })
                }
            }
        }
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(tr("پیشرفت مهارت‌ها", "Skill progression"))
                for (cat in Catalog.categories) {
                    val ranks = (1..3).sumOf { ui.ranks["${cat.id}_$it"] ?: 0 }
                    Row(Modifier.fillMaxWidth()) { Text(cat.emoji + " " + cat.name(lang), color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text("${num(ranks)}/${num(9)}", color = c.textDim, fontSize = 12.sp) }
                    XpBar(ranks / 9f, Modifier.fillMaxWidth(), height = 7.dp, colors = colorOf(cat.color).let { listOf(it.copy(alpha = 0.6f), it) })
                }
            }
        }
        RpgCard(accent = c.gold) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("رکوردهای شخصی", "Personal bests"))
                KeyValue(tr("بیشترین XP در یک روز", "Best day XP"), num(bests.bestDayXp))
                KeyValue(tr("بیشترین ماموریت در یک روز", "Most quests in a day"), num(bests.bestDayCount))
                KeyValue(tr("بهترین استریک", "Best streak"), num(bests.bestStreak) + " 🔥")
                KeyValue(tr("کل XP", "Total XP"), num(bests.totalXp))
            }
        }
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("تاریخچهٔ سطح‌ها", "Level history"))
                for (l in ui.levelHistory.reversed().take(12)) {
                    val day = Instant.ofEpochMilli(l.reachedAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                    KeyValue(tr("سطح ${num(l.level)}", "Level ${l.level}"), fmtDay(day, lang) + " · " + num(l.totalXp) + " XP")
                }
            }
        }
    }
}
