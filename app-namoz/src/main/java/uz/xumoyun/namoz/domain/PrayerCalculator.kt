package uz.xumoyun.namoz.domain

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerAdjustments
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class CalcInput(
    val latitude: Double,
    val longitude: Double,
    val zone: ZoneId,
    val method: CalcMethod,
    val madhab: AsrMadhab,
    val highLatRule: HighLatRule = HighLatRule.MIDDLE_OF_THE_NIGHT,
    /** Foydalanuvchining qo'lda qo'shgan ± daqiqalari. */
    val adjustments: Map<Prayer, Int> = emptyMap(),
)

data class DayTimes(val date: LocalDate, val times: Map<Prayer, ZonedDateTime>) {
    operator fun get(prayer: Prayer): ZonedDateTime = times.getValue(prayer)
    val ordered: List<Pair<Prayer, ZonedDateTime>> get() = Prayer.entries.map { it to get(it) }
}

/** Namoz vaqtlarini telefonning o'zida, internetsiz hisoblaydi (adhan). */
object PrayerCalculator {

    fun day(input: CalcInput, date: LocalDate): DayTimes {
        val params = parameters(input)
        val pt = PrayerTimes(
            Coordinates(input.latitude, input.longitude),
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            params,
        )
        // adhan daqiqaga yaxlitlaydi, lekin millisekundlarni tozalamaydi.
        fun z(d: java.util.Date) = Instant.ofEpochMilli(d.time).truncatedTo(ChronoUnit.MINUTES).atZone(input.zone)
        return DayTimes(
            date,
            mapOf(
                Prayer.FAJR to z(pt.fajr),
                Prayer.SUNRISE to z(pt.sunrise),
                Prayer.DHUHR to z(pt.dhuhr),
                Prayer.ASR to z(pt.asr),
                Prayer.MAGHRIB to z(pt.maghrib),
                Prayer.ISHA to z(pt.isha),
            ),
        )
    }

    fun month(input: CalcInput, month: YearMonth): List<DayTimes> =
        (1..month.lengthOfMonth()).map { day(input, month.atDay(it)) }

    /** Ka'ba yo'nalishi, shimoldan soat yo'nalishida gradus. */
    fun qibla(latitude: Double, longitude: Double): Double = Qibla(Coordinates(latitude, longitude)).direction

    internal fun parameters(input: CalcInput): CalculationParameters {
        val p = baseParameters(input.method)
        p.madhab = if (input.madhab == AsrMadhab.HANAFI) Madhab.HANAFI else Madhab.SHAFI
        p.highLatitudeRule = when (input.highLatRule) {
            HighLatRule.MIDDLE_OF_THE_NIGHT -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
            HighLatRule.SEVENTH_OF_THE_NIGHT -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
            HighLatRule.TWILIGHT_ANGLE -> HighLatitudeRule.TWILIGHT_ANGLE
        }
        val a = input.adjustments
        p.adjustments = PrayerAdjustments(
            a[Prayer.FAJR] ?: 0, a[Prayer.SUNRISE] ?: 0, a[Prayer.DHUHR] ?: 0,
            a[Prayer.ASR] ?: 0, a[Prayer.MAGHRIB] ?: 0, a[Prayer.ISHA] ?: 0,
        )
        return p
    }

    private fun baseParameters(method: CalcMethod): CalculationParameters = when (method) {
        CalcMethod.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        CalcMethod.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA.parameters
        CalcMethod.DUBAI -> CalculationMethod.DUBAI.parameters
        CalcMethod.KUWAIT -> CalculationMethod.KUWAIT.parameters
        CalcMethod.QATAR -> CalculationMethod.QATAR.parameters
        CalcMethod.EGYPTIAN -> CalculationMethod.EGYPTIAN.parameters
        CalcMethod.KARACHI -> CalculationMethod.KARACHI.parameters
        CalcMethod.SINGAPORE -> CalculationMethod.SINGAPORE.parameters
        CalcMethod.NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA.parameters
        CalcMethod.MOONSIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
        // Diyanet: Bomdod 18°, Xufton 17° va rasmiy taqvimdagi tamkin tuzatishlari.
        CalcMethod.TURKEY_DIYANET -> CalculationParameters(18.0, 17.0).apply {
            methodAdjustments = PrayerAdjustments(0, -7, 5, 4, 7, 0)
        }
        // O'zbekiston musulmonlari idorasi taqvimiga moslash uchun boshlang'ich qiymatlar.
        // Rasmiy taqvim bilan solishtirib (docs/prayer-tables) aniqlashtiriladi.
        CalcMethod.UZBEKISTAN -> CalculationParameters(15.0, 15.0).apply {
            methodAdjustments = PrayerAdjustments(0, 0, 0, 0, 0, 0)
        }
    }
}
