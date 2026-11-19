package uz.xumoyun.feature.calendar.domain

import uz.xumoyun.core.common.hijri.HijriCalendar
import uz.xumoyun.core.common.hijri.HijriYmd
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class DayCell(
    val date: LocalDate,
    val hijri: HijriYmd,
    val events: List<IslamicEvent>,
    val isToday: Boolean,
)

/** Oy setkasi: har hafta 7 katak, oy boshidan oldingi va oxiridan keyingi kataklar null. */
data class MonthGrid(val month: YearMonth, val weeks: List<List<DayCell?>>) {
    val days: List<DayCell> get() = weeks.flatten().filterNotNull()
}

/** Islomiy sana tushgan kun ro'yxati (oy pastidagi ro'yxat uchun). */
data class EventOccurrence(val date: LocalDate, val hijri: HijriYmd, val event: IslamicEvent)

object CalendarGrid {
    fun build(month: YearMonth, firstDayOfWeek: DayOfWeek, today: LocalDate, hijri: HijriCalendar): MonthGrid {
        val first = month.atDay(1)
        // Haftaning boshidan oy boshigacha bo'lgan bo'sh kataklar soni (0..6).
        val leading = Math.floorMod(first.dayOfWeek.value - firstDayOfWeek.value, 7)
        val cells = ArrayList<DayCell?>()
        repeat(leading) { cells += null }
        for (d in 1..month.lengthOfMonth()) {
            val date = month.atDay(d)
            val h = hijri.ymd(date)
            cells += DayCell(date, h, IslamicEvent.on(h), date == today)
        }
        while (cells.size % 7 != 0) cells += null
        return MonthGrid(month, cells.chunked(7))
    }

    /** Oydagi islomiy sanalar, sana bo'yicha tartiblangan. */
    fun events(grid: MonthGrid): List<EventOccurrence> =
        grid.days.flatMap { cell -> cell.events.map { EventOccurrence(cell.date, cell.hijri, it) } }
}
