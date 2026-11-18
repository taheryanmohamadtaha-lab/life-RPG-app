package com.liferpg.adventure.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.data.RewardEntity
import com.liferpg.adventure.domain.RewardEconomy
import com.liferpg.adventure.ui.components.ConfirmDialog
import com.liferpg.adventure.ui.components.CurrencyRow
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.RpgDialog
import com.liferpg.adventure.ui.components.RpgTextField
import com.liferpg.adventure.ui.components.ScreenScaffold
import com.liferpg.adventure.ui.components.SectionTitle
import com.liferpg.adventure.ui.components.Stepper
import com.liferpg.adventure.ui.components.SwitchRow
import com.liferpg.adventure.ui.components.Tag
import com.liferpg.adventure.ui.components.fmtDay
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel

@Composable
fun ShopScreen(ui: GameUi, vm: GameViewModel, onBack: () -> Unit, onGear: () -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    var editing by remember { mutableStateOf<RewardEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RewardEntity?>(null) }

    ScreenScaffold(tr("فروشگاه پاداش", "Reward shop"), onBack) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("موجودی شما", "Your balance"), color = c.textDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
            CurrencyRow(ui.player.gold, ui.player.gems)
        }
        Text(
            tr("پاداش‌های واقعی خودت را تعریف کن و با سکه‌هایی که کسب کرده‌ای خرجشان کن. پاداش‌ها باید سالم و در حد بودجهٔ خودت باشند؛ برنامه هیچ‌وقت به پرخرجی یا کم‌خوابی تشویق نمی‌کند.",
                "Define your own real-life rewards and spend the gold you've earned. Keep rewards healthy and within your own budget; the app never encourages overspending or losing sleep."),
            color = c.textDim, fontSize = 12.sp,
        )
        SectionTitle(tr("پاداش‌های من", "My rewards"), trailing = { GlowButton(tr("＋ جدید", "＋ New"), { creating = true }, color = c.emerald) })
        for (r in ui.rewards) {
            val usedToday = ui.purchases.count { it.rewardId == r.id && it.epochDay == ui.today }
            val limitHit = usedToday >= r.maxPerDay
            RpgCard(accent = c.gold) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.emoji, fontSize = 30.sp)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(r.title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (r.note.isNotBlank()) Text(r.note, color = c.textDim, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RewardChip("🪙", num(r.cost), c.gold)
                            if (r.kind == "REST") Tag("🛌 " + tr("روز استراحت", "Rest day"), c.emerald)
                            Tag(tr("امروز ${num(usedToday)}/${num(r.maxPerDay)}", "Today $usedToday/${r.maxPerDay}"), c.textDim)
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GlowButton(tr("دریافت پاداش", "Redeem"), { vm.redeem(r.id) }, Modifier.weight(1.4f), color = c.gold, enabled = ui.player.gold >= r.cost && !limitHit)
                    OutlineButton(tr("ویرایش", "Edit"), { editing = r }, Modifier.weight(1f))
                    OutlineButton(tr("حذف", "Delete"), { deleting = r }, Modifier.weight(1f), color = c.danger)
                }
            }
        }
        SectionTitle(tr("تجهیزات", "Gear"))
        OutlineButton(tr("🎒 فروشگاه تجهیزات و کوله‌پشتی", "🎒 Gear store & inventory"), onGear, Modifier.fillMaxWidth())
        if (ui.purchases.isNotEmpty()) {
            SectionTitle(tr("تاریخچهٔ پاداش‌ها", "Reward history"))
            for (p in ui.purchases.take(8)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(p.title, color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(fmtDay(p.epochDay, lang) + "  −" + num(p.cost) + " 🪙", color = c.textDim, fontSize = 12.sp)
                }
            }
        }
    }

    if (creating || editing != null) {
        val ex = editing
        RewardEditor(vm, ex) { creating = false; editing = null }
    }
    deleting?.let { r ->
        ConfirmDialog(tr("حذف پاداش؟", "Delete reward?"), "«${r.title}»", tr("حذف", "Delete"), { vm.deleteReward(r.id); deleting = null }, { deleting = null }, danger = true)
    }
}

@Composable
private fun RewardEditor(vm: GameViewModel, existing: RewardEntity?, onClose: () -> Unit) {
    val c = LocalRpg.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var cost by remember { mutableStateOf(existing?.cost ?: 100) }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🎁") }
    var rest by remember { mutableStateOf(existing?.kind == "REST") }
    var perDay by remember { mutableStateOf(existing?.maxPerDay ?: 1) }
    var error by remember { mutableStateOf(false) }
    RpgDialog(onClose, if (existing == null) tr("پاداش جدید 🎁", "New reward 🎁") else tr("ویرایش پاداش", "Edit reward")) {
        RpgTextField(title, { title = it }, tr("عنوان (مثلاً تماشای فیلم)", "Title (e.g. Watch a movie)"))
        RpgTextField(note, { note = it }, tr("یادداشت یا بودجه (اختیاری)", "Note or budget (optional)"))
        RpgTextField(emoji, { if (it.length <= 4) emoji = it }, tr("ایموجی", "Emoji"))
        Stepper(tr("هزینه (سکه)", "Cost (gold)"), num(cost), { cost = (cost - 10).coerceAtLeast(RewardEconomy.MIN_COST) }, { cost = (cost + 10).coerceAtMost(RewardEconomy.MAX_COST) })
        Stepper(tr("حداکثر در روز", "Max per day"), num(perDay), { perDay = (perDay - 1).coerceAtLeast(1) }, { perDay = (perDay + 1).coerceAtMost(RewardEconomy.MAX_PER_DAY_LIMIT) })
        SwitchRow(tr("روز استراحت (استریک حفظ می‌شود)", "Rest day (keeps your streak)"), rest, { rest = it })
        if (error) Text("⚠ " + tr("عنوان و هزینه را درست وارد کن.", "Please enter a valid title and cost."), color = c.danger, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GlowButton(tr("ذخیره", "Save"), { vm.saveReward(existing, title, note, cost, emoji, rest, perDay) { ok -> if (ok) onClose() else error = true } }, Modifier.weight(1f), color = c.emerald)
            OutlineButton(tr("انصراف", "Cancel"), onClose, Modifier.weight(1f))
        }
    }
}
