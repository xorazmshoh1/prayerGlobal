package uz.xumoyun.feature.qibla.data

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Haqiqiy kompas: TYPE_ROTATION_VECTOR; bo'lmasa akselerometr + magnitometr (TZ 8).
 * Magnit shimol [GeomagneticField] og'ishi bilan haqiqiy shimolga o'tkaziladi.
 */
class SensorCompassSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : CompassSource {
    private val sensors: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun isAvailable(): Boolean =
        sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
            (sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null && sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)

    override fun readings(latitude: Double, longitude: Double): Flow<CompassReading> = callbackFlow {
        val rotation = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = if (rotation == null) sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) else null
        val magnetometer = if (rotation == null) sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) else null
        if (rotation == null && (accelerometer == null || magnetometer == null)) {
            close()
            awaitClose()
            return@callbackFlow
        }

        val declination = GeomagneticField(
            latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis(),
        ).declination.toDouble()

        val matrix = FloatArray(9)
        val inclination = FloatArray(9)
        val orientation = FloatArray(3)
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null
        var accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    Sensor.TYPE_ACCELEROMETER -> gravity = event.values.copyOf()
                    Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.copyOf()
                    else -> return
                }
                if (rotation == null) {
                    val g = gravity ?: return
                    val m = geomagnetic ?: return
                    if (!SensorManager.getRotationMatrix(matrix, inclination, g, m)) return
                }
                SensorManager.getOrientation(matrix, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble()) + declination
                trySend(CompassReading(azimuth, accuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracyValue: Int) {
                accuracy = accuracyValue
            }
        }

        if (rotation != null) {
            sensors.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensors.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensors.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }
        awaitClose { sensors.unregisterListener(listener) }
    }
}
