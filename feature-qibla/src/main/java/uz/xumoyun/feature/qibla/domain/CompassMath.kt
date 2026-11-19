package uz.xumoyun.feature.qibla.domain

import kotlin.math.abs

/** Kompas matematikasi (TZ 8): burchaklar, qiblaga tekislanish va kalibrlash qarorlari. Android'siz. */
object CompassMath {
    /** Strelka qiblaga "tekis" deb hisoblanadigan chegara: kirish va chiqish (gisterezis, tebranish titramasin). */
    const val ENTER_TOLERANCE_DEG = 4.0
    const val EXIT_TOLERANCE_DEG = 6.0

    /** SensorManager.SENSOR_STATUS_* qiymatlari: 0 ishonchsiz, 1 past, 2 o'rtacha, 3 yuqori. */
    const val ACCURACY_UNRELIABLE = 0
    const val ACCURACY_LOW = 1

    /** Burchakni [0, 360) oralig'iga keltiradi. */
    fun normalize(deg: Double): Double {
        val r = deg % 360.0
        return if (r < 0) r + 360.0 else r
    }

    /** [heading] dan [target] gacha eng qisqa burilish, (-180, 180] gradus. Musbat — o'ngga. */
    fun signedDelta(target: Double, heading: Double): Double {
        val d = normalize(target - heading)
        return if (d > 180.0) d - 360.0 else d
    }

    /** Qiblaga tekislik. [wasAligned] — oldingi holat: tekis bo'lgandan keyin chegara kengroq. */
    fun isAligned(delta: Double, wasAligned: Boolean): Boolean {
        val a = abs(delta)
        return if (wasAligned) a <= EXIT_TOLERANCE_DEG else a < ENTER_TOLERANCE_DEG
    }

    /** Sensor aniqligi past yoki ishonchsiz bo'lsa kalibrlash ko'rsatiladi. */
    fun needsCalibration(accuracy: Int): Boolean = accuracy <= ACCURACY_LOW

    /** Burchakning silliq filtri (aylana bo'ylab, 359° → 1° sakrashsiz). [alpha] — yangi o'qish ulushi (0..1). */
    fun smooth(previous: Double, next: Double, alpha: Double): Double =
        normalize(previous + alpha * signedDelta(next, previous))
}
