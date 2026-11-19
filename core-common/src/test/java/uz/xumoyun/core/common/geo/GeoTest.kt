package uz.xumoyun.core.common.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {
    /** TZ 7-bo'lim: Toshkent 240.3°, Jakarta 295.2°, London 119.0° (±1°). */
    @Test fun qiblaBearingMatchesSpec() {
        assertEquals(240.3, Geo.qiblaBearing(41.2995, 69.2401), 1.0)
        assertEquals(295.2, Geo.qiblaBearing(-6.2088, 106.8456), 1.0)
        assertEquals(119.0, Geo.qiblaBearing(51.5074, -0.1278), 1.0)
    }

    @Test fun bearingIsAlwaysInZeroToThreeSixty() {
        for (lat in -89..89 step 7) for (lon in -179..179 step 11) {
            val b = Geo.qiblaBearing(lat.toDouble(), lon.toDouble())
            assertTrue("bearing $b at $lat,$lon", b >= 0.0 && b < 360.0)
        }
    }

    @Test fun distanceToKaabaFromMeccaIsZero() {
        assertEquals(0.0, Geo.qiblaDistanceKm(21.4225, 39.8262), 1e-6)
    }

    @Test fun oneDegreeOnEquatorIsAboutOneHundredElevenKm() {
        assertEquals(111.195, Geo.distanceKm(0.0, 0.0, 0.0, 1.0), 0.01)
    }

    @Test fun distanceIsSymmetric() {
        val ab = Geo.distanceKm(41.2995, 69.2401, 51.5074, -0.1278)
        val ba = Geo.distanceKm(51.5074, -0.1278, 41.2995, 69.2401)
        assertEquals(ab, ba, 1e-9)
    }

    /** Toshkent → Ka'ba: 3531.4 km (mustaqil Python hisobi, haversine, R = 6371 km). */
    @Test fun tashkentToKaabaDistance() {
        assertEquals(3531.4, Geo.qiblaDistanceKm(41.2995, 69.2401), 1.0)
    }
}
