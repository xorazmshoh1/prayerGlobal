package uz.xumoyun.feature.hifz.audio.domain

/** Qurilmaning hozirgi tarmoq holati (WorkManager cheklovidan mustaqil). */
enum class NetState { NONE, METERED, UNMETERED }

/** WorkManager talabi. Android qatlamida androidx.work.NetworkType ga aylantiriladi. */
enum class NetRequirement { UNMETERED, CONNECTED }

object DownloadPolicy {
    /** TZ 8-bo'lim: standart faqat Wi-Fi; sozlamada o'zgartiriladi. */
    const val WIFI_ONLY_DEFAULT = true

    fun requirement(wifiOnly: Boolean): NetRequirement =
        if (wifiOnly) NetRequirement.UNMETERED else NetRequirement.CONNECTED

    fun mayRun(net: NetState, wifiOnly: Boolean): Boolean = when (net) {
        NetState.NONE -> false
        NetState.METERED -> !wifiOnly
        NetState.UNMETERED -> true
    }
}

enum class HttpAction { APPEND, RESTART, RETRY, FAIL }

object HttpPolicy {
    /**
     * Server javobiga qarab nima qilish kerak.
     * - 206: qolgan qismi keldi, faylga qo'shiladi.
     * - 200: server Range'ni e'tiborsiz qoldirdi (yoki to'liq fayl): fayl boshidan yoziladi.
     * - 416: so'ralgan diapazon yaroqsiz (qisman fayl buzilgan): boshidan. Offset 0 bo'lsa — xato.
     * - 408/425/429/5xx: vaqtinchalik, keyinroq urinish.
     * - boshqa 4xx (404, 403, ...): qaytarib bo'lmaydi.
     */
    fun classify(code: Int, requestedOffset: Long): HttpAction = when {
        code == 206 -> HttpAction.APPEND
        code == 200 -> HttpAction.RESTART
        code == 416 -> if (requestedOffset > 0) HttpAction.RESTART else HttpAction.FAIL
        code == 408 || code == 425 || code == 429 || code in 500..599 -> HttpAction.RETRY
        else -> HttpAction.FAIL
    }

    /** Offset 0 bo'lsa Range yuborilmaydi (null). */
    fun rangeHeader(offset: Long): String? = if (offset > 0) "bytes=$offset-" else null
}

object RetryPolicy {
    /** WorkManager urinishlari soni (runAttemptCount, 0 dan boshlab). Undan keyin ish muvaffaqiyatsiz yakunlanadi. */
    const val MAX_ATTEMPTS = 6

    fun shouldRetry(runAttemptCount: Int): Boolean = runAttemptCount + 1 < MAX_ATTEMPTS
}

object SuraPlan {
    /**
     * Sura ichida hali yuklanmagan oyatlar (1-based), tartib bilan.
     * [finished] — diskda to'liq (".part" emas) turgan oyat raqamlari.
     */
    fun ayahsToDownload(surah: Int, ayahCount: Int, finished: Set<Int>): List<Int> {
        require(surah in 1..114) { "sura: $surah" }
        require(ayahCount in 1..MAX_AYAHS_PER_SURAH) { "oyatlar soni: $ayahCount" }
        return (1..ayahCount).filter { it !in finished }
    }
}
