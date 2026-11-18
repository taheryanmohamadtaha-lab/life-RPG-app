package com.liferpg.adventure.ui

import android.app.Activity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.liferpg.adventure.data.QuestEntity
import com.liferpg.adventure.ui.components.AppBackground
import com.liferpg.adventure.ui.screens.AchievementsScreen
import com.liferpg.adventure.ui.screens.CalendarScreen
import com.liferpg.adventure.ui.screens.HeroScreen
import com.liferpg.adventure.ui.screens.HomeScreen
import com.liferpg.adventure.ui.screens.InventoryScreen
import com.liferpg.adventure.ui.screens.OnboardingScreen
import com.liferpg.adventure.ui.screens.OverlayHost
import com.liferpg.adventure.ui.screens.ProfileScreen
import com.liferpg.adventure.ui.screens.QuestEditorDialog
import com.liferpg.adventure.ui.screens.QuestsScreen
import com.liferpg.adventure.ui.screens.SettingsScreen
import com.liferpg.adventure.ui.screens.ShopScreen
import com.liferpg.adventure.ui.screens.SkillTreeScreen
import com.liferpg.adventure.ui.screens.StatsScreen
import com.liferpg.adventure.ui.screens.ToastHost
import com.liferpg.adventure.ui.screens.WorldMapScreen
import com.liferpg.adventure.ui.theme.LifeRpgTheme
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel

object Routes {
    const val HOME = "home"
    const val QUESTS = "quests"
    const val MAP = "map"
    const val HERO = "hero"
    const val PROFILE = "profile"
    const val SKILLS = "skills"
    const val INVENTORY = "inventory"
    const val ACHIEVEMENTS = "achievements"
    const val STATS = "stats"
    const val CALENDAR = "calendar"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
    val TABS = listOf(HOME, QUESTS, MAP, HERO, PROFILE)
}

private class EditorRequest(val existing: QuestEntity?, val category: String?)

@Composable
fun AppRoot(vm: GameViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val dark = (ui?.settings?.theme ?: "dark") == "dark"
    val lang = ui?.settings?.language ?: "fa"
    LifeRpgTheme(dark = dark, lang = lang) {
        val view = LocalView.current
        SideEffect {
            val w = (view.context as? Activity)?.window
            if (w != null) WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
        }
        AppBackground {
            val u = ui
            when {
                u == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("⚔️", fontSize = 64.sp) }
                !u.player.onboarded -> OnboardingScreen(vm, u.lang)
                else -> MainNav(vm, u)
            }
            OverlayHost(vm, u)
            ToastHost(vm)
        }
    }
}

@Composable
private fun MainNav(vm: GameViewModel, ui: GameUi) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.HOME
    var editor by remember { mutableStateOf<EditorRequest?>(null) }
    var regionFilter by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun go(r: String) { nav.navigate(r) { launchSingleTop = true } }
    fun tab(r: String) {
        nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val back: () -> Unit = { nav.popBackStack() }
    val isTab = route in Routes.TABS

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).then(if (isTab) Modifier else Modifier.navigationBarsPadding())) {
            NavHost(
                nav, startDestination = Routes.HOME,
                enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) }, popExitTransition = { fadeOut(tween(160)) },
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        ui, vm, onAddQuest = { editor = EditorRequest(null, null) }, onEditQuest = { editor = EditorRequest(it, null) },
                        onOpenQuests = { tab(Routes.QUESTS) }, onOpenHero = { tab(Routes.HERO) },
                        onOpenCalendar = { go(Routes.CALENDAR) }, onOpenShop = { go(Routes.SHOP) },
                    )
                }
                composable(Routes.QUESTS) {
                    Box(Modifier.fillMaxSize()) {
                        QuestsScreen(ui, vm, regionFilter, { regionFilter = emptySet() }, onEdit = { editor = EditorRequest(it, null) })
                        Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                            val c = LocalRpg.current
                            Box(
                                Modifier.background(Brush.horizontalGradient(listOf(c.purple, c.magic)), androidx.compose.foundation.shape.CircleShape)
                                    .clickable { editor = EditorRequest(null, regionFilter.firstOrNull()) }.padding(horizontal = 18.dp, vertical = 14.dp),
                            ) { Text("＋ " + tr("جدید", "New"), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                        }
                    }
                }
                composable(Routes.MAP) {
                    WorldMapScreen(
                        ui,
                        onEnter = { r -> regionFilter = r.categoryIds.toSet(); tab(Routes.QUESTS) },
                        onNewQuest = { r -> editor = EditorRequest(null, r.categoryIds.firstOrNull()) },
                    )
                }
                composable(Routes.HERO) { HeroScreen(ui, { go(Routes.INVENTORY) }, { go(Routes.SKILLS) }, { go(Routes.ACHIEVEMENTS) }, { go(Routes.STATS) }) }
                composable(Routes.PROFILE) {
                    ProfileScreen(ui, vm, { go(Routes.SKILLS) }, { go(Routes.INVENTORY) }, { go(Routes.ACHIEVEMENTS) }, { go(Routes.STATS) }, { go(Routes.CALENDAR) }, { go(Routes.SHOP) }, { go(Routes.SETTINGS) })
                }
                composable(Routes.SKILLS) { SkillTreeScreen(ui, vm, back) }
                composable(Routes.INVENTORY) { InventoryScreen(ui, vm, back) }
                composable(Routes.ACHIEVEMENTS) { AchievementsScreen(ui, vm, back) }
                composable(Routes.STATS) { StatsScreen(ui, back) }
                composable(Routes.CALENDAR) { CalendarScreen(ui, vm, back) }
                composable(Routes.SHOP) { ShopScreen(ui, vm, back, onGear = { go(Routes.INVENTORY) }) }
                composable(Routes.SETTINGS) { SettingsScreen(ui, vm, back) }
            }
        }
        if (isTab) BottomBar(route) { vm.click(); tab(it) }
    }
    editor?.let { req -> QuestEditorDialog(ui, vm, req.existing, req.category) { editor = null } }
}

@Composable
private fun BottomBar(route: String, onTab: (String) -> Unit) {
    val c = LocalRpg.current
    val items = listOf(
        Triple(Routes.HOME, "🏠", tr("خانه", "Home")),
        Triple(Routes.QUESTS, "📜", tr("ماموریت‌ها", "Quests")),
        Triple(Routes.MAP, "🗺️", tr("نقشه", "Map")),
        Triple(Routes.HERO, "⚔️", tr("قهرمان", "Hero")),
        Triple(Routes.PROFILE, "👤", tr("پروفایل", "Profile")),
    )
    Row(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(c.panelHi, c.panel))).navigationBarsPadding().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for ((r, emoji, label) in items) {
            val sel = r == route
            Column(
                Modifier.weight(1f).clickable { onTab(r) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(emoji, fontSize = if (sel) 26.sp else 22.sp, modifier = Modifier.alpha(if (sel) 1f else 0.6f))
                Text(label, color = if (sel) c.gold else c.textDim, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
