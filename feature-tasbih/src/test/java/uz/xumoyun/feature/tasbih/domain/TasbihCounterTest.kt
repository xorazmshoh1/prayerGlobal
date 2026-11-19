package uz.xumoyun.feature.tasbih.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasbihCounterTest {
    @Test fun reachedOnlyAtMultiplesOfTarget() {
        assertTrue(TasbihCounter.reached(33, 33))
        assertTrue(TasbihCounter.reached(66, 33))
        assertFalse(TasbihCounter.reached(34, 33))
        assertFalse(TasbihCounter.reached(0, 33))
        assertFalse(TasbihCounter.reached(33, 0))
    }

    @Test fun progressFillsRingPerRoundAndRestarts() {
        assertEquals(0f, TasbihCounter.progress(0, 33), 1e-6f)
        assertEquals(1f / 33, TasbihCounter.progress(1, 33), 1e-6f)
        assertEquals(1f, TasbihCounter.progress(33, 33), 1e-6f)
        assertEquals(1f / 33, TasbihCounter.progress(34, 33), 1e-6f)
        assertEquals(0f, TasbihCounter.progress(5, 0), 1e-6f)
    }

    @Test fun roundsCountCompletedGoals() {
        assertEquals(0, TasbihCounter.rounds(32, 33))
        assertEquals(2, TasbihCounter.rounds(66, 33))
        assertEquals(0, TasbihCounter.rounds(10, 0))
    }

    @Test fun validTargetRange() {
        assertNull(TasbihCounter.validTarget(null))
        assertNull(TasbihCounter.validTarget(0))
        assertEquals(1, TasbihCounter.validTarget(1))
        assertEquals(100_000, TasbihCounter.validTarget(100_000))
        assertNull(TasbihCounter.validTarget(100_001))
    }

    @Test fun presetsMatchSpec() {
        assertEquals(listOf(33, 99, 100), TasbihCounter.PRESET_TARGETS)
    }
}
