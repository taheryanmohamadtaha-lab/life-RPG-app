package com.liferpg.adventure.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.liferpg.adventure.domain.Stat
import com.liferpg.adventure.domain.XpRules
import kotlinx.serialization.Serializable

/** Single-row table holding the hero. */
@Serializable
@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val avatarId: Int = 0,
    val petType: Int = 0,
    val petName: String = "",
    val onboarded: Boolean = false,
    val totalXp: Long = 0,
    val level: Int = 1,
    val gold: Long = 0,
    val gems: Long = 0,
    val skillPoints: Int = 0,
    val strength: Int = 1,
    val intelligence: Int = 1,
    val discipline: Int = 1,
    val vitality: Int = 1,
    val creativity: Int = 1,
    val focus: Int = 1,
    val wisdom: Int = 1,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDay: Long = -1,
    val streakShields: Int = 1,
    val dailyClaimDay: Long = -1,
    val dailyClaimCycle: Int = 0,
    val lastEventDay: Long = -1,
    val equippedWeapon: String = "",
    val equippedArmor: String = "",
    val equippedHelmet: String = "",
    val equippedAccessory: String = "",
    val createdAt: Long = 0,
) {
    fun stat(s: Stat): Int = when (s) {
        Stat.STRENGTH -> strength
        Stat.INTELLIGENCE -> intelligence
        Stat.DISCIPLINE -> discipline
        Stat.VITALITY -> vitality
        Stat.CREATIVITY -> creativity
        Stat.FOCUS -> focus
        Stat.WISDOM -> wisdom
    }

    fun withStatAdded(s: Stat, amount: Int): PlayerEntity {
        val v = (stat(s) + amount).coerceIn(0, XpRules.STAT_CAP)
        return when (s) {
            Stat.STRENGTH -> copy(strength = v)
            Stat.INTELLIGENCE -> copy(intelligence = v)
            Stat.DISCIPLINE -> copy(discipline = v)
            Stat.VITALITY -> copy(vitality = v)
            Stat.CREATIVITY -> copy(creativity = v)
            Stat.FOCUS -> copy(focus = v)
            Stat.WISDOM -> copy(wisdom = v)
        }
    }

    fun baseStats(): Map<Stat, Int> = Stat.entries.associateWith { stat(it) }
    fun equippedIds(): List<String> = listOf(equippedWeapon, equippedArmor, equippedHelmet, equippedAccessory).filter { it.isNotEmpty() }
}

@Serializable
@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val categoryId: String,
    val kind: String,
    val difficulty: String,
    val xpReward: Int,
    val coinReward: Int = 0,
    val deadlineDay: Long? = null,
    /** bit0 = Saturday ... bit6 = Friday, 0 = every day (daily quests only). */
    val weekdaysMask: Int = 0,
    val notes: String = "",
    val status: String = "ACTIVE",
    val snoozeUntilDay: Long = -1,
    val postponedCount: Int = 0,
    /** Minutes after midnight for an optional reminder, -1 = none. */
    val reminderMinute: Int = -1,
    val origin: String = "USER",
    val createdAt: Long = 0,
    val completedAt: Long? = null,
)

/** One reward per (quest, period). The unique index makes duplicate rewards impossible at DB level. */
@Serializable
@Entity(
    tableName = "completions",
    indices = [Index(value = ["questId", "periodKey"], unique = true), Index(value = ["epochDay"])],
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questId: String,
    val periodKey: String,
    val questTitle: String,
    val categoryId: String,
    val difficulty: String,
    val epochDay: Long,
    val completedAt: Long,
    val xp: Int,
    val coins: Int,
    val gems: Int = 0,
)

@Serializable
@Entity(tableName = "level_history")
data class LevelHistoryEntity(@PrimaryKey val level: Int, val reachedAt: Long, val totalXp: Long)

@Serializable
@Entity(tableName = "achievement_unlocks")
data class AchievementUnlockEntity(@PrimaryKey val id: String, val unlockedAt: Long)

@Serializable
@Entity(tableName = "custom_achievements")
data class CustomAchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    /** Empty = any category. */
    val categoryId: String = "",
    val target: Int,
    val rewardGold: Int = 50,
    val createdAt: Long = 0,
)

@Serializable
@Entity(tableName = "skill_ranks")
data class SkillRankEntity(@PrimaryKey val skillId: String, val rank: Int)

@Serializable
@Entity(tableName = "owned_items")
data class OwnedItemEntity(@PrimaryKey val itemId: String, val acquiredAt: Long)

/** A real-life reward the user defines for themselves (spent with virtual gold). */
@Serializable
@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String = "",
    val cost: Int,
    val emoji: String = "🎁",
    /** REAL or REST (a rest day declares today as a streak-safe rest day). */
    val kind: String = "REAL",
    val maxPerDay: Int = 2,
    val createdAt: Long = 0,
)

@Serializable
@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardId: String,
    val title: String,
    val cost: Int,
    val epochDay: Long,
    val at: Long,
)

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val nameFa: String,
    val nameEn: String,
    val emoji: String,
    val color: Long,
    val primaryStat: String,
    val secondaryStat: String,
    val builtIn: Boolean = false,
    val sortOrder: Int = 0,
) {
    fun name(lang: String) = if (lang == "fa") nameFa else nameEn
}

@Serializable
@Entity(tableName = "rest_days")
data class RestDayEntity(@PrimaryKey val epochDay: Long, val createdAt: Long = 0)

data class CategoryCount(val categoryId: String, val n: Int)
data class DayXp(val epochDay: Long, val xp: Int, val n: Int)

