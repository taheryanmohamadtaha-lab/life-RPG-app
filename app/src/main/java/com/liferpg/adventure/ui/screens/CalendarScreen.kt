package com.liferpg.adventure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.domain.Digits
import com.liferpg.adventure.domain.Jalali
import com.liferpg.adventure.domain.PeriodKeys
import com.liferpg.adventure.domain.Schedule
import com.liferpg.adventure.domain.StatsCalc
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.KeyValue
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.ScreenScaffold
import com.liferpg.adventure.ui.components.SectionTitle
import com.liferpg.adventure.ui.components.fmtDay
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel
import java.time.LocalDate

@Composable
fun CalendarScreen(ui: GameUi, vm: GameViewModel, onBack: () -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    val fa = lang == "fa"
    var offset by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Long?>(null) }
    val today = ui.today

    // month bounds (Jalali for Persian, Gregorian for English)
    val first: Long
    val length: Int
    val title: String
    if (fa) {
        val j = Jalali.fromEpochDay(today)
        val total = j.year * 12 + (j.month - 1) + offset
        val y = total / 12
        val m = total % 12 + 1
        first = Jalali.toEpochDay(y, m, 1)
        length = Jalali.monthLength(y, m)
        title = Jalali.MONTH_NAMES_FA[m - 1] + " " + Digits.localize(y, "fa")
    } else {
        val d = LocalDate.ofEpochDay(today).withDayOfMonth(1).plusMonths(offset.toLong())
        first = d.toEpochDay()
        length = d.lengthOfMonth()
        title = d.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + d.year
    }
    val perDay = StatsCalc.countPerDay(ui.doneList)
    val lead = Schedule.dayBit(first)
    val cells = lead + length
    val rows = (cells + 6) / 7
    val names = if (fa) Jalali.WEEKDAYS_SHORT_FA_SAT_FIRST else listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr")

    val monthDays = first until first + length
    val monthDone = ui.completions.filter { it.epochDay in monthDays }
    val weekStart = PeriodKeys.weekStart(today)
    val weekDone = ui.completions.filter { it.epochDay in weekStart..(weekStart + 6) }

    ScreenScaffold(tr("تقویم عادت‌ها", "Habit calendar"), onBack) {
        RpgCard(accent = c.gold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🔥 " + tr("استریک: ${num(ui.streak)} روز", "Streak: ${ui.streak} days"), color = c.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(tr("بهترین: ${num(ui.player.bestStreak)} · سپرها: ${num(ui.player.streakShields)} 🛡️", "Best: ${ui.player.bestStreak} · Shields: ${ui.player.streakShields} 🛡️"), color = c.textDim, fontSize = 12.sp)
                }
                GlowButton(tr("روز استراحت 🛌", "Rest day 🛌"), { vm.declareRest() }, color = c.emerald)
            }
            Spacer(Modifier.size(6.dp))
            Text(
                tr("اگر روزی از دست رفت اشکالی ندارد: سپرها روزهای خالی را پوشش می‌دهند، روز استراحت (تا ۲ بار در هفته) استریک را نمی‌شکند، و اگر استریک تمام شد، فقط دوباره شروع می‌کنیم. هر روز یک فرصت تازه است 🌱",
                    "Missed a day? That's fine: shields cover gaps, rest days (up to 2 a week) never break your streak, and if a streak ends we simply begin again. Every day is a fresh chance 🌱"),
                color = c.textDim, fontSize = 12.sp,
            )
        }
        RpgCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlineButton("‹", { offset-- })
                Text(title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                OutlineButton("›", { offset++ })
            }
            Spacer(Modifier.size(8.dp))
            Row(Modifier.fillMaxWidth()) {
                for (n in names) Text(n, color = c.textDim, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.size(4.dp))
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0..6) {
                        val idx = r * 7 + col
                        val dayNum = idx - lead + 1
                        if (dayNum < 1 || dayNum > length) { Spacer(Modifier.weight(1f).aspectRatio(1f)); continue }
                        val day = first + dayNum - 1
                        val n = perDay[day] ?: 0
                        val rest = day in ui.restDays
                        val isToday = day == today
                        val bg = when {
                            n >= 4 -> c.emerald.copy(alpha = 0.85f)
                            n >= 2 -> c.emerald.copy(alpha = 0.55f)
                            n == 1 -> c.emerald.copy(alpha = 0.30f)
                            rest -> c.magic.copy(alpha = 0.30f)
                            else -> c.track
                        }
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(bg)
                                .border(if (isToday || selected == day) 2.dp else 1.dp, if (selected == day) c.purple else if (isToday) c.gold else c.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { selected = day },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (rest && n == 0) "🛌" else Digits.localize(dayNum, lang), color = if (day > today) c.textDim else c.text, fontSize = 12.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.size(4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RewardChip("🟩", tr("انجام شده", "Done"), c.emerald)
                RewardChip("🛌", tr("استراحت", "Rest"), c.magic)
            }
        }
        selected?.let { day ->
            val list = ui.completions.filter { it.epochDay == day }
            RpgCard(accent = c.purple) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle(fmtDay(day, lang) + if (day in ui.restDays) "  🛌" else "")
                    if (list.isEmpty()) Text(tr("چیزی ثبت نشده؛ و همین هم اشکالی ندارد.", "Nothing recorded, and that's perfectly okay."), color = c.textDim, fontSize = 12.sp)
                    for (cp in list) Text("• ${cp.questTitle}  (+${num(cp.xp)} XP)", color = c.text, fontSize = 13.sp)
                }
            }
        }
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("آمار این هفته", "This week"))
                KeyValue(tr("ماموریت‌های انجام‌شده", "Quests done"), num(weekDone.size))
                KeyValue(tr("روزهای فعال", "Active days"), num(weekDone.map { it.epochDay }.toSet().size) + " / ۷".let { if (fa) it else " / 7" })
                KeyValue("XP", num(weekDone.sumOf { it.xp }))
                SectionTitle(tr("آمار این ماه", "This month"))
                KeyValue(tr("ماموریت‌های انجام‌شده", "Quests done"), num(monthDone.size))
                KeyValue(tr("روزهای فعال", "Active days"), num(monthDone.map { it.epochDay }.toSet().size) + " / " + num(length))
                KeyValue("XP", num(monthDone.sumOf { it.xp }))
            }
        }
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle(tr("تاریخچهٔ انجام‌شده‌ها", "Completion history"))
                for (cp in ui.completions.take(12)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(cp.questTitle, color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(fmtDay(cp.epochDay, lang), color = c.textDim, fontSize = 11.sp)
                    }
                }
                if (ui.completions.isEmpty()) Text(tr("هنوز چیزی ثبت نشده.", "Nothing yet."), color = c.textDim, fontSize = 12.sp)
            }
        }
    }
}
