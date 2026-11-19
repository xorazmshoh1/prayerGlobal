package uz.xumoyun.feature.qibla.data

import kotlinx.coroutines.flow.Flow

/** Bitta kompas o'qishi: haqiqiy shimolga nisbatan azimut (gradus) va sensor aniqligi. */
data class CompassReading(val azimuth: Double, val accuracy: Int)

/** Kompas manbai. Haqiqiysi — [SensorCompassSource]; testlarda soxta. */
interface CompassSource {
    /** Qurilmada kerakli sensorlar bormi (rotation vector yoki akselerometr + magnitometr). */
    fun isAvailable(): Boolean

    /** Berilgan joy uchun magnit og'ish tuzatilgan o'qishlar oqimi. */
    fun readings(latitude: Double, longitude: Double): Flow<CompassReading>
}
