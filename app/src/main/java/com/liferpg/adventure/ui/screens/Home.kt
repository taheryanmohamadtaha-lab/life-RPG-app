package com.liferpg.adventure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.data.QuestEntity
import com.liferpg.adventure.data.isOnTodayBoard
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.DailyReward
import com.liferpg.adventure.ui.art.DragonView
import com.liferpg.adventure.ui.art.HeroPortrait
import com.liferpg.adventure.ui.art.HeroView
import com.liferpg.adventure.ui.components.CurrencyRow
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.SectionTitle
import com.liferpg.adventure.ui.components.XpBar
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel

@Composable
fun HomeScreen(
    ui: GameUi,
    vm: GameViewModel,
    onAddQuest: () -> Unit,
    onEditQuest: (QuestEntity) -> Unit,
    onOpenQuests: () -> Unit,
    onOpenHero: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenShop: () -> Unit,
) {
    val c = LocalRpg.current
    val lang = ui.lang
    val p = ui.player
    val board = ui.quests.filter { it.isOnTodayBoard(ui.today) }.sortedBy { if (ui.isDone(it)) 1 else 0 }
    val shown = board.take(6)
    val doneCount = board.count { ui.isDone(it) }
    val canClaim = DailyReward.canClaim(p.dailyClaimDay, ui.today)
    val nextCycle = DailyReward.nextCycleDay(p.dailyClaimDay, ui.today, p.dailyClaimCycle)
    val cap = ui.settings.rules.dailyXpCap

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- header: portrait, name, level, XP, currencies
            RpgCard(accent = c.purple) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeroPortrait(ui.look(), Modifier.size(64.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(p.name.ifBlank { tr("قهرمان", "Hero") }, color = c.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            tr("سطح ${num(p.level)} · ", "Level ${p.level} · ") + Catalog.heroTitle(p.level, lang),
                            color = c.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    CurrencyRow(p.gold, p.gems)
                }
                Spacer(Modifier.height(10.dp))
                XpBar(ui.xp.fraction, Modifier.fillMaxWidth(), height = 12.dp)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        if (ui.xp.maxed) tr("حداکثر سطح!", "Max level!") else "XP ${num(ui.xp.currentXp)} / ${num(ui.xp.xpForNext)}",
                        color = c.textDim, fontSize = 12.sp, modifier = Modifier.weight(1f),
                    )
                    Text(tr("کل: ", "Total: ") + num(p.totalXp) + " XP", color = c.textDim, fontSize = 12.sp)
                }
            }

            // ---- hero + dragon showcase
            RpgCard(Modifier.fillMaxWidth(), accent = c.gold, onClick = onOpenHero, padding = 8.dp) {
                Row(Modifier.fillMaxWidth().height(230.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeroView(ui.look(), Modifier.weight(1.1f).fillMaxSize())
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        DragonView(Catalog.pet(p.petType), Catalog.petStage(p.level), Modifier.size(130.dp))
                        Text(p.petName.ifBlank { Catalog.pet(p.petType).let { if (lang == "fa") it.fa else it.en } }, color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(tr("مرحلهٔ ${num(Catalog.petStage(p.level))} از ۳", "Stage ${Catalog.petStage(p.level)} of 3"), color = c.textDim, fontSize = 11.sp)
                    }
                }
            }

            // ---- streak & rest
            RpgCard(accent = c.gold) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("🔥 " + tr("استریک: ${num(ui.streak)} روز", "Streak: ${ui.streak} days"), color = c.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (ui.streak == 0) tr("امروز یک شروع تازه است 🌱", "Today is a fresh start 🌱") else tr("بهترین: ${num(p.bestStreak)} · سپر: ${num(p.streakShields)} 🛡️", "Best: ${p.bestStreak} · Shields: ${p.streakShields} 🛡️"),
                            color = c.textDim, fontSize = 12.sp,
                        )
                    }
                    OutlineButton(tr("روز استراحت 🛌", "Rest day 🛌"), { vm.declareRest() })
                }
                Spacer(Modifier.height(8.dp))
                OutlineButton(tr("تقویم عادت‌ها", "Habit calendar"), onOpenCalendar, Modifier.fillMaxWidth())
            }

            // ---- daily reward
            RpgCard(accent = c.emerald) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎁 " + tr("پاداش روزانه", "Daily reward"), color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (canClaim) GlowButton(tr("دریافت", "Claim"), { vm.claimDaily() }, color = c.emerald)
                    else Text("✓ " + tr("دریافت شد", "Claimed"), color = c.emerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (d in 1..7) {
                        val r = DailyReward.reward(d)
                        val current = canClaim && d == nextCycle
                        val past = if (canClaim) d < nextCycle else d <= p.dailyClaimCycle
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (current) c.emerald.copy(alpha = 0.25f) else c.track)
                                .border(1.dp, if (current) c.emerald else c.border, RoundedCornerShape(10.dp)).padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(if (past) "✓" else if (d == 7) "💎" else "🪙", fontSize = 14.sp)
                            Text(num(r.gold), color = c.gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // ---- today's quests
            SectionTitle(
                tr("ماموریت‌های امروز", "Today's quests") + "  (${num(doneCount)}/${num(board.size)})",
                trailing = { OutlineButton(tr("همه", "All"), onOpenQuests) },
            )
            Text(
                tr("امروز ${num(ui.xpToday)} از ${num(cap)} XP · سقف روزانه برای سلامت تعادل شماست", "Today ${ui.xpToday} of $cap XP · a daily cap keeps things balanced"),
                color = c.textDim, fontSize = 11.sp,
            )
            if (shown.isEmpty()) {
                RpgCard { Text(tr("هنوز ماموریتی برای امروز نداری. با دکمهٔ + اولین ماموریتت را بساز.", "No quests for today yet. Tap + to create your first one."), color = c.textDim, fontSize = 13.sp) }
            }
            for (q in shown) {
                QuestCard(ui, q, { vm.completeQuest(q.id) }, { onEditQuest(q) }, { vm.postpone(q.id, it) }, { vm.deleteQuest(q.id) })
            }
            OutlineButton(tr("فروشگاه پاداش 🛍️", "Reward shop 🛍️"), onOpenShop, Modifier.fillMaxWidth())
            Spacer(Modifier.height(90.dp))
        }

        // ---- prominent add-quest button
        Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Row(
                Modifier.clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(c.purple, c.magic)))
                    .clickable(onClick = onAddQuest).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("＋", color = androidx.compose.ui.graphics.Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(tr("ماموریت جدید", "New quest"), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
