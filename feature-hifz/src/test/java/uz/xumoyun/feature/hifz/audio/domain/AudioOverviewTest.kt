package uz.xumoyun.feature.hifz.audio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AudioOverviewTest {

    private val counts = List(114) { 7 }

    @Test
    fun statusesFromDiskDownloadsAndJobs() {
        val downloads = listOf(
            DownloadedSura("alafasy", 1, 500_000L, 1L),
            DownloadedSura("husary", 6, 9L, 1L),
        )
        val rows = AudioOverview.rows(
            reciterKey = "alafasy",
            ayahCounts = counts,
            downloads = downloads,
            finished = mapOf(2 to 3),
            jobs = mapOf(3 to JobState.RUNNING, 4 to JobState.ENQUEUED, 5 to JobState.FAILED),
        )
        assertEquals(114, rows.size)
        assertEquals(SuraStatus.DOWNLOADED, rows[0].status)
        assertEquals(500_000L, rows[0].bytes)
        assertEquals(7, rows[0].finishedAyahs)
        assertEquals(SuraStatus.NOT_DOWNLOADED, rows[1].status)
        assertEquals(3, rows[1].finishedAyahs)
        assertEquals(SuraStatus.DOWNLOADING, rows[2].status)
        assertEquals(SuraStatus.QUEUED, rows[3].status)
        assertEquals(SuraStatus.FAILED, rows[4].status)
        // Boshqa qorining yozuvi bu qorida ko'rinmaydi.
        assertEquals(SuraStatus.NOT_DOWNLOADED, rows[5].status)
        assertEquals(0L, rows[5].bytes)
    }

    @Test
    fun finishedIsClampedToAyahCount() {
        val rows = AudioOverview.rows("alafasy", counts, emptyList(), mapOf(2 to 99), emptyMap())
        assertEquals(7, rows[1].finishedAyahs)
    }

    @Test
    fun requiresAll114Suras() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioOverview.rows("alafasy", List(113) { 1 }, emptyList(), emptyMap(), emptyMap())
        }
    }
}
