package uz.xumoyun.feature.qibla.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaTrackerTest {
    @Test fun noHeadingBeforeFirstReading() {
        val t = QiblaTracker(bearing = 100.0)
        assertNull(t.headingDeg)
        assertFalse(t.needsCalibration)
        assertFalse(t.aligned)
    }

    @Test fun entersAlignmentOnlyOnce() {
        val t = QiblaTracker(bearing = 100.0, alpha = 1.0)
        assertTrue(t.onReading(100.0, accuracy = 3))
        assertTrue(t.aligned)
        assertFalse(t.onReading(100.5, accuracy = 3))
        assertTrue(t.aligned)
    }

    @Test fun hysteresisKeepsAlignmentInsideExitTolerance() {
        val t = QiblaTracker(bearing = 100.0, alpha = 1.0)
        assertTrue(t.onReading(100.0, accuracy = 3))
        assertFalse(t.onReading(105.0, accuracy = 3))   // 5° — hali tekis
        assertTrue(t.aligned)
        assertFalse(t.onReading(107.0, accuracy = 3))   // 7° — chiqdi
        assertFalse(t.aligned)
        assertTrue(t.onReading(100.0, accuracy = 3))    // qayta kirdi — yana bir marta signal
    }

    @Test fun dialRotationIsContinuousAcrossNorth() {
        val t = QiblaTracker(bearing = 0.0, alpha = 1.0)
        t.onReading(359.0, accuracy = 3)
        assertEquals(-359.0, t.dialRotationDeg, 1e-9)
        t.onReading(1.0, accuracy = 3)
        assertEquals(1.0, t.headingDeg!!, 1e-9)
        assertEquals(-361.0, t.dialRotationDeg, 1e-9)
    }

    @Test fun lowAccuracyRequestsCalibrationOnlyAfterReading() {
        val t = QiblaTracker(bearing = 0.0, alpha = 1.0)
        t.onReading(50.0, accuracy = 1)
        assertTrue(t.needsCalibration)
        t.onReading(50.0, accuracy = 3)
        assertFalse(t.needsCalibration)
    }

    @Test fun farFromQiblaIsNotAligned() {
        val t = QiblaTracker(bearing = 240.3, alpha = 1.0)
        assertFalse(t.onReading(60.0, accuracy = 3))
        assertFalse(t.aligned)
    }
}
