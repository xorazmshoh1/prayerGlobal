package uz.xumoyun.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.xumoyun.core.common.ThemeMode

/** Material 3 sxemasida yo'q ranglar: urg'u, to'g'ri, xato. */
data class ExtraColors(val accent: Color, val success: Color, val failure: Color, val isDark: Boolean)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(Color(0xFFFFB300), Color(0xFF2E7D32), Color(0xFFC62828), false)
}

private val base = Typography()

/** TZ: sarlavha 22–24 sp, oddiy matn 16 sp, izoh 13 sp. */
val AppTypography = Typography(
    headlineLarge = base.headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = base.bodySmall.copy(fontSize = 13.sp),
    labelSmall = base.labelSmall.copy(fontSize = 12.sp),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Raqamlar teng kenglikda (taymer, vaqtlar). */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun AppTheme(
    palette: AppPalette,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val extra = ExtraColors(
        accent = palette.accentFor(dark),
        success = if (dark) palette.success.tone(0.65f) else palette.success,
        failure = if (dark) palette.failure.tone(0.70f) else palette.failure,
        isDark = dark,
    )
    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = if (dark) palette.dark else palette.light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
