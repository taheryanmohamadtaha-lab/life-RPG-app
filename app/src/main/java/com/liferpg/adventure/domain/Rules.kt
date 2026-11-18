package com.liferpg.adventure.domain

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/** Level curve: xpToNext(L) = round10(60 * L^1.3). Level 1 -> 2 costs 60 XP, 10 -> 11 costs 1 200 XP. */
object LevelCurve {
    const val MAX_LEVEL = 99

    data class Progress(val level: Int, val currentXp: Long, val xpForNext: Long, val fraction: Float, val maxed: Boolean)

    fun xpToNext(level: Int): Long {
        val l = level.coerceIn(1, MAX_LEVEL)
        return (60.0 * l.toDouble().pow(1.3) / 10.0).roundToLong() * 10L
    }

    private val cumulative: LongArray by lazy {
        val a = LongArray(MAX_LEVEL)
        for (i in 1 until MAX_LEVEL) a[i] = a[i - 1] + xpToNext(i)
        a
    }

    /** Total XP needed to reach [level] (level 1 = 0). */
    fun xpForLevel(level: Int): Long = cumulative[level.coerceIn(1, MAX_LEVEL) - 1]

    val MAX_TOTAL_XP: Long get() = xpForLevel(MAX_LEVEL)

    fun levelForXp(totalXp: Long): Int {
        val xp = totalXp.coerceAtLeast(0)
        var lo = 1
        var hi = MAX_LEVEL
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (cumulative[mid - 1] <= xp) lo = mid else hi = mid - 1
        }
        return lo
    }

    fun progress(totalXp: Long): Progress {
        val xp = totalXp.coerceIn(0, MAX_TOTAL_XP)
        val level = levelForXp(xp)
        if (level >= MAX_LEVEL) return Progress(level, 0, 0, 1f, true)
        val cur = xp - xpForLevel(level)
        val need = xpToNext(level)
        return Progress(level, cur, need, (cur.toFloat() / need.toFloat()).coerceIn(0f, 1f), false)
    }
}

/** Configurable XP rules (Settings -> Custom XP rules). Everything is clamped so XP can never be unbounded. */
data class XpRules(
    val baseXp: Map<Difficulty, Int> = Difficulty.entries.associateWith { it.defaultXp },
    val dailyXpCap: Int = DEFAULT_DAILY_XP_CAP,
    val dailyCoinCap: Int = DEFAULT_DAILY_COIN_CAP,
    val dailyGemCap: Int = DEFAULT_DAILY_GEM_CAP,
    val streakBonusEnabled: Boolean = true,
) {
    fun baseXp(d: Difficulty): Int = (baseXp[d] ?: d.defaultXp).coerceIn(MIN_BASE_XP, MAX_BASE_XP)
    fun maxXp(d: Difficulty): Int = baseXp(d) * 2
    fun defaultCoins(d: Difficulty): Int = max(1, baseXp(d) / 2)
    fun maxCoins(d: Difficulty): Int = baseXp(d)

    fun sanitized(): XpRules = copy(
        baseXp = Difficulty.entries.associateWith { baseXp(it) },
        dailyXpCap = dailyXpCap.coerceIn(MIN_DAILY_XP_CAP, MAX_DAILY_XP_CAP),
        dailyCoinCap = dailyCoinCap.coerceIn(50, 5000),
        dailyGemCap = dailyGemCap.coerceIn(0, 10),
    )

    companion object {
        const val MIN_BASE_XP = 1
        const val MAX_BASE_XP = 500
        const val MIN_DAILY_XP_CAP = 200
        const val MAX_DAILY_XP_CAP = 5000
        const val DEFAULT_DAILY_XP_CAP = 1500
        const val DEFAULT_DAILY_COIN_CAP = 600
        const val DEFAULT_DAILY_GEM_CAP = 5
        const val STAT_CAP = 200
        const val MAX_STREAK_BONUS_DAYS = 10
        const val STREAK_BONUS_PER_DAY = 5
        const val MAX_BONUS_PERCENT = 100
    }
}

/** Period keys decide "one reward per period". Weeks start on Saturday, months follow the Jalali calendar. */
object PeriodKeys {
    const val ONCE = "once"

    fun day(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).toString()

    fun weekIndex(epochDay: Long): Long = Math.floorDiv(epochDay - 2L, 7L) // 1970-01-03 was a Saturday
    fun week(epochDay: Long): String = "W${weekIndex(epochDay)}"
    fun weekStart(epochDay: Long): Long = weekIndex(epochDay) * 7L + 2L

    fun month(epochDay: Long): String {
        val j = Jalali.fromEpochDay(epochDay)
        return "J%04d-%02d".format(j.year, j.month)
    }

    fun monthRange(epochDay: Long): LongRange {
        val j = Jalali.fromEpochDay(epochDay)
        val start = Jalali.toEpochDay(j.year, j.month, 1)
        return start..(start + Jalali.monthLength(j.year, j.month) - 1)
    }

    fun forKind(kind: QuestKind, epochDay: Long): String = when (kind) {
        QuestKind.DAILY -> day(epochDay)
        QuestKind.WEEKLY -> week(epochDay)
        QuestKind.MONTHLY -> month(epochDay)
        QuestKind.GOAL, QuestKind.ONE_TIME -> ONCE
    }

    /** All keys that can be "current" today (used to query this period's completions). */
    fun currentKeys(epochDay: Long): List<String> = listOf(day(epochDay), week(epochDay), month(epochDay), ONCE)
}

/** Global streak with gentle recovery: rest days are free, shields cover missed days. Nobody is shamed. */
object StreakCalculator {
    const val MAX_SHIELDS = 3
    const val SHIELD_EVERY_DAYS = 7
    const val MAX_REST_DAYS_PER_WEEK = 2

    data class State(
        val streak: Int,
        val shields: Int,
        val usedShields: Int = 0,
        val restarted: Boolean = false,
        val earnedShield: Boolean = false,
    )

    private fun uncoveredMissedDays(lastActiveDay: Long, today: Long, restDays: Set<Long>): Int {
        var missed = 0
        var d = lastActiveDay + 1
        while (d < today) {
            if (d !in restDays) missed++
            d++
        }
        return missed
    }

    /** Called when the player completes something on [today]. */
    fun advance(lastActiveDay: Long, today: Long, streak: Int, shields: Int, restDays: Set<Long>): State {
        if (lastActiveDay < 0) return State(1, shields)
        if (today <= lastActiveDay) return State(max(streak, 1), shields)
        val missed = uncoveredMissedDays(lastActiveDay, today, restDays)
        val next: State = when {
            missed == 0 -> State(streak + 1, shields)
            missed <= shields -> State(streak + 1, shields - missed, usedShields = missed)
            else -> State(1, shields, restarted = true)
        }
        if (!next.restarted && next.streak % SHIELD_EVERY_DAYS == 0 && next.shields < MAX_SHIELDS) {
            return next.copy(shields = next.shields + 1, earnedShield = true)
        }
        return next
    }

    /** Streak to display today (a streak that can no longer be saved shows 0 -> "start fresh"). */
    fun effective(lastActiveDay: Long, today: Long, streak: Int, shields: Int, restDays: Set<Long>): Int {
        if (lastActiveDay < 0 || streak <= 0) return 0
        if (today - lastActiveDay <= 1) return streak
        return if (uncoveredMissedDays(lastActiveDay, today, restDays) <= shields) streak else 0
    }
}

/** Daily login reward: a 7 day cycle that quietly restarts after a gap. */
object DailyReward {
    data class Reward(val gold: Int, val gems: Int)

    fun reward(cycleDay: Int): Reward {
        val d = cycleDay.coerceIn(1, 7)
        return if (d == 7) Reward(100, 2) else Reward(20 + 10 * (d - 1), 0)
    }

    fun nextCycleDay(lastClaimDay: Long, today: Long, lastCycleDay: Int): Int = when {
        lastClaimDay == today - 1 -> lastCycleDay % 7 + 1
        else -> 1
    }

    fun canClaim(lastClaimDay: Long, today: Long) = lastClaimDay != today
}

object RewardEconomy {
    sealed class Check {
        data object Ok : Check()
        data object NotEnough : Check()
        data object DailyLimit : Check()
        data object Invalid : Check()
    }

    const val MAX_COST = 100_000
    const val MIN_COST = 1
    const val MAX_PER_DAY_LIMIT = 5

    fun checkRedeem(balance: Long, cost: Int, redeemedToday: Int, maxPerDay: Int): Check = when {
        cost < MIN_COST || cost > MAX_COST -> Check.Invalid
        redeemedToday >= maxPerDay.coerceIn(1, MAX_PER_DAY_LIMIT) -> Check.DailyLimit
        balance < cost -> Check.NotEnough
        else -> Check.Ok
    }

    fun checkSpend(balance: Long, cost: Long): Boolean = cost >= 0 && balance >= cost
}

/** Skill tree rules. */
object SkillRules {
    sealed class Check {
        data class Ok(val cost: Int) : Check()
        data object MaxRank : Check()
        data class NeedPrerequisite(val prerequisiteId: String, val rank: Int) : Check()
        data class NotEnoughPoints(val cost: Int) : Check()
    }

    fun check(def: SkillDef, ranks: Map<String, Int>, skillPoints: Int): Check {
        val current = ranks[def.id] ?: 0
        if (current >= SkillDef.MAX_RANK) return Check.MaxRank
        val parent = def.parentId
        if (parent != null && (ranks[parent] ?: 0) < def.parentRank) return Check.NeedPrerequisite(parent, def.parentRank)
        val cost = def.costOfNextRank()
        if (skillPoints < cost) return Check.NotEnoughPoints(cost)
        return Check.Ok(cost)
    }

    fun xpBonusPercent(categoryId: String, ranks: Map<String, Int>): Int =
        Catalog.skills.filter { it.categoryId == categoryId }.sumOf { (ranks[it.id] ?: 0) * it.xpPercentPerRank }

    fun statBonuses(ranks: Map<String, Int>): Map<Stat, Int> {
        val out = HashMap<Stat, Int>()
        for (s in Catalog.skills) {
            val r = ranks[s.id] ?: 0
            val stat = s.bonusStat() ?: continue
            if (r > 0) out[stat] = (out[stat] ?: 0) + r
        }
        return out
    }

    fun pointsSpent(ranks: Map<String, Int>): Int =
        Catalog.skills.sumOf { s -> (1..(ranks[s.id] ?: 0)).sumOf { s.tier } }
}

data class CompletionInput(
    val difficulty: Difficulty,
    val xpReward: Int,
    val coinReward: Int,
    val primaryStat: Stat,
    val secondaryStat: Stat?,
    val totalXp: Long,
    val level: Int,
    val streak: Int,
    val lastActiveDay: Long,
    val shields: Int,
    val restDays: Set<Long>,
    val today: Long,
    val rules: XpRules,
    /** Percent XP bonus from skills + equipment (the streak bonus is added on top, total capped). */
    val xpBonusPercent: Int,
    val coinBonusPercent: Int,
    val earnedXpToday: Int,
    val earnedCoinsToday: Int,
    val earnedGemsToday: Int,
)

data class CompletionComputation(
    val baseXp: Int,
    val xpAwarded: Int,
    val streakBonusPercent: Int,
    val totalBonusPercent: Int,
    val coinsAwarded: Int,
    val gemsAwarded: Int,
    val capped: Boolean,
    val newTotalXp: Long,
    val oldLevel: Int,
    val newLevel: Int,
    val levelGold: Int,
    val levelGems: Int,
    val skillPoints: Int,
    val streak: StreakCalculator.State,
    val statGains: Map<Stat, Int>,
)

/** Pure game math for completing a quest. All persistence happens in the repository, inside one DB transaction. */
object CompletionEngine {
    fun levelUpGold(level: Int) = 25 * level
    fun levelUpGems(level: Int) = if (level % 5 == 0) 1 else 0

    fun compute(i: CompletionInput): CompletionComputation {
        val rules = i.rules.sanitized()
        val baseXp = i.xpReward.coerceIn(1, rules.maxXp(i.difficulty))
        val streak = StreakCalculator.advance(i.lastActiveDay, i.today, i.streak, i.shields, i.restDays)
        val streakPct = if (rules.streakBonusEnabled) {
            min(streak.streak, XpRules.MAX_STREAK_BONUS_DAYS) * XpRules.STREAK_BONUS_PER_DAY
        } else 0
        val totalPct = (i.xpBonusPercent.coerceAtLeast(0) + streakPct).coerceAtMost(XpRules.MAX_BONUS_PERCENT)
        val requested = baseXp + baseXp * totalPct / 100
        val remaining = (rules.dailyXpCap - i.earnedXpToday).coerceAtLeast(0)
        val awarded = min(requested, remaining)
        val capped = awarded < requested

        val baseCoins = i.coinReward.coerceIn(0, rules.maxCoins(i.difficulty))
        val coinsRequested = baseCoins + baseCoins * i.coinBonusPercent.coerceIn(0, 50) / 100
        val scaled = if (requested > 0) (coinsRequested.toLong() * awarded / requested).toInt() else 0
        val coins = min(scaled, (rules.dailyCoinCap - i.earnedCoinsToday).coerceAtLeast(0))

        val gemsWanted = if (awarded > 0) i.difficulty.gems else 0
        val gems = min(gemsWanted, (rules.dailyGemCap - i.earnedGemsToday).coerceAtLeast(0))

        val newTotal = (i.totalXp + awarded).coerceIn(0, LevelCurve.MAX_TOTAL_XP)
        val newLevel = LevelCurve.levelForXp(newTotal)
        var levelGold = 0
        var levelGems = 0
        var points = 0
        for (l in (i.level + 1)..newLevel) {
            levelGold += levelUpGold(l)
            levelGems += levelUpGems(l)
            points += 1
        }

        val gains = HashMap<Stat, Int>()
        if (awarded > 0) {
            gains[i.primaryStat] = i.difficulty.statGain
            val sec = i.secondaryStat
            if (sec != null && i.difficulty.statGain >= 2) gains[sec] = (gains[sec] ?: 0) + 1
        }

        return CompletionComputation(
            baseXp = baseXp, xpAwarded = awarded, streakBonusPercent = streakPct, totalBonusPercent = totalPct,
            coinsAwarded = coins, gemsAwarded = gems, capped = capped, newTotalXp = newTotal,
            oldLevel = i.level, newLevel = max(newLevel, i.level), levelGold = levelGold, levelGems = levelGems,
            skillPoints = points, streak = streak, statGains = gains,
        )
    }
}

/** Schedule helpers shared by the UI and the repository. */
object Schedule {
    /** weekdaysMask: bit0 = Saturday ... bit6 = Friday; 0 means every day. */
    fun dayBit(epochDay: Long): Int = Math.floorMod(epochDay - 2L, 7L).toInt()

    fun isScheduledOn(mask: Int, epochDay: Long): Boolean = mask == 0 || (mask shr dayBit(epochDay)) and 1 == 1

    /** Special weekend: Friday/Saturday for Persian users (Iranian weekend), Saturday/Sunday otherwise. */
    fun isWeekend(epochDay: Long, lang: String): Boolean {
        val dow = LocalDate.ofEpochDay(epochDay).dayOfWeek.value // Mon=1..Sun=7
        return if (lang == "fa") dow == 5 || dow == 6 else dow == 6 || dow == 7
    }
}

data class RandomEvent(val type: Type, val gold: Int, val gems: Int, val titleFa: String, val titleEn: String) {
    enum class Type { GOLD_GIFT, GEM_FIND, FAIRY_BLESSING, PET_GIFT }
}

object RandomEvents {
    const val CHANCE_PERCENT = 35

    /** Deterministic per day so restarting the app can never re-roll a better event. Always positive. */
    fun roll(epochDay: Long): RandomEvent? {
        val rnd = java.util.Random(epochDay * 7919L + 13L)
        if (rnd.nextInt(100) >= CHANCE_PERCENT) return null
        return when (rnd.nextInt(4)) {
            0 -> RandomEvent(RandomEvent.Type.GOLD_GIFT, 30 + rnd.nextInt(41), 0, "گنجینه‌ای در راه پیدا کردی!", "You found a treasure on the road!")
            1 -> RandomEvent(RandomEvent.Type.GEM_FIND, 0, 1, "یک گوهر درخشان پیدا کردی!", "You discovered a shining gem!")
            2 -> RandomEvent(RandomEvent.Type.FAIRY_BLESSING, 50 + rnd.nextInt(51), 0, "پریِ جنگل تو را برکت داد!", "A forest fairy blessed you!")
            else -> RandomEvent(RandomEvent.Type.PET_GIFT, 25 + rnd.nextInt(26), 0, "اژدهای همراهت هدیه‌ای آورد!", "Your dragon companion brought a gift!")
        }
    }
}

/** Quiet hours window in minutes of the day; may wrap past midnight (e.g. 22:00 -> 08:00). */
object QuietHours {
    fun isQuiet(nowMinute: Int, enabled: Boolean, start: Int, end: Int): Boolean {
        if (!enabled || start == end) return false
        return if (start < end) nowMinute in start until end else nowMinute >= start || nowMinute < end
    }
}

enum class QuestError { TITLE_EMPTY, TITLE_TOO_LONG, DESC_TOO_LONG, NOTES_TOO_LONG, XP_OUT_OF_RANGE, COINS_OUT_OF_RANGE, DEADLINE_IN_PAST }

object QuestValidator {
    const val MAX_TITLE = 80
    const val MAX_DESC = 500
    const val MAX_NOTES = 1000

    fun validate(
        title: String, description: String, notes: String, xp: Int, coins: Int, difficulty: Difficulty,
        rules: XpRules, deadlineDay: Long?, today: Long, isNew: Boolean,
    ): List<QuestError> {
        val errors = ArrayList<QuestError>()
        val t = title.trim()
        if (t.isEmpty()) errors += QuestError.TITLE_EMPTY
        if (t.length > MAX_TITLE) errors += QuestError.TITLE_TOO_LONG
        if (description.length > MAX_DESC) errors += QuestError.DESC_TOO_LONG
        if (notes.length > MAX_NOTES) errors += QuestError.NOTES_TOO_LONG
        if (xp < 1 || xp > rules.maxXp(difficulty)) errors += QuestError.XP_OUT_OF_RANGE
        if (coins < 0 || coins > rules.maxCoins(difficulty)) errors += QuestError.COINS_OUT_OF_RANGE
        if (isNew && deadlineDay != null && deadlineDay < today) errors += QuestError.DEADLINE_IN_PAST
        return errors
    }
}
