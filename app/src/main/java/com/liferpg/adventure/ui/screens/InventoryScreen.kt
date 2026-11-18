package com.liferpg.adventure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferpg.adventure.domain.Catalog
import com.liferpg.adventure.domain.Currency
import com.liferpg.adventure.domain.ItemDef
import com.liferpg.adventure.domain.ItemSlot
import com.liferpg.adventure.ui.art.HeroView
import com.liferpg.adventure.ui.components.ChipRow
import com.liferpg.adventure.ui.components.CurrencyRow
import com.liferpg.adventure.ui.components.GlowButton
import com.liferpg.adventure.ui.components.OutlineButton
import com.liferpg.adventure.ui.components.RewardChip
import com.liferpg.adventure.ui.components.RpgCard
import com.liferpg.adventure.ui.components.ScreenScaffold
import com.liferpg.adventure.ui.components.Tag
import com.liferpg.adventure.ui.components.num
import com.liferpg.adventure.ui.components.rarityColor
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import com.liferpg.adventure.ui.vm.GameUi
import com.liferpg.adventure.ui.vm.GameViewModel

@Composable
fun InventoryScreen(ui: GameUi, vm: GameViewModel, onBack: () -> Unit) {
    val c = LocalRpg.current
    val lang = ui.lang
    var slot by remember { mutableStateOf(ItemSlot.WEAPON) }
    val items = Catalog.items.filter { it.slot == slot }.sortedBy { it.rarity.ordinal }

    ScreenScaffold(tr("کوله‌پشتی و فروشگاه تجهیزات", "Inventory & gear store"), onBack) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(150.dp).weight(1f, fill = false)) { HeroView(ui.look(), Modifier.size(150.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CurrencyRow(ui.player.gold, ui.player.gems)
                Text(tr("تجهیزات ظاهر قهرمان را تغییر می‌دهند و ویژگی‌های بازی را کمی بالا می‌برند.", "Gear changes your hero's look and slightly boosts game attributes."), color = c.textDim, fontSize = 12.sp)
            }
        }
        ChipRow(ItemSlot.entries, slot, label = { it.emoji + " " + it.label(lang) }, onSelect = { slot = it })
        for (item in items) ItemCard(ui, vm, item)
    }
}

@Composable
private fun ItemCard(ui: GameUi, vm: GameViewModel, item: ItemDef) {
    val c = LocalRpg.current
    val lang = ui.lang
    val col = rarityColor(item.rarity)
    val owned = item.id in ui.owned
    val equipped = item.id in ui.player.equippedIds()
    val gems = item.currency == Currency.GEMS
    RpgCard(accent = col) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(col.copy(alpha = 0.15f)).border(2.dp, col, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(item.emoji, fontSize = 28.sp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (lang == "fa") item.fa else item.en, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(item.rarity.label(lang), col)
                    if (equipped) Tag("✓ " + tr("پوشیده", "Equipped"), c.emerald) else if (owned) Tag(tr("دارم", "Owned"), c.textDim)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(if (lang == "fa") item.descFa else item.descEn, color = c.textDim, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((s, v) in item.stats) RewardChip(s.emoji, "+${num(v)}", c.text)
            if (item.xpPercent > 0) RewardChip("✨", "+${num(item.xpPercent)}٪", c.purple)
            if (item.coinPercent > 0) RewardChip("🪙", "+${num(item.coinPercent)}٪", c.gold)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RewardChip(if (gems) "💎" else "🪙", num(item.price), if (gems) c.magic else c.gold)
            Spacer(Modifier.weight(1f))
            if (item.consumableShield) {
                GlowButton(tr("خرید", "Buy"), { vm.buy(item.id) }, color = c.gold)
            } else if (!owned) {
                GlowButton(tr("خرید", "Buy"), { vm.buy(item.id) }, color = c.gold)
            } else if (item.slot.equippable) {
                if (equipped) OutlineButton(tr("درآوردن", "Unequip"), { vm.unequip(item.slot) })
                else GlowButton(tr("پوشیدن", "Equip"), { vm.equip(item.id) }, color = c.emerald)
            } else {
                Text(tr("فعال", "Active"), color = c.emerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
