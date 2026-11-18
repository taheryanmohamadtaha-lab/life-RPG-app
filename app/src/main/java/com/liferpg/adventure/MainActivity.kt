package com.liferpg.adventure

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.liferpg.adventure.data.SettingsRepository
import com.liferpg.adventure.ui.AppRoot
import com.liferpg.adventure.ui.vm.GameViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels { GameViewModel.Factory((application as LifeRpgApp).container) }

    /** Persian is the default; the saved language is applied before anything is drawn. */
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale.forLanguageTag(SettingsRepository.bootLanguage(newBase))
        Locale.setDefault(locale)
        val cfg = Configuration(newBase.resources.configuration)
        cfg.setLocale(locale)
        cfg.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(cfg))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot(vm) }
    }

    override fun onResume() {
        super.onResume()
        (application as LifeRpgApp).container.sound.setForeground(true)
        vm.onResumed()
    }

    override fun onPause() {
        super.onPause()
        (application as LifeRpgApp).container.sound.setForeground(false)
    }
}
