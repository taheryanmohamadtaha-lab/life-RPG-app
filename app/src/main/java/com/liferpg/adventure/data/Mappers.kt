package com.liferpg.adventure.data

import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.Difficulty
import com.liferpg.adventure.domain.ItemSlot
import com.liferpg.adventure.domain.PeriodKeys
import com.liferpg.adventure.domain.QuestKind
import com.liferpg.adventure.domain.QuestOrigin
import com.liferpg.adventure.domain.QuestStatus
import com.liferpg.adventure.domain.Schedule
import com.liferpg.adventure.domain.SkillRules
import com.liferpg.adventure.domain.Stat
import com.liferpg.adventure.domain.enumOr

fun QuestEntity.kindEnum(): QuestKind = enumOr(kind, QuestKind.ONE_TIME)
fun QuestEntity.difficultyEnum(): Difficulty = enumOr(difficulty, Difficulty.NORMAL)
fun QuestEntity.statusEnum(): QuestStatus = enumOr(status, QuestStatus.ACTIVE)
fun QuestEntity.originEnum(): QuestOrigin = enumOr(origin, QuestOrigin.USER)

/** The period key this quest would be rewarded under if completed on [today]. */
fun QuestEntity.periodKey(today: Long): String = PeriodKeys.forKind(kindEnum(), today)

/** Is the quest available to complete on [today]? (scheduled today, not archived/done) */
fun QuestEntity.isAvailableOn(today: Long): Boolean {
    if (statusEnum() != QuestStatus.ACTIVE) return false
    return if (kindEnum() == QuestKind.DAILY) Schedule.isScheduledOn(weekdaysMask, today) else true
}

/** Shown on the "today" board: available and not snoozed. */
fun QuestEntity.isOnTodayBoard(today: Long): Boolean = isAvailableOn(today) && snoozeUntilDay <= today

fun CategoryEntity.primary(): Stat = enumOr(primaryStat, Stat.DISCIPLINE)
fun CategoryEntity.secondary(): Stat = enumOr(secondaryStat, Stat.FOCUS)

fun categoryDefToEntity(index: Int, c: com.liferpg.adventure.domain.CategoryDef) = CategoryEntity(
    id = c.id, nameFa = c.fa, nameEn = c.en, emoji = c.emoji, color = c.color,
    primaryStat = c.primary.name, secondaryStat = c.secondary.name, builtIn = true, sortOrder = index,
)

/** Equipment + skills summed into the numbers that actually affect the game. */
data class Bonuses(
    val xpPercent: Int,
    val coinPercent: Int,
    val stats: Map<Stat, Int>,
)

object BonusCalculator {
    fun compute(player: PlayerEntity, ownedIds: Set<String>, ranks: Map<String, Int>): Bonuses {
        var xp = 0
        var coin = 0
        val stats = HashMap<Stat, Int>()
        fun addItem(id: String) {
            val def = Catalog.item(id) ?: return
            xp += def.xpPercent
            coin += def.coinPercent
            for ((s, v) in def.stats) stats[s] = (stats[s] ?: 0) + v
        }
        for (id in player.equippedIds()) if (id in ownedIds) addItem(id)
        // Magical items are passive: owning them is enough.
        for (id in ownedIds) {
            val def = Catalog.item(id) ?: continue
            if (def.slot == ItemSlot.MAGIC && !def.consumableShield) addItem(id)
        }
        for ((s, v) in SkillRules.statBonuses(ranks)) stats[s] = (stats[s] ?: 0) + v
        return Bonuses(xp, coin, stats)
    }

    fun effectiveStats(player: PlayerEntity, bonuses: Bonuses): Map<Stat, Int> =
        Stat.entries.associateWith { player.stat(it) + (bonuses.stats[it] ?: 0) }
}
