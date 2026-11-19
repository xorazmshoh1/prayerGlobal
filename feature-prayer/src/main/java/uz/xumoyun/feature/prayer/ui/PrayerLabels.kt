package uz.xumoyun.feature.prayer.ui

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
import uz.xumoyun.feature.prayer.R
import uz.xumoyun.feature.prayer.data.LocationLabel
import uz.xumoyun.feature.prayer.domain.AlertMode
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.HighLatRule
import uz.xumoyun.feature.prayer.domain.Prayer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

/* Namoz moduli matnlari: ilovaning boshqa qismlari (bildirishnoma, vidjet, sozlamalar) ham shulardan foydalanadi. */

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
fun alertModeName(mode: AlertMode): Int = when (mode) {
    AlertMode.SOUND -> R.string.alert_mode_sound
    AlertMode.SILENT -> R.string.alert_mode_silent
    AlertMode.OFF -> R.string.alert_mode_off
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

    /** Raqamlar locale bo'yicha (TZ 9): ar — arab-hind, fa/ur — fors raqamlari. */
    fun format(context: Context, time: ZonedDateTime): String =
        uz.xumoyun.core.common.text.LocalDigits.forLanguage(currentLocale(context).language)(formatter(context).format(time))

    fun currentLocale(context: Context): Locale = context.resources.configuration.locales[0]
}
