package uz.xumoyun.feature.hifz.audio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTest {

    @Test
    fun speedIsClampedTo075_125() {
        assertEquals(0.75f, PlaybackSpeed.clamp(0.5f), 0f)
        assertEquals(1.25f, PlaybackSpeed.clamp(2f), 0f)
        assertEquals(1f, PlaybackSpeed.clamp(Float.NaN), 0f)
        assertEquals(1.1f, PlaybackSpeed.clamp(1.1f), 0.0001f)
        assertEquals(0.75f, PlaybackSpeed.clamp(0.75f), 0f)
        assertEquals(1.25f, PlaybackSpeed.clamp(1.25f), 0f)
    }

    @Test
    fun speedStepsAreEleven() {
        assertEquals(11, PlaybackSpeed.steps.size)
        assertEquals(0.75f, PlaybackSpeed.steps.first(), 0f)
        assertEquals(1.25f, PlaybackSpeed.steps.last(), 0.0001f)
        assertTrue(PlaybackSpeed.steps.all { it in PlaybackSpeed.MIN..PlaybackSpeed.MAX })
    }

    @Test
    fun rangeRejectsInvalid() {
        assertThrows(IllegalArgumentException::class.java) { RepeatRange(3, 5, 0) }
        assertThrows(IllegalArgumentException::class.java) { RepeatRange(3, 5, RepeatRange.MAX_TIMES + 1) }
        assertThrows(IllegalArgumentException::class.java) { RepeatRange(5, 3, 1) }
        assertThrows(IllegalArgumentException::class.java) { RepeatRange(1, MAX_AYAHS_PER_SURAH + 1, 1) }
    }

    @Test
    fun playOrderRepeatsRange() {
        assertEquals(listOf(3, 4, 5, 3, 4, 5), playOrder(RepeatRange(3, 5, 2)))
    }

    @Test
    fun singleAyahRepeatsItself() {
        assertEquals(listOf(7, 7, 7), playOrder(RepeatRange(7, 7, 3)))
        assertEquals(listOf(7), playOrder(RepeatRange(7, 7, 1)))
    }

    @Test
    fun playOrderLengthIsRangeTimesCount() {
        val r = RepeatRange(10, 20, 4)
        assertEquals(11 * 4, playOrder(r).size)
    }

    @Test
    fun repeatCounterStopsAfterTimes() {
        val c = RepeatCounter(3)
        assertFalse(c.isFinished)
        assertTrue(c.onPassEnded())
        assertTrue(c.onPassEnded())
        assertFalse(c.onPassEnded())
        assertTrue(c.isFinished)
        assertFalse(c.onPassEnded())
    }

    @Test
    fun repeatCounterOnceDoesNotRepeat() {
        val c = RepeatCounter(1)
        assertFalse(c.onPassEnded())
        assertTrue(c.isFinished)
    }
}
