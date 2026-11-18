package com.liferpg.adventure.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

/** Extra palette on top of Material 3: deep navy, purple, gold, emerald and magical blue. */
data class RpgColors(
    val isDark: Boolean,
    val bg: Color,
    val bgTop: Color,
    val panel: Color,
    val panelHi: Color,
    val track: Color,
    val border: Color,
    val text: Color,
    val textDim: Color,
    val purple: Color,
    val gold: Color,
    val emerald: Color,
    val magic: Color,
    val danger: Color,
)

val DarkRpg = RpgColors(
    isDark = true,
    bg = Color(0xFF080C1E), bgTop = Color(0xFF1A1346),
    panel = Color(0xFF0F1632), panelHi = Color(0xFF1A2350),
    track = Color(0xFF0A1028), border = Color(0xFF2B3A78),
    text = Color(0xFFF2F4FF), textDim = Color(0xFF9AA6D6),
    purple = Color(0xFF8B5CF6), gold = Color(0xFFFFC857), emerald = Color(0xFF2ED8A3),
    magic = Color(0xFF39A0FF), danger = Color(0xFFFF6B7A),
)

val LightRpg = RpgColors(
    isDark = false,
    bg = Color(0xFFF4EEE0), bgTop = Color(0xFFE6DDFB),
    panel = Color(0xFFFFFBF2), panelHi = Color(0xFFFFFFFF),
    track = Color(0xFFD9D2C0), border = Color(0xFFC9B98F),
    text = Color(0xFF1B1840), textDim = Color(0xFF5E5A82),
    purple = Color(0xFF6D3FE0), gold = Color(0xFFC98A00), emerald = Color(0xFF0E9F73),
    magic = Color(0xFF1479D6), danger = Color(0xFFD63B4D),
)

val LocalRpg = staticCompositionLocalOf { DarkRpg }
val LocalLang = staticCompositionLocalOf { "fa" }

/** Translate inline: Persian first, English second. */
@Composable
fun tr(fa: String, en: String): String = if (LocalLang.current == "fa") fa else en

@Composable
fun isFa(): Boolean = LocalLang.current == "fa"

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun LifeRpgTheme(dark: Boolean, lang: String, content: @Composable () -> Unit) {
    val c = if (dark) DarkRpg else LightRpg
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.purple, onPrimary = Color.White, secondary = c.gold, onSecondary = Color(0xFF241A00),
            tertiary = c.emerald, background = c.bg, onBackground = c.text, surface = c.panel, onSurface = c.text,
            surfaceVariant = c.panelHi, onSurfaceVariant = c.textDim, outline = c.border, error = c.danger,
        )
    } else {
        lightColorScheme(
            primary = c.purple, onPrimary = Color.White, secondary = c.gold, onSecondary = Color.White,
            tertiary = c.emerald, background = c.bg, onBackground = c.text, surface = c.panel, onSurface = c.text,
            surfaceVariant = c.panelHi, onSurfaceVariant = c.textDim, outline = c.border, error = c.danger,
        )
    }
    CompositionLocalProvider(
        LocalRpg provides c,
        LocalLang provides lang,
        LocalLayoutDirection provides if (lang == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
