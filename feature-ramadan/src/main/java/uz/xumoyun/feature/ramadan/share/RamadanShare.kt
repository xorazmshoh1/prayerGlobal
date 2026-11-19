package uz.xumoyun.feature.ramadan.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import uz.xumoyun.feature.ramadan.domain.RamadanDayRow

/** Ulashiladigan rasm (TZ 11-bo'lim): 1080×1920, to'q yashil fon, sarlavha, 30 qator, pastda ilova nomi va Play havolasi. */
data class ShareCardText(
    val title: String,
    val columns: List<String>,
    val appName: String,
    val playLink: String,
)

object RamadanShare {
    /** TZ 12: qisqa Play havolasi (ilova ID'si bo'yicha). */
    const val PLAY_LINK = "play.google.com/store/apps/details?id=uz.xumoyun.namozhifz"
    const val WIDTH = 1080
    const val HEIGHT = 1920
    private const val BG = 0xFF0F5C4D.toInt()
    private const val GOLD = 0xFFC9A227.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    /** Jadval qatorlari: kun, milodiy, hijriy, saharlik, iftor (matn tayyor). */
    fun rowsText(rows: List<RamadanDayRow>, time: (java.time.Instant?) -> String, locale: Locale): List<String> {
        val fmt = DateTimeFormatter.ofPattern("d MMM", locale)
        return rows.map { r ->
            listOf("${r.number}", fmt.format(r.date), "${r.hijri.day}/${r.hijri.month}", time(r.imsak), time(r.iftar)).joinToString("  |  ")
        }
    }

    /** Rasmni chizadi (Compose'siz — deterministik, ekran o'lchamidan mustaqil). */
    fun render(text: ShareCardText): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BG)
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD; textSize = 64f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE; textSize = 34f; typeface = Typeface.MONOSPACE }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE; textSize = 34f; textAlign = Paint.Align.CENTER }
        canvas.drawText(text.title, WIDTH / 2f, 170f, title)
        var y = 300f
        text.columns.forEach { line ->
            canvas.drawText(line, 40f, y, body)
            y += 52f
        }
        canvas.drawText(text.appName, WIDTH / 2f, HEIGHT - 170f, title.apply { textSize = 48f })
        canvas.drawText(text.playLink, WIDTH / 2f, HEIGHT - 110f, small)
        return bitmap
    }

    /** PNG faylni keshga yozadi va ulashish oynasini ochadi (FileProvider orqali). */
    fun share(context: Context, card: ShareCardText, fileName: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> render(card).compress(Bitmap.CompressFormat.PNG, 100, out) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
