package uz.xumoyun.feature.hifz.plan.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonTest {

    private val today = LocalDate.of(2027, 2, 8)
    private fun items(vararg ids: Int, type: ReviewType = ReviewType.SABAQ) = ids.map { LessonItem(it, type) }

    @Test fun `rating moves to the next unrated ayah and finishes at the end`() {
        var s = LessonState(items(1, 2, 3))
        s = s.rate(Quality.EASY)
        assertEquals(1, s.index)
        assertEquals(setOf(1), s.ratings.keys)
        s = s.rate(Quality.HARD).rate(Quality.FORGOT)
        assertTrue(s.finished)
        assertEquals(mapOf(1 to 5, 2 to 3, 3 to 1), s.ratings)
    }

    @Test fun `rating rejects values outside easy hard forgot`() {
        assertThrows(IllegalArgumentException::class.java) { LessonState(items(1)).rate(4) }
    }

    @Test fun `navigation is clamped and resets revealed words`() {
        val s = LessonState(items(1, 2), revealedWords = 3)
        assertEquals(0, s.previous().index)
        assertEquals(1, s.next().index)
        assertEquals(1, s.next().next().index)
        assertEquals(0, s.next().revealedWords)
    }

    @Test fun `empty lesson is never finished and has no current item`() {
        val s = LessonState(emptyList())
        assertNull(s.current)
        assertFalse(s.finished)
        assertEquals(0, s.moveTo(5).index)
    }

    @Test fun `hidden mode reveals one word at a time and is capped`() {
        var s = LessonState(items(1)).toggleHidden()
        assertTrue(s.hidden)
        s = s.revealNextWord(2).revealNextWord(2).revealNextWord(2)
        assertEquals(2, s.revealedWords)
        val toggled = s.toggleHidden()
        assertFalse(toggled.hidden)
        assertEquals(0, toggled.revealedWords)
    }

    @Test fun `arabic words split on any whitespace`() {
        assertEquals(listOf("a", "b", "c"), arabicWords("  a   b\tc "))
        assertEquals(emptyList<String>(), arabicWords("   "))
    }

    @Test fun `lesson items keep the first step per ayah and sabaq sabqi manzil order`() {
        val lesson = DailyLesson(sabaq = listOf(5, 6), sabqi = listOf(4, 5), manzil = listOf(1, 4))
        assertEquals(
            listOf(
                LessonItem(5, ReviewType.SABAQ), LessonItem(6, ReviewType.SABAQ),
                LessonItem(4, ReviewType.SABQI), LessonItem(1, ReviewType.MANZIL),
            ),
            lessonItemsOf(lesson),
        )
    }

    @Test fun `sessions group contiguous ayahs per step with average rating`() {
        val list = items(1, 2, 3) + items(9, type = ReviewType.MANZIL)
        val ratings = mapOf(1 to 5, 2 to 3, 3 to 5, 9 to 1)
        val sessions = lessonSessions(today, list, ratings, durationSec = 80)
        assertEquals(2, sessions.size)
        val sabaq = sessions.single { it.type == ReviewType.SABAQ }
        assertEquals(1, sabaq.ayahFrom)
        assertEquals(3, sabaq.ayahTo)
        assertEquals(13.0 / 3, sabaq.avgRating!!, 1e-9)
        assertEquals(60, sabaq.durationSec) // 80 * 3 / 4
        val manzil = sessions.single { it.type == ReviewType.MANZIL }
        assertEquals(20, manzil.durationSec) // 80 * 1 / 4
        assertEquals(1.0, manzil.avgRating!!, 1e-9)
    }

    @Test fun `unrated ayahs are not recorded and split runs when a gap appears`() {
        val list = items(1, 2, 3, 4, 5)
        val sessions = lessonSessions(today, list, mapOf(1 to 5, 2 to 5, 4 to 3, 5 to 1), durationSec = 0)
        assertEquals(listOf(1 to 2, 4 to 5), sessions.map { it.ayahFrom to it.ayahTo })
        assertEquals(listOf(0, 0), sessions.map { it.durationSec })
    }

    @Test fun `no ratings means no sessions`() {
        assertTrue(lessonSessions(today, items(1, 2), emptyMap(), 10).isEmpty())
    }

    @Test fun `contiguous runs of sorted ids`() {
        assertEquals(listOf(listOf(1, 2, 3), listOf(7), listOf(9, 10)), contiguousRuns(listOf(1, 2, 3, 7, 9, 10)))
        assertEquals(emptyList<List<Int>>(), contiguousRuns(emptyList()))
    }
}
