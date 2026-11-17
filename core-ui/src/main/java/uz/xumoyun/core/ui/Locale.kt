package uz.xumoyun.core.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Ilova tilini almashtiradi (Android 13+ da tizimning "App language" bilan bir xil). */
object AppLocale {
    fun set(tag: String?) {
        val list = if (tag.isNullOrBlank()) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(list)
    }

    /** Hozirgi ilova tili ("uz", "ru", ...) yoki null (tizim tili). */
    fun current(): String? = AppCompatDelegate.getApplicationLocales()
        .takeIf { !it.isEmpty }?.get(0)?.language
        ?.let { if (it == "in") "id" else it }
}
