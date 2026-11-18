package uz.xumoyun.namoz.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import uz.xumoyun.core.ui.theme.AppTheme
import uz.xumoyun.core.ui.theme.NamozHifzPalette

/** TZ 11-bo'lim ranglari (core-ui). */
val NamozPalette = NamozHifzPalette

@Composable
fun NamozPreview(content: @Composable () -> Unit) {
    AppTheme(NamozPalette) { Surface { content() } }
}
