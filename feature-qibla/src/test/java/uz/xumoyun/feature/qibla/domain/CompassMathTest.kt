package uz.xumoyun.feature.qibla.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CompassMathTest {
    @Test fun normalizeKeepsAngleInZeroToThreeSixty() {
        assertEquals(350.0, CompassMath.normalize(-10.0), 1e-9)
        assertEquals(10.0, CompassMath.normalize(370.0), 1e-9)
        assertEquals(0.0, CompassMath.normalize(360.0), 1e-9)
        assertEquals(0.0, CompassMath.normalize(0.0), 1e-9)
    }

    @Test fun signedDeltaTakesShortestTurn() {
        assertEquals(20.0, CompassMath.signedDelta(target = 10.0, heading = 350.0), 1e-9)
        assertEquals(-20.0, CompassMath.signedDelta(target = 350.0, heading = 10.0), 1e-9)
        assertEquals(0.0, CompassMath.signedDelta(target = 123.0, heading = 123.0), 1e-9)
    }

    @Test fun signedDeltaRangeIsHalfOpen() {
        assertEquals(180.0, CompassMath.signedDelta(target = 180.0, heading = 0.0), 1e-9)
        assertEquals(180.0, CompassMath.signedDelta(target = 0.0, heading = 180.0), 1e-9)
    }

    @Test fun enterToleranceIsStrict() {
        assertTrue(CompassMath.isAligned(3.9, wasAligned = false))
        assertFalse(CompassMath.isAligned(4.0, wasAligned = false))
        assertTrue(CompassMath.isAligned(-3.9, wasAligned = false))
    }

    @Test fun exitToleranceIsWiderThanEnter() {
        assertTrue(CompassMath.isAligned(5.9, wasAligned = true))
        assertTrue(CompassMath.isAligned(6.0, wasAligned = true))
        assertFalse(CompassMath.isAligned(6.1, wasAligned = true))
        assertFalse(CompassMath.isAligned(-6.1, wasAligned = true))
    }

    @Test fun calibrationNeededForUnreliableAndLowAccuracy() {
        assertTrue(CompassMath.needsCalibration(0))
        assertTrue(CompassMath.needsCalibration(1))
        assertFalse(CompassMath.needsCalibration(2))
        assertFalse(CompassMath.needsCalibration(3))
    }

    @Test fun smoothingCrossesNorthWithoutJumping() {
        val s = CompassMath.smooth(350.0, 10.0, 0.5)
        assertTrue("got $s", abs(CompassMath.signedDelta(s, 0.0)) < 1e-9)
    }

    @Test fun smoothingWithSameValueIsIdentity() {
        assertEquals(90.0, CompassMath.smooth(90.0, 90.0, 0.3), 1e-9)
    }
}
