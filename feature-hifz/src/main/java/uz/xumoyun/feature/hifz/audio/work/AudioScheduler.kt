package uz.xumoyun.feature.hifz.audio.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import uz.xumoyun.feature.hifz.audio.domain.DownloadPolicy
import uz.xumoyun.feature.hifz.audio.domain.JobState
import uz.xumoyun.feature.hifz.audio.domain.NetRequirement
import java.util.concurrent.TimeUnit

/**
 * Sura yuklash ishlarini navbatga qo'yadi: bitta sura — bitta unique ish.
 * Tarmoq cheklovi [DownloadPolicy] dan olinadi (standart: faqat Wi-Fi / UNMETERED).
 */
class AudioScheduler(private val workManager: WorkManager) {

    fun enqueueSura(reciterKey: String, surah: Int, wifiOnly: Boolean) {
        val network = when (DownloadPolicy.requirement(wifiOnly)) {
            NetRequirement.UNMETERED -> NetworkType.UNMETERED
            NetRequirement.CONNECTED -> NetworkType.CONNECTED
        }
        val request = OneTimeWorkRequestBuilder<SuraDownloadWorker>()
            .setInputData(workDataOf(KEY_RECITER to reciterKey, KEY_SURAH to surah))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(network).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .addTag(tagOf(reciterKey, surah))
            .build()
        workManager.enqueueUniqueWork(uniqueName(reciterKey, surah), ExistingWorkPolicy.KEEP, request)
    }

    fun cancelSura(reciterKey: String, surah: Int) {
        workManager.cancelUniqueWork(uniqueName(reciterKey, surah))
    }

    /** Qori bo'yicha ish holatlari (sura -> holat). Bitta so'rov bilan barcha suralar. */
    fun jobStates(reciterKey: String): Map<Int, JobState> {
        val infos = workManager.getWorkInfosByTag(TAG).get()
        val prefix = "$TAG:$reciterKey:"
        val result = HashMap<Int, JobState>()
        for (info in infos) {
            val surah = info.tags.firstNotNullOfOrNull { tag ->
                if (tag.startsWith(prefix)) tag.removePrefix(prefix).toIntOrNull() else null
            } ?: continue
            // Bir sura uchun eski (FAILED) va yangi yozuv birga bo'lishi mumkin: faol holat ustun.
            val next = info.state.toJobState()
            val prev = result[surah]
            if (prev == null || rank(next) > rank(prev)) result[surah] = next
        }
        return result
    }

    private fun rank(state: JobState): Int = when (state) {
        JobState.RUNNING -> 3
        JobState.ENQUEUED -> 2
        JobState.FAILED -> 1
        JobState.NONE -> 0
    }

    companion object {
        const val TAG = "audio-sura"
        const val KEY_RECITER = "reciter"
        const val KEY_SURAH = "surah"

        fun uniqueName(reciterKey: String, surah: Int): String = "audio-$reciterKey-$surah"

        fun tagOf(reciterKey: String, surah: Int): String = "$TAG:$reciterKey:$surah"

        internal fun WorkInfo.State?.toJobState(): JobState = when (this) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> JobState.ENQUEUED
            WorkInfo.State.RUNNING -> JobState.RUNNING
            WorkInfo.State.FAILED -> JobState.FAILED
            WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED, null -> JobState.NONE
        }
    }
}
