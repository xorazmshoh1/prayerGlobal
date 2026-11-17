package uz.xumoyun.core.common

import java.time.Duration
import java.util.Locale

object DurationFormat {
    /** 01:24:10 ko'rinishi. Manfiy qiymat 00:00:00 bo'ladi. */
    fun hms(duration: Duration, locale: Locale = Locale.ROOT): String {
        val total = duration.seconds.coerceAtLeast(0)
        return String.format(locale, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
    }

    /** 24:59 ko'rinishi (test taymeri uchun). */
    fun ms(seconds: Long, locale: Locale = Locale.ROOT): String {
        val s = seconds.coerceAtLeast(0)
        return String.format(locale, "%02d:%02d", s / 60, s % 60)
    }
}
