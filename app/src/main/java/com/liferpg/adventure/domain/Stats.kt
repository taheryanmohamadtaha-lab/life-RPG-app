package com.liferpg.adventure.domain

/** Minimal completion record for analytics (decoupled from Room so it is trivially testable). */
data class Done(val epochDay: Long, val xp: Int, val categoryId: String, val questId: String = "")

/** Analytics computed from real, locally stored completions only. */
object StatsCalc {
    /** XP per day for the [days] days ending at [today] (oldest first). */
    fun xpPerDay(done: List<Done>, today: Long, days: Int): List<Pair<Long, Int>> {
        val byDay = done.groupBy { it.epochDay }.mapValues { e -> e.value.sumOf { it.xp } }
        return (today - days + 1..today).map { it to (byDay[it] ?: 0) }
    }

    fun countPerDay(done: List<Done>): Map<Long, Int> = done.groupingBy { it.epochDay }.eachCount()

    fun xpToday(done: List<Done>, today: Long): Int = done.filter { it.epochDay == today }.sumOf { it.xp }

    /** Share of the last [days] days (rest days excluded) on which at least one quest was completed. */
    fun consistency(done: List<Done>, restDays: Set<Long>, today: Long, days: Int, firstDay: Long): Float {
        val active = done.map { it.epochDay }.toSet()
        var counted = 0
        var hit = 0
        for (d in (today - days + 1)..today) {
            if (d < firstDay) continue
            if (d in restDays) continue
            counted++
            if (d in active) hit++
        }
        return if (counted == 0) 0f else hit.toFloat() / counted
    }

    /**
     * Completion rate of the last [days] days: completions divided by what was expected
     * (scheduled daily quests plus the other completions), capped at 100%.
     */
    fun completionRate(doneInRange: Int, expectedDaily: Int, nonDailyDone: Int): Float {
        val expected = expectedDaily + nonDailyDone
        if (expected <= 0) return 0f
        return (doneInRange.toFloat() / expected).coerceIn(0f, 1f)
    }

    data class Bests(val bestDayXp: Int, val bestDayCount: Int, val bestStreak: Int, val totalXp: Long)

    fun bests(done: List<Done>, bestStreak: Int, totalXp: Long): Bests {
        val byDay = done.groupBy { it.epochDay }
        return Bests(
            bestDayXp = byDay.values.maxOfOrNull { l -> l.sumOf { it.xp } } ?: 0,
            bestDayCount = byDay.values.maxOfOrNull { it.size } ?: 0,
            bestStreak = bestStreak, totalXp = totalXp,
        )
    }

    fun topCategories(done: List<Done>, limit: Int = 5): List<Pair<String, Int>> =
        done.groupingBy { it.categoryId }.eachCount().entries.sortedByDescending { it.value }.take(limit).map { it.key to it.value }

    /** Number of days (counted back from today) in the current unbroken run of active-or-rest days. */
    fun currentRun(done: List<Done>, restDays: Set<Long>, today: Long): Int {
        val active = done.map { it.epochDay }.toSet()
        var d = if (today in active) today else today - 1
        var n = 0
        while (d in active || d in restDays) {
            if (d in active) n++
            d--
        }
        return n
    }
}
