package uz.xumoyun.feature.prayer.tracker.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tracker.FakeTrackerPrefs
import uz.xumoyun.feature.prayer.tracker.FakeTrackerStore
import uz.xumoyun.feature.prayer.tracker.DirectTransactions
import uz.xumoyun.feature.prayer.tracker.domain.PrayerStatus
import uz.xumoyun.feature.prayer.tracker.domain.QadaPrayer
import uz.xumoyun.feature.prayer.tracker.domain.QadaSource
import java.time.LocalDate

class TrackerRepositoryTest {
    private val day = LocalDate.of(2026, 11, 19)
    private val store = FakeTrackerStore()
    private val prefs = FakeTrackerPrefs(autoQada = true)
    private val repo = TrackerRepository(store, DirectTransactions, prefs)

    private suspend fun remaining(p: QadaPrayer) = repo.counters.first().first { it.prayer == p }.remaining

    @Test fun missedIncrementsQadaByOneWithAutoSource() = runTest {
        repo.setStatus(day, Prayer.DHUHR, PrayerStatus.MISSED, now = 1)
        assertEquals(1, remaining(QadaPrayer.DHUHR))
        val e = store.allEvents().single()
        assertEquals(QadaSource.AUTO_MISSED, e.source)
        assertEquals(1, e.delta)
    }

    @Test fun repeatedMissedDoesNotIncrementTwice() = runTest {
        repo.setStatus(day, Prayer.ASR, PrayerStatus.MISSED, now = 1)
        repo.setStatus(day, Prayer.ASR, PrayerStatus.MISSED, now = 2)
        assertEquals(1, remaining(QadaPrayer.ASR))
    }

    @Test fun missedDoesNothingWhenAutoQadaOff() = runTest {
        prefs.setAutoQadaOnMissed(false)
        repo.setStatus(day, Prayer.FAJR, PrayerStatus.MISSED, now = 1)
        assertEquals(0, remaining(QadaPrayer.FAJR))
        assertTrue(store.allEvents().isEmpty())
    }

    @Test fun prayedDoesNotTouchQada() = runTest {
        repo.setStatus(day, Prayer.ISHA, PrayerStatus.PRAYED, now = 1)
        assertEquals(0, remaining(QadaPrayer.ISHA))
    }

    @Test fun clearingStatusDeletesLogAndDoesNotReverseQada() = runTest {
        repo.setStatus(day, Prayer.MAGHRIB, PrayerStatus.MISSED, now = 1)
        repo.setStatus(day, Prayer.MAGHRIB, null, now = 2)
        assertEquals(null, store.getLog(day, Prayer.MAGHRIB))
        assertEquals(1, remaining(QadaPrayer.MAGHRIB))
    }

    @Test fun manualMinusAtZeroStaysZeroAndRecordsNothing() = runTest {
        assertEquals(0, repo.adjust(QadaPrayer.FAJR, -1, now = 1))
        assertEquals(0, remaining(QadaPrayer.FAJR))
        assertTrue(store.allEvents().isEmpty())
    }

    @Test fun undoRevertsLastChangeOnly() = runTest {
        repo.adjust(QadaPrayer.ISHA, 3, now = 1)
        repo.adjust(QadaPrayer.ISHA, -1, now = 2)
        assertTrue(repo.undoLast(now = 3))
        assertEquals(3, remaining(QadaPrayer.ISHA))
        assertTrue(repo.undoLast(now = 4))
        assertEquals(0, remaining(QadaPrayer.ISHA))
        assertFalse(repo.undoLast(now = 5))
    }

    @Test fun undoWithNoHistoryReturnsFalse() = runTest {
        assertFalse(repo.undoLast(now = 1))
    }

    @Test fun wizardAddsPerPrayerForShafiWithoutWitr() = runTest {
        val perPrayer = 366
        repo.applyEstimate(perPrayer, AsrMadhab.SHAFI, now = 1)
        assertEquals(366, remaining(QadaPrayer.FAJR))
        assertEquals(366, remaining(QadaPrayer.ISHA))
        assertEquals(0, remaining(QadaPrayer.WITR))
        assertEquals(5, store.allEvents().count { it.source == QadaSource.WIZARD })
    }

    @Test fun wizardIncludesWitrForHanafi() = runTest {
        repo.applyEstimate(366, AsrMadhab.HANAFI, now = 1)
        assertEquals(366, remaining(QadaPrayer.WITR))
        assertEquals(6, store.allEvents().count { it.source == QadaSource.WIZARD })
    }

    @Test fun undoOfWizardRevertsWholeEstimate() = runTest {
        repo.applyEstimate(10, AsrMadhab.SHAFI, now = 1)
        repeat(5) { repo.undoLast(now = 2) }
        assertEquals(0, remaining(QadaPrayer.FAJR))
    }

    @Test fun setRemainingRecordsDifferenceAsManual() = runTest {
        repo.adjust(QadaPrayer.DHUHR, 5, now = 1)
        repo.setRemaining(QadaPrayer.DHUHR, 2, now = 2)
        assertEquals(2, remaining(QadaPrayer.DHUHR))
        assertEquals(-3, store.allEvents().last().delta)
    }

    @Test fun dailyGoalAppliesToEveryPrayerButFast() = runTest {
        repo.setDailyGoal(2, now = 1)
        val counters = repo.counters.first()
        assertTrue(counters.filter { it.prayer != QadaPrayer.FAST }.all { it.dailyGoal == 2 })
        assertEquals(0, counters.first { it.prayer == QadaPrayer.FAST }.dailyGoal)
    }
}
