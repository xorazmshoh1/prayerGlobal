package uz.xumoyun.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * Ilovaning ranglari. Asosiy (seed) rangdan Material 3 uslubidagi yorug' va
 * qorong'i sxemalar yaratiladi: HSL yorug'ligini o'zgartirib tonlar olinadi.
 */
@Immutable
data class AppPalette(
    val seed: Color,
    val accent: Color,
    val success: Color = Color(0xFF2E7D32),
    val failure: Color = Color(0xFFC62828),
) {
    val light: ColorScheme by lazy { schemeFrom(seed, accent, dark = false) }
    val dark: ColorScheme by lazy { schemeFrom(seed, accent, dark = true) }
}

/** Rangning HSL yorug'ligini [lightness] (0..1) ga o'rnatadi. */
fun Color.tone(lightness: Float, saturationScale: Float = 1f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = (hsl[1] * saturationScale).coerceIn(0f, 1f)
    hsl[2] = lightness.coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun schemeFrom(seed: Color, accent: Color, dark: Boolean): ColorScheme {
    val neutral = { l: Float -> seed.tone(l, saturationScale = 0.12f) }
    val neutralVariant = { l: Float -> seed.tone(l, saturationScale = 0.2f) }
    return if (!dark) lightColorScheme(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = seed.tone(0.90f),
        onPrimaryContainer = seed.tone(0.12f),
        secondary = accent.tone(0.38f),
        onSecondary = Color.White,
        secondaryContainer = accent.tone(0.88f),
        onSecondaryContainer = accent.tone(0.14f),
        tertiary = accent,
        onTertiary = Color.Black,
        tertiaryContainer = accent.tone(0.90f),
        onTertiaryContainer = accent.tone(0.15f),
        background = neutral(0.985f),
        onBackground = neutral(0.10f),
        surface = neutral(0.985f),
        onSurface = neutral(0.10f),
        surfaceVariant = neutralVariant(0.91f),
        onSurfaceVariant = neutralVariant(0.30f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = neutral(0.965f),
        surfaceContainer = neutral(0.95f),
        surfaceContainerHigh = neutral(0.93f),
        surfaceContainerHighest = neutral(0.90f),
        outline = neutralVariant(0.50f),
        outlineVariant = neutralVariant(0.82f),
        error = Color(0xFFBA1A1A),
    ) else darkColorScheme(
        primary = seed.tone(0.78f),
        onPrimary = seed.tone(0.18f),
        primaryContainer = seed.tone(0.28f),
        onPrimaryContainer = seed.tone(0.90f),
        secondary = accent.tone(0.75f),
        onSecondary = accent.tone(0.18f),
        secondaryContainer = accent.tone(0.26f),
        onSecondaryContainer = accent.tone(0.90f),
        tertiary = accent.tone(0.70f),
        onTertiary = accent.tone(0.15f),
        tertiaryContainer = accent.tone(0.28f),
        onTertiaryContainer = accent.tone(0.90f),
        background = neutral(0.07f),
        onBackground = neutral(0.90f),
        surface = neutral(0.07f),
        onSurface = neutral(0.90f),
        surfaceVariant = neutralVariant(0.28f),
        onSurfaceVariant = neutralVariant(0.80f),
        surfaceContainerLowest = neutral(0.05f),
        surfaceContainerLow = neutral(0.10f),
        surfaceContainer = neutral(0.12f),
        surfaceContainerHigh = neutral(0.16f),
        surfaceContainerHighest = neutral(0.20f),
        outline = neutralVariant(0.58f),
        outlineVariant = neutralVariant(0.30f),
        error = Color(0xFFFFB4AB),
    )
}
