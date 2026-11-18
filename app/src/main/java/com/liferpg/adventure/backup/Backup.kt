package com.liferpg.adventure.backup

import com.liferpg.adventure.data.AchievementUnlockEntity
import com.liferpg.adventure.data.CategoryEntity
import com.liferpg.adventure.data.CompletionEntity
import com.liferpg.adventure.data.CustomAchievementEntity
import com.liferpg.adventure.data.LevelHistoryEntity
import com.liferpg.adventure.data.OwnedItemEntity
import com.liferpg.adventure.data.PlayerEntity
import com.liferpg.adventure.data.PurchaseEntity
import com.liferpg.adventure.data.QuestEntity
import com.liferpg.adventure.data.RestDayEntity
import com.liferpg.adventure.data.RewardEntity
import com.liferpg.adventure.data.SkillRankEntity
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.Difficulty
import com.liferpg.adventure.domain.ItemSlot
import com.liferpg.adventure.domain.LevelCurve
import com.liferpg.adventure.domain.QuestKind
import com.liferpg.adventure.domain.QuestOrigin
import com.liferpg.adventure.domain.QuestStatus
import com.liferpg.adventure.domain.RewardEconomy
import com.liferpg.adventure.domain.SkillDef
import com.liferpg.adventure.domain.Stat
import com.liferpg.adventure.domain.StreakCalculator
import com.liferpg.adventure.domain.XpRules
import com.liferpg.adventure.domain.enumOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

const val BACKUP_APP_ID = "com.liferpg.adventure"
const val BACKUP_SCHEMA = 1
const val BACKUP_MAX_BYTES = 10 * 1024 * 1024

@Serializable
data class BackupFile(
    val app: String = BACKUP_APP_ID,
    val schema: Int = BACKUP_SCHEMA,
    val exportedAt: Long = 0,
    val player: PlayerEntity = PlayerEntity(),
    val quests: List<QuestEntity> = emptyList(),
    val completions: List<CompletionEntity> = emptyList(),
    val levelHistory: List<LevelHistoryEntity> = emptyList(),
    val achievements: List<AchievementUnlockEntity> = emptyList(),
    val customAchievements: List<CustomAchievementEntity> = emptyList(),
    val skills: List<SkillRankEntity> = emptyList(),
    val items: List<OwnedItemEntity> = emptyList(),
    val rewards: List<RewardEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val restDays: List<RestDayEntity> = emptyList(),
)

sealed class BackupParse {
    data class Ok(val file: BackupFile) : BackupParse()
    data class Invalid(val reasons: List<String>) : BackupParse()
}

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** Decodes and validates untrusted text. Never throws. */
    fun parse(text: String): BackupParse {
        if (text.length > BACKUP_MAX_BYTES) return BackupParse.Invalid(listOf("File is too large"))
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            return BackupParse.Invalid(listOf("Not a valid Life RPG backup file"))
        } catch (e: IllegalArgumentException) {
            return BackupParse.Invalid(listOf("Not a valid Life RPG backup file"))
        }
        val errors = BackupValidator.validate(file)
        return if (errors.isEmpty()) BackupParse.Ok(BackupValidator.sanitize(file)) else BackupParse.Invalid(errors.take(8))
    }
}

/** Strict validation of imported backups so a hand-edited or corrupted file can never break (or cheat) the game. */
object BackupValidator {
    private const val MAX_STR = 600
    private const val MAX_DAY = 200_000L

    fun validate(f: BackupFile): List<String> {
        val e = ArrayList<String>()
        if (f.app != BACKUP_APP_ID) e += "Backup belongs to a different app"
        if (f.schema < 1 || f.schema > BACKUP_SCHEMA) e += "Unsupported backup schema ${f.schema}"
        if (f.quests.size > 5000) e += "Too many quests"
        if (f.completions.size > 300_000) e += "Too many completions"
        if (f.categories.size > 60 || f.rewards.size > 500 || f.purchases.size > 100_000 || f.customAchievements.size > 200) e += "Too many records"
        if (e.isNotEmpty()) return e

        val p = f.player
        if (p.totalXp < 0 || p.totalXp > LevelCurve.MAX_TOTAL_XP) e += "Player XP out of range"
        if (p.gold !in 0..1_000_000_000L || p.gems !in 0..1_000_000L) e += "Player currency out of range"
        if (p.skillPoints !in 0..1000) e += "Skill points out of range"
        for (s in Stat.entries) if (p.stat(s) !in 0..XpRules.STAT_CAP) e += "Stat ${s.name} out of range"
        if (p.currentStreak !in 0..100_000 || p.bestStreak !in 0..100_000) e += "Streak out of range"
        if (p.streakShields !in 0..StreakCalculator.MAX_SHIELDS) e += "Shield count out of range"
        if (p.name.length > 40 || p.petName.length > 40) e += "Name too long"
        if (p.avatarId !in 0 until Catalog.avatars.size || p.petType !in 0 until Catalog.pets.size) e += "Avatar or pet out of range"
        val slots = mapOf(p.equippedWeapon to ItemSlot.WEAPON, p.equippedArmor to ItemSlot.ARMOR, p.equippedHelmet to ItemSlot.HELMET, p.equippedAccessory to ItemSlot.ACCESSORY)
        for ((id, slot) in slots) {
            if (id.isNotEmpty()) {
                val def = Catalog.item(id)
                if (def == null || def.slot != slot) e += "Invalid equipped item '$id'"
            }
        }

        val questIds = HashSet<String>()
        for (q in f.quests) {
            if (!questIds.add(q.id)) e += "Duplicate quest id"
            if (q.id.isBlank() || q.id.length > MAX_STR) e += "Invalid quest id"
            if (q.title.isBlank() || q.title.length > MAX_STR) e += "Invalid quest title"
            if (q.description.length > 2000 || q.notes.length > 4000) e += "Quest text too long"
            if (enumOrNull<QuestKind>(q.kind) == null) e += "Invalid quest kind '${q.kind}'"
            if (enumOrNull<Difficulty>(q.difficulty) == null) e += "Invalid difficulty '${q.difficulty}'"
            if (enumOrNull<QuestStatus>(q.status) == null) e += "Invalid quest status '${q.status}'"
            if (enumOrNull<QuestOrigin>(q.origin) == null) e += "Invalid quest origin '${q.origin}'"
            if (q.xpReward !in 1..XpRules.MAX_BASE_XP * 2) e += "Quest XP out of range"
            if (q.coinReward !in 0..XpRules.MAX_BASE_XP) e += "Quest coins out of range"
            if (q.weekdaysMask !in 0..127) e += "Invalid weekday mask"
            if (e.size > 20) break
        }
        val seen = HashSet<String>()
        for (c in f.completions) {
            if (!seen.add(c.questId + "|" + c.periodKey)) { e += "Duplicate completion for the same period"; break }
            if (c.xp !in 0..5000 || c.coins !in 0..5000 || c.gems !in 0..20) { e += "Completion rewards out of range"; break }
            if (c.epochDay !in 0..MAX_DAY) { e += "Completion date out of range"; break }
            if (enumOrNull<Difficulty>(c.difficulty) == null) { e += "Invalid completion difficulty"; break }
        }
        for (l in f.levelHistory) if (l.level !in 1..LevelCurve.MAX_LEVEL) { e += "Invalid level history"; break }
        for (a in f.achievements) {
            if (Catalog.achievement(a.id) == null && !a.id.startsWith("custom_")) { e += "Unknown achievement '${a.id}'"; break }
        }
        for (s in f.skills) {
            if (Catalog.skill(s.skillId) == null || s.rank !in 1..SkillDef.MAX_RANK) { e += "Invalid skill '${s.skillId}'"; break }
        }
        for (i in f.items) if (Catalog.item(i.itemId) == null) { e += "Unknown item '${i.itemId}'"; break }
        for (r in f.rewards) {
            if (r.id.isBlank() || r.title.isBlank() || r.title.length > MAX_STR || r.note.length > MAX_STR) { e += "Invalid reward"; break }
            if (r.cost !in RewardEconomy.MIN_COST..RewardEconomy.MAX_COST || r.maxPerDay !in 1..RewardEconomy.MAX_PER_DAY_LIMIT) { e += "Reward values out of range"; break }
            if (r.kind != "REAL" && r.kind != "REST") { e += "Invalid reward kind"; break }
        }
        val catIds = HashSet<String>()
        for (c in f.categories) {
            if (c.id.isBlank() || c.id.length > 60 || !catIds.add(c.id)) { e += "Invalid or duplicate category id"; break }
            if (c.nameFa.isBlank() || c.nameEn.isBlank() || c.nameFa.length > 60 || c.nameEn.length > 60) { e += "Invalid category name"; break }
            if (enumOrNull<Stat>(c.primaryStat) == null || enumOrNull<Stat>(c.secondaryStat) == null) { e += "Invalid category stat"; break }
        }
        for (a in f.customAchievements) {
            if (a.id.isBlank() || a.title.isBlank() || a.title.length > MAX_STR || a.target !in 1..10_000 || a.rewardGold !in 0..1000) { e += "Invalid custom achievement"; break }
        }
        for (d in f.restDays) if (d.epochDay !in 0..MAX_DAY) { e += "Invalid rest day"; break }
        for (pu in f.purchases) if (pu.cost !in 0..RewardEconomy.MAX_COST || pu.epochDay !in 0..MAX_DAY) { e += "Invalid purchase"; break }
        return e
    }

    /** Recomputes derived values so they can never disagree with the stored XP. */
    fun sanitize(f: BackupFile): BackupFile =
        f.copy(player = f.player.copy(id = 1, level = LevelCurve.levelForXp(f.player.totalXp), onboarded = true))
}
