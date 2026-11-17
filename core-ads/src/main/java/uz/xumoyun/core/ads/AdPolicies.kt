package uz.xumoyun.core.ads

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Interstitial chastotasi: ko'rsatishlar orasida kamida [minIntervalMs],
 * bitta sessiyada ko'pi bilan [maxPerSession] marta.
 */
class InterstitialPolicy(
    private val minIntervalMs: Long = 3 * 60 * 1000L,
    private val maxPerSession: Int = 3,
) {
    private var shownCount = 0
    private var lastShownAt: Long? = null

    fun canShow(nowMs: Long): Boolean {
        if (shownCount >= maxPerSession) return false
        val last = lastShownAt ?: return true
        return nowMs - last >= minIntervalMs
    }

    fun recordShown(nowMs: Long) {
        shownCount++
        lastShownAt = nowMs
    }

    /** Yangi sessiya (ilova to'liq yopilib ochilganda). */
    fun resetSession() {
        shownCount = 0
        lastShownAt = null
    }
}

/** App Open: kuniga ko'pi bilan 1 marta. Oxirgi ko'rsatilgan kun tashqarida saqlanadi. */
object AppOpenPolicy {
    fun canShow(lastShownDay: LocalDate?, now: Instant, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val today = LocalDate.ofInstant(now, zone)
        return lastShownDay == null || lastShownDay.isBefore(today)
    }
}
