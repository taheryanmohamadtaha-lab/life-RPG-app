package com.liferpg.adventure.data

import androidx.room.withTransaction
import com.liferpg.adventure.backup.BACKUP_APP_ID
import com.liferpg.adventure.backup.BACKUP_SCHEMA
import com.liferpg.adventure.backup.BackupFile
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.LevelCurve
import com.liferpg.adventure.domain.PeriodKeys
import com.liferpg.adventure.domain.QuestError
import com.liferpg.adventure.domain.QuestKind
import com.liferpg.adventure.domain.QuestOrigin
import com.liferpg.adventure.domain.QuestStatus
import com.liferpg.adventure.domain.QuestValidator
import com.liferpg.adventure.domain.RewardEconomy
import com.liferpg.adventure.domain.Stat
import com.liferpg.adventure.domain.TimeProvider
import com.liferpg.adventure.domain.XpRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Queries and simple CRUD. Game-rule transactions live in [GameActions]. */
class GameRepository(
    val db: LifeDatabase,
    val settings: SettingsRepository,
    val time: TimeProvider,
) {
    private val playerDao get() = db.playerDao()
    private val questDao get() = db.questDao()
    private val categoryDao get() = db.categoryDao()
    private val completionDao get() = db.completionDao()
    private val progressDao get() = db.progressDao()
    private val rewardDao get() = db.rewardDao()

    val player: Flow<PlayerEntity?> = playerDao.observe()
    val quests: Flow<List<QuestEntity>> = questDao.observeVisible()
    val categories: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val allCompletions: Flow<List<CompletionEntity>> = completionDao.observeAll()
    val categoryCounts: Flow<List<CategoryCount>> = completionDao.observeCountsByCategory()
    val levelHistory: Flow<List<LevelHistoryEntity>> = progressDao.observeLevels()
    val unlocks: Flow<List<AchievementUnlockEntity>> = progressDao.observeUnlocks()
    val customAchievements: Flow<List<CustomAchievementEntity>> = progressDao.observeCustomAchievements()
    val skillRanks: Flow<Map<String, Int>> = progressDao.observeSkillRanks().map { l -> l.associate { it.skillId to it.rank } }
    val ownedItems: Flow<Set<String>> = progressDao.observeItems().map { l -> l.map { it.itemId }.toSet() }
    val restDays: Flow<Set<Long>> = progressDao.observeRestDays().map { l -> l.map { it.epochDay }.toSet() }
    val rewards: Flow<List<RewardEntity>> = rewardDao.observeRewards()
    val purchases: Flow<List<PurchaseEntity>> = rewardDao.observePurchases()

    fun completionsForKeys(keys: List<String>): Flow<List<CompletionEntity>> = completionDao.observeByKeys(keys)

    // ------------------------------------------------------------------ first run / seeding
    suspend fun ensureSeeded() {
        val lang = settings.current().language
        db.withTransaction {
            val now = time.nowMillis()
            if (playerDao.insertIfAbsent(PlayerEntity(createdAt = now)) != -1L) {
                progressDao.insertLevel(LevelHistoryEntity(1, now, 0))
            }
            if (categoryDao.count() == 0) {
                categoryDao.upsertAll(Catalog.categories.mapIndexed { i, c -> categoryDefToEntity(i, c) })
            }
            if (rewardDao.countRewards() == 0) rewardDao.upsertRewards(defaultRewards(lang, now))
        }
    }

    private fun defaultRewards(lang: String, now: Long): List<RewardEntity> {
        val fa = lang == "fa"
        return listOf(
            RewardEntity("reward-movie", if (fa) "تماشای یک فیلم" else "Watch a movie", "", 150, "🎬", "REAL", 2, now),
            RewardEntity("reward-game", if (fa) "یک ساعت بازی" else "Play a game for an hour", "", 100, "🎮", "REAL", 2, now + 1),
            RewardEntity("reward-rest", if (fa) "یک روز استراحت" else "Take a rest day", if (fa) "استراحت بخشی از پیشرفت است؛ استریک شما حفظ می‌شود." else "Rest is part of progress; your streak stays safe.", 200, "🛌", "REST", 1, now + 2),
            RewardEntity("reward-outing", if (fa) "گردش و بیرون رفتن" else "Go on an outing", "", 300, "🌳", "REAL", 1, now + 3),
            RewardEntity("reward-treat", if (fa) "یک خرید کوچک (در حد بودجه)" else "A small treat (within your budget)", if (fa) "فقط در چارچوب بودجه‌ای که از قبل تعیین کرده‌اید." else "Only within the budget you've set beforehand.", 250, "🛍️", "REAL", 1, now + 4),
        )
    }

    suspend fun getPlayer(): PlayerEntity? = playerDao.get()

    // ------------------------------------------------------------------ profile
    suspend fun saveProfile(name: String, avatarId: Int, petType: Int, petName: String, onboarded: Boolean = true) {
        db.withTransaction {
            val p = playerDao.get() ?: PlayerEntity(createdAt = time.nowMillis())
            playerDao.upsert(
                p.copy(
                    name = name.trim().take(40), avatarId = avatarId.coerceIn(0, Catalog.avatars.size - 1),
                    petType = petType.coerceIn(0, Catalog.pets.size - 1), petName = petName.trim().take(40), onboarded = onboarded,
                ),
            )
        }
    }

    // ------------------------------------------------------------------ quests
    suspend fun getQuest(id: String): QuestEntity? = questDao.get(id)

    /** Creates or updates a quest after validating it. Returns the validation errors (empty = saved). */
    suspend fun saveQuest(existing: QuestEntity?, draft: QuestEntity, rules: XpRules): List<QuestError> {
        val today = time.today()
        val errors = QuestValidator.validate(
            draft.title, draft.description, draft.notes, draft.xpReward, draft.coinReward, draft.difficultyEnum(),
            rules, draft.deadlineDay, today, isNew = existing == null,
        )
        if (errors.isNotEmpty()) return errors
        val clean = draft.copy(
            title = draft.title.trim(), description = draft.description.trim(), notes = draft.notes.trim(),
            weekdaysMask = if (draft.kindEnum() == QuestKind.DAILY) draft.weekdaysMask.coerceIn(0, 127) else 0,
            id = existing?.id ?: draft.id.ifBlank { UUID.randomUUID().toString() },
            createdAt = existing?.createdAt ?: time.nowMillis(),
            status = existing?.status ?: QuestStatus.ACTIVE.name,
            completedAt = existing?.completedAt,
            postponedCount = existing?.postponedCount ?: 0,
            snoozeUntilDay = existing?.snoozeUntilDay ?: -1,
            origin = existing?.origin ?: QuestOrigin.USER.name,
        )
        questDao.upsert(clean)
        return emptyList()
    }

    /** User quests are deleted (their reward history is kept); auto-generated challenges are archived so they don't respawn. */
    suspend fun deleteQuest(id: String) {
        val q = questDao.get(id) ?: return
        if (q.originEnum() == QuestOrigin.USER) questDao.delete(id) else questDao.upsert(q.copy(status = QuestStatus.ARCHIVED.name))
    }

    suspend fun postponeQuest(id: String, days: Int): Boolean {
        val q = questDao.get(id) ?: return false
        if (q.statusEnum() != QuestStatus.ACTIVE || q.postponedCount >= MAX_POSTPONES) return false
        val d = days.coerceIn(1, 7)
        val today = time.today()
        questDao.upsert(
            q.copy(
                snoozeUntilDay = today + d,
                deadlineDay = q.deadlineDay?.let { it.coerceAtLeast(today) + d },
                postponedCount = q.postponedCount + 1,
            ),
        )
        return true
    }

    // ------------------------------------------------------------------ categories
    suspend fun saveCustomCategory(nameFa: String, nameEn: String, emoji: String, color: Long, primary: Stat, secondary: Stat): Boolean {
        val fa = nameFa.trim(); val en = nameEn.trim()
        if (fa.isEmpty() || fa.length > 40 || en.isEmpty() || en.length > 40) return false
        if (categoryDao.count() >= 40) return false
        categoryDao.upsert(
            CategoryEntity(
                id = "custom_" + UUID.randomUUID().toString().take(8), nameFa = fa, nameEn = en,
                emoji = emoji.trim().ifEmpty { "⭐" }.take(4), color = color, primaryStat = primary.name,
                secondaryStat = secondary.name, builtIn = false, sortOrder = 100 + categoryDao.count(),
            ),
        )
        return true
    }

    suspend fun deleteCustomCategory(id: String) {
        db.withTransaction {
            val c = categoryDao.get(id) ?: return@withTransaction
            if (c.builtIn) return@withTransaction
            questDao.moveCategory(id, Catalog.CAREER)
            categoryDao.deleteCustom(id)
        }
    }

    // ------------------------------------------------------------------ rewards (real-life, user defined)
    suspend fun saveReward(existing: RewardEntity?, title: String, note: String, cost: Int, emoji: String, rest: Boolean, maxPerDay: Int): Boolean {
        val t = title.trim()
        if (t.isEmpty() || t.length > 60 || note.length > 200) return false
        if (cost !in RewardEconomy.MIN_COST..RewardEconomy.MAX_COST) return false
        rewardDao.upsertReward(
            RewardEntity(
                id = existing?.id ?: UUID.randomUUID().toString(), title = t, note = note.trim(), cost = cost,
                emoji = emoji.trim().ifEmpty { "🎁" }.take(4), kind = if (rest) "REST" else "REAL",
                maxPerDay = maxPerDay.coerceIn(1, RewardEconomy.MAX_PER_DAY_LIMIT), createdAt = existing?.createdAt ?: time.nowMillis(),
            ),
        )
        return true
    }

    suspend fun deleteReward(id: String) = rewardDao.deleteReward(id)

    // ------------------------------------------------------------------ custom achievements
    suspend fun addCustomAchievement(title: String, description: String, categoryId: String, target: Int, gold: Int): Boolean {
        val t = title.trim()
        if (t.isEmpty() || t.length > 60 || description.length > 200 || target !in 1..10_000 || gold !in 0..1000) return false
        progressDao.upsertCustomAchievement(
            CustomAchievementEntity(UUID.randomUUID().toString(), t, description.trim(), categoryId, target, gold, time.nowMillis()),
        )
        return true
    }

    suspend fun deleteCustomAchievement(id: String) {
        db.withTransaction {
            progressDao.deleteCustomAchievement(id)
            progressDao.deleteUnlock("custom_$id")
        }
    }

    // ------------------------------------------------------------------ backup / reset
    suspend fun exportBackup(): BackupFile = db.withTransaction {
        BackupFile(
            app = BACKUP_APP_ID, schema = BACKUP_SCHEMA, exportedAt = time.nowMillis(),
            player = playerDao.get() ?: PlayerEntity(),
            quests = questDao.getAll(), completions = completionDao.getAll(), levelHistory = progressDao.getLevels(),
            achievements = progressDao.getUnlocks(), customAchievements = progressDao.getCustomAchievements(),
            skills = progressDao.getSkillRanks(), items = progressDao.getItems(), rewards = rewardDao.getRewards(),
            purchases = rewardDao.getPurchases(), categories = categoryDao.getAll(), restDays = progressDao.getRestDays(),
        )
    }

    /** Replaces ALL local data with an already validated backup, atomically. */
    suspend fun importBackup(f: BackupFile) {
        db.withTransaction {
            wipeAll()
            playerDao.upsert(f.player)
            questDao.upsertAll(f.quests)
            completionDao.upsertAll(f.completions)
            progressDao.upsertLevels(f.levelHistory)
            progressDao.upsertUnlocks(f.achievements)
            progressDao.upsertCustomAchievements(f.customAchievements)
            progressDao.upsertSkillRanks(f.skills)
            progressDao.upsertItems(f.items)
            rewardDao.upsertRewards(f.rewards)
            rewardDao.upsertPurchases(f.purchases)
            categoryDao.upsertAll(f.categories)
            progressDao.upsertRestDays(f.restDays)
        }
        ensureSeeded()
    }

    private suspend fun wipeAll() {
        playerDao.deleteAll(); questDao.deleteAll(); completionDao.deleteAll(); progressDao.deleteLevels()
        progressDao.deleteUnlocks(); progressDao.deleteCustomAchievements(); progressDao.deleteSkillRanks()
        progressDao.deleteItems(); rewardDao.deleteRewards(); rewardDao.deletePurchases(); categoryDao.deleteAll()
        progressDao.deleteRestDays()
    }

    /** Wipes progress but keeps the hero's identity (name, avatar, pet). */
    suspend fun resetProgress() {
        db.withTransaction {
            val old = playerDao.get()
            wipeAll()
            val now = time.nowMillis()
            playerDao.upsert(
                PlayerEntity(
                    name = old?.name ?: "", avatarId = old?.avatarId ?: 0, petType = old?.petType ?: 0,
                    petName = old?.petName ?: "", onboarded = true, createdAt = now,
                ),
            )
            progressDao.insertLevel(LevelHistoryEntity(1, now, 0))
        }
        ensureSeeded()
    }

    companion object {
        const val MAX_POSTPONES = 5
    }
}

