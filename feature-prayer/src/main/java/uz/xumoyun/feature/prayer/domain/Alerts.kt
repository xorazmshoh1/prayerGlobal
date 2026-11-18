package uz.xumoyun.feature.prayer.domain

/**
 * TZ 5-bo'lim, 5-band: har bir namoz uchun ovozli, faqat bildirishnoma (ovozsiz) yoki o'chiq.
 * Ovoz — namoz kanalining ovozi (azon, TZ 8-bo'lim).
 */
enum class AlertMode {
    SOUND, SILENT, OFF;

    /** Namoz ekranidagi belgi bosilganda: ovozli → ovozsiz → o'chiq → ovozli. */
    fun next(): AlertMode = when (this) {
        SOUND -> SILENT
        SILENT -> OFF
        OFF -> SOUND
    }
}

/** Bitta vaqt uchun ogohlantirish: rejim va [minutesBefore] daqiqa oldin eslatma (0 — eslatma yo'q). */
data class PrayerAlert(val mode: AlertMode, val minutesBefore: Int = 0) {
    val enabled: Boolean get() = mode != AlertMode.OFF

    companion object {
        /** TZ 5-bo'lim: namozdan 0–30 daqiqa oldin eslatma. */
        const val MAX_MINUTES_BEFORE = 30

        /** Sozlamalar ekranidagi tanlovlar. */
        val MINUTES_BEFORE_OPTIONS = listOf(0, 5, 10, 15, 20, 30)

        /** Standart: 5 namoz ovozli, Quyosh o'chiq, eslatma yo'q. */
        fun default(prayer: Prayer) = PrayerAlert(if (prayer.isPrayer) AlertMode.SOUND else AlertMode.OFF, 0)

        fun clampMinutes(minutes: Int): Int = minutes.coerceIn(0, MAX_MINUTES_BEFORE)
    }
}
