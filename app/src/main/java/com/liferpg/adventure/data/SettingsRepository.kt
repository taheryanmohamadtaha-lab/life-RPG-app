package com.liferpg.adventure.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.liferpg.adventure.domain.Difficulty
import com.liferpg.adventure.domain.XpRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "life_rpg_settings")

data class AppSettings(
    val language: String = "fa",
    /** "dark" (default) or "light". */
    val theme: String = "dark",
    val sfx: Boolean = true,
    val music: Boolean = false,
    val notifMaster: Boolean = false,
    val notifDaily: Boolean = true,
    val notifHabit: Boolean = true,
    val notifDeadline: Boolean = true,
    val notifWeekly: Boolean = true,
    val notifStreak: Boolean = true,
    val dailyMinute: Int = 9 * 60,
    val streakMinute: Int = 20 * 60,
    val quietEnabled: Boolean = true,
    val quietStart: Int = 22 * 60,
    val quietEnd: Int = 8 * 60,
    val randomEvents: Boolean = true,
    val dailyChallenges: Boolean = true,
    val rules: XpRules = XpRules(),
) {
    val isFa: Boolean get() = language == "fa"
}

class SettingsRepository(private val context: Context) {
    private object K {
        val lang = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val sfx = booleanPreferencesKey("sfx")
        val music = booleanPreferencesKey("music")
        val nMaster = booleanPreferencesKey("notif_master")
        val nDaily = booleanPreferencesKey("notif_daily")
        val nHabit = booleanPreferencesKey("notif_habit")
        val nDeadline = booleanPreferencesKey("notif_deadline")
        val nWeekly = booleanPreferencesKey("notif_weekly")
        val nStreak = booleanPreferencesKey("notif_streak")
        val dailyMin = intPreferencesKey("daily_minute")
        val streakMin = intPreferencesKey("streak_minute")
        val quiet = booleanPreferencesKey("quiet_enabled")
        val quietStart = intPreferencesKey("quiet_start")
        val quietEnd = intPreferencesKey("quiet_end")
        val events = booleanPreferencesKey("random_events")
        val challenges = booleanPreferencesKey("daily_challenges")
        val xpCap = intPreferencesKey("xp_daily_cap")
        val coinCap = intPreferencesKey("coin_daily_cap")
        val streakBonus = booleanPreferencesKey("streak_bonus")
        fun base(d: Difficulty) = intPreferencesKey("xp_base_${d.name.lowercase()}")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { read(it) }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val next = transform(read(prefs))
            write(prefs, next)
            // Mirror language synchronously so the activity can pick the right locale before DataStore is ready.
            context.getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE).edit().putString("language", next.language).apply()
        }
    }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        val rules = XpRules(
            baseXp = Difficulty.entries.associateWith { p[K.base(it)] ?: it.defaultXp },
            dailyXpCap = p[K.xpCap] ?: XpRules.DEFAULT_DAILY_XP_CAP,
            dailyCoinCap = p[K.coinCap] ?: XpRules.DEFAULT_DAILY_COIN_CAP,
            streakBonusEnabled = p[K.streakBonus] ?: true,
        ).sanitized()
        return AppSettings(
            language = (p[K.lang] ?: d.language).let { if (it == "en") "en" else "fa" },
            theme = (p[K.theme] ?: d.theme).let { if (it == "light") "light" else "dark" },
            sfx = p[K.sfx] ?: d.sfx,
            music = p[K.music] ?: d.music,
            notifMaster = p[K.nMaster] ?: d.notifMaster,
            notifDaily = p[K.nDaily] ?: d.notifDaily,
            notifHabit = p[K.nHabit] ?: d.notifHabit,
            notifDeadline = p[K.nDeadline] ?: d.notifDeadline,
            notifWeekly = p[K.nWeekly] ?: d.notifWeekly,
            notifStreak = p[K.nStreak] ?: d.notifStreak,
            dailyMinute = (p[K.dailyMin] ?: d.dailyMinute).coerceIn(0, 1439),
            streakMinute = (p[K.streakMin] ?: d.streakMinute).coerceIn(0, 1439),
            quietEnabled = p[K.quiet] ?: d.quietEnabled,
            quietStart = (p[K.quietStart] ?: d.quietStart).coerceIn(0, 1439),
            quietEnd = (p[K.quietEnd] ?: d.quietEnd).coerceIn(0, 1439),
            randomEvents = p[K.events] ?: d.randomEvents,
            dailyChallenges = p[K.challenges] ?: d.dailyChallenges,
            rules = rules,
        )
    }

    private fun write(p: androidx.datastore.preferences.core.MutablePreferences, s: AppSettings) {
        p[K.lang] = s.language
        p[K.theme] = s.theme
        p[K.sfx] = s.sfx
        p[K.music] = s.music
        p[K.nMaster] = s.notifMaster
        p[K.nDaily] = s.notifDaily
        p[K.nHabit] = s.notifHabit
        p[K.nDeadline] = s.notifDeadline
        p[K.nWeekly] = s.notifWeekly
        p[K.nStreak] = s.notifStreak
        p[K.dailyMin] = s.dailyMinute.coerceIn(0, 1439)
        p[K.streakMin] = s.streakMinute.coerceIn(0, 1439)
        p[K.quiet] = s.quietEnabled
        p[K.quietStart] = s.quietStart.coerceIn(0, 1439)
        p[K.quietEnd] = s.quietEnd.coerceIn(0, 1439)
        p[K.events] = s.randomEvents
        p[K.challenges] = s.dailyChallenges
        val r = s.rules.sanitized()
        for (d in Difficulty.entries) p[K.base(d)] = r.baseXp(d)
        p[K.xpCap] = r.dailyXpCap
        p[K.coinCap] = r.dailyCoinCap
        p[K.streakBonus] = r.streakBonusEnabled
    }

    companion object {
        const val BOOT_PREFS = "life_rpg_boot"

        /** Synchronous language lookup used by MainActivity.attachBaseContext. */
        fun bootLanguage(context: Context): String =
            context.getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE).getString("language", "fa").let { if (it == "en") "en" else "fa" }
    }
}
