package uz.xumoyun.feature.hifz.plan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class Sm2Test {

    private val eps = 1e-9

    @Test
    fun easyThreeTimesGivesIntervals1_6_17AndEf26_27_28() {
        // TZ 7-bo'lim: q = 5 ketma-ket 3 marta → interval 1, 6, 17 kun (EF 2.6, 2.7, 2.8)
        var card = Sm2Card.NEW
        card = sm2Next(card, Quality.EASY)
        assertEquals(1, card.intervalDays)
        assertEquals(2.6, card.easeFactor, eps)
        card = sm2Next(card, Quality.EASY)
        assertEquals(6, card.intervalDays)
        assertEquals(2.7, card.easeFactor, eps)
        card = sm2Next(card, Quality.EASY)
        assertEquals(17, card.intervalDays)
        assertEquals(2.8, card.easeFactor, eps)
        assertEquals(3, card.repetitions)
    }

    @Test
    fun forgotResetsRepetitionsAndIntervalToOneDay() {
        // TZ 7-bo'lim: q = 1 → interval 1, repetitions 0
        val after = sm2Next(Sm2Card(2.7, 17, 3), Quality.FORGOT)
        assertEquals(1, after.intervalDays)
        assertEquals(0, after.repetitions)
        // EF: 2.7 + 0.1 − 4(0.08 + 0.08) = 2.16
        assertEquals(2.16, after.easeFactor, eps)
    }

    @Test
    fun hardRatingCountsAsCorrectRepetition() {
        // q = 3 — "qiyin" lekin to'g'ri javob (q >= 3): takror sanaladi
        val hard = sm2Next(Sm2Card.NEW, Quality.HARD)
        assertEquals(1, hard.repetitions)
        assertEquals(1, hard.intervalDays)
        assertEquals(2.36, hard.easeFactor, eps)
    }

    @Test
    fun easeFactorNeverDropsBelow13() {
        var card = Sm2Card.NEW
        repeat(20) { card = sm2Next(card, Quality.FORGOT) }
        assertEquals(1.3, card.easeFactor, eps)
        repeat(5) { card = sm2Next(card, 0) }
        assertEquals(1.3, card.easeFactor, eps)
    }

    @Test
    fun intervalUsesNewEaseFactor() {
        // 3-takrorda interval = round(6 × EF'), EF' = 2.8 → 17 (eski EF 2.7 bo'lganda 16 bo'lardi)
        val card = sm2Next(Sm2Card(2.7, 6, 2), Quality.EASY)
        assertEquals(17, card.intervalDays)
    }

    @Test
    fun invalidQualityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { sm2Next(Sm2Card.NEW, 6) }
        assertThrows(IllegalArgumentException::class.java) { sm2Next(Sm2Card.NEW, -1) }
    }

    @Test
    fun reviewAyahSetsDueDateAndState() {
        val clock = FixedTimeSource(Instant.parse("2027-01-10T06:00:00Z"), ZoneId.of("UTC"))
        val today = clock.today()

        val first = reviewAyah(1, null, Quality.EASY, today, clock.now())
        assertEquals(MemoState.LEARNING, first.state)
        assertEquals(LocalDate.of(2027, 1, 11), first.dueDate)
        assertEquals(Quality.EASY, first.lastRating)
        assertEquals(clock.now(), first.lastReviewedAt)
    }

    @Test
    fun reviewAyahBecomesMemorizedAtThirtyDaysWithGoodRating() {
        val today = LocalDate.of(2027, 1, 10)
        val now = Instant.parse("2027-01-10T06:00:00Z")
        // interval 12 × EF' 2.6 = 31 (>= 30), baho 5 → MEMORIZED
        val prev = AyahProgress(5, MemoState.LEARNING, 2.5, 12, 4, today, Quality.EASY, now)
        val next = reviewAyah(5, prev, Quality.EASY, today, now)
        assertEquals(MemoState.MEMORIZED, next.state)
        assertEquals(today.plusDays(next.intervalDays.toLong()), next.dueDate)
    }

    @Test
    fun reviewAyahStaysLearningWithHardRatingEvenIfIntervalLong() {
        val today = LocalDate.of(2027, 1, 10)
        val now = Instant.parse("2027-01-10T06:00:00Z")
        val prev = AyahProgress(5, MemoState.MEMORIZED, 2.5, 40, 6, today, Quality.EASY, now)
        val next = reviewAyah(5, prev, Quality.HARD, today, now)
        // q = 3 < 4 → MEMORIZED bo'lmaydi
        assertEquals(MemoState.LEARNING, next.state)
    }

    @Test
    fun reviewAyahRejectsMismatchedId() {
        val now = Instant.parse("2027-01-10T06:00:00Z")
        val prev = AyahProgress(5, MemoState.LEARNING, 2.5, 1, 1, LocalDate.of(2027, 1, 10), 5, now)
        assertThrows(IllegalArgumentException::class.java) {
            reviewAyah(6, prev, Quality.EASY, LocalDate.of(2027, 1, 10), now)
        }
    }
}
