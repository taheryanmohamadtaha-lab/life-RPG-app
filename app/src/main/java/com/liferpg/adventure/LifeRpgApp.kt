package com.liferpg.adventure

import android.app.Application
import android.content.Context
import com.liferpg.adventure.audio.SoundManager
import com.liferpg.adventure.data.GameActions
import com.liferpg.adventure.data.GameRepository
import com.liferpg.adventure.data.LifeDatabase
import com.liferpg.adventure.data.SettingsRepository
import com.liferpg.adventure.domain.SystemTimeProvider
import com.liferpg.adventure.domain.TimeProvider
import com.liferpg.adventure.notify.Notifier
import com.liferpg.adventure.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Tiny hand-written service locator: everything lives on-device, nothing talks to a server. */
class AppContainer(val context: Context) {
    val db: LifeDatabase = LifeDatabase.build(context)
    val settings = SettingsRepository(context)
    val time: TimeProvider = SystemTimeProvider
    val repo = GameRepository(db, settings, time)
    val actions = GameActions(db, settings, time)
    val sound = SoundManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun start() {
        scope.launch { repo.ensureSeeded() }
        scope.launch {
            settings.settings.collect { s ->
                sound.sfxEnabled = s.sfx
                sound.setMusicEnabled(s.music)
                ReminderScheduler.sync(context, s.notifMaster)
            }
        }
    }
}

class LifeRpgApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
        container = AppContainer(this)
        container.start()
    }
}
