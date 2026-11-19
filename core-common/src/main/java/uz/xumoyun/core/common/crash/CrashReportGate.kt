package uz.xumoyun.core.common.crash

/** Xato hisobotining jo'natuvchisi (Crashlytics yoki hech narsa). Foydalanuvchi identifikatori hech qachon uzatilmaydi (TZ 8). */
interface CrashSink {
    fun record(throwable: Throwable)
}

/** Hozircha Firebase Crashlytics ulanmagan: hech narsa yuborilmaydi (reliz oldidan almashtiriladi). */
object NoopCrashSink : CrashSink {
    override fun record(throwable: Throwable) = Unit
}

/**
 * Sozlamadagi "xato hisobotlari" kalitining keshlangan qiymati bilan ishlaydi. Uncaught handler sinxron
 * ishlagani uchun DataStore'ni o'qiy olmaydi; qiymat [enabled] ga ilova boshlanishida va o'zgarganda yoziladi.
 * Standart: yoqilgan (TZ 8: sozlamada o'chirish mumkin).
 */
class CrashReportGate(private val sink: CrashSink) {
    @Volatile
    var enabled: Boolean = true

    /** Yuborildi bo'lsa true. O'chirilgan bo'lsa hech narsa qilmaydi. */
    fun report(throwable: Throwable): Boolean {
        if (!enabled) return false
        return try {
            sink.record(throwable)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
