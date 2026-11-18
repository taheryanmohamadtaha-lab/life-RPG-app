package com.liferpg.adventure.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liferpg.adventure.AppContainer
import com.liferpg.adventure.audio.Cue
import com.liferpg.adventure.backup.BackupCodec
import com.liferpg.adventure.backup.BackupParse
import com.liferpg.adventure.data.AchievementUnlockEntity
import com.liferpg.adventure.data.AppSettings
import com.liferpg.adventure.data.BonusCalculator
import com.liferpg.adventure.data.Bonuses
import com.liferpg.adventure.data.BuyResult
import com.liferpg.adventure.data.CategoryEntity
import com.liferpg.adventure.data.CompleteResult
import com.liferpg.adventure.data.CompletionEntity
import com.liferpg.adventure.data.CustomAchievementEntity
import com.liferpg.adventure.data.DailyClaim
import com.liferpg.adventure.data.LevelHistoryEntity
import com.liferpg.adventure.data.PlayerEntity
import com.liferpg.adventure.data.PurchaseEntity
import com.liferpg.adventure.data.QuestEntity
import com.liferpg.adventure.data.RedeemResult
import com.liferpg.adventure.data.RestResult
import com.liferpg.adventure.data.RewardEntity
import com.liferpg.adventure.data.SkillResult
import com.liferpg.adventure.data.UnlockedAchievement
import com.liferpg.adventure.data.periodKey
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.Done
import com.liferpg.adventure.domain.ItemSlot
import com.liferpg.adventure.domain.LevelCurve
import com.liferpg.adventure.domain.PeriodKeys
import com.liferpg.adventure.domain.QuestError
import com.liferpg.adventure.domain.RandomEvent
import com.liferpg.adventure.domain.Stat
import com.liferpg.adventure.domain.StreakCalculator
import com.liferpg.adventure.ui.art.HeroLook
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the screens need, derived from the local database. */
data class GameUi(
    val player: PlayerEntity,
    val settings: AppSettings,
    val today: Long,
    val quests: List<QuestEntity>,
    val categories: List<CategoryEntity>,
    val doneKeys: Set<String>,
    val ranks: Map<String, Int>,
    val owned: Set<String>,
    val restDays: Set<Long>,
    val unlocks: Map<String, Long>,
    val customAch: List<CustomAchievementEntity>,
    val completions: List<CompletionEntity>,
    val levelHistory: List<LevelHistoryEntity>,
    val rewards: List<RewardEntity>,
    val purchases: List<PurchaseEntity>,
    val categoryCounts: Map<String, Int>,
) {
    val lang: String get() = settings.language
    val bonuses: Bonuses = BonusCalculator.compute(player, owned, ranks)
    val effStats: Map<Stat, Int> = BonusCalculator.effectiveStats(player, bonuses)
    val xp: LevelCurve.Progress = LevelCurve.progress(player.totalXp)
    val streak: Int = StreakCalculator.effective(player.lastActiveDay, today, player.currentStreak, player.streakShields, restDays)
    val xpToday: Int = completions.filter { it.epochDay == today }.sumOf { it.xp }
    val doneList: List<Done> = completions.map { Done(it.epochDay, it.xp, it.categoryId, it.questId) }

    fun category(id: String): CategoryEntity? = categories.firstOrNull { it.id == id }
    fun isDone(q: QuestEntity): Boolean = "${q.id}|${q.periodKey(today)}" in doneKeys

    fun item(slot: ItemSlot): String = when (slot) {
        ItemSlot.WEAPON -> player.equippedWeapon
        ItemSlot.ARMOR -> player.equippedArmor
        ItemSlot.HELMET -> player.equippedHelmet
        ItemSlot.ACCESSORY -> player.equippedAccessory
        else -> ""
    }

    fun look(): HeroLook = HeroLook(
        avatar = Catalog.avatar(player.avatarId), tier = Catalog.heroTier(player.level),
        weapon = Catalog.item(player.equippedWeapon), armor = Catalog.item(player.equippedArmor),
        helmet = Catalog.item(player.equippedHelmet), accessory = Catalog.item(player.equippedAccessory),
    )
}

sealed class Overlay {
    data class QuestDone(val r: CompleteResult.Success) : Overlay()
    data class LevelUp(val r: CompleteResult.Success) : Overlay()
    data class Achievement(val a: UnlockedAchievement) : Overlay()
    data class Daily(val claim: DailyClaim) : Overlay()
    data class Event(val ev: RandomEvent) : Overlay()
}

private data class Core(
    val player: PlayerEntity?, val settings: AppSettings, val quests: List<QuestEntity>,
    val cats: List<CategoryEntity>, val doneNow: List<CompletionEntity>,
)

private data class Prog(
    val ranks: Map<String, Int>, val owned: Set<String>, val rest: Set<Long>,
    val unlocks: List<AchievementUnlockEntity>, val custom: List<CustomAchievementEntity>,
)

private data class Hist(
    val all: List<CompletionEntity>, val levels: List<LevelHistoryEntity>, val rewards: List<RewardEntity>,
    val purchases: List<PurchaseEntity>, val counts: Map<String, Int>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel(private val app: AppContainer) : ViewModel() {
    private val repo = app.repo
    private val actions = app.actions
    private val day = MutableStateFlow(app.time.today())

    private val core = combine(
        repo.player, app.settings.settings, repo.quests, repo.categories,
        day.flatMapLatest { d -> repo.completionsForKeys(PeriodKeys.currentKeys(d)) },
    ) { p, s, q, c, done -> Core(p, s, q, c, done) }

    private val prog = combine(repo.skillRanks, repo.ownedItems, repo.restDays, repo.unlocks, repo.customAchievements) { a, b, c, d, e ->
        Prog(a, b, c, d, e)
    }

    private val hist = combine(
        repo.allCompletions, repo.levelHistory, repo.rewards, repo.purchases,
        repo.categoryCounts.map { l -> l.associate { it.categoryId to it.n } },
    ) { a, b, c, d, e -> Hist(a, b, c, d, e) }

    val ui: StateFlow<GameUi?> = combine(core, prog, hist, day) { c, p, h, d ->
        val player = c.player ?: return@combine null
        GameUi(
            player = player, settings = c.settings, today = d, quests = c.quests, categories = c.cats,
            doneKeys = c.doneNow.map { "${it.questId}|${it.periodKey}" }.toSet(),
            ranks = p.ranks, owned = p.owned, restDays = p.rest, unlocks = p.unlocks.associate { it.id to it.unlockedAt },
            customAch = p.custom, completions = h.all, levelHistory = h.levels, rewards = h.rewards,
            purchases = h.purchases, categoryCounts = h.counts,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val overlays = MutableStateFlow<List<Overlay>>(emptyList())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    private fun lang(): String = ui.value?.lang ?: "fa"
    private fun t(fa: String, en: String) = if (lang() == "fa") fa else en
    private fun say(fa: String, en: String) { _messages.tryEmit(t(fa, en)) }
    private fun push(o: Overlay) { overlays.value = overlays.value + o }

    fun dismissOverlay() { overlays.value = overlays.value.drop(1) }
    fun click() = app.sound.play(Cue.CLICK)

    /** Called whenever the app comes to the foreground (also handles passing midnight). */
    fun onResumed() {
        day.value = app.time.today()
        viewModelScope.launch {
            val p = repo.getPlayer() ?: return@launch
            if (!p.onboarded) return@launch
            actions.refreshDailyContent()
            actions.rollDailyEvent()?.let { ev ->
                app.sound.play(Cue.COIN)
                push(Overlay.Event(ev))
            }
        }
    }

    // ------------------------------------------------------------------------------------------ profile
    fun saveProfile(name: String, avatarId: Int, petType: Int, petName: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.saveProfile(name, avatarId, petType, petName)
            onDone()
            onResumed()
        }
    }

    // ------------------------------------------------------------------------------------------ quests
    fun completeQuest(id: String) {
        viewModelScope.launch {
            when (val r = actions.completeQuest(id)) {
                is CompleteResult.Success -> {
                    app.sound.play(Cue.COMPLETE)
                    push(Overlay.QuestDone(r))
                    if (r.leveledUp) { app.sound.play(Cue.LEVEL_UP); push(Overlay.LevelUp(r)) }
                    for (a in r.achievements) { app.sound.play(Cue.ACHIEVEMENT); push(Overlay.Achievement(a)) }
                }
                CompleteResult.AlreadyDone -> say("این ماموریت در این دوره قبلاً کامل شده است.", "This quest is already completed for this period.")
                CompleteResult.NotAvailable -> say("این ماموریت امروز در برنامه نیست.", "This quest is not scheduled today.")
                CompleteResult.DailyCapReached -> say("به سقف پاداش امروز رسیدی؛ استراحت هم بخشی از پیشرفت است. فردا ادامه بده 🌙", "You've reached today's reward limit. Rest is part of progress; continue tomorrow 🌙")
                CompleteResult.NotFound -> say("ماموریت پیدا نشد.", "Quest not found.")
            }
        }
    }

    fun saveQuest(existing: QuestEntity?, draft: QuestEntity, onResult: (List<QuestError>) -> Unit) {
        viewModelScope.launch {
            val errors = repo.saveQuest(existing, draft, app.settings.current().rules)
            if (errors.isEmpty()) { app.sound.play(Cue.CLICK); say("ماموریت ذخیره شد ✨", "Quest saved ✨") }
            onResult(errors)
        }
    }

    fun deleteQuest(id: String) { viewModelScope.launch { repo.deleteQuest(id); say("ماموریت حذف شد.", "Quest deleted.") } }

    fun postpone(id: String, days: Int) {
        viewModelScope.launch {
            if (repo.postponeQuest(id, days)) say("ماموریت به تعویق افتاد؛ بدون عجله ⏳", "Quest postponed. No rush ⏳")
            else say("امکان تعویق بیشتر نیست.", "No more postpones available.")
        }
    }

    // ------------------------------------------------------------------------------------------ daily reward / rest
    fun claimDaily() {
        viewModelScope.launch {
            val c = actions.claimDailyReward()
            if (c != null) { app.sound.play(Cue.COIN); push(Overlay.Daily(c)) } else say("پاداش امروز را گرفته‌ای.", "You've already claimed today's reward.")
        }
    }

    fun declareRest() {
        viewModelScope.launch {
            when (actions.declareRestDay()) {
                RestResult.OK -> say("امروز روز استراحت است؛ استریک تو امن است 🛌", "Rest day set. Your streak is safe 🛌")
                RestResult.ALREADY -> say("امروز قبلاً روز استراحت شده است.", "Today is already a rest day.")
                RestResult.LIMIT -> say("در هر هفته حداکثر ۲ روز استراحت ثبت می‌شود.", "Up to 2 rest days per week.")
            }
        }
    }

    // ------------------------------------------------------------------------------------------ skills & items
    fun upgradeSkill(id: String) {
        viewModelScope.launch {
            when (val r = actions.upgradeSkill(id)) {
                is SkillResult.Ok -> { app.sound.play(Cue.ACHIEVEMENT); say("مهارت ارتقا یافت ✨", "Skill upgraded ✨") }
                is SkillResult.Failed -> say("شرایط ارتقا فراهم نیست (امتیاز یا پیش‌نیاز).", "Requirements not met (points or prerequisite).")
                SkillResult.NotFound -> Unit
            }
        }
    }

    fun buy(itemId: String) {
        viewModelScope.launch {
            when (actions.buyItem(itemId)) {
                BuyResult.OK -> { app.sound.play(Cue.COIN); say("خرید انجام شد!", "Purchased!") }
                BuyResult.NOT_ENOUGH -> say("سکه یا جواهر کافی نیست.", "Not enough gold or gems.")
                BuyResult.OWNED -> say("این آیتم را داری.", "You already own this.")
                BuyResult.SHIELD_MAX -> say("حداکثر ۳ سپر می‌توانی داشته باشی.", "You can hold at most 3 shields.")
                BuyResult.NOT_FOUND -> Unit
            }
        }
    }

    fun equip(itemId: String) { viewModelScope.launch { if (actions.equip(itemId)) app.sound.play(Cue.CLICK) } }
    fun unequip(slot: ItemSlot) { viewModelScope.launch { actions.unequip(slot) } }

    fun redeem(rewardId: String) {
        viewModelScope.launch {
            when (actions.redeemReward(rewardId)) {
                RedeemResult.OK -> { app.sound.play(Cue.COIN); say("پاداش خرج شد؛ از آن لذت ببر! 🎉", "Reward redeemed. Enjoy it! 🎉") }
                RedeemResult.NOT_ENOUGH -> say("سکهٔ کافی نداری؛ با ماموریت‌ها جمع کن.", "Not enough gold yet; earn more with quests.")
                RedeemResult.DAILY_LIMIT -> say("سقف استفادهٔ امروز از این پاداش پر شده است.", "Today's limit for this reward is reached.")
                RedeemResult.INVALID -> say("مقدار پاداش معتبر نیست.", "Invalid reward value.")
                RedeemResult.REST_LIMIT -> say("در هر هفته حداکثر ۲ روز استراحت.", "Up to 2 rest days per week.")
                RedeemResult.ALREADY_REST -> say("امروز قبلاً روز استراحت است.", "Today is already a rest day.")
                RedeemResult.NOT_FOUND -> Unit
            }
        }
    }

    fun saveReward(existing: RewardEntity?, title: String, note: String, cost: Int, emoji: String, rest: Boolean, maxPerDay: Int, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(repo.saveReward(existing, title, note, cost, emoji, rest, maxPerDay)) }
    }

    fun deleteReward(id: String) { viewModelScope.launch { repo.deleteReward(id) } }

    fun addCustomAchievement(title: String, desc: String, categoryId: String, target: Int, gold: Int, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(repo.addCustomAchievement(title, desc, categoryId, target, gold)) }
    }

    fun deleteCustomAchievement(id: String) { viewModelScope.launch { repo.deleteCustomAchievement(id) } }

    // ------------------------------------------------------------------------------------------ settings
    fun updateSettings(transform: (AppSettings) -> AppSettings) { viewModelScope.launch { app.settings.update(transform) } }

    fun addCategory(fa: String, en: String, emoji: String, color: Long, primary: Stat, secondary: Stat, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(repo.saveCustomCategory(fa, en, emoji, color, primary, secondary)) }
    }

    fun deleteCategory(id: String) { viewModelScope.launch { repo.deleteCustomCategory(id) } }

    // ------------------------------------------------------------------------------------------ backup & reset
    suspend fun exportJson(): String = BackupCodec.encode(repo.exportBackup())

    fun importJson(text: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val r = BackupCodec.parse(text)) {
                is BackupParse.Ok -> { repo.importBackup(r.file); onResult(true, "") }
                is BackupParse.Invalid -> onResult(false, r.reasons.joinToString("\n"))
            }
        }
    }

    fun resetProgress(onDone: () -> Unit) { viewModelScope.launch { repo.resetProgress(); onDone() } }

    class Factory(private val app: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GameViewModel(app) as T
    }
}
