package uz.xumoyun.feature.hifz.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/**
 * Qur'on shrifti: `res/font/amiri_quran.ttf` (Amiri Quran, SIL OFL 1.1) bo'lsa shu ishlatiladi.
 * Fayl yo'q bo'lsa tizim serif shrifti (arab yozuvi tizim fallback orqali chiziladi).
 * Fayl repoda yo'q: uni qo'lda qo'shing (feature-hifz/README.md). Resurs nomi bo'yicha qidiriladi,
 * shuning uchun fayl bo'lmasa ham build buzilmaydi.
 */
object QuranFont {
    const val RESOURCE_NAME = "amiri_quran"

    @Composable
    fun family(): FontFamily {
        val context = LocalContext.current
        val resId = remember(context) { resolve(context) }
        return if (resId == 0) FontFamily.Serif else FontFamily(Font(resId))
    }

    internal fun resolve(context: Context): Int =
        context.resources.getIdentifier(RESOURCE_NAME, "font", context.packageName)
}
