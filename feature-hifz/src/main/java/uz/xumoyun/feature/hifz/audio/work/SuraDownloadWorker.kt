package uz.xumoyun.feature.hifz.audio.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.xumoyun.feature.hifz.audio.data.AyahFileDownloader
import uz.xumoyun.feature.hifz.audio.data.DownloadResult
import uz.xumoyun.feature.hifz.audio.domain.AyahUrl
import uz.xumoyun.feature.hifz.audio.domain.RetryPolicy
import uz.xumoyun.feature.hifz.audio.domain.SuraPlan

/**
 * Bitta sura: oyatma-oyat yuklaydi. To'liq yuklangan oyat qayta yuklanmaydi (fayl bor),
 * yarim qolgan oyat Range bilan davom etadi. Vaqtinchalik xatoda WorkManager qayta navbatga qo'yadi.
 *
 * Eslatma: loyihada WorkManager standart initializer bilan ishlaydi (HiltWorkerFactory yo'q),
 * shuning uchun bog'liqliklar [AudioEntryPoint] orqali olinadi.
 */
class SuraDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val reciterKey = inputData.getString(AudioScheduler.KEY_RECITER) ?: return@withContext Result.failure()
        val surah = inputData.getInt(AudioScheduler.KEY_SURAH, 0)
        if (surah !in 1..114) return@withContext Result.failure()

        val deps = EntryPointAccessors.fromApplication(applicationContext, AudioEntryPoint::class.java)
        val reciter = deps.reciters().find(reciterKey) ?: return@withContext Result.failure()
        val ayahCount = deps.quran().surahs().firstOrNull { it.id == surah }?.ayahCount
            ?: return@withContext Result.failure()
        val store = deps.store()
        val downloader = AyahFileDownloader()

        val todo = SuraPlan.ayahsToDownload(surah, ayahCount, store.finishedAyahs(reciterKey, surah))
        for (ayah in todo) {
            if (isStopped) return@withContext Result.retry()
            val url = AyahUrl.build(reciter, surah, ayah)
            when (val result = downloader.download(url, store.ayahFile(reciterKey, surah, ayah))) {
                is DownloadResult.Done -> Unit
                is DownloadResult.Retry ->
                    return@withContext if (RetryPolicy.shouldRetry(runAttemptCount)) Result.retry() else Result.failure()
                is DownloadResult.Failed ->
                    return@withContext Result.failure(workDataOf(KEY_ERROR_CODE to result.code))
            }
        }
        store.markDownloaded(reciterKey, surah, System.currentTimeMillis())
        Result.success()
    }

    companion object {
        const val KEY_ERROR_CODE = "error_code"
    }
}
