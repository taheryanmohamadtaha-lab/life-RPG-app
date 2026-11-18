package com.liferpg.adventure

import com.liferpg.adventure.backup.BackupCodec
import com.liferpg.adventure.backup.BackupFile
import com.liferpg.adventure.backup.BackupParse
import com.liferpg.adventure.data.PlayerEntity
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.CompletionEngine
import com.liferpg.adventure.domain.CompletionInput
import com.liferpg.adventure.domain.DailyReward
import com.liferpg.adventure.domain.Difficulty
import com.liferpg.adventure.domain.LevelCurve
import com.liferpg.adventure.domain.PeriodKeys
import com.liferpg.adventure.domain.RewardEconomy
import com.liferpg.adventure.domain.SkillRules
import com.liferpg.adventure.domain.Stat
import com.liferpg.adventure.domain.StreakCalculator
import com.liferpg.adventure.domain.XpRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCurveTest {
    @Test fun firstLevelCosts60() = assertEquals(60L, LevelCurve.xpToNext(1))

    @Test fun curveIsStrictlyIncreasing() {
        for (l in 1 until LevelCurve.MAX_LEVEL - 1) assertTrue(LevelCurve.xpToNext(l + 1) > LevelCurve.xpToNext(l))
    }

    @Test fun levelForXpRoundTrips() {
        for (l in 1..LevelCurve.MAX_LEVEL) {
            assertEquals(l, LevelCurve.levelForXp(LevelCurve.xpForLevel(l)))
            if (l > 1) assertEquals(l - 1, LevelCurve.levelForXp(LevelCurve.xpForLevel(l) - 1))
        }
    }

    @Test fun xpIsClampedAtMax() {
        val p = LevelCurve.progress(Long.MAX_VALUE / 2)
        assertTrue(p.maxed)
        assertEquals(LevelCurve.MAX_LEVEL, p.level)
        assertEquals(1, LevelCurve.progress(-50).level)
    }
}

class StreakTest {
    @Test fun firstEverActivityStartsAtOne() = assertEquals(1, StreakCalculator.advance(-1, 100, 0, 0, emptySet()).streak)

    @Test fun consecutiveDayIncrements() = assertEquals(6, StreakCalculator.advance(100, 101, 5, 0, emptySet()).streak)

    @Test fun sameDayDoesNotIncrement() = assertEquals(5, StreakCalculator.advance(100, 100, 5, 0, emptySet()).streak)

    @Test fun shieldCoversMissedDay() {
        val s = StreakCalculator.advance(100, 102, 5, 1, emptySet())
        assertEquals(6, s.streak); assertEquals(0, s.shields); assertEquals(1, s.usedShields)
    }

    @Test fun restDayIsFree() {
        val s = StreakCalculator.advance(100, 102, 5, 0, setOf(101L))
        assertEquals(6, s.streak); assertFalse(s.restarted)
    }

    @Test fun gentleRestartWhenUncovered() {
        val s = StreakCalculator.advance(100, 110, 5, 0, emptySet())
        assertEquals(1, s.streak); assertTrue(s.restarted)
    }

    @Test fun shieldEarnedEverySeventhDayAndCapped() {
        assertTrue(StreakCalculator.advance(100, 101, 6, 0, emptySet()).earnedShield)
        val full = StreakCalculator.advance(100, 101, 6, StreakCalculator.MAX_SHIELDS, emptySet())
        assertEquals(StreakCalculator.MAX_SHIELDS, full.shields)
    }

    @Test fun effectiveStreakShowsZeroWhenLost() {
        assertEquals(0, StreakCalculator.effective(100, 120, 9, 0, emptySet()))
        assertEquals(9, StreakCalculator.effective(100, 101, 9, 0, emptySet()))
    }
}

class CompletionEngineTest {
    private fun input(
        xp: Int = 25, earned: Int = 0, streak: Int = 0, bonus: Int = 0, rules: XpRules = XpRules(), coins: Int = 12,
        earnedCoins: Int = 0, earnedGems: Int = 0, level: Int = 1, totalXp: Long = 0, diff: Difficulty = Difficulty.NORMAL,
    ) = CompletionInput(
        difficulty = diff, xpReward = xp, coinReward = coins, primaryStat = Stat.STRENGTH, secondaryStat = null,
        totalXp = totalXp, level = level, streak = streak, lastActiveDay = if (streak > 0) 99 else -1, shields = 0,
        restDays = emptySet(), today = 100, rules = rules, xpBonusPercent = bonus, coinBonusPercent = 0,
        earnedXpToday = earned, earnedCoinsToday = earnedCoins, earnedGemsToday = earnedGems,
    )

    @Test fun baseRewardWithoutBonus() {
        val r = CompletionEngine.compute(input())
        assertEquals(25, r.xpAwarded); assertEquals(1, r.streak.streak)
    }

    @Test fun xpRequestIsClampedToTwiceBase() {
        val r = CompletionEngine.compute(input(xp = 1_000_000))
        assertEquals(Difficulty.NORMAL.defaultXp * 2, r.baseXp)
    }

    @Test fun bonusIsCapped() {
        val r = CompletionEngine.compute(input(bonus = 5000, streak = 40))
        assertEquals(XpRules.MAX_BONUS_PERCENT, r.totalBonusPercent)
        assertTrue(r.xpAwarded <= 25 * 2)
    }

    @Test fun dailyCapStopsRewards() {
        val rules = XpRules()
        val r = CompletionEngine.compute(input(earned = rules.dailyXpCap))
        assertEquals(0, r.xpAwarded); assertTrue(r.capped); assertEquals(0, r.coinsAwarded); assertEquals(0, r.gemsAwarded)
    }

    @Test fun partialCapAwardsOnlyRemainder() {
        val rules = XpRules()
        val r = CompletionEngine.compute(input(earned = rules.dailyXpCap - 5))
        assertEquals(5, r.xpAwarded); assertTrue(r.capped)
    }

    @Test fun levelUpGivesRewardsAndSkillPoint() {
        val r = CompletionEngine.compute(input(totalXp = 50, level = 1))
        assertEquals(2, r.newLevel); assertEquals(1, r.skillPoints); assertEquals(CompletionEngine.levelUpGold(2), r.levelGold)
    }

    @Test fun levelNeverDecreases() {
        val r = CompletionEngine.compute(input(totalXp = LevelCurve.xpForLevel(5), level = 5))
        assertTrue(r.newLevel >= 5)
    }
}

class PeriodKeyTest {
    @Test fun weekStartsOnSaturday() {
        val sat = 3L // 1970-01-04 is Sunday; 1970-01-03 (day 2) is Saturday
        assertEquals(2L, PeriodKeys.weekStart(sat))
        assertEquals(PeriodKeys.week(2), PeriodKeys.week(8))
        assertTrue(PeriodKeys.week(8) != PeriodKeys.week(9))
    }

    @Test fun dayKeysDiffer() = assertTrue(PeriodKeys.day(1000) != PeriodKeys.day(1001))
}

class EconomyAndSkillTest {
    @Test fun redeemChecks() {
        assertEquals(RewardEconomy.Check.Ok, RewardEconomy.checkRedeem(100, 50, 0, 1))
        assertEquals(RewardEconomy.Check.NotEnough, RewardEconomy.checkRedeem(10, 50, 0, 1))
        assertEquals(RewardEconomy.Check.DailyLimit, RewardEconomy.checkRedeem(1000, 50, 1, 1))
        assertEquals(RewardEconomy.Check.Invalid, RewardEconomy.checkRedeem(1000, 0, 0, 1))
        assertEquals(RewardEconomy.Check.Invalid, RewardEconomy.checkRedeem(Long.MAX_VALUE, RewardEconomy.MAX_COST + 1, 0, 1))
    }

    @Test fun spendNeverNegative() {
        assertFalse(RewardEconomy.checkSpend(5, 6)); assertFalse(RewardEconomy.checkSpend(5, -1)); assertTrue(RewardEconomy.checkSpend(5, 5))
    }

    @Test fun skillNeedsPointsAndPrerequisite() {
        val child = Catalog.skills.first { it.parentId != null }
        assertTrue(SkillRules.check(child, emptyMap(), 100) is SkillRules.Check.NeedPrerequisite)
        val parent = Catalog.skills.first { it.id == child.parentId }
        val ranks = mapOf(parent.id to child.parentRank)
        assertTrue(SkillRules.check(child, ranks, 0) is SkillRules.Check.NotEnoughPoints)
        assertTrue(SkillRules.check(child, ranks, 100) is SkillRules.Check.Ok)
        assertTrue(SkillRules.check(child, ranks + (child.id to 3), 100) is SkillRules.Check.MaxRank)
    }

    @Test fun dailyRewardCycle() {
        assertTrue(DailyReward.canClaim(5, 6)); assertFalse(DailyReward.canClaim(6, 6))
        assertEquals(1, DailyReward.nextCycleDay(1, 10, 3)) // missed days restart quietly
    }
}

class BackupTest {
    @Test fun roundTrip() {
        val f = BackupFile(player = PlayerEntity(name = "Taha", totalXp = 500, onboarded = true))
        val parsed = BackupCodec.parse(BackupCodec.encode(f))
        assertTrue(parsed is BackupParse.Ok)
        assertEquals("Taha", (parsed as BackupParse.Ok).file.player.name)
    }

    @Test fun garbageIsRejectedWithoutCrash() {
        assertTrue(BackupCodec.parse("not json") is BackupParse.Invalid)
        assertTrue(BackupCodec.parse("") is BackupParse.Invalid)
    }

    @Test fun foreignAppRejected() = assertTrue(BackupCodec.parse(BackupCodec.encode(BackupFile(app = "other.app"))) is BackupParse.Invalid)

    @Test fun cheatedXpRejected() {
        val f = BackupFile(player = PlayerEntity(totalXp = Long.MAX_VALUE / 4))
        assertTrue(BackupCodec.parse(BackupCodec.encode(f)) is BackupParse.Invalid)
    }

    @Test fun negativeCurrencyRejected() {
        assertTrue(BackupCodec.parse(BackupCodec.encode(BackupFile(player = PlayerEntity(gold = -5)))) is BackupParse.Invalid)
    }
}
