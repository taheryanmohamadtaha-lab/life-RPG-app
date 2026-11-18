package com.liferpg.adventure.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.liferpg.adventure.domain.Digits
import com.liferpg.adventure.domain.Jalali
import com.liferpg.adventure.ui.theme.LocalLang
import com.liferpg.adventure.ui.theme.LocalRpg
import com.liferpg.adventure.ui.theme.tr
import java.time.LocalDate

fun fmtDay(day: Long, lang: String): String =
    if (lang == "fa") Digits.localize(Jalali.format(day), "fa") else LocalDate.ofEpochDay(day).toString()

fun fmtMinute(minute: Int, lang: String): String {
    val m = minute.coerceIn(0, 1439)
    return Digits.localize("%02d:%02d".format(m / 60, m % 60), lang)
}

@Composable
fun RpgTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    number: Boolean = false,
    maxLines: Int = if (singleLine) 1 else 5,
) {
    val c = LocalRpg.current
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(),
        singleLine = singleLine, maxLines = maxLines,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.text, unfocusedTextColor = c.text,
            focusedBorderColor = c.purple, unfocusedBorderColor = c.border,
            focusedLabelColor = c.purple, unfocusedLabelColor = c.textDim, cursorColor = c.gold,
        ),
    )
}

/** Horizontally scrolling single-choice chips. */
@Composable
fun <T> ChipRow(options: List<T>, selected: T?, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier, accent: (T) -> Color? = { null }) {
    val c = LocalRpg.current
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (o in options) {
            val sel = o == selected
            val col = accent(o) ?: c.purple
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (sel) col.copy(alpha = 0.28f) else c.track)
                    .border(1.dp, if (sel) col else c.border, RoundedCornerShape(50))
                    .clickable { onSelect(o) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) { Text(label(o), color = if (sel) c.text else c.textDim, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
        }
    }
}

@Composable
fun Stepper(label: String, valueText: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalRpg.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        StepBtn("−", onMinus)
        Text(valueText, color = c.gold, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
        StepBtn("+", onPlus)
    }
}

@Composable
private fun StepBtn(text: String, onClick: () -> Unit) {
    val c = LocalRpg.current
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(c.panelHi).border(1.dp, c.border, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    val c = LocalRpg.current
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = c.text, fontSize = 14.sp)
            if (hint != null) Text(hint, color = c.textDim, fontSize = 11.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.purple, checkedThumbColor = Color.White),
        )
    }
}

@Composable
fun RpgDialog(onDismiss: () -> Unit, title: String, fullScreen: Boolean = false, content: @Composable () -> Unit) {
    val c = LocalRpg.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = !fullScreen)) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.panel).border(1.dp, c.border, RoundedCornerShape(20.dp)).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, color = c.gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            content()
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    val c = LocalRpg.current
    RpgDialog(onDismiss, title) {
        Text(text, color = c.text, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GlowButton(confirmLabel, onConfirm, Modifier.weight(1f), color = if (danger) c.danger else c.purple)
            OutlineButton(tr("انصراف", "Cancel"), onDismiss, Modifier.weight(1f))
        }
    }
}

/** Gold / gems / streak line used in headers. */
@Composable
fun CurrencyRow(gold: Long, gems: Long, modifier: Modifier = Modifier) {
    val c = LocalRpg.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RewardChip("🪙", num(gold), c.gold)
        RewardChip("💎", num(gems), c.magic)
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalRpg.current.border.copy(alpha = 0.5f)))
}

@Composable
fun KeyValue(k: String, v: String) {
    val c = LocalRpg.current
    Row(Modifier.fillMaxWidth()) {
        Text(k, color = c.textDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(v, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun langCode(): String = LocalLang.current
