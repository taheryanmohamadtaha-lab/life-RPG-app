package com.liferpg.adventure.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.backup.BACKUP_MAX_BYTES
import com.liferpg.adventure.domain.Difficulty
import com.liferpg.adventure.domain.Stat
import com.liferpg.adventure.domain.XpRules
import com.liferpg.adventure.notify.Notifier
import com.liferpg.adventure.ui.components.ChipRow
import com.liferpg.adventure.ui.components.ConfirmDialog
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.RpgDialog
import com.liferpg.adventure.ui.components.RpgTextField
import com.liferpg.adventure.ui.components.ScreenScaffold
import com.liferpg.adventure.ui.components.SectionTitle
import com.liferpg.adventure.ui.components.Stepper
import com.liferpg.adventure.ui.components.SwitchRow
import com.liferpg.adventure.ui.components.fmtMinute
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Reads at most [max] bytes; returns null when the stream is larger (protects against huge files). */
private fun readLimited(ins: java.io.InputStream, max: Int): ByteArray? {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    var total = 0
    while (true) {
        val n = ins.read(buf)
        if (n < 0) break
        total += n
        if (total > max) return null
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

private val CATEGORY_COLORS = listOf(0xFFFF7A59, 0xFF39A0FF, 0xFFFFC857, 0xFF2ED8A3, 0xFFE879F9, 0xFFFF8FB1, 0xFF9D7CFF)

@Composable
fun SettingsScreen(ui: GameUi, vm: GameViewModel, onBack: () -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    val s = ui.settings
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var addCategory by remember { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.updateSettings { it.copy(notifMaster = true) }
        else info = if (lang == "fa") "اجازهٔ اعلان داده نشد. می‌توانی بعداً از تنظیمات گوشی آن را فعال کنی." else "Notification permission was not granted. You can enable it later in phone settings."
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = try {
                val json = vm.exportJson()
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } != null }
            } catch (e: Exception) { false }
            info = if (ok) (if (lang == "fa") "پشتیبان ذخیره شد ✅" else "Backup saved ✅") else (if (lang == "fa") "ذخیرهٔ پشتیبان ناموفق بود." else "Could not save the backup.")
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = try {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { ins ->
                        readLimited(ins, BACKUP_MAX_BYTES)?.let { String(it, Charsets.UTF_8) }
                    }
                }
            } catch (e: Exception) { null }
            if (text == null) info = if (lang == "fa") "فایل خوانده نشد یا خیلی بزرگ است." else "The file could not be read or is too large."
            else confirmImport = text
        }
    }

    ScreenScaffold(tr("تنظیمات", "Settings"), onBack) {
        // ---- general
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("عمومی", "General"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GlowButton("فارسی", { vm.updateSettings { it.copy(language = "fa") } }, Modifier.weight(1f), color = if (lang == "fa") c.purple else c.track)
                    GlowButton("English", { vm.updateSettings { it.copy(language = "en") } }, Modifier.weight(1f), color = if (lang == "en") c.purple else c.track)
                }
                SwitchRow(tr("تم تیره", "Dark theme"), s.theme == "dark", { v -> vm.updateSettings { it.copy(theme = if (v) "dark" else "light") } })
                SwitchRow(tr("جلوه‌های صوتی", "Sound effects"), s.sfx, { v -> vm.updateSettings { it.copy(sfx = v) } }, hint = tr("در حالت بی‌صدا یا ویبره پخش نمی‌شود.", "Muted in silent or vibrate mode."))
                SwitchRow(tr("موسیقی پس‌زمینه", "Background music"), s.music, { v -> vm.updateSettings { it.copy(music = v) } })
                SwitchRow(tr("رویدادهای تصادفی مثبت", "Positive random events"), s.randomEvents, { v -> vm.updateSettings { it.copy(randomEvents = v) } })
                SwitchRow(tr("چالش‌های روزانه", "Daily challenges"), s.dailyChallenges, { v -> vm.updateSettings { it.copy(dailyChallenges = v) } })
            }
        }

        // ---- notifications
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("اعلان‌ها", "Notifications"))
                SwitchRow(
                    tr("فعال‌سازی یادآورها", "Enable reminders"), s.notifMaster,
                    { v ->
                        if (!v) vm.updateSettings { it.copy(notifMaster = false) }
                        else if (Build.VERSION.SDK_INT >= 33 && !Notifier.hasPermission(ctx)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else vm.updateSettings { it.copy(notifMaster = true) }
                    },
                    hint = tr("حداکثر ${num(4)} یادآور در روز؛ هیچ‌وقت بیشتر.", "At most 4 reminders a day, never more."),
                )
                if (s.notifMaster) {
                    SwitchRow(tr("ماموریت‌های روزانه", "Daily missions"), s.notifDaily, { v -> vm.updateSettings { it.copy(notifDaily = v) } })
                    SwitchRow(tr("یادآور عادت‌ها", "Habit reminders"), s.notifHabit, { v -> vm.updateSettings { it.copy(notifHabit = v) } })
                    SwitchRow(tr("مهلت‌های نزدیک", "Upcoming deadlines"), s.notifDeadline, { v -> vm.updateSettings { it.copy(notifDeadline = v) } })
                    SwitchRow(tr("یادآور استریک", "Streak reminder"), s.notifStreak, { v -> vm.updateSettings { it.copy(notifStreak = v) } })
                    SwitchRow(tr("خلاصهٔ هفتگی", "Weekly summary"), s.notifWeekly, { v -> vm.updateSettings { it.copy(notifWeekly = v) } })
                    Stepper(tr("ساعت یادآور روزانه", "Daily reminder time"), fmtMinute(s.dailyMinute, lang), { vm.updateSettings { it.copy(dailyMinute = (it.dailyMinute - 30 + 1440) % 1440) } }, { vm.updateSettings { it.copy(dailyMinute = (it.dailyMinute + 30) % 1440) } })
                    Stepper(tr("ساعت یادآور استریک", "Streak reminder time"), fmtMinute(s.streakMinute, lang), { vm.updateSettings { it.copy(streakMinute = (it.streakMinute - 30 + 1440) % 1440) } }, { vm.updateSettings { it.copy(streakMinute = (it.streakMinute + 30) % 1440) } })
                    SwitchRow(tr("ساعت سکوت", "Quiet hours"), s.quietEnabled, { v -> vm.updateSettings { it.copy(quietEnabled = v) } })
                    if (s.quietEnabled) {
                        Stepper(tr("شروع سکوت", "Quiet from"), fmtMinute(s.quietStart, lang), { vm.updateSettings { it.copy(quietStart = (it.quietStart - 30 + 1440) % 1440) } }, { vm.updateSettings { it.copy(quietStart = (it.quietStart + 30) % 1440) } })
                        Stepper(tr("پایان سکوت", "Quiet until"), fmtMinute(s.quietEnd, lang), { vm.updateSettings { it.copy(quietEnd = (it.quietEnd - 30 + 1440) % 1440) } }, { vm.updateSettings { it.copy(quietEnd = (it.quietEnd + 30) % 1440) } })
                    }
                }
            }
        }

        // ---- XP rules
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("قوانین XP", "XP rules"))
                Text(tr("XP پایه برای هر سختی. پاداش هر ماموریت حداکثر دو برابر مقدار پایه است و سقف روزانه هم از سوءاستفاده جلوگیری می‌کند.", "Base XP per difficulty. A quest can award at most double its base, and daily caps prevent exploitation."), color = c.textDim, fontSize = 12.sp)
                for (d in Difficulty.entries) {
                    Stepper(
                        d.label(lang), num(s.rules.baseXp(d)),
                        { vm.updateSettings { st -> st.copy(rules = st.rules.copy(baseXp = st.rules.baseXp + (d to (st.rules.baseXp(d) - 5)))) } },
                        { vm.updateSettings { st -> st.copy(rules = st.rules.copy(baseXp = st.rules.baseXp + (d to (st.rules.baseXp(d) + 5)))) } },
                    )
                }
                Stepper(tr("سقف XP روزانه", "Daily XP cap"), num(s.rules.dailyXpCap), { vm.updateSettings { st -> st.copy(rules = st.rules.copy(dailyXpCap = st.rules.dailyXpCap - 100)) } }, { vm.updateSettings { st -> st.copy(rules = st.rules.copy(dailyXpCap = st.rules.dailyXpCap + 100)) } })
                Stepper(tr("سقف سکهٔ روزانه", "Daily coin cap"), num(s.rules.dailyCoinCap), { vm.updateSettings { st -> st.copy(rules = st.rules.copy(dailyCoinCap = st.rules.dailyCoinCap - 50)) } }, { vm.updateSettings { st -> st.copy(rules = st.rules.copy(dailyCoinCap = st.rules.dailyCoinCap + 50)) } })
                SwitchRow(tr("پاداش استریک", "Streak bonus"), s.rules.streakBonusEnabled, { v -> vm.updateSettings { st -> st.copy(rules = st.rules.copy(streakBonusEnabled = v)) } })
                OutlineButton(tr("بازگشت به پیش‌فرض", "Reset to defaults"), { vm.updateSettings { it.copy(rules = XpRules()) } }, Modifier.fillMaxWidth())
            }
        }

        // ---- categories
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("دسته‌ها", "Categories"), trailing = { GlowButton(tr("＋ جدید", "＋ New"), { addCategory = true }, color = c.emerald) })
                for (cat in ui.categories) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(cat.color.toInt())))
                        Spacer(Modifier.size(8.dp))
                        Text(cat.emoji + " " + cat.name(lang), color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (!cat.builtIn) OutlineButton(tr("حذف", "Delete"), { vm.deleteCategory(cat.id) }, color = c.danger)
                    }
                }
            }
        }

        // ---- backup
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(tr("پشتیبان‌گیری", "Backup"))
                Text(tr("همهٔ داده‌ها را در یک فایل JSON ذخیره کن یا از آن بازیابی کن. فایل‌های وارد‌شده قبل از اعمال بررسی می‌شوند.", "Export all your data to a JSON file or restore from one. Imported files are validated before use."), color = c.textDim, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GlowButton(tr("خروجی JSON", "Export JSON"), { exporter.launch("life-rpg-backup.json") }, Modifier.weight(1f), color = c.emerald)
                    GlowButton(tr("ورود از JSON", "Import JSON"), { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, Modifier.weight(1f), color = c.magic)
                }
            }
        }

        // ---- reset / privacy / about
        RpgCard(accent = c.danger) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(tr("بازنشانی", "Reset"))
                Text(tr("همهٔ پیشرفت، ماموریت‌ها و پاداش‌ها پاک می‌شود. نام و چهره‌ات می‌ماند. پیشنهاد: قبلش پشتیبان بگیر.", "All progress, quests and rewards are erased. Your name and look are kept. Tip: export a backup first."), color = c.textDim, fontSize = 12.sp)
                OutlineButton(tr("بازنشانی پیشرفت…", "Reset progress…"), { showReset = true }, Modifier.fillMaxWidth(), color = c.danger)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlineButton(tr("🔒 حریم خصوصی", "🔒 Privacy"), { showPrivacy = true }, Modifier.weight(1f))
            OutlineButton(tr("ℹ️ دربارهٔ برنامه", "ℹ️ About"), { showAbout = true }, Modifier.weight(1f))
        }
    }

    info?.let { m -> RpgDialog({ info = null }, tr("پیام", "Message")) { Text(m, color = c.text, fontSize = 14.sp); GlowButton(tr("باشه", "OK"), { info = null }, Modifier.fillMaxWidth()) } }

    confirmImport?.let { text ->
        ConfirmDialog(
            tr("بازیابی از پشتیبان؟", "Restore from backup?"),
            tr("تمام داده‌های فعلی با محتوای این فایل جایگزین می‌شود.", "All current data will be replaced by this file's content."),
            tr("بازیابی", "Restore"),
            {
                confirmImport = null
                vm.importJson(text) { ok, why -> info = if (ok) (if (lang == "fa") "بازیابی با موفقیت انجام شد ✅" else "Restore completed ✅") else (if (lang == "fa") "فایل پشتیبان معتبر نیست:\n$why" else "Invalid backup file:\n$why") }
            },
            { confirmImport = null }, danger = true,
        )
    }
    if (showReset) ResetDialog(vm, { showReset = false }) { info = if (lang == "fa") "پیشرفت بازنشانی شد. شروعی تازه! 🌱" else "Progress reset. A fresh start! 🌱" }
    if (showPrivacy) RpgDialog({ showPrivacy = false }, tr("حریم خصوصی", "Privacy")) {
        Text(
            tr(
                "• همهٔ داده‌ها فقط روی همین دستگاه ذخیره می‌شود.\n• حساب کاربری لازم نیست و هیچ اطلاعات شخصی جمع‌آوری نمی‌شود.\n• برنامه مجوز اینترنت ندارد و هیچ ردیابی یا تبلیغی در آن نیست.\n• تنها مجوز: ارسال اعلان‌های محلی (اختیاری).\n• پشتیبان JSON فقط جایی ذخیره می‌شود که خودت انتخاب کنی.",
                "• All data is stored only on this device.\n• No account is needed and no personal information is collected.\n• The app has no internet permission and contains no tracking or ads.\n• The only permission: local notifications (optional).\n• JSON backups are saved only where you choose.",
            ),
            color = c.text, fontSize = 14.sp,
        )
        GlowButton(tr("بستن", "Close"), { showPrivacy = false }, Modifier.fillMaxWidth())
    }
    if (showAbout) RpgDialog({ showAbout = false }, "Life RPG") {
        Text("⚔️ " + tr("لایف RPG – ماجراجویی زندگی واقعی", "Life RPG – Real Life Adventure"), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(tr("نسخهٔ ۱٫۰٫۰", "Version 1.0.0"), color = c.textDim, fontSize = 13.sp)
        Text(tr("ساخته‌شده توسط: محمدطاها طاهریان", "Created by: Mohammad Taha Taherian (محمدطاها طاهریان)"), color = c.text, fontSize = 13.sp)
        Text(tr("متن‌باز با مجوز MIT. همهٔ تصاویر برداری و صداها در خود پروژه ساخته شده‌اند و کاملاً اصیل‌اند.", "Open source under the MIT license. All vector art and sounds were created for this project and are original."), color = c.textDim, fontSize = 12.sp)
        Text(tr("آمار بازی با اندازه‌گیری‌های واقعی سلامت بی‌ارتباط است.", "Game stats are unrelated to real health measurements."), color = c.textDim, fontSize = 12.sp)
        GlowButton(tr("بستن", "Close"), { showAbout = false }, Modifier.fillMaxWidth())
    }
    if (addCategory) CategoryDialog(vm) { addCategory = false }
}

@Composable
private fun ResetDialog(vm: GameViewModel, onClose: () -> Unit, onDone: () -> Unit) {
    val c = LocalRpg.current
    var typed by remember { mutableStateOf("") }
    RpgDialog(onClose, tr("بازنشانی پیشرفت", "Reset progress")) {
        Text(tr("این کار قابل بازگشت نیست. برای تأیید کلمهٔ RESET را بنویس.", "This cannot be undone. Type RESET to confirm."), color = c.text, fontSize = 14.sp)
        RpgTextField(typed, { typed = it }, "RESET")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GlowButton(tr("بازنشانی", "Reset"), { vm.resetProgress { onClose(); onDone() } }, Modifier.weight(1f), color = c.danger, enabled = typed.trim().uppercase() == "RESET")
            OutlineButton(tr("انصراف", "Cancel"), onClose, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CategoryDialog(vm: GameViewModel, onClose: () -> Unit) {
    val c = LocalRpg.current
    var fa by remember { mutableStateOf("") }
    var en by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("⭐") }
    var color by remember { mutableStateOf(CATEGORY_COLORS[0]) }
    var primary by remember { mutableStateOf(Stat.DISCIPLINE) }
    var secondary by remember { mutableStateOf(Stat.FOCUS) }
    var error by remember { mutableStateOf(false) }
    RpgDialog(onClose, tr("دستهٔ جدید", "New category")) {
        RpgTextField(fa, { fa = it }, tr("نام فارسی", "Persian name"))
        RpgTextField(en, { en = it }, tr("نام انگلیسی", "English name"))
        RpgTextField(emoji, { if (it.length <= 4) emoji = it }, tr("ایموجی", "Emoji"))
        ChipRow(CATEGORY_COLORS, color, label = { "●" }, onSelect = { color = it }, accent = { Color(it.toInt()) })
        Text(tr("ویژگی اصلی", "Primary attribute"), color = c.textDim, fontSize = 12.sp)
        ChipRow(Stat.entries, primary, label = { it.emoji + " " + it.fa }, onSelect = { primary = it })
        Text(tr("ویژگی دوم", "Secondary attribute"), color = c.textDim, fontSize = 12.sp)
        ChipRow(Stat.entries, secondary, label = { it.emoji + " " + it.fa }, onSelect = { secondary = it })
        if (error) Text("⚠ " + tr("نام‌ها را درست وارد کن.", "Please enter valid names."), color = c.danger, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GlowButton(tr("ذخیره", "Save"), { vm.addCategory(fa, en, emoji, color, primary, secondary) { ok -> if (ok) onClose() else error = true } }, Modifier.weight(1f), color = c.emerald)
            OutlineButton(tr("انصراف", "Cancel"), onClose, Modifier.weight(1f))
        }
    }
}
