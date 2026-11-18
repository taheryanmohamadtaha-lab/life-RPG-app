package com.liferpg.adventure.notify

import com.liferpg.adventure.data.AppSettings
import com.liferpg.adventure.data.QuestEntity
import com.liferpg.adventure.data.isAvailableOn
import com.liferpg.adventure.data.kindEnum
import com.liferpg.adventure.data.statusEnum
import com.liferpg.adventure.domain.PeriodKeys
import com.liferpg.adventure.domain.QuestKind
import com.liferpg.adventure.domain.QuestStatus
import com.liferpg.adventure.domain.QuietHours
import java.time.DayOfWeek
import java.time.LocalDate

enum class ReminderKind { DAILY, STREAK, HABIT, DEADLINE, WEEKLY }

data class DueReminder(val kind: ReminderKind, val key: String, val count: Int = 0, val title: String = "")

/**
 * Pure decision logic for local reminders (easy to unit test). It never produces more than [MAX_PER_DAY]
 * reminders a day, stays silent during quiet hours and only fires inside a 3-hour window after the chosen time.
 */
object ReminderPlanner {
    const val MAX_PER_DAY = 4
    private const val WINDOW = 180

    private fun inWindow(now: Int, start: Int) = now >= start && now < start + WINDOW

    fun plan(
        s: AppSettings,
        nowMinute: Int,
        today: Long,
        quests: List<QuestEntity>,
        doneQuestIds: Set<String>,
        effectiveStreak: Int,
        activeToday: Boolean,
    ): List<DueReminder> {
        if (!s.notifMaster) return emptyList()
        if (QuietHours.isQuiet(nowMinute, s.quietEnabled, s.quietStart, s.quietEnd)) return emptyList()
        val out = ArrayList<DueReminder>()

        val open = quests.filter { it.statusEnum() == QuestStatus.ACTIVE && it.isAvailableOn(today) && it.id !in doneQuestIds }
        val dailyOpen = open.filter { it.kindEnum() == QuestKind.DAILY && it.snoozeUntilDay <= today }

        if (s.notifDaily && inWindow(nowMinute, s.dailyMinute) && dailyOpen.isNotEmpty()) {
            out += DueReminder(ReminderKind.DAILY, "daily-$today", dailyOpen.size)
        }
        if (s.notifHabit) {
            for (q in open) {
                if (q.reminderMinute >= 0 && q.snoozeUntilDay <= today && inWindow(nowMinute, q.reminderMinute)) {
                    out += DueReminder(ReminderKind.HABIT, "habit-${q.id}-$today", 1, q.title)
                }
            }
        }
        if (s.notifDeadline && inWindow(nowMinute, s.dailyMinute)) {
            val soon = open.filter { it.kindEnum() != QuestKind.DAILY && it.deadlineDay != null && it.deadlineDay in today..(today + 1) }
            if (soon.isNotEmpty()) out += DueReminder(ReminderKind.DEADLINE, "deadline-$today", soon.size, soon.first().title)
        }
        if (s.notifStreak && inWindow(nowMinute, s.streakMinute) && effectiveStreak > 0 && !activeToday) {
            out += DueReminder(ReminderKind.STREAK, "streak-$today", effectiveStreak)
        }
        if (s.notifWeekly && inWindow(nowMinute, s.streakMinute)) {
            val dow = LocalDate.ofEpochDay(today).dayOfWeek
            val summaryDay = if (s.isFa) DayOfWeek.FRIDAY else DayOfWeek.SUNDAY
            if (dow == summaryDay) out += DueReminder(ReminderKind.WEEKLY, "weekly-${PeriodKeys.weekIndex(today)}")
        }
        return out
    }
}
