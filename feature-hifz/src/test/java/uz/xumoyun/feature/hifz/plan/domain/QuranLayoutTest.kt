package uz.xumoyun.feature.hifz.plan.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import uz.xumoyun.feature.hifz.ui.SyntheticQuran

class QuranLayoutTest {

    private val layout = SyntheticQuran.layout
    private val today = LocalDate.of(2027, 2, 8)

    @Test fun `juz boundaries map to the right juz`() {
        assertEquals(1, layout.juzOf(1))
        assertEquals(1, layout.juzOf(207))
        assertEquals(2, layout.juzOf(208))
        assertEquals(30, layout.juzOf(QURAN_AYAH_COUNT))
    }

    @Test fun `juz ranges are contiguous and cover the whole Quran`() {
        val ranges = (1..30).map { layout.juzRange(it) }
        assertEquals(1, ranges.first().from)
        assertEquals(QURAN_AYAH_COUNT, ranges.last().to)
        ranges.zipWithNext().forEach { (a, b) -> assertEquals(a.to + 1, b.from) }
        assertEquals(QURAN_AYAH_COUNT, ranges.sumOf { it.count })
    }

    @Test fun `locate converts a global id to surah and ayah number`() {
        assertEquals(AyahLocation(1, 1), layout.locate(1))
        assertEquals(AyahLocation(2, 1), layout.locate(56))
        assertEquals(AyahLocation(114, 54), layout.locate(QURAN_AYAH_COUNT))
    }

    @Test fun `surah range matches its ayah count`() {
        assertEquals(AyahRange(56, 110), layout.surahRange(2))
    }

    @Test fun `plan built for a juz covers exactly that juz`() {
        val plan = newHifzPlan(PlanTarget.Juz(1), layout, 5, null, 0, today)
        assertEquals(1, plan.fromAyah)
        assertEquals(207, plan.toAyah)
        assertEquals(PlanScope.JUZ, plan.scope)
        assertEquals(today, plan.startDate)
        assertEquals(PlanTarget.Juz(1), layout.targetOf(plan))
    }

    @Test fun `surah target round-trips through the stored plan`() {
        val plan = newHifzPlan(PlanTarget.Surah(2), layout, 3, "ASR", 15, today)
        assertEquals(PlanTarget.Surah(2), layout.targetOf(plan))
        assertEquals("ASR", plan.reminderAfterPrayer)
        assertEquals(15, plan.reminderOffsetMin)
    }

    @Test fun `whole Quran plan`() {
        val plan = newHifzPlan(PlanTarget.All, layout, 10, null, 0, today)
        assertEquals(AyahRange(1, QURAN_AYAH_COUNT), AyahRange(plan.fromAyah, plan.toAyah))
        assertEquals(PlanTarget.All, layout.targetOf(plan))
    }

    @Test fun `target lookup returns null for a range that matches nothing`() {
        val plan = HifzPlan(
            scope = PlanScope.SURAH, fromAyah = 57, toAyah = 60, dailyNewAyahs = 1,
            reminderAfterPrayer = null, reminderOffsetMin = 0, startDate = today, isActive = true,
        )
        assertNull(layout.targetOf(plan))
    }

    @Test fun `ayah range validates bounds`() {
        assertThrows(IllegalArgumentException::class.java) { AyahRange(0, 5) }
        assertThrows(IllegalArgumentException::class.java) { AyahRange(5, 4) }
        assertThrows(IllegalArgumentException::class.java) { AyahRange(1, QURAN_AYAH_COUNT + 1) }
        assertEquals(3, AyahRange(1, 3).count)
    }
}
