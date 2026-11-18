package uz.xumoyun.feature.prayer.domain

import java.time.ZonedDateTime

/** Bosh ekran vidjeti ma'lumoti: keyingi namoz, qolgan vaqt va bugungi 6 vaqt (TZ 5-bo'lim, 6-band). */
data class WidgetSnapshot(
    val city: String,
    val next: NextEvent,
    val rows: List<Pair<Prayer, ZonedDateTime>>,
)

object WidgetModel {
    fun build(now: ZonedDateTime, city: String, today: DayTimes, tomorrow: DayTimes): WidgetSnapshot =
        WidgetSnapshot(city, Schedule.next(now, today, tomorrow), today.ordered)
}
