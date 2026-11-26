package ru.sguardo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.sguardo.app.R

/** Цвета из макета: переменные --m-* (Material 3) и общие токены (--fg, --muted, оттенки карточек). */
@Immutable
data class SgColors(
    val dark: Boolean,
    val surface: Color, val cLow: Color, val c: Color, val cHigh: Color,
    val primary: Color, val onPrimary: Color, val pc: Color, val onPc: Color,
    val sc: Color, val onSc: Color, val outline: Color, val ov: Color, val inv: Color, val onInv: Color,
    val fg: Color, val muted: Color, val faint: Color, val teal: Color, val onTeal: Color,
    val wine: Color, val ok: Color, val warn: Color, val hair: Color, val rowBg: Color,
    val tBlue: Color, val tGreen: Color, val tYellow: Color, val tLilac: Color, val tPink: Color, val tPeach: Color,
    val danger: Color, val statInk: Color,
)

val LightColors = SgColors(
    dark = false,
    surface = Color(0xFFF6F1EA), cLow = Color(0xFFEFE9E0), c = Color(0xFFE9E3D9), cHigh = Color(0xFFE2DCD1),
    primary = Color(0xFF0F3D42), onPrimary = Color.White, pc = Color(0xFFBFE4DF), onPc = Color(0xFF002022),
    sc = Color(0xFFCFE3DF), onSc = Color(0xFF0B1F1F), outline = Color(0xFF76837F), ov = Color(0xFFC6CECB),
    inv = Color(0xFF2B3231), onInv = Color(0xFFEEF1EF),
    fg = Color(0xFF14211F), muted = Color(0xFF5F6D6A), faint = Color(0xFF8A9694), teal = Color(0xFF0F3D42), onTeal = Color(0xFFF3EFE8),
    wine = Color(0xFF7B1419), ok = Color(0xFF1E6B4A), warn = Color(0xFF9A6408), hair = Color(0x1414211F), rowBg = Color(0xFFF3EEE7),
    tBlue = Color(0xFFC3DAE3), tGreen = Color(0xFFBFE0CF), tYellow = Color(0xFFF0DDA8), tLilac = Color(0xFFD9D0EC), tPink = Color(0xFFEFC0BC), tPeach = Color(0xFFF3C9AE),
    danger = Color(0xFFB3261E), statInk = Color(0xFF14211F),
)

val DarkColors = SgColors(
    dark = true,
    surface = Color(0xFF0F1515), cLow = Color(0xFF171D1D), c = Color(0xFF1B2121), cHigh = Color(0xFF252B2B),
    primary = Color(0xFF9FD3CC), onPrimary = Color(0xFF003739), pc = Color(0xFF1F4E50), onPc = Color(0xFFBCEDE7),
    sc = Color(0xFF334B4A), onSc = Color(0xFFCDE8E4), outline = Color(0xFF899390), ov = Color(0xFF3F4948),
    inv = Color(0xFFDEE4E2), onInv = Color(0xFF2B3231),
    fg = Color(0xFFE8E4DC), muted = Color(0xFF9AA6A3), faint = Color(0xFF6B7875), teal = Color(0xFF9FD3CC), onTeal = Color(0xFF0B2224),
    wine = Color(0xFFE8918F), ok = Color(0xFF7FD1A8), warn = Color(0xFFE8C070), hair = Color(0x14FFFFFF), rowBg = Color(0xFF182223),
    tBlue = Color(0xFF29434D), tGreen = Color(0xFF26443A), tYellow = Color(0xFF4A4127), tLilac = Color(0xFF3A3450), tPink = Color(0xFF52302F), tPeach = Color(0xFF52392A),
    danger = Color(0xFFF08A84), statInk = Color(0xFFE8E4DC),
)

val Onest = FontFamily(
    Font(R.font.onest_regular, FontWeight.Normal),
    Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold),
    Font(R.font.onest_bold, FontWeight.Bold),
)
val Unbounded = FontFamily(
    Font(R.font.unbounded_medium, FontWeight.Medium),
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_bold, FontWeight.Bold),
)

val LocalSg = staticCompositionLocalOf { LightColors }

object Sg {
    val c: SgColors @Composable get() = LocalSg.current
}

fun disp(size: Int, w: FontWeight = FontWeight.SemiBold) = TextStyle(fontFamily = Unbounded, fontWeight = w, fontSize = size.sp, letterSpacing = (-0.01 * size).sp)
fun body(size: Float, w: FontWeight = FontWeight.Normal) = TextStyle(fontFamily = Onest, fontWeight = w, fontSize = size.sp)

@Composable
fun SgTheme(mode: String = "system", content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, primaryContainer = c.pc, onPrimaryContainer = c.onPc,
        secondaryContainer = c.sc, onSecondaryContainer = c.onSc, surface = c.surface, onSurface = c.fg,
        background = c.surface, onBackground = c.fg, surfaceContainerLowest = c.surface, surfaceContainerLow = c.cLow,
        surfaceContainer = c.c, surfaceContainerHigh = c.cHigh, surfaceContainerHighest = c.cHigh,
        outline = c.outline, outlineVariant = c.ov, inverseSurface = c.inv, inverseOnSurface = c.onInv,
        onSurfaceVariant = c.muted, error = c.danger, surfaceVariant = c.cHigh,
    ) else lightColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, primaryContainer = c.pc, onPrimaryContainer = c.onPc,
        secondaryContainer = c.sc, onSecondaryContainer = c.onSc, surface = c.surface, onSurface = c.fg,
        background = c.surface, onBackground = c.fg, surfaceContainerLowest = c.surface, surfaceContainerLow = c.cLow,
        surfaceContainer = c.c, surfaceContainerHigh = c.cHigh, surfaceContainerHighest = c.cHigh,
        outline = c.outline, outlineVariant = c.ov, inverseSurface = c.inv, inverseOnSurface = c.onInv,
        onSurfaceVariant = c.muted, error = c.danger, surfaceVariant = c.cHigh,
    )
    val t = Typography()
    val typo = Typography(
        bodyLarge = t.bodyLarge.copy(fontFamily = Onest), bodyMedium = t.bodyMedium.copy(fontFamily = Onest),
        bodySmall = t.bodySmall.copy(fontFamily = Onest), labelLarge = t.labelLarge.copy(fontFamily = Onest, fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontFamily = Onest), labelSmall = t.labelSmall.copy(fontFamily = Onest),
        titleLarge = t.titleLarge.copy(fontFamily = Onest), titleMedium = t.titleMedium.copy(fontFamily = Onest),
        titleSmall = t.titleSmall.copy(fontFamily = Onest), headlineSmall = t.headlineSmall.copy(fontFamily = Onest),
        headlineMedium = t.headlineMedium.copy(fontFamily = Onest), headlineLarge = t.headlineLarge.copy(fontFamily = Onest),
    )
    CompositionLocalProvider(LocalSg provides c) {
        MaterialTheme(colorScheme = scheme, typography = typo, content = content)
    }
}
