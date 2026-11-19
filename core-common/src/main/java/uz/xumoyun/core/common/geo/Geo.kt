package uz.xumoyun.core.common.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geografik hisoblar (TZ 7-bo'lim): qibla azimuti va Ka'bagacha masofa. Toza Kotlin, Android'siz.
 * Formulalar TZ 7-bo'limda: qibla — sferik azimut, masofa — haversine (R = 6371 km).
 */
object Geo {
    /** Ka'baning koordinatalari (TZ 7-bo'lim). */
    const val KAABA_LATITUDE = 21.4225
    const val KAABA_LONGITUDE = 39.8262
    const val EARTH_RADIUS_KM = 6371.0

    /**
     * Qibla azimuti: shimoldan soat yo'nalishida gradus, [0, 360).
     * θ = atan2(sin Δλ · cos φk, cos φ · sin φk − sin φ · cos φk · cos Δλ), Δλ = λk − λ.
     */
    fun qiblaBearing(latitude: Double, longitude: Double): Double {
        val phi = Math.toRadians(latitude)
        val phiK = Math.toRadians(KAABA_LATITUDE)
        val dLambda = Math.toRadians(KAABA_LONGITUDE - longitude)
        val y = sin(dLambda) * cos(phiK)
        val x = cos(phi) * sin(phiK) - sin(phi) * cos(phiK) * cos(dLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Ka'bagacha masofa, km (haversine). */
    fun qiblaDistanceKm(latitude: Double, longitude: Double): Double =
        distanceKm(latitude, longitude, KAABA_LATITUDE, KAABA_LONGITUDE)

    /** Ikki nuqta orasidagi katta doira masofasi, km (haversine). */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dPhi = p2 - p1
        val dLambda = Math.toRadians(lon2 - lon1)
        val a = sin(dPhi / 2) * sin(dPhi / 2) + cos(p1) * cos(p2) * sin(dLambda / 2) * sin(dLambda / 2)
        return 2 * EARTH_RADIUS_KM * atan2(sqrt(a), sqrt(1 - a))
    }
}
