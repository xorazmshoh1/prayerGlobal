package uz.xumoyun.feature.hifz.audio.data

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Sun'iy server: Range'ni hurmat qiladi (206), xohlasa e'tiborsiz qoldiradi (200),
 * uzilishni taqlid qiladi (kontent-uzunligi e'lon qilinadi, kam bayt yuboriladi).
 * Bu synthetic bayt massivi — haqiqiy audio emas.
 */
private class FakeServer(
    private val body: ByteArray,
    var status: Int = 200,
    var ignoreRange: Boolean = false,
    var truncateAfter: Int? = null,
) {
    val seenRanges = mutableListOf<String?>()

    fun open(url: String): HttpURLConnection = FakeConnection(URL(url), this)

    fun respond(range: String?): Triple<Int, Int, InputStream> {
        seenRanges += range
        if (status != 200) return Triple(status, 0, ByteArrayInputStream(ByteArray(0)))
        val start = if (!ignoreRange && range != null) Regex("bytes=(\\d+)-").find(range)!!.groupValues[1].toInt() else 0
        if (start >= body.size) return Triple(416, 0, ByteArrayInputStream(ByteArray(0)))
        val code = if (start > 0) 206 else 200
        val declared = body.size - start
        val sent = body.copyOfRange(start, body.size).let { part ->
            truncateAfter?.let { part.copyOf(minOf(it, part.size)) } ?: part
        }
        return Triple(code, declared, ByteArrayInputStream(sent))
    }
}

private class FakeConnection(url: URL, private val server: FakeServer) : HttpURLConnection(url) {
    private var range: String? = null
    private var code = 0
    private var length = 0
    private var stream: InputStream? = null

    override fun connect() {}
    override fun disconnect() {}
    override fun usingProxy(): Boolean = false

    override fun setRequestProperty(key: String, value: String) {
        if (key.equals("Range", ignoreCase = true)) range = value
    }

    private fun ensure() {
        if (stream == null) {
            val (c, l, s) = server.respond(range)
            code = c; length = l; stream = s
        }
    }

    override fun getResponseCode(): Int { ensure(); return code }
    override fun getContentLengthLong(): Long { ensure(); return length.toLong() }
    override fun getInputStream(): InputStream {
        ensure()
        if (code >= 400) throw IOException("HTTP $code")
        return stream!!
    }
}

class AyahFileDownloaderTest {

    @get:Rule val tmp = TemporaryFolder()

    private val original = ByteArray(10_000) { (it * 31 % 251).toByte() }

    private fun downloader(server: FakeServer) = AyahFileDownloader(open = server::open)

    @Test
    fun freshDownload_writesFinalFileAndNoPart() {
        val server = FakeServer(original)
        val target = File(tmp.root, "audio/x/001/001.mp3")
        val result = downloader(server).download("https://x.test/a.mp3", target)
        assertEquals(DownloadResult.Done(original.size.toLong()), result)
        assertArrayEquals(original, target.readBytes())
        assertFalse(File(target.path + ".part").exists())
        assertEquals(listOf<String?>(null), server.seenRanges)
    }

    @Test
    fun interruptedDownload_resumesWithRange() {
        val server = FakeServer(original, truncateAfter = 3_000)
        val target = File(tmp.root, "003.mp3")
        val d = downloader(server)

        val first = d.download("https://x.test/a.mp3", target)
        assertTrue(first is DownloadResult.Retry)
        assertEquals(3_000L, File(target.path + ".part").length())
        assertFalse(target.exists())

        server.truncateAfter = null
        val second = d.download("https://x.test/a.mp3", target)
        assertEquals(DownloadResult.Done(original.size.toLong()), second)
        assertArrayEquals(original, target.readBytes())
        assertEquals(listOf<String?>(null, "bytes=3000-"), server.seenRanges)
    }

    @Test
    fun serverIgnoringRange_restartsFromZero() {
        val target = File(tmp.root, "004.mp3")
        File(target.path + ".part").apply { parentFile?.mkdirs(); writeBytes(ByteArray(3_000) { 9 }) }
        val server = FakeServer(original, ignoreRange = true)
        val result = downloader(server).download("https://x.test/a.mp3", target)
        assertEquals(DownloadResult.Done(original.size.toLong()), result)
        assertArrayEquals(original, target.readBytes())
        assertEquals(listOf<String?>("bytes=3000-"), server.seenRanges)
    }

    @Test
    fun badPartialFile_416_isRestartedFromScratch() {
        val target = File(tmp.root, "005.mp3")
        // .part server hajmiga teng/katta: diapazon yaroqsiz -> 416 -> boshidan.
        File(target.path + ".part").apply { parentFile?.mkdirs(); writeBytes(ByteArray(original.size) { 1 }) }
        val server = FakeServer(original)
        val result = downloader(server).download("https://x.test/a.mp3", target)
        assertEquals(DownloadResult.Done(original.size.toLong()), result)
        assertArrayEquals(original, target.readBytes())
        assertEquals(listOf<String?>("bytes=${original.size}-", null), server.seenRanges)
    }

    @Test
    fun notFound_isPermanentFailure_andLeavesNoFile() {
        val target = File(tmp.root, "006.mp3")
        val server = FakeServer(original, status = 404)
        assertEquals(DownloadResult.Failed(404), downloader(server).download("https://x.test/a.mp3", target))
        assertFalse(target.exists())
    }

    @Test
    fun serviceUnavailable_isRetryable() {
        val target = File(tmp.root, "007.mp3")
        val server = FakeServer(original, status = 503)
        val result = downloader(server).download("https://x.test/a.mp3", target)
        assertEquals(DownloadResult.Retry("HTTP 503"), result)
    }

    @Test
    fun existingFinalFile_isNotFetchedAgain() {
        val target = File(tmp.root, "008.mp3").apply { writeBytes(original) }
        val d = AyahFileDownloader(open = { throw AssertionError("tarmoq chaqirilmasligi kerak") })
        assertEquals(DownloadResult.Done(original.size.toLong()), d.download("https://x.test/a.mp3", target))
    }

    @Test
    fun connectionFailure_isRetryable() {
        val target = File(tmp.root, "009.mp3")
        val d = AyahFileDownloader(open = { throw IOException("unreachable") })
        assertEquals(DownloadResult.Retry("unreachable"), d.download("https://x.test/a.mp3", target))
    }
}
