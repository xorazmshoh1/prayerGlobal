package uz.xumoyun.feature.hifz.plan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class PlanModelsTest {

    private val start = LocalDate.of(2027, 1, 10)

    @Test
    fun planAyahIdsAreInclusiveRange() {
        val plan = HifzPlan(scope = PlanScope.SURAH, fromAyah = 1, toAyah = 7, dailyNewAyahs = 3,
            reminderAfterPrayer = null, reminderOffsetMin = 0, startDate = start, isActive = true)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), plan.ayahIds)
    }

    @Test
    fun planRejectsInvalidRanges() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzPlan(scope = PlanScope.ALL, fromAyah = 0, toAyah = 5, dailyNewAyahs = 1,
                reminderAfterPrayer = null, reminderOffsetMin = 0, startDate = start, isActive = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            HifzPlan(scope = PlanScope.ALL, fromAyah = 10, toAyah = 5, dailyNewAyahs = 1,
                reminderAfterPrayer = null, reminderOffsetMin = 0, startDate = start, isActive = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            HifzPlan(scope = PlanScope.ALL, fromAyah = 1, toAyah = 6237, dailyNewAyahs = 1,
                reminderAfterPrayer = null, reminderOffsetMin = 0, startDate = start, isActive = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            HifzPlan(scope = PlanScope.ALL, fromAyah = 1, toAyah = 5, dailyNewAyahs = 0,
                reminderAfterPrayer = null, reminderOffsetMin = 0, startDate = start, isActive = true)
        }
    }

    @Test
    fun reviewSessionRejectsInvalidRange() {
        assertThrows(IllegalArgumentException::class.java) {
            ReviewSession(date = start, type = ReviewType.SABAQ, ayahFrom = 9, ayahTo = 3, durationSec = 0, avgRating = null)
        }
    }
}
