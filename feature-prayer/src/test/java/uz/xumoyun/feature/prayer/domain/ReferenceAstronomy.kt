package uz.xumoyun.feature.prayer.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/**
 * Mustaqil tekshiruv uchun soddalashtirilgan astronomik hisob (PrayTimes.org algoritmi:
 * quyosh o'rni USNO taqribiy formulasi bilan). adhan esa Meeus formulalaridan foydalanadi —
 * ikkala natija bir-biriga mos kelishi usul parametrlari to'g'ri ulanganini ko'rsatadi.
 * Faqat testlarda ishlatiladi.
 */
object ReferenceAstronomy {

    /** Usulning e'lon qilingan parametrlari (manba: tegishli idoralar; adhan hujjati bilan bir xil). */
    data class Method(
        val fajrAngle: Double,
        val ishaAngle: Double = 0.0,
        val ishaMinutes: Int = 0,
        /** Peshinga qo'shiladigan xavfsizlik daqiqasi (MWL, Misr, Karachi, ISNA, Singapur: +1). */
        val dhuhrMinutes: Int = 0,
    )

    val MWL = Method(18.0, 17.0, dhuhrMinutes = 1)
    val EGYPTIAN = Method(19.5, 17.5, dhuhrMinutes = 1)
    val KARACHI = Method(18.0, 18.0, dhuhrMinutes = 1)
    val ISNA = Method(15.0, 15.0, dhuhrMinutes = 1)
    val SINGAPORE = Method(20.0, 18.0, dhuhrMinutes = 1)
    val UMM_AL_QURA = Method(18.5, ishaMinutes = 90)

    fun times(lat: Double, lng: Double, zone: ZoneId, date: LocalDate, m: Method, asrShadow: Int): Map<Prayer, LocalTime> {
        val tz = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15 * 24.0)

        fun sun(jd: Double): Pair<Double, Double> { // (declination, equation of time)
            val d = jd - 2451545.0
            val g = fixAngle(357.529 + 0.98560028 * d)
            val q = fixAngle(280.459 + 0.98564736 * d)
            val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            val e = 23.439 - 0.00000036 * d
            val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15
            val eqt = q / 15 - fixHour(ra)
            val decl = darcsin(dsin(e) * dsin(l))
            return decl to eqt
        }

        fun midDay(t: Double) = fixHour(12 - sun(jDate + t).second)

        fun angleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sun(jDate + t).first
            val noon = midDay(t)
            val h = darccos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))) / 15
            return noon + if (ccw) -h else h
        }

        // Soya uzunligi peshindagi soyaga nisbatan olinadi, shuning uchun og'ish peshin vaqtidagi.
        fun asr(t: Double, noon: Double): Double {
            val decl = sun(jDate + noon).first
            val angle = -darccot(asrShadow + dtan(abs(lat - decl)))
            return angleTime(angle, t, false)
        }

        // Ikki iteratsiya: taxminiy vaqtlardan boshlab aniqlashtiriladi.
        var t = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0)
        repeat(4) {
            val p = t.map { it / 24 }
            t = doubleArrayOf(
                angleTime(m.fajrAngle, p[0], true),
                angleTime(0.833, p[1], true),
                midDay(p[2]),
                asr(p[3], p[2]),
                angleTime(0.833, p[4], false),
                if (m.ishaMinutes > 0) 0.0 else angleTime(m.ishaAngle, p[5], false),
            )
        }
        val shift = tz - lng / 15
        val fajr = t[0] + shift
        val sunrise = t[1] + shift
        val dhuhr = t[2] + shift + m.dhuhrMinutes / 60.0
        val asrT = t[3] + shift
        val maghrib = t[4] + shift
        val isha = if (m.ishaMinutes > 0) maghrib + m.ishaMinutes / 60.0 else t[5] + shift
        return mapOf(
            Prayer.FAJR to fajr, Prayer.SUNRISE to sunrise, Prayer.DHUHR to dhuhr,
            Prayer.ASR to asrT, Prayer.MAGHRIB to maghrib, Prayer.ISHA to isha,
        ).mapValues { (_, h) -> LocalTime.ofSecondOfDay(((fixHour(h) * 60).roundToLong() * 60) % 86400) }
    }

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year
        var mo = month
        if (mo <= 2) { y -= 1; mo += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (mo + 1)) + day + b - 1524.5
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1 / x))
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)
    private fun fix(a: Double, b: Double): Double {
        val r = a - b * floor(a / b)
        return if (r < 0) r + b else r
    }
}
