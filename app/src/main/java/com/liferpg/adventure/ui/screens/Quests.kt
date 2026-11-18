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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.data.QuestEntity
import com.liferpg.adventure.data.difficultyEnum
import com.liferpg.adventure.data.isAvailableOn
import com.liferpg.adventure.data.isOnTodayBoard
import com.liferpg.adventure.data.kindEnum
import com.liferpg.adventure.data.originEnum
import com.liferpg.adventure.data.statusEnum
import com.liferpg.adventure.domain.Difficulty
import com.liferpg.adventure.domain.Jalali
import com.liferpg.adventure.domain.QuestError
import com.liferpg.adventure.domain.QuestKind
import com.liferpg.adventure.domain.QuestOrigin
import com.liferpg.adventure.domain.QuestStatus
import com.liferpg.adventure.ui.components.ChipRow
import com.liferpg.adventure.ui.components.ConfirmDialog
import com.liferpg.adventure.ui.components.EmptyHint
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.RpgDialog
import com.liferpg.adventure.ui.components.RpgTextField
import com.liferpg.adventure.ui.components.Stepper
import com.liferpg.adventure.ui.components.SwitchRow
import com.liferpg.adventure.ui.components.Tag
import com.liferpg.adventure.ui.components.colorOf
import com.liferpg.adventure.ui.components.fmtDay
import com.liferpg.adventure.ui.components.fmtMinute
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel

fun diffColor(d: Difficulty): Color = when (d) {
    Difficulty.EASY -> Color(0xFF7BD88F)
    Difficulty.NORMAL -> Color(0xFF39A0FF)
    Difficulty.HARD -> Color(0xFFFF9A3D)
    Difficulty.EPIC -> Color(0xFFB36BFF)
    Difficulty.LEGENDARY -> Color(0xFFFFC857)
}

/** Mission card styled like the reference "Missions" screen: icon, title, reward chips and a claim button. */
@Composable
fun QuestCard(
    ui: GameUi,
    q: QuestEntity,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onPostpone: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalRpg.current
    val lang = ui.lang
    val cat = ui.category(q.categoryId)
    val col = cat?.let { colorOf(it.color) } ?: c.magic
    val diff = q.difficultyEnum()
    val done = ui.isDone(q) || q.statusEnum() == QuestStatus.DONE
    var open by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    RpgCard(modifier, accent = col, onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(col.copy(alpha = 0.18f))
                    .border(1.dp, col.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(cat?.emoji ?: "📜", fontSize = 26.sp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(q.title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(diff.label(lang), diffColor(diff))
                    Tag(q.kindEnum().label(lang), c.textDim)
                    if (q.originEnum() != QuestOrigin.USER) Tag("✨", c.gold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            RewardChip("✨", "${num(q.xpReward)} XP", c.purple)
            if (q.coinReward > 0) RewardChip("🪙", num(q.coinReward), c.gold)
            if (diff.gems > 0) RewardChip("💎", num(diff.gems), c.magic)
        }
        if (q.snoozeUntilDay > ui.today && !done) {
            Spacer(Modifier.height(6.dp))
            Tag("⏳ " + tr("تعویق تا ", "Postponed to ") + fmtDay(q.snoozeUntilDay, lang), c.textDim)
        }
        if (q.deadlineDay != null) {
            Spacer(Modifier.height(6.dp))
            val late = q.deadlineDay < ui.today && !done
            Tag("🗓 " + tr("مهلت: ", "Due: ") + fmtDay(q.deadlineDay, lang), if (late) c.gold else c.textDim)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (done) Tag("✓ " + tr("انجام شد", "Done"), c.emerald)
            else if (!q.isAvailableOn(ui.today)) Tag(tr("امروز در برنامه نیست", "Not scheduled today"), c.textDim)
            else GlowButton(tr("انجام شد ✓", "Complete ✓"), onComplete)
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            if (q.description.isNotBlank()) Text(q.description, color = c.text, fontSize = 13.sp)
            if (q.notes.isNotBlank()) Text("📝 " + q.notes, color = c.textDim, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlineButton(tr("ویرایش", "Edit"), onEdit, Modifier.weight(1f))
                if (!done) OutlineButton(tr("تعویق ۱ روز", "Postpone 1d"), { onPostpone(1) }, Modifier.weight(1f))
                OutlineButton(tr("حذف", "Delete"), { confirmDelete = true }, Modifier.weight(1f), color = c.danger)
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            tr("حذف ماموریت؟", "Delete quest?"),
            tr("«${q.title}» حذف می‌شود. تاریخچهٔ پاداش‌های گرفته‌شده حفظ می‌شود.", "“${q.title}” will be deleted. Rewards you already earned stay in your history."),
            tr("حذف", "Delete"), { confirmDelete = false; onDelete() }, { confirmDelete = false }, danger = true,
        )
    }
}

private enum class QTab { TODAY, DAILY, WEEKLY, MONTHLY, GOAL, ONE_TIME, DONE }

@Composable
fun QuestsScreen(
    ui: GameUi,
    vm: GameViewModel,
    regionFilter: Set<String>,
    onClearRegion: () -> Unit,
    onEdit: (QuestEntity?) -> Unit,
) {
    val c = LocalRpg.current
    val lang = ui.lang
    var tab by remember { mutableStateOf(QTab.TODAY) }
    var cat by remember { mutableStateOf<String?>(null) }
    val today = ui.today

    val list = ui.quests.filter { q ->
        val kindOk = when (tab) {
            QTab.TODAY -> q.isOnTodayBoard(today) && !ui.isDone(q)
            QTab.DAILY -> q.kindEnum() == QuestKind.DAILY && q.statusEnum() == QuestStatus.ACTIVE
            QTab.WEEKLY -> q.kindEnum() == QuestKind.WEEKLY && q.statusEnum() == QuestStatus.ACTIVE
            QTab.MONTHLY -> q.kindEnum() == QuestKind.MONTHLY && q.statusEnum() == QuestStatus.ACTIVE
            QTab.GOAL -> q.kindEnum() == QuestKind.GOAL && q.statusEnum() == QuestStatus.ACTIVE
            QTab.ONE_TIME -> q.kindEnum() == QuestKind.ONE_TIME && q.statusEnum() == QuestStatus.ACTIVE
            QTab.DONE -> q.statusEnum() == QuestStatus.DONE || ui.isDone(q)
        }
        val catOk = cat == null || q.categoryId == cat
        val regionOk = regionFilter.isEmpty() || q.categoryId in regionFilter
        kindOk && catOk && regionOk
    }.sortedBy { if (ui.isDone(it)) 1 else 0 }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            tr("ماموریت‌ها", "Quests"), color = c.text, fontWeight = FontWeight.Bold, fontSize = 20.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        )
        ChipRow(
            QTab.entries, tab,
            label = {
                when (it) {
                    QTab.TODAY -> tr("امروز", "Today")
                    QTab.DAILY -> tr("روزانه", "Daily")
                    QTab.WEEKLY -> tr("هفتگی", "Weekly")
                    QTab.MONTHLY -> tr("ماهانه", "Monthly")
                    QTab.GOAL -> tr("اهداف", "Goals")
                    QTab.ONE_TIME -> tr("یک‌بار", "One-time")
                    QTab.DONE -> tr("انجام‌شده", "Completed")
                }
            },
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Spacer(Modifier.height(8.dp))
        ChipRow(
            listOf<String?>(null) + ui.categories.map { it.id }, cat,
            label = { id -> if (id == null) tr("همه", "All") else ui.category(id)?.let { it.emoji + " " + it.name(lang) } ?: id },
            onSelect = { cat = it }, modifier = Modifier.padding(horizontal = 14.dp),
            accent = { id -> id?.let { ui.category(it)?.let { cc -> colorOf(cc.color) } } },
        )
        if (regionFilter.isNotEmpty()) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Tag("🗺️ " + tr("فیلتر منطقه فعال است", "Region filter on"), c.gold)
                Spacer(Modifier.width(8.dp))
                OutlineButton(tr("پاک کردن", "Clear"), onClearRegion)
            }
        }
        if (list.isEmpty()) {
            EmptyHint(
                if (tab == QTab.DONE) "🏆" else "🌟",
                if (tab == QTab.DONE) tr("هنوز ماموریت کامل‌شده‌ای نیست؛ اولین قدم همین نزدیکی است.", "Nothing completed yet; your first step is close.")
                else tr("ماموریتی اینجا نیست. با دکمهٔ + یک ماموریت تازه بساز!", "No quests here. Tap + to create a new one!"),
            )
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(list, key = { it.id }) { q ->
                QuestCard(ui, q, { vm.completeQuest(q.id) }, { onEdit(q) }, { vm.postpone(q.id, it) }, { vm.deleteQuest(q.id) })
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------- editor

private fun errorText(e: QuestError, fa: Boolean): String = when (e) {
    QuestError.TITLE_EMPTY -> if (fa) "عنوان را وارد کن." else "Please enter a title."
    QuestError.TITLE_TOO_LONG -> if (fa) "عنوان خیلی بلند است." else "Title is too long."
    QuestError.DESC_TOO_LONG -> if (fa) "توضیح خیلی بلند است." else "Description is too long."
    QuestError.NOTES_TOO_LONG -> if (fa) "یادداشت خیلی بلند است." else "Notes are too long."
    QuestError.XP_OUT_OF_RANGE -> if (fa) "مقدار XP خارج از بازهٔ مجاز این سختی است." else "XP is outside the allowed range."
    QuestError.COINS_OUT_OF_RANGE -> if (fa) "مقدار سکه خارج از بازهٔ مجاز است." else "Coins are outside the allowed range."
    QuestError.DEADLINE_IN_PAST -> if (fa) "مهلت نمی‌تواند در گذشته باشد." else "The deadline can't be in the past."
}

@Composable
fun QuestEditorDialog(ui: GameUi, vm: GameViewModel, existing: QuestEntity?, presetCategory: String?, onClose: () -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    val fa = lang == "fa"
    val rules = ui.settings.rules
    val firstCat = presetCategory ?: ui.categories.firstOrNull()?.id ?: "career"

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var category by remember { mutableStateOf(existing?.categoryId ?: firstCat) }
    var kind by remember { mutableStateOf(existing?.kindEnum() ?: QuestKind.DAILY) }
    var diff by remember { mutableStateOf(existing?.difficultyEnum() ?: Difficulty.NORMAL) }
    var xp by remember { mutableStateOf(existing?.xpReward ?: rules.baseXp(Difficulty.NORMAL)) }
    var coins by remember { mutableStateOf(existing?.coinReward ?: rules.defaultCoins(Difficulty.NORMAL)) }
    var hasDeadline by remember { mutableStateOf(existing?.deadlineDay != null) }
    var deadlineOffset by remember { mutableStateOf(existing?.deadlineDay?.let { (it - ui.today).toInt().coerceIn(0, 365) } ?: 7) }
    var mask by remember { mutableStateOf(existing?.weekdaysMask ?: 0) }
    var hasReminder by remember { mutableStateOf((existing?.reminderMinute ?: -1) >= 0) }
    var reminder by remember { mutableStateOf((existing?.reminderMinute ?: -1).let { if (it >= 0) it else 20 * 60 }) }
    var errors by remember { mutableStateOf<List<QuestError>>(emptyList()) }

    RpgDialog(onClose, if (existing == null) tr("ماموریت جدید ✨", "New quest ✨") else tr("ویرایش ماموریت", "Edit quest"), fullScreen = true) {
        RpgTextField(title, { title = it }, tr("عنوان", "Title"))
        RpgTextField(desc, { desc = it }, tr("توضیح (اختیاری)", "Description (optional)"), singleLine = false)
        Text(tr("دسته", "Category"), color = c.textDim, fontSize = 12.sp)
        ChipRow(
            ui.categories.map { it.id }, category,
            label = { id -> ui.category(id)?.let { it.emoji + " " + it.name(lang) } ?: id },
            onSelect = { category = it }, accent = { id -> ui.category(id)?.let { colorOf(it.color) } },
        )
        Text(tr("نوع", "Type"), color = c.textDim, fontSize = 12.sp)
        ChipRow(QuestKind.entries, kind, label = { it.emoji + " " + it.label(lang) }, onSelect = { kind = it })
        Text(tr("سختی", "Difficulty"), color = c.textDim, fontSize = 12.sp)
        ChipRow(
            Difficulty.entries, diff, label = { it.label(lang) },
            onSelect = { d ->
                diff = d
                xp = rules.baseXp(d); coins = rules.defaultCoins(d)
            },
            accent = { diffColor(it) },
        )
        Stepper(
            "XP", num(xp), { xp = (xp - 5).coerceAtLeast(1) }, { xp = (xp + 5).coerceAtMost(rules.maxXp(diff)) },
        )
        Stepper(
            tr("سکه", "Coins"), num(coins), { coins = (coins - 5).coerceAtLeast(0) }, { coins = (coins + 5).coerceAtMost(rules.maxCoins(diff)) },
        )
        Text(
            tr("حداکثر پاداش برای این سختی: ${num(rules.maxXp(diff))} XP و ${num(rules.maxCoins(diff))} سکه (برای جلوگیری از سوءاستفاده).",
                "Max for this difficulty: ${num(rules.maxXp(diff))} XP and ${num(rules.maxCoins(diff))} coins (keeps rewards fair)."),
            color = c.textDim, fontSize = 11.sp,
        )
        if (kind == QuestKind.DAILY) {
            Text(tr("روزهای هفته (هیچ‌کدام = هر روز)", "Weekdays (none = every day)"), color = c.textDim, fontSize = 12.sp)
            val names = if (fa) Jalali.WEEKDAYS_SHORT_FA_SAT_FIRST else listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..6) {
                    val on = (mask shr i) and 1 == 1
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (on) c.purple.copy(alpha = 0.35f) else c.track)
                            .border(1.dp, if (on) c.purple else c.border, RoundedCornerShape(10.dp))
                            .clickable { mask = mask xor (1 shl i) }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(names[i], color = if (on) c.text else c.textDim, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        SwitchRow(tr("مهلت دارد", "Has a deadline"), hasDeadline, { hasDeadline = it })
        if (hasDeadline) {
            Stepper(
                fmtDay(ui.today + deadlineOffset, lang), tr("${num(deadlineOffset)} روز دیگر", "in ${deadlineOffset}d"),
                { deadlineOffset = (deadlineOffset - 1).coerceAtLeast(0) }, { deadlineOffset = (deadlineOffset + 1).coerceAtMost(365) },
            )
        }
        SwitchRow(tr("یادآور", "Reminder"), hasReminder, { hasReminder = it }, hint = tr("برای نوتیفیکیشن، اعلان‌ها را در تنظیمات فعال کن.", "Enable notifications in Settings to receive it."))
        if (hasReminder) {
            Stepper(tr("ساعت یادآوری", "Reminder time"), fmtMinute(reminder, lang), { reminder = (reminder - 15 + 1440) % 1440 }, { reminder = (reminder + 15) % 1440 })
        }
        RpgTextField(notes, { notes = it }, tr("یادداشت (اختیاری)", "Notes (optional)"), singleLine = false)
        for (e in errors) Text("⚠ " + errorText(e, fa), color = c.danger, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GlowButton(tr("ذخیره", "Save"), {
                val draft = QuestEntity(
                    id = "", title = title, description = desc, categoryId = category, kind = kind.name, difficulty = diff.name,
                    xpReward = xp, coinReward = coins, deadlineDay = if (hasDeadline) ui.today + deadlineOffset else null,
                    weekdaysMask = mask, notes = notes, reminderMinute = if (hasReminder) reminder else -1,
                )
                vm.saveQuest(existing, draft) { errs -> if (errs.isEmpty()) onClose() else errors = errs }
            }, Modifier.weight(1f), color = c.emerald)
            OutlineButton(tr("انصراف", "Cancel"), onClose, Modifier.weight(1f))
        }
    }
}
