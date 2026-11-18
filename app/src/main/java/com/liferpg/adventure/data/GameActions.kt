package com.liferpg.adventure.data

import androidx.room.withTransaction
import com.liferpg.adventure.domain.AchievementMetrics
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.CompletionEngine
import com.liferpg.adventure.domain.CompletionInput
import com.liferpg.adventure.domain.ChallengeTemplate
import com.liferpg.adventure.domain.Currency
import com.liferpg.adventure.domain.DailyReward
import com.liferpg.adventure.domain.Difficulty
import com.liferpg.adventure.domain.ItemSlot
import com.liferpg.adventure.domain.LevelCurve
import com.liferpg.adventure.domain.PeriodKeys
import com.liferpg.adventure.domain.QuestKind
import com.liferpg.adventure.domain.QuestOrigin
import com.liferpg.adventure.domain.RandomEvent
import com.liferpg.adventure.domain.RandomEvents
import com.liferpg.adventure.domain.RewardEconomy
import com.liferpg.adventure.domain.Schedule
import com.liferpg.adventure.domain.SkillRules
import com.liferpg.adventure.domain.Stat
import com.liferpg.adventure.domain.StreakCalculator
import com.liferpg.adventure.domain.TimeProvider
import kotlin.math.max

data class UnlockedAchievement(val id: String, val titleFa: String, val titleEn: String, val emoji: String, val gold: Int, val gems: Int) {
    fun title(lang: String) = if (lang == "fa") titleFa else titleEn
}

sealed class CompleteResult {
    data class Success(
        val questTitle: String,
        val xp: Int,
        val baseXp: Int,
        val bonusPercent: Int,
        val coins: Int,
        val gems: Int,
        val levelGold: Int,
        val levelGems: Int,
        val oldLevel: Int,
        val newLevel: Int,
        val skillPoints: Int,
        val streak: Int,
        val usedShields: Int,
        val restarted: Boolean,
        val earnedShield: Boolean,
        val capped: Boolean,
        val statGains: Map<Stat, Int>,
        val achievements: List<UnlockedAchievement>,
        val petEvolved: Boolean,
        val heroEvolved: Boolean,
    ) : CompleteResult() {
        val leveledUp: Boolean get() = newLevel > oldLevel
    }

    data object AlreadyDone : CompleteResult()
    data object NotAvailable : CompleteResult()
    data object DailyCapReached : CompleteResult()
    data object NotFound : CompleteResult()
}

data class DailyClaim(val cycleDay: Int, val gold: Int, val gems: Int)

sealed class SkillResult {
    data class Ok(val newRank: Int, val spent: Int) : SkillResult()
    data class Failed(val reason: SkillRules.Check) : SkillResult()
    data object NotFound : SkillResult()
}

enum class BuyResult { OK, NOT_ENOUGH, OWNED, SHIELD_MAX, NOT_FOUND }
enum class RedeemResult { OK, NOT_ENOUGH, DAILY_LIMIT, INVALID, REST_LIMIT, ALREADY_REST, NOT_FOUND }
enum class RestResult { OK, LIMIT, ALREADY }

/**
 * Every rule that changes the player's progress runs here, each inside ONE database transaction so the
 * game can never end up half-updated (for example XP granted without the completion being recorded).
 */
class GameActions(
    private val db: LifeDatabase,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    private val playerDao get() = db.playerDao()
    private val questDao get() = db.questDao()
    private val categoryDao get() = db.categoryDao()
    private val completionDao get() = db.completionDao()
    private val progressDao get() = db.progressDao()
    private val rewardDao get() = db.rewardDao()

    // ------------------------------------------------------------------------------------------ quests
    suspend fun completeQuest(questId: String): CompleteResult {
        val rules = settings.current().rules
        return db.withTransaction {
            val q = questDao.get(questId) ?: return@withTransaction CompleteResult.NotFound
            val today = time.today()
            if (!q.isAvailableOn(today)) return@withTransaction CompleteResult.NotAvailable
            val key = q.periodKey(today)
            if (completionDao.existsFor(q.id, key) > 0) return@withTransaction CompleteResult.AlreadyDone
            val player = playerDao.get() ?: return@withTransaction CompleteResult.NotFound

            val owned = progressDao.getItems().map { it.itemId }.toSet()
            val ranks = progressDao.getSkillRanks().associate { it.skillId to it.rank }
            val bonuses = BonusCalculator.compute(player, owned, ranks)
            val cat = categoryDao.get(q.categoryId)
            val primary = cat?.primary() ?: Stat.DISCIPLINE
            val secondary = cat?.secondary()
            val restDays = progressDao.getRestDays().map { it.epochDay }.toSet()

            val comp = CompletionEngine.compute(
                CompletionInput(
                    difficulty = q.difficultyEnum(), xpReward = q.xpReward, coinReward = q.coinReward,
                    primaryStat = primary, secondaryStat = secondary,
                    totalXp = player.totalXp, level = player.level, streak = player.currentStreak,
                    lastActiveDay = player.lastActiveDay, shields = player.streakShields, restDays = restDays,
                    today = today, rules = rules,
                    xpBonusPercent = bonuses.xpPercent + SkillRules.xpBonusPercent(q.categoryId, ranks),
                    coinBonusPercent = bonuses.coinPercent,
                    earnedXpToday = completionDao.xpOnDay(today), earnedCoinsToday = completionDao.coinsOnDay(today),
                    earnedGemsToday = completionDao.gemsOnDay(today),
                ),
            )
            // Daily limit reached: nothing is recorded, the quest simply stays open for tomorrow.
            if (comp.xpAwarded <= 0) return@withTransaction CompleteResult.DailyCapReached

            val now = time.nowMillis()
            val inserted = completionDao.insertIgnore(
                CompletionEntity(
                    questId = q.id, periodKey = key, questTitle = q.title, categoryId = q.categoryId, difficulty = q.difficulty,
                    epochDay = today, completedAt = now, xp = comp.xpAwarded, coins = comp.coinsAwarded, gems = comp.gemsAwarded,
                ),
            )
            if (inserted == -1L) return@withTransaction CompleteResult.AlreadyDone // unique (quest, period) index

            var p = player.copy(
                totalXp = comp.newTotalXp, level = comp.newLevel,
                gold = player.gold + comp.coinsAwarded + comp.levelGold,
                gems = player.gems + comp.gemsAwarded + comp.levelGems,
                skillPoints = player.skillPoints + comp.skillPoints,
                currentStreak = comp.streak.streak,
                bestStreak = max(player.bestStreak, comp.streak.streak),
                lastActiveDay = max(player.lastActiveDay, today),
                streakShields = comp.streak.shields,
            )
            for ((s, g) in comp.statGains) p = p.withStatAdded(s, g)
            for (l in (comp.oldLevel + 1)..comp.newLevel) progressDao.insertLevel(LevelHistoryEntity(l, now, LevelCurve.xpForLevel(l)))

            val kind = q.kindEnum()
            if (kind == QuestKind.ONE_TIME || kind == QuestKind.GOAL) {
                questDao.upsert(q.copy(status = "DONE", completedAt = now))
            }

            // achievements ------------------------------------------------------------------
            val unlockedNow = ArrayList<UnlockedAchievement>()
            val already = progressDao.getUnlocks().map { it.id }.toSet()
            val perCat = completionDao.countsByCategory().associate { it.categoryId to it.n }
            val total = completionDao.count()
            val metrics = AchievementMetrics(
                total = total, bestStreak = p.bestStreak, level = p.level,
                monthCount = completionDao.countBetween(PeriodKeys.monthRange(today).first, PeriodKeys.monthRange(today).last),
                hardPlus = completionDao.countHardPlus(), perCategory = perCat,
            )
            for (def in Catalog.achievements) {
                if (def.id in already || !def.isReached(metrics)) continue
                if (progressDao.insertUnlock(AchievementUnlockEntity(def.id, now)) == -1L) continue
                p = p.copy(gold = p.gold + def.gold, gems = p.gems + def.gems)
                unlockedNow += UnlockedAchievement(def.id, def.fa, def.en, def.emoji, def.gold, def.gems)
            }
            for (ca in progressDao.getCustomAchievements()) {
                val id = "custom_${ca.id}"
                if (id in already) continue
                val value = if (ca.categoryId.isEmpty()) total else (perCat[ca.categoryId] ?: 0)
                if (value < ca.target) continue
                if (progressDao.insertUnlock(AchievementUnlockEntity(id, now)) == -1L) continue
                p = p.copy(gold = p.gold + ca.rewardGold)
                unlockedNow += UnlockedAchievement(id, ca.title, ca.title, "🏵️", ca.rewardGold, 0)
            }
            playerDao.upsert(p)

            CompleteResult.Success(
                questTitle = q.title, xp = comp.xpAwarded, baseXp = comp.baseXp, bonusPercent = comp.totalBonusPercent,
                coins = comp.coinsAwarded, gems = comp.gemsAwarded, levelGold = comp.levelGold, levelGems = comp.levelGems,
                oldLevel = comp.oldLevel, newLevel = comp.newLevel, skillPoints = comp.skillPoints, streak = comp.streak.streak,
                usedShields = comp.streak.usedShields, restarted = comp.streak.restarted, earnedShield = comp.streak.earnedShield,
                capped = comp.capped, statGains = comp.statGains, achievements = unlockedNow,
                petEvolved = Catalog.petStage(comp.oldLevel) != Catalog.petStage(comp.newLevel),
                heroEvolved = Catalog.heroTier(comp.oldLevel) != Catalog.heroTier(comp.newLevel),
            )
        }
    }

    // ------------------------------------------------------------------------------------------ daily reward
    suspend fun claimDailyReward(): DailyClaim? = db.withTransaction {
        val p = playerDao.get() ?: return@withTransaction null
        val today = time.today()
        if (!DailyReward.canClaim(p.dailyClaimDay, today)) return@withTransaction null
        val cycle = DailyReward.nextCycleDay(p.dailyClaimDay, today, p.dailyClaimCycle)
        val r = DailyReward.reward(cycle)
        playerDao.upsert(p.copy(gold = p.gold + r.gold, gems = p.gems + r.gems, dailyClaimDay = today, dailyClaimCycle = cycle))
        DailyClaim(cycle, r.gold, r.gems)
    }

    // ------------------------------------------------------------------------------------------ skills
    suspend fun upgradeSkill(skillId: String): SkillResult = db.withTransaction {
        val def = Catalog.skill(skillId) ?: return@withTransaction SkillResult.NotFound
        val p = playerDao.get() ?: return@withTransaction SkillResult.NotFound
        val ranks = progressDao.getSkillRanks().associate { it.skillId to it.rank }
        when (val c = SkillRules.check(def, ranks, p.skillPoints)) {
            is SkillRules.Check.Ok -> {
                val next = (ranks[skillId] ?: 0) + 1
                progressDao.upsertSkillRank(SkillRankEntity(skillId, next))
                playerDao.upsert(p.copy(skillPoints = p.skillPoints - c.cost))
                SkillResult.Ok(next, c.cost)
            }
            else -> SkillResult.Failed(c)
        }
    }

    // ------------------------------------------------------------------------------------------ inventory & store
    suspend fun buyItem(itemId: String): BuyResult = db.withTransaction {
        val def = Catalog.item(itemId) ?: return@withTransaction BuyResult.NOT_FOUND
        val p = playerDao.get() ?: return@withTransaction BuyResult.NOT_FOUND
        val owned = progressDao.getItems().map { it.itemId }.toSet()
        if (def.consumableShield) {
            if (p.streakShields >= StreakCalculator.MAX_SHIELDS) return@withTransaction BuyResult.SHIELD_MAX
        } else if (itemId in owned) return@withTransaction BuyResult.OWNED
        val balance = if (def.currency == Currency.GOLD) p.gold else p.gems
        if (!RewardEconomy.checkSpend(balance, def.price.toLong())) return@withTransaction BuyResult.NOT_ENOUGH
        var np = if (def.currency == Currency.GOLD) p.copy(gold = p.gold - def.price) else p.copy(gems = p.gems - def.price)
        if (def.consumableShield) np = np.copy(streakShields = np.streakShields + 1)
        else progressDao.insertItem(OwnedItemEntity(itemId, time.nowMillis()))
        playerDao.upsert(np)
        BuyResult.OK
    }

    suspend fun equip(itemId: String): Boolean = db.withTransaction {
        val def = Catalog.item(itemId) ?: return@withTransaction false
        if (!def.slot.equippable) return@withTransaction false
        val p = playerDao.get() ?: return@withTransaction false
        if (progressDao.getItems().none { it.itemId == itemId }) return@withTransaction false
        playerDao.upsert(
            when (def.slot) {
                ItemSlot.WEAPON -> p.copy(equippedWeapon = itemId)
                ItemSlot.ARMOR -> p.copy(equippedArmor = itemId)
                ItemSlot.HELMET -> p.copy(equippedHelmet = itemId)
                else -> p.copy(equippedAccessory = itemId)
            },
        )
        true
    }

    suspend fun unequip(slot: ItemSlot) = db.withTransaction {
        val p = playerDao.get() ?: return@withTransaction
        playerDao.upsert(
            when (slot) {
                ItemSlot.WEAPON -> p.copy(equippedWeapon = "")
                ItemSlot.ARMOR -> p.copy(equippedArmor = "")
                ItemSlot.HELMET -> p.copy(equippedHelmet = "")
                ItemSlot.ACCESSORY -> p.copy(equippedAccessory = "")
                else -> p
            },
        )
    }

    // ------------------------------------------------------------------------------------------ real-life rewards
    suspend fun redeemReward(rewardId: String): RedeemResult = db.withTransaction {
        val r = rewardDao.getReward(rewardId) ?: return@withTransaction RedeemResult.NOT_FOUND
        val p = playerDao.get() ?: return@withTransaction RedeemResult.NOT_FOUND
        val today = time.today()
        when (RewardEconomy.checkRedeem(p.gold, r.cost, rewardDao.countPurchasesOnDay(r.id, today), r.maxPerDay)) {
            RewardEconomy.Check.Invalid -> return@withTransaction RedeemResult.INVALID
            RewardEconomy.Check.DailyLimit -> return@withTransaction RedeemResult.DAILY_LIMIT
            RewardEconomy.Check.NotEnough -> return@withTransaction RedeemResult.NOT_ENOUGH
            RewardEconomy.Check.Ok -> Unit
        }
        if (r.kind == "REST") {
            when (insertRestDayChecked(today)) {
                RestResult.LIMIT -> return@withTransaction RedeemResult.REST_LIMIT
                RestResult.ALREADY -> return@withTransaction RedeemResult.ALREADY_REST
                RestResult.OK -> Unit
            }
        }
        playerDao.upsert(p.copy(gold = p.gold - r.cost))
        rewardDao.insertPurchase(PurchaseEntity(rewardId = r.id, title = r.title, cost = r.cost, epochDay = today, at = time.nowMillis()))
        RedeemResult.OK
    }

    /** A free, streak-safe rest day (limited per week so rest never replaces progress). */
    suspend fun declareRestDay(): RestResult = db.withTransaction { insertRestDayChecked(time.today()) }

    private suspend fun insertRestDayChecked(today: Long): RestResult {
        val start = PeriodKeys.weekStart(today)
        if (progressDao.countRestDays(today, today) > 0) return RestResult.ALREADY
        if (progressDao.countRestDays(start, start + 6) >= StreakCalculator.MAX_REST_DAYS_PER_WEEK) return RestResult.LIMIT
        progressDao.insertRestDay(RestDayEntity(today, time.nowMillis()))
        return RestResult.OK
    }

    // ------------------------------------------------------------------------------------------ daily content
    /** Once per day: a small positive surprise. The roll is deterministic per day, so it can never be re-rolled. */
    suspend fun rollDailyEvent(): RandomEvent? {
        if (!settings.current().randomEvents) return null
        return db.withTransaction {
            val p = playerDao.get() ?: return@withTransaction null
            val today = time.today()
            if (!p.onboarded || p.lastEventDay == today) return@withTransaction null
            val ev = RandomEvents.roll(today)
            playerDao.upsert(p.copy(lastEventDay = today, gold = p.gold + (ev?.gold ?: 0), gems = p.gems + (ev?.gems ?: 0)))
            ev
        }
    }

    /** Creates today's optional challenge quests from the offline templates (never twice, never re-created after deleting). */
    suspend fun refreshDailyContent() {
        val s = settings.current()
        val today = time.today()
        db.withTransaction {
            questDao.archiveExpiredSpecials(today)
            if (s.dailyChallenges) {
                addSpecial("challenge-$today", Catalog.dailyChallengeFor(today), s, QuestOrigin.CHALLENGE, today)
                if (Schedule.isWeekend(today, s.language)) {
                    addSpecial("weekend-$today", Catalog.weekendChallengeFor(today), s, QuestOrigin.CHALLENGE, today)
                }
            }
            if (s.randomEvents && today % 5L == 0L) {
                val id = "bonus-$today"
                if (questDao.get(id) == null) {
                    val d = Difficulty.EASY
                    questDao.upsert(
                        QuestEntity(
                            id = id, title = if (s.isFa) Catalog.bonusMissionFa else Catalog.bonusMissionEn, categoryId = Catalog.MINDFUL,
                            kind = QuestKind.ONE_TIME.name, difficulty = d.name, xpReward = s.rules.baseXp(d),
                            coinReward = s.rules.defaultCoins(d), deadlineDay = today, origin = QuestOrigin.EVENT.name, createdAt = time.nowMillis(),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun addSpecial(id: String, t: ChallengeTemplate, s: AppSettings, origin: QuestOrigin, today: Long) {
        if (questDao.get(id) != null) return
        questDao.upsert(
            QuestEntity(
                id = id, title = if (s.isFa) t.fa else t.en, categoryId = t.categoryId, kind = QuestKind.ONE_TIME.name,
                difficulty = t.difficulty.name, xpReward = s.rules.baseXp(t.difficulty), coinReward = s.rules.defaultCoins(t.difficulty),
                deadlineDay = today, origin = origin.name, createdAt = time.nowMillis(),
            ),
        )
    }
}
