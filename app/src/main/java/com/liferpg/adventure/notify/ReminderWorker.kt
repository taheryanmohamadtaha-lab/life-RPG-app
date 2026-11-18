package com.liferpg.adventure.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.liferpg.adventure.LifeRpgApp
import com.liferpg.adventure.domain.Digits
import com.liferpg.adventure.domain.PeriodKeys
import com.liferpg.adventure.domain.StreakCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Runs every ~30 minutes, asks [ReminderPlanner] what is due and sends at most a few gentle notifications. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? LifeRpgApp ?: return Result.success()
        val c = app.container
        val s = c.settings.current()
        if (!s.notifMaster || !Notifier.hasPermission(applicationContext)) return Result.success()

        val today = c.time.today()
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        val player = c.repo.getPlayer() ?: return Result.success()
        if (!player.onboarded) return Result.success()
        val quests = c.db.questDao().active()
        val done = c.db.completionDao().observeByKeys(PeriodKeys.currentKeys(today)).first().map { it.questId }.toSet()
        val restDays = c.db.progressDao().getRestDays().map { it.epochDay }.toSet()
        val streak = StreakCalculator.effective(player.lastActiveDay, today, player.currentStreak, player.streakShields, restDays)

        val due = ReminderPlanner.plan(s, now, today, quests, done, streak, player.lastActiveDay == today)
        if (due.isEmpty()) return Result.success()

        val prefs = applicationContext.getSharedPreferences("life_rpg_reminders", Context.MODE_PRIVATE)
        val sentToday = prefs.getStringSet("sent_$today", emptySet()) ?: emptySet()
        val sent = HashSet(sentToday)
        var budget = ReminderPlanner.MAX_PER_DAY - sent.size
        for (d in due) {
            if (budget <= 0) break
            if (d.key in sent) continue
            val (title, text) = texts(d, s.isFa)
            Notifier.show(applicationContext, d.key.hashCode(), title, text)
            sent += d.key
            budget--
        }
        prefs.edit().putStringSet("sent_$today", sent).remove("sent_${today - 3}").apply()
        return Result.success()
    }

    private fun texts(d: DueReminder, fa: Boolean): Pair<String, String> {
        val lang = if (fa) "fa" else "en"
        val n = Digits.localize(d.count, lang)
        return when (d.kind) {
            ReminderKind.DAILY -> if (fa) "ماموریت‌های امروز ⚔️" to "$n ماموریت روزانه منتظر توست. هر قدم کوچک حساب می‌شود." else "Today's quests ⚔️" to "$n daily quest(s) await. Every small step counts."
            ReminderKind.HABIT -> if (fa) "زمان عادت شماست 🌱" to d.title else "Habit time 🌱" to d.title
            ReminderKind.DEADLINE -> if (fa) "مهلت نزدیک است ⏳" to "«${d.title}» و $n ماموریت دیگر به زودی مهلتشان می‌رسد؛ بدون عجله، اما فراموش نکن." else "A deadline is near ⏳" to "“${d.title}” ($n quest(s)) is due soon. No rush, just a gentle nudge."
            ReminderKind.STREAK -> if (fa) "استریک $n روزه‌ات منتظر توست 🔥" to "یک ماموریت کوچک کافی است تا ادامه پیدا کند." else "Your $n-day streak is waiting 🔥" to "One small quest keeps it going."
            ReminderKind.WEEKLY -> if (fa) "خلاصهٔ هفته 📜" to "ببین این هفته چه کرده‌ای؛ آمار و پیشرفتت در بخش آمار است." else "Weekly summary 📜" to "See how your week went in the Statistics screen."
        }
    }
}

object ReminderScheduler {
    private const val NAME = "life_rpg_reminders"

    fun sync(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (enabled) {
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(30, TimeUnit.MINUTES).build()
            wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(NAME)
        }
    }
}
