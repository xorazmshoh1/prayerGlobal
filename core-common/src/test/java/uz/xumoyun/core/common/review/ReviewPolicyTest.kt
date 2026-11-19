package uz.xumoyun.core.common.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReviewPolicyTest {
    private val today = LocalDate.of(2027, 2, 20)

    @Test fun sevenDayHifzStreakTriggers() {
        assertFalse(ReviewPolicy.shouldRequest(today, null, hifzStreakDays = 6, ramadanDay = null))
        assertTrue(ReviewPolicy.shouldRequest(today, null, hifzStreakDays = 7, ramadanDay = null))
    }

    @Test fun tenthRamadanDayTriggersOnlyOnThatDay() {
        assertTrue(ReviewPolicy.shouldRequest(today, null, hifzStreakDays = 0, ramadanDay = 10))
        assertFalse(ReviewPolicy.shouldRequest(today, null, hifzStreakDays = 0, ramadanDay = 9))
        assertFalse(ReviewPolicy.shouldRequest(today, null, hifzStreakDays = 0, ramadanDay = null))
    }

    @Test fun atMostOncePerThirtyDays() {
        assertFalse(ReviewPolicy.shouldRequest(today, LocalDate.of(2027, 2, 1), hifzStreakDays = 10, ramadanDay = null))
        assertTrue(ReviewPolicy.shouldRequest(today, LocalDate.of(2027, 1, 21), hifzStreakDays = 10, ramadanDay = null))
    }

    @Test fun nothingWithoutValuableEvent() {
        assertEquals(false, ReviewPolicy.shouldRequest(today, null, hifzStreakDays = 3, ramadanDay = 3))
    }
}
