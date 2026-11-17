package uz.xumoyun.namoz.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import uz.xumoyun.core.ads.AdsManager
import uz.xumoyun.core.ui.theme.AppPalette
import uz.xumoyun.core.ui.theme.AppTheme

/** TZ: asosiy rang zumrad yashil #00695C, urg'u rangi oltin #C9A227. */
val NamozPalette = AppPalette(seed = Color(0xFF00695C), accent = Color(0xFFC9A227))

/** Reklama menejeri ekranlarga CompositionLocal orqali beriladi (Preview'da null). */
val LocalAds = staticCompositionLocalOf<AdsManager?> { null }

@Composable
fun NamozPreview(content: @Composable () -> Unit) {
    AppTheme(NamozPalette) { Surface { content() } }
}
