package uz.xumoyun.namoz.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.Brightness5
import androidx.compose.material.icons.rounded.Brightness7
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.data.AlertSound
import uz.xumoyun.namoz.data.db.LocationLabel
import uz.xumoyun.namoz.domain.AsrMadhab
import uz.xumoyun.namoz.domain.CalcMethod
import uz.xumoyun.namoz.domain.HighLatRule
import uz.xumoyun.namoz.domain.Prayer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

@StringRes
fun prayerName(prayer: Prayer): Int = when (prayer) {
    Prayer.FAJR -> R.string.prayer_fajr
    Prayer.SUNRISE -> R.string.prayer_sunrise
    Prayer.DHUHR -> R.string.prayer_dhuhr
    Prayer.ASR -> R.string.prayer_asr
    Prayer.MAGHRIB -> R.string.prayer_maghrib
    Prayer.ISHA -> R.string.prayer_isha
}

/** "Asrgacha" kabi teskari sanoq sarlavhasi. */
@StringRes
fun untilPrayer(prayer: Prayer): Int = when (prayer) {
    Prayer.FAJR -> R.string.until_fajr
    Prayer.SUNRISE -> R.string.until_sunrise
    Prayer.DHUHR -> R.string.until_dhuhr
    Prayer.ASR -> R.string.until_asr
    Prayer.MAGHRIB -> R.string.until_maghrib
    Prayer.ISHA -> R.string.until_isha
}

/** Quyosh holatini ko'rsatuvchi ikonka. */
fun prayerIcon(prayer: Prayer): ImageVector = when (prayer) {
    Prayer.FAJR -> Icons.Rounded.WbTwilight
    Prayer.SUNRISE -> Icons.Rounded.Brightness5
    Prayer.DHUHR -> Icons.Rounded.WbSunny
    Prayer.ASR -> Icons.Rounded.Brightness7
    Prayer.MAGHRIB -> Icons.Rounded.Brightness3
    Prayer.ISHA -> Icons.Rounded.NightsStay
}

@StringRes
fun methodName(method: CalcMethod): Int = when (method) {
    CalcMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_mwl
    CalcMethod.UMM_AL_QURA -> R.string.method_umm_al_qura
    CalcMethod.DUBAI -> R.string.method_dubai
    CalcMethod.KUWAIT -> R.string.method_kuwait
    CalcMethod.QATAR -> R.string.method_qatar
    CalcMethod.EGYPTIAN -> R.string.method_egyptian
    CalcMethod.KARACHI -> R.string.method_karachi
    CalcMethod.TURKEY_DIYANET -> R.string.method_turkey
    CalcMethod.UZBEKISTAN -> R.string.method_uzbekistan
    CalcMethod.SINGAPORE -> R.string.method_singapore
    CalcMethod.NORTH_AMERICA -> R.string.method_isna
    CalcMethod.MOONSIGHTING_COMMITTEE -> R.string.method_moonsighting
}

@StringRes
fun madhabName(madhab: AsrMadhab): Int = when (madhab) {
    AsrMadhab.SHAFI -> R.string.madhab_shafi
    AsrMadhab.HANAFI -> R.string.madhab_hanafi
}

@StringRes
fun highLatName(rule: HighLatRule): Int = when (rule) {
    HighLatRule.MIDDLE_OF_THE_NIGHT -> R.string.highlat_middle
    HighLatRule.SEVENTH_OF_THE_NIGHT -> R.string.highlat_seventh
    HighLatRule.TWILIGHT_ANGLE -> R.string.highlat_angle
}

@StringRes
fun soundName(sound: AlertSound): Int = when (sound) {
    AlertSound.AZAN -> R.string.sound_azan
    AlertSound.BEEP -> R.string.sound_beep
    AlertSound.SILENT -> R.string.sound_silent
}

@StringRes
fun labelName(label: LocationLabel): Int = when (label) {
    LocationLabel.NONE -> R.string.label_none
    LocationLabel.HOME -> R.string.label_home
    LocationLabel.WORK -> R.string.label_work
    LocationLabel.TRAVEL -> R.string.label_travel
}

/** Vaqt formati: telefon sozlamasiga qarab 12/24 soat, til raqamlari bilan. */
object TimeFormat {
    fun formatter(context: Context, locale: Locale = currentLocale(context)): DateTimeFormatter {
        val pattern = if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.of(locale))
    }

    fun format(context: Context, time: ZonedDateTime): String = formatter(context).format(time)

    fun currentLocale(context: Context): Locale = context.resources.configuration.locales[0]
}

/** Qo'llab-quvvatlanadigan 12 til (TZ: "Tillar va lokalizatsiya"). RTL: ar, ur, fa. */
val SupportedLanguages = listOf(
    "en" to "English",
    "ar" to "العربية",
    "id" to "Bahasa Indonesia",
    "ur" to "اردو",
    "bn" to "বাংলা",
    "tr" to "Türkçe",
    "fa" to "فارسی",
    "ms" to "Bahasa Melayu",
    "fr" to "Français",
    "ru" to "Русский",
    "uz" to "O‘zbekcha",
    "de" to "Deutsch",
)
