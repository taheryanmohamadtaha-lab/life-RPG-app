package com.liferpg.adventure.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liferpg.adventure.data.CompleteResult
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.ui.art.DragonView
import com.liferpg.adventure.ui.components.ConfettiBurst
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel
import com.liferpg.adventure.ui.vm.Overlay
import kotlinx.coroutines.delay

@Composable
fun OverlayHost(vm: GameViewModel, ui: GameUi?) {
    val list by vm.overlays.collectAsStateWithLifecycle()
    val cur = list.firstOrNull() ?: return
    val c = LocalRpg.current
    val scale = remember(cur) { Animatable(0.6f) }
    LaunchedEffect(cur) { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
    if (cur is Overlay.QuestDone) LaunchedEffect(cur) { delay(2800); vm.dismissOverlay() }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.dismissOverlay() },
        contentAlignment = Alignment.Center,
    ) {
        ConfettiBurst(colors = listOf(c.gold, c.purple, c.emerald, c.magic, Color(0xFFFF6B9D)), count = if (cur is Overlay.LevelUp) 140 else 80)
        RpgCard(Modifier.padding(24.dp).fillMaxWidth().scale(scale.value), accent = c.gold, padding = 20.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (cur) {
                    is Overlay.QuestDone -> QuestDoneBody(cur.r)
                    is Overlay.LevelUp -> LevelUpBody(cur.r, ui)
                    is Overlay.Achievement -> {
                        val lang = ui?.lang ?: "fa"
                        Text(cur.a.emoji, fontSize = 64.sp)
                        Text(tr("دستاورد جدید!", "Achievement unlocked!"), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(cur.a.title(lang), color = c.text, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (cur.a.gold > 0) RewardChip("🪙", "+" + num(cur.a.gold), c.gold)
                            if (cur.a.gems > 0) RewardChip("💎", "+" + num(cur.a.gems), c.magic)
                        }
                        GlowButton(tr("عالیه!", "Awesome!"), { vm.dismissOverlay() }, color = c.gold)
                    }
                    is Overlay.Daily -> {
                        Text("🎁", fontSize = 60.sp)
                        Text(tr("پاداش روز ${num(cur.claim.cycleDay)}", "Day ${cur.claim.cycleDay} reward"), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RewardChip("🪙", "+" + num(cur.claim.gold), c.gold)
                            if (cur.claim.gems > 0) RewardChip("💎", "+" + num(cur.claim.gems), c.magic)
                        }
                        Text(tr("فردا دوباره بیا؛ اگر یک روز هم نشد، بدون نگرانی از اول شروع می‌کنیم.", "Come back tomorrow; if you miss a day, we simply start the cycle again, no worries."), color = c.textDim, fontSize = 12.sp, textAlign = TextAlign.Center)
                        GlowButton(tr("دریافت شد", "Got it"), { vm.dismissOverlay() })
                    }
                    is Overlay.Event -> {
                        val fa = (ui?.lang ?: "fa") == "fa"
                        Text("🍀", fontSize = 60.sp)
                        Text(tr("رویداد شگفت‌انگیز!", "Surprise event!"), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(if (fa) cur.ev.titleFa else cur.ev.titleEn, color = c.text, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (cur.ev.gold > 0) RewardChip("🪙", "+" + num(cur.ev.gold), c.gold)
                            if (cur.ev.gems > 0) RewardChip("💎", "+" + num(cur.ev.gems), c.magic)
                        }
                        GlowButton(tr("چه خوب!", "Nice!"), { vm.dismissOverlay() })
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestDoneBody(r: CompleteResult.Success) {
    val c = LocalRpg.current
    Text("🎉", fontSize = 52.sp)
    Text(tr("ماموریت کامل شد!", "Quest complete!"), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    Text(r.questTitle, color = c.text, fontSize = 15.sp, textAlign = TextAlign.Center)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RewardChip("✨", "+${num(r.xp)} XP", c.purple)
        if (r.coins > 0) RewardChip("🪙", "+" + num(r.coins), c.gold)
        if (r.gems > 0) RewardChip("💎", "+" + num(r.gems), c.magic)
    }
    if (r.bonusPercent > 0) Text(tr("پاداش ویژه: +${num(r.bonusPercent)}٪ (استریک و مهارت‌ها)", "Bonus: +${r.bonusPercent}% (streak & skills)"), color = c.emerald, fontSize = 12.sp)
    if (r.statGains.isNotEmpty()) {
        Text(
            r.statGains.entries.joinToString("  ") { (s, g) -> "${s.emoji} +$g" },
            color = c.text, fontSize = 13.sp,
        )
    }
    Text("🔥 " + tr("استریک: ${num(r.streak)} روز", "Streak: ${r.streak} days"), color = c.text, fontSize = 13.sp)
    if (r.usedShields > 0) Text("🛡️ " + tr("یک سپر استریک تو را نجات داد؛ ادامه بده!", "A streak shield saved your streak. Keep going!"), color = c.magic, fontSize = 12.sp, textAlign = TextAlign.Center)
    if (r.restarted) Text("🌱 " + tr("شروعی تازه! هر روز فرصتی دوباره است.", "A fresh start! Every day is a new chance."), color = c.emerald, fontSize = 12.sp, textAlign = TextAlign.Center)
    if (r.earnedShield) Text("🛡️ " + tr("یک سپر استریک جدید گرفتی!", "You earned a streak shield!"), color = c.magic, fontSize = 12.sp)
    if (r.capped) Text(tr("به سقف روزانهٔ XP نزدیک شدی؛ مراقب خودت باش 🌙", "You hit the daily XP limit. Take care of yourself 🌙"), color = c.textDim, fontSize = 11.sp, textAlign = TextAlign.Center)
}

@Composable
private fun LevelUpBody(r: CompleteResult.Success, ui: GameUi?) {
    val c = LocalRpg.current
    val lang = ui?.lang ?: "fa"
    Text("⬆️", fontSize = 52.sp)
    Text(tr("سطح ${num(r.newLevel)}!", "Level ${r.newLevel}!"), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 28.sp)
    Text(Catalog.heroTitle(r.newLevel, lang), color = c.text, fontSize = 15.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (r.levelGold > 0) RewardChip("🪙", "+" + num(r.levelGold), c.gold)
        if (r.levelGems > 0) RewardChip("💎", "+" + num(r.levelGems), c.magic)
        if (r.skillPoints > 0) RewardChip("🌟", "+${num(r.skillPoints)} " + tr("امتیاز مهارت", "skill pt"), c.emerald)
    }
    if (r.petEvolved && ui != null) {
        Text(tr("اژدهای همراهت تکامل یافت! 🐉", "Your dragon companion evolved! 🐉"), color = c.emerald, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
        DragonView(Catalog.pet(ui.player.petType), Catalog.petStage(r.newLevel), Modifier.size(150.dp))
    }
    if (r.heroEvolved) Text(tr("ظاهر قهرمانت تغییر کرد ✨", "Your hero's look has evolved ✨"), color = c.purple, fontSize = 13.sp)
    Spacer(Modifier.height(2.dp))
    Text(tr("برای ادامه لمس کن", "Tap to continue"), color = c.textDim, fontSize = 11.sp)
}

/** Short message pill (errors, confirmations, gentle limits). */
@Composable
fun ToastHost(vm: GameViewModel) {
    val c = LocalRpg.current
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.messages.collect { msg = it } }
    LaunchedEffect(msg) { if (msg != null) { delay(3200); msg = null } }
    val m = msg ?: return
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier.padding(horizontal = 18.dp)
                .background(c.panelHi, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) { Text(m, color = c.text, fontSize = 13.sp, textAlign = TextAlign.Center) }
    }
}
