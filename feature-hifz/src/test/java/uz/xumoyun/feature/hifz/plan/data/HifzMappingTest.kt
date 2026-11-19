package uz.xumoyun.feature.hifz.plan.data

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.HifzPlan
import uz.xumoyun.feature.hifz.plan.domain.MemoState
import uz.xumoyun.feature.hifz.plan.domain.PlanScope
import uz.xumoyun.feature.hifz.plan.domain.ReviewSession
import uz.xumoyun.feature.hifz.plan.domain.ReviewType
import java.time.Instant
import java.time.LocalDate

class HifzMappingTest {

    @Test
    fun planRoundTrips() {
        val plan = HifzPlan(id = 3, scope = PlanScope.JUZ, fromAyah = 149, toAyah = 248, dailyNewAyahs = 4,
            reminderAfterPrayer = "FAJR", reminderOffsetMin = 15, startDate = LocalDate.of(2027, 1, 10), isActive = true)
        assertEquals(plan, plan.toEntity().toDomain())
    }

    @Test
    fun progressRoundTripsIncludingNullables() {
        val withReview = AyahProgress(7, MemoState.MEMORIZED, 2.8, 17, 3, LocalDate.of(2027, 1, 27), 5,
            Instant.ofEpochMilli(1_800_000_000_000L))
        assertEquals(withReview, withReview.toEntity().toDomain())

        val fresh = AyahProgress(8, MemoState.LEARNING, 2.6, 1, 1, LocalDate.of(2027, 1, 11), null, null)
        assertEquals(fresh, fresh.toEntity().toDomain())
    }

    @Test
    fun sessionRoundTrips() {
        val s = ReviewSession(id = 9, date = LocalDate.of(2027, 1, 10), type = ReviewType.MANZIL,
            ayahFrom = 1, ayahTo = 20, durationSec = 540, avgRating = 4.5)
        assertEquals(s, s.toEntity().toDomain())
        val noRating = s.copy(avgRating = null)
        assertEquals(noRating, noRating.toEntity().toDomain())
    }
}
