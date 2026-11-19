package uz.xumoyun.feature.hifz.audio.data

import uz.xumoyun.feature.hifz.audio.domain.AudioLayout
import uz.xumoyun.feature.hifz.audio.domain.HttpAction
import uz.xumoyun.feature.hifz.audio.domain.HttpPolicy
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface DownloadResult {
    /** Fayl to'liq saqlandi; [bytes] — yakuniy hajm. */
    data class Done(val bytes: Long) : DownloadResult

    /** Vaqtinchalik xato. Qisman fayl saqlanadi, keyingi urinish davom etadi. */
    data class Retry(val reason: String) : DownloadResult

    /** Qaytarib bo'lmaydi (masalan 404). */
    data class Failed(val code: Int) : DownloadResult
}

/**
 * Bitta oyat faylini yuklaydi: `.part` ga yozadi, uzilsa Range bilan davom etadi, to'liq bo'lgach nomini o'zgartiradi.
 * Faqat java.net/java.io — JVM'da ham, Android'da ham ishlaydi.
 */
class AyahFileDownloader(
    private val open: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection },
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
) {
    fun download(url: String, target: File): DownloadResult {
        if (target.isFile && target.length() > 0) return DownloadResult.Done(target.length())
        target.parentFile?.mkdirs()
        val part = File(target.path + AudioLayout.PART_SUFFIX)
        // 416 (diapazon yaroqsiz) bo'lsa qisman fayl o'chiriladi va bir marta boshidan so'raladi.
        repeat(2) {
            when (val result = attempt(url, part, target)) {
                null -> part.delete()
                else -> return result
            }
        }
        return DownloadResult.Retry("diapazon qayta tiklanmadi")
    }

    /** null — "boshidan qayta urinish kerak" (416). */
    private fun attempt(url: String, part: File, target: File): DownloadResult? {
        val offset = if (part.isFile) part.length() else 0L
        val conn = try {
            open(url)
        } catch (e: IOException) {
            return DownloadResult.Retry(e.message ?: "ulanish xatosi")
        }
        return try {
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.requestMethod = "GET"
            HttpPolicy.rangeHeader(offset)?.let { conn.setRequestProperty("Range", it) }
            val code = conn.responseCode
            when (HttpPolicy.classify(code, offset)) {
                HttpAction.FAIL -> DownloadResult.Failed(code)
                HttpAction.RETRY -> DownloadResult.Retry("HTTP $code")
                HttpAction.RESTART ->
                    if (code == 416) null else writeBody(conn, part, target, append = false)
                HttpAction.APPEND -> writeBody(conn, part, target, append = true)
            }
        } catch (e: IOException) {
            DownloadResult.Retry(e.message ?: "I/O xatosi")
        } finally {
            conn.disconnect()
        }
    }

    private fun writeBody(conn: HttpURLConnection, part: File, target: File, append: Boolean): DownloadResult {
        val expected = conn.contentLengthLong
        var copied = 0L
        conn.inputStream.use { input ->
            FileOutputStream(part, append).use { out -> copied = input.copyTo(out) }
        }
        // Uzilish: kelgan qism .part da qoladi, keyingi urinish Range bilan davom etadi.
        if (expected >= 0 && copied != expected) {
            return DownloadResult.Retry("uzildi: $copied/$expected bayt")
        }
        if (target.exists() && !target.delete()) return DownloadResult.Retry("eski faylni o'chirib bo'lmadi")
        if (!part.renameTo(target)) return DownloadResult.Retry("faylni saqlab bo'lmadi")
        return DownloadResult.Done(target.length())
    }
}
