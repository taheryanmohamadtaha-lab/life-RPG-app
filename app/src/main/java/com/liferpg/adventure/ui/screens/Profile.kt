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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.ui.art.DragonView
import com.liferpg.adventure.ui.art.HeroLook
import com.liferpg.adventure.ui.art.HeroPortrait
import com.liferpg.adventure.ui.components.AppBackground
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.RpgTextField
import com.liferpg.adventure.ui.components.SectionTitle
import com.liferpg.adventure.ui.components.SwitchRow
import com.liferpg.adventure.ui.components.KeyValue
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel

/** Name, avatar and dragon companion pickers (used by onboarding and the profile tab). */
@Composable
fun IdentityEditor(
    lang: String,
    name: String, onName: (String) -> Unit,
    avatarId: Int, onAvatar: (Int) -> Unit,
    petType: Int, onPet: (Int) -> Unit,
    petName: String, onPetName: (String) -> Unit,
    petStage: Int,
) {
    val c = LocalRpg.current
    RpgTextField(name, { if (it.length <= 40) onName(it) }, tr("نام قهرمان", "Hero name"))
    Text(tr("انتخاب چهره (بدون جنسیت مشخص)", "Choose an appearance (gender-neutral)"), color = c.textDim, fontSize = 12.sp)
    for (row in 0..1) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..3) {
                val id = row * 4 + i
                val av = Catalog.avatar(id)
                val sel = id == avatarId
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (sel) c.purple.copy(alpha = 0.25f) else c.track)
                        .border(if (sel) 2.dp else 1.dp, if (sel) c.gold else c.border, RoundedCornerShape(14.dp))
                        .clickable { onAvatar(id) }.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HeroPortrait(HeroLook(av, 0), Modifier.size(54.dp))
                    Text(if (lang == "fa") av.fa else av.en, color = if (sel) c.text else c.textDim, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
    Text(tr("اژدهای همراه", "Dragon companion"), color = c.textDim, fontSize = 12.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (pet in Catalog.pets) {
            val sel = pet.id == petType
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (sel) c.purple.copy(alpha = 0.25f) else c.track)
                    .border(if (sel) 2.dp else 1.dp, if (sel) c.gold else c.border, RoundedCornerShape(14.dp))
                    .clickable { onPet(pet.id) }.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DragonView(pet, petStage, Modifier.size(84.dp))
                Text(if (lang == "fa") pet.fa else pet.en, color = if (sel) c.text else c.textDim, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
    RpgTextField(petName, { if (it.length <= 40) onPetName(it) }, tr("نام اژدها", "Dragon name"))
}

@Composable
fun OnboardingScreen(vm: GameViewModel, lang: String) {
    val c = LocalRpg.current
    var name by remember { mutableStateOf("") }
    var avatar by remember { mutableIntStateOf(0) }
    var pet by remember { mutableIntStateOf(0) }
    var petName by remember { mutableStateOf("") }
    AppBackground {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Text("⚔️", fontSize = 56.sp)
            Text(tr("به لایف RPG خوش آمدی", "Welcome to Life RPG"), color = c.gold, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center)
            Text(
                tr("کارهای واقعی زندگی‌ات را به ماجراجویی تبدیل کن. قهرمانت را بساز و اژدهای همراهت را انتخاب کن؛ با هر قدم کوچک، هر دو رشد می‌کنید.",
                    "Turn real-life actions into an adventure. Create your hero and pick a dragon companion; both grow with every small step."),
                color = c.textDim, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
            RpgCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IdentityEditor(lang, name, { name = it }, avatar, { avatar = it }, pet, { pet = it }, petName, { petName = it }, petStage = 1)
                }
            }
            GlowButton(
                tr("شروع ماجراجویی ✨", "Begin the adventure ✨"),
                {
                    val pn = Catalog.pet(pet)
                    vm.saveProfile(
                        name.trim().ifEmpty { if (lang == "fa") "قهرمان" else "Hero" }, avatar, pet,
                        petName.trim().ifEmpty { if (lang == "fa") pn.fa else pn.en },
                    )
                },
                Modifier.fillMaxWidth(), color = c.emerald,
            )
            Text(
                tr("همه چیز روی گوشی خودت ذخیره می‌شود. حساب کاربری و اینترنت لازم نیست.", "Everything stays on your phone. No account or internet needed."),
                color = c.textDim, fontSize = 11.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun ProfileScreen(
    ui: GameUi,
    vm: GameViewModel,
    onSkills: () -> Unit,
    onInventory: () -> Unit,
    onAchievements: () -> Unit,
    onStats: () -> Unit,
    onCalendar: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalRpg.current
    val p = ui.player
    var name by remember(p.name) { mutableStateOf(p.name) }
    var avatar by remember(p.avatarId) { mutableIntStateOf(p.avatarId) }
    var pet by remember(p.petType) { mutableIntStateOf(p.petType) }
    var petName by remember(p.petName) { mutableStateOf(p.petName) }
    val changed = name != p.name || avatar != p.avatarId || pet != p.petType || petName != p.petName

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(tr("پروفایل", "Profile"), color = c.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        RpgCard(accent = c.purple) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IdentityEditor(ui.lang, name, { name = it }, avatar, { avatar = it }, pet, { pet = it }, petName, { petName = it }, Catalog.petStage(p.level))
                GlowButton(
                    tr("ذخیرهٔ تغییرات", "Save changes"),
                    {
                        if (name.isNotBlank()) vm.saveProfile(name, avatar, pet, petName.ifBlank { Catalog.pet(pet).let { d -> if (ui.lang == "fa") d.fa else d.en } })
                    },
                    Modifier.fillMaxWidth(), color = c.emerald, enabled = changed && name.isNotBlank(),
                )
            }
        }
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("ظاهر برنامه", "Appearance"))
                SwitchRow(tr("تم تیره (فانتزی تاریک)", "Dark fantasy theme"), ui.settings.theme == "dark", { vm.updateSettings { s -> s.copy(theme = if (it) "dark" else "light") } })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GlowButton("فارسی", { vm.updateSettings { it.copy(language = "fa") } }, Modifier.weight(1f), color = if (ui.lang == "fa") c.purple else c.track)
                    GlowButton("English", { vm.updateSettings { it.copy(language = "en") } }, Modifier.weight(1f), color = if (ui.lang == "en") c.purple else c.track)
                }
            }
        }
        RpgCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle(tr("آمار شما", "Your journey"))
                KeyValue(tr("سطح", "Level"), num(p.level))
                KeyValue(tr("کل XP", "Total XP"), num(p.totalXp))
                KeyValue(tr("ماموریت‌های انجام‌شده", "Quests completed"), num(ui.completions.size))
                KeyValue(tr("بهترین استریک", "Best streak"), num(p.bestStreak))
                KeyValue(tr("امتیاز مهارت", "Skill points"), num(p.skillPoints))
            }
        }
        SectionTitle(tr("بخش‌ها", "Explore"))
        val links = listOf(
            Triple("🌳", tr("درخت مهارت‌ها", "Skill tree"), onSkills),
            Triple("🎒", tr("کوله‌پشتی و تجهیزات", "Inventory & equipment"), onInventory),
            Triple("🏆", tr("دستاوردها", "Achievements"), onAchievements),
            Triple("📊", tr("آمار و تحلیل", "Statistics"), onStats),
            Triple("📅", tr("تقویم عادت‌ها", "Habit calendar"), onCalendar),
            Triple("🛍️", tr("فروشگاه پاداش", "Reward shop"), onShop),
            Triple("⚙️", tr("تنظیمات", "Settings"), onSettings),
        )
        for ((emoji, label, go) in links) {
            RpgCard(Modifier.fillMaxWidth(), onClick = go, padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 22.sp)
                    Spacer(Modifier.size(10.dp))
                    Text(label, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text("›", color = c.textDim, fontSize = 20.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
