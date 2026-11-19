package uz.xumoyun.feature.tasbih.domain

/** Hisoblagich qoidalari (TZ 5, 11): maqsad, halqa to'lishi, davralar soni. Android'siz. */
object TasbihCounter {
    const val DEFAULT_TARGET = 33
    const val MAX_TARGET = 100_000

    /** Tayyor maqsadlar; boshqa qiymat "Ixtiyoriy" orqali. */
    val PRESET_TARGETS = listOf(33, 99, 100)

    /** Maqsad 1..[MAX_TARGET] oralig'ida bo'lsa qaytaradi, aks holda null. */
    fun validTarget(input: Int?): Int? = input?.takeIf { it in 1..MAX_TARGET }

    /** Maqsadga yetildi: hisob maqsadning karralisi (0 dan tashqari). */
    fun reached(count: Int, target: Int): Boolean = target > 0 && count > 0 && count % target == 0

    /**
     * Joriy davra halqasi (0..1). 33 da halqa to'liq bo'ladi, 34 da yana bo'sh davra boshlanadi.
     * Hisob 0 bo'lsa — bo'sh halqa.
     */
    fun progress(count: Int, target: Int): Float {
        if (target <= 0 || count <= 0) return 0f
        return ((count - 1) % target + 1).toFloat() / target
    }

    /** Tugallangan davralar soni. */
    fun rounds(count: Int, target: Int): Int = if (target <= 0) 0 else count / target
}
