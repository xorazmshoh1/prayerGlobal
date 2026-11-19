package uz.xumoyun.feature.hifz.audio.domain

/** Tezlik chegaralari (TZ 6-bo'lim): 0.75x–1.25x. */
object PlaybackSpeed {
    const val MIN = 0.75f
    const val MAX = 1.25f
    const val DEFAULT = 1f

    /** UI qadamlari: 0.75, 0.80, ..., 1.25 (11 ta). */
    val steps: List<Float> = (75..125 step 5).map { it / 100f }

    fun clamp(value: Float): Float = if (value.isNaN()) DEFAULT else value.coerceIn(MIN, MAX)
}

/**
 * Takrorlash oraliqi: [fromAyah]..[toAyah] (bitta oyat bo'lishi ham mumkin), [times] marta.
 * Bitta oyat uchun ham, oraliq uchun ham shu model.
 */
data class RepeatRange(val fromAyah: Int, val toAyah: Int, val times: Int) {
    init {
        require(fromAyah in 1..MAX_AYAHS_PER_SURAH) { "boshlang'ich oyat: $fromAyah" }
        require(toAyah in fromAyah..MAX_AYAHS_PER_SURAH) { "oxirgi oyat: $toAyah" }
        require(times in 1..MAX_TIMES) { "takror soni: $times" }
    }

    val ayahs: List<Int> get() = (fromAyah..toAyah).toList()

    companion object {
        const val MAX_TIMES = 100
    }
}

/**
 * Oraliq necha marta o'ynalganini sanaydi. Har bir o'tish tugagach [onPassEnded] chaqiriladi;
 * true qaytsa — oraliq boshidan yana o'ynaladi.
 */
class RepeatCounter(times: Int) {
    private val total = times
    private var passesDone = 0

    init {
        require(times >= 1) { "takror soni: $times" }
    }

    val isFinished: Boolean get() = passesDone >= total

    fun onPassEnded(): Boolean {
        if (isFinished) return false
        passesDone++
        return !isFinished
    }
}

/** Ijro tartibi (test va tekshiruv uchun): oraliq [RepeatRange.times] marta ketma-ket. */
fun playOrder(range: RepeatRange): List<Int> = List(range.times) { range.ayahs }.flatten()
