package uz.xumoyun.feature.hifz.audio.domain

enum class SuraStatus { NOT_DOWNLOADED, QUEUED, DOWNLOADING, DOWNLOADED, FAILED }

/** WorkManager ishining holati (Android qatlamidan sof modelga aylantiriladi). */
enum class JobState { NONE, ENQUEUED, RUNNING, FAILED }

data class SuraRow(
    val surah: Int,
    val ayahCount: Int,
    val status: SuraStatus,
    /** Diskda to'liq turgan oyatlar soni (yarim yuklangan sura uchun progress). */
    val finishedAyahs: Int,
    /** DOWNLOADED bo'lsa — egallagan bayt, aks holda 0. */
    val bytes: Long,
)

object AudioOverview {
    /**
     * Audio boshqaruv ekrani uchun qatorlar.
     * @param ayahCounts indeks 0 — 1-sura oyatlari soni (114 ta bo'lishi kerak).
     * @param downloads tanlangan qori uchun AudioDownload yozuvlari.
     * @param finished surah -> diskda to'liq oyatlar soni.
     * @param jobs surah -> WorkManager holati (yo'q bo'lsa NONE).
     */
    fun rows(
        reciterKey: String,
        ayahCounts: List<Int>,
        downloads: List<DownloadedSura>,
        finished: Map<Int, Int>,
        jobs: Map<Int, JobState>,
    ): List<SuraRow> {
        require(ayahCounts.size == 114) { "114 sura kerak, keldi: ${ayahCounts.size}" }
        val byRow = downloads.filter { it.reciterKey == reciterKey }.associateBy { it.surah }
        return ayahCounts.mapIndexed { index, count ->
            val surah = index + 1
            val done = byRow[surah]
            val job = jobs[surah] ?: JobState.NONE
            val status = when {
                done != null -> SuraStatus.DOWNLOADED
                job == JobState.RUNNING -> SuraStatus.DOWNLOADING
                job == JobState.ENQUEUED -> SuraStatus.QUEUED
                job == JobState.FAILED -> SuraStatus.FAILED
                else -> SuraStatus.NOT_DOWNLOADED
            }
            SuraRow(
                surah = surah,
                ayahCount = count,
                status = status,
                finishedAyahs = if (done != null) count else (finished[surah] ?: 0).coerceIn(0, count),
                bytes = done?.bytes ?: 0L,
            )
        }
    }
}
