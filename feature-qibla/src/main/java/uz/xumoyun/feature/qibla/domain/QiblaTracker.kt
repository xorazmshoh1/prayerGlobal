package uz.xumoyun.feature.qibla.domain

/**
 * Qibla kompasining holati: kelgan o'qishlarni silliqlaydi, siferblat burchagini uzluksiz hisoblaydi
 * (360° o'tishda sakramaydi) va qiblaga tekislikni kuzatadi. Android'siz.
 */
class QiblaTracker(private val bearing: Double, private val alpha: Double = DEFAULT_ALPHA) {
    private var heading: Double? = null
    private var unwrapped = 0.0

    /** Oxirgi o'qish sifati (SensorManager.SENSOR_STATUS_*). */
    var accuracy: Int = CompassMath.ACCURACY_UNRELIABLE
        private set

    var aligned: Boolean = false
        private set

    /** Silliqlangan haqiqiy shimol azimuti; hali o'qish bo'lmasa null. */
    val headingDeg: Double? get() = heading

    /** Siferblat aylanishi: shimol doim yuqorida turishi uchun -heading (uzluksiz, animatsiya uchun). */
    val dialRotationDeg: Double get() = -unwrapped

    val needsCalibration: Boolean
        get() = heading != null && CompassMath.needsCalibration(accuracy)

    /** Yangi o'qish. Qiblaga endi kirilgan bo'lsa true qaytaradi (bir marta — tebranish uchun). */
    fun onReading(azimuth: Double, accuracy: Int): Boolean {
        val previous = heading
        val next = if (previous == null) CompassMath.normalize(azimuth) else CompassMath.smooth(previous, azimuth, alpha)
        unwrapped = if (previous == null) next else unwrapped + CompassMath.signedDelta(next, previous)
        heading = next
        this.accuracy = accuracy
        val wasAligned = aligned
        aligned = CompassMath.isAligned(CompassMath.signedDelta(bearing, next), wasAligned)
        return !wasAligned && aligned
    }

    companion object {
        const val DEFAULT_ALPHA = 0.3
    }
}
