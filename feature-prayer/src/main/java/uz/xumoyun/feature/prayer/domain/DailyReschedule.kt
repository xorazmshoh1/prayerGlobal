package uz.xumoyun.feature.prayer.domain

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/** Kunlik qayta hisoblash (WorkManager) vaqti: yarim tundan keyin (TZ 8-bo'lim). */
object DailyReschedule {
    val RUN_AT: LocalTime = LocalTime.of(0, 5)

    /** [now] dan keyingi birinchi [at]; DST o'tishida mavjud bo'lmagan vaqt oldinga suriladi. */
    fun nextRun(now: ZonedDateTime, at: LocalTime = RUN_AT): ZonedDateTime {
        val today = now.toLocalDate().atTime(at).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(at).atZone(now.zone)
    }

    fun delayUntilNext(now: ZonedDateTime, at: LocalTime = RUN_AT): Duration = Duration.between(now, nextRun(now, at))
}
