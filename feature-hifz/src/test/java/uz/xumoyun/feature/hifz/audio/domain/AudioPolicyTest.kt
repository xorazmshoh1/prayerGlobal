package uz.xumoyun.feature.hifz.audio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPolicyTest {

    @Test
    fun wifiOnlyIsDefault_andMapsToUnmetered() {
        assertTrue(DownloadPolicy.WIFI_ONLY_DEFAULT)
        assertEquals(NetRequirement.UNMETERED, DownloadPolicy.requirement(true))
        assertEquals(NetRequirement.CONNECTED, DownloadPolicy.requirement(false))
    }

    @Test
    fun mayRun_respectsWifiOnly() {
        assertEquals(false, DownloadPolicy.mayRun(NetState.NONE, wifiOnly = false))
        assertEquals(false, DownloadPolicy.mayRun(NetState.METERED, wifiOnly = true))
        assertEquals(true, DownloadPolicy.mayRun(NetState.METERED, wifiOnly = false))
        assertEquals(true, DownloadPolicy.mayRun(NetState.UNMETERED, wifiOnly = true))
        assertEquals(true, DownloadPolicy.mayRun(NetState.UNMETERED, wifiOnly = false))
    }

    @Test
    fun httpClassification() {
        assertEquals(HttpAction.APPEND, HttpPolicy.classify(206, 3000))
        assertEquals(HttpAction.RESTART, HttpPolicy.classify(200, 3000))
        assertEquals(HttpAction.RESTART, HttpPolicy.classify(200, 0))
        assertEquals(HttpAction.RESTART, HttpPolicy.classify(416, 3000))
        assertEquals(HttpAction.FAIL, HttpPolicy.classify(416, 0))
        assertEquals(HttpAction.RETRY, HttpPolicy.classify(503, 0))
        assertEquals(HttpAction.RETRY, HttpPolicy.classify(429, 0))
        assertEquals(HttpAction.RETRY, HttpPolicy.classify(408, 0))
        assertEquals(HttpAction.FAIL, HttpPolicy.classify(404, 0))
        assertEquals(HttpAction.FAIL, HttpPolicy.classify(403, 0))
    }

    @Test
    fun rangeHeader() {
        assertNull(HttpPolicy.rangeHeader(0))
        assertEquals("bytes=1234-", HttpPolicy.rangeHeader(1234))
    }

    @Test
    fun retryLimit() {
        assertTrue(RetryPolicy.shouldRetry(0))
        assertTrue(RetryPolicy.shouldRetry(RetryPolicy.MAX_ATTEMPTS - 2))
        assertEquals(false, RetryPolicy.shouldRetry(RetryPolicy.MAX_ATTEMPTS - 1))
    }

    @Test
    fun suraPlan_skipsFinishedAyahs() {
        assertEquals(listOf(3, 4, 6, 7), SuraPlan.ayahsToDownload(1, 7, setOf(1, 2, 5)))
        assertEquals(emptyList<Int>(), SuraPlan.ayahsToDownload(1, 3, setOf(1, 2, 3)))
        assertEquals((1..286).toList(), SuraPlan.ayahsToDownload(2, 286, emptySet()))
    }

    @Test
    fun suraPlan_rejectsBadInput() {
        assertThrows(IllegalArgumentException::class.java) { SuraPlan.ayahsToDownload(0, 7, emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { SuraPlan.ayahsToDownload(115, 7, emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { SuraPlan.ayahsToDownload(1, 0, emptySet()) }
    }
}
