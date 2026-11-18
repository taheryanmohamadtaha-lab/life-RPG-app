package com.liferpg.adventure.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player WHERE id = 1")
    fun observe(): Flow<PlayerEntity?>

    @Query("SELECT * FROM player WHERE id = 1")
    suspend fun get(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(p: PlayerEntity): Long

    @Query("DELETE FROM player")
    suspend fun deleteAll()
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests WHERE status != 'ARCHIVED' ORDER BY createdAt DESC")
    fun observeVisible(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun get(id: String): QuestEntity?

    @Query("SELECT * FROM quests")
    suspend fun getAll(): List<QuestEntity>

    @Query("SELECT * FROM quests WHERE status = 'ACTIVE' AND reminderMinute >= 0")
    suspend fun withReminders(): List<QuestEntity>

    @Query("SELECT * FROM quests WHERE status = 'ACTIVE'")
    suspend fun active(): List<QuestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(q: QuestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<QuestEntity>)

    @Query("DELETE FROM quests WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM quests")
    suspend fun deleteAll()

    @Query("UPDATE quests SET status = 'ARCHIVED' WHERE origin != 'USER' AND status = 'ACTIVE' AND deadlineDay IS NOT NULL AND deadlineDay < :today")
    suspend fun archiveExpiredSpecials(today: Long)

    @Query("UPDATE quests SET categoryId = :to WHERE categoryId = :from")
    suspend fun moveCategory(from: String, to: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun get(id: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id AND builtIn = 0")
    suspend fun deleteCustom(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface CompletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(c: CompletionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<CompletionEntity>)

    @Query("SELECT COUNT(*) FROM completions WHERE questId = :questId AND periodKey = :key")
    suspend fun existsFor(questId: String, key: String): Int

    @Query("SELECT * FROM completions WHERE periodKey IN (:keys)")
    fun observeByKeys(keys: List<String>): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions ORDER BY completedAt")
    suspend fun getAll(): List<CompletionEntity>

    @Query("SELECT COUNT(*) FROM completions")
    suspend fun count(): Int

    @Query("SELECT categoryId, COUNT(*) AS n FROM completions GROUP BY categoryId")
    suspend fun countsByCategory(): List<CategoryCount>

    @Query("SELECT categoryId, COUNT(*) AS n FROM completions GROUP BY categoryId")
    fun observeCountsByCategory(): Flow<List<CategoryCount>>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM completions WHERE epochDay = :day")
    suspend fun xpOnDay(day: Long): Int

    @Query("SELECT COALESCE(SUM(coins), 0) FROM completions WHERE epochDay = :day")
    suspend fun coinsOnDay(day: Long): Int

    @Query("SELECT COALESCE(SUM(gems), 0) FROM completions WHERE epochDay = :day")
    suspend fun gemsOnDay(day: Long): Int

    @Query("SELECT COUNT(*) FROM completions WHERE epochDay BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM completions WHERE difficulty IN ('HARD', 'EPIC', 'LEGENDARY')")
    suspend fun countHardPlus(): Int

    @Query("SELECT COUNT(*) FROM completions WHERE categoryId = :categoryId")
    suspend fun countCategory(categoryId: String): Int

    @Query("DELETE FROM completions")
    suspend fun deleteAll()
}

@Dao
interface ProgressDao {
    // level history
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLevel(l: LevelHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevels(list: List<LevelHistoryEntity>)

    @Query("SELECT * FROM level_history ORDER BY level")
    fun observeLevels(): Flow<List<LevelHistoryEntity>>

    @Query("SELECT * FROM level_history ORDER BY level")
    suspend fun getLevels(): List<LevelHistoryEntity>

    @Query("DELETE FROM level_history")
    suspend fun deleteLevels()

    // achievements
    @Query("SELECT * FROM achievement_unlocks")
    fun observeUnlocks(): Flow<List<AchievementUnlockEntity>>

    @Query("SELECT * FROM achievement_unlocks")
    suspend fun getUnlocks(): List<AchievementUnlockEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(a: AchievementUnlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnlocks(list: List<AchievementUnlockEntity>)

    @Query("DELETE FROM achievement_unlocks")
    suspend fun deleteUnlocks()

    @Query("SELECT * FROM custom_achievements ORDER BY createdAt")
    fun observeCustomAchievements(): Flow<List<CustomAchievementEntity>>

    @Query("SELECT * FROM custom_achievements ORDER BY createdAt")
    suspend fun getCustomAchievements(): List<CustomAchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomAchievement(a: CustomAchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomAchievements(list: List<CustomAchievementEntity>)

    @Query("DELETE FROM custom_achievements WHERE id = :id")
    suspend fun deleteCustomAchievement(id: String)

    @Query("DELETE FROM achievement_unlocks WHERE id = :id")
    suspend fun deleteUnlock(id: String)

    @Query("DELETE FROM custom_achievements")
    suspend fun deleteCustomAchievements()

    // skills
    @Query("SELECT * FROM skill_ranks")
    fun observeSkillRanks(): Flow<List<SkillRankEntity>>

    @Query("SELECT * FROM skill_ranks")
    suspend fun getSkillRanks(): List<SkillRankEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkillRank(r: SkillRankEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkillRanks(list: List<SkillRankEntity>)

    @Query("DELETE FROM skill_ranks")
    suspend fun deleteSkillRanks()

    // inventory
    @Query("SELECT * FROM owned_items")
    fun observeItems(): Flow<List<OwnedItemEntity>>

    @Query("SELECT * FROM owned_items")
    suspend fun getItems(): List<OwnedItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(i: OwnedItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(list: List<OwnedItemEntity>)

    @Query("DELETE FROM owned_items")
    suspend fun deleteItems()

    // rest days
    @Query("SELECT * FROM rest_days ORDER BY epochDay")
    fun observeRestDays(): Flow<List<RestDayEntity>>

    @Query("SELECT * FROM rest_days ORDER BY epochDay")
    suspend fun getRestDays(): List<RestDayEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRestDay(d: RestDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRestDays(list: List<RestDayEntity>)

    @Query("SELECT COUNT(*) FROM rest_days WHERE epochDay BETWEEN :from AND :to")
    suspend fun countRestDays(from: Long, to: Long): Int

    @Query("DELETE FROM rest_days")
    suspend fun deleteRestDays()
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards ORDER BY createdAt, id")
    fun observeRewards(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards ORDER BY createdAt, id")
    suspend fun getRewards(): List<RewardEntity>

    @Query("SELECT * FROM rewards WHERE id = :id")
    suspend fun getReward(id: String): RewardEntity?

    @Query("SELECT COUNT(*) FROM rewards")
    suspend fun countRewards(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReward(r: RewardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRewards(list: List<RewardEntity>)

    @Query("DELETE FROM rewards WHERE id = :id")
    suspend fun deleteReward(id: String)

    @Query("DELETE FROM rewards")
    suspend fun deleteRewards()

    @Query("SELECT * FROM purchases ORDER BY at DESC")
    fun observePurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY at")
    suspend fun getPurchases(): List<PurchaseEntity>

    @Query("SELECT COUNT(*) FROM purchases WHERE rewardId = :rewardId AND epochDay = :day")
    suspend fun countPurchasesOnDay(rewardId: String, day: Long): Int

    @Insert
    suspend fun insertPurchase(p: PurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPurchases(list: List<PurchaseEntity>)

    @Query("DELETE FROM purchases")
    suspend fun deletePurchases()
}
