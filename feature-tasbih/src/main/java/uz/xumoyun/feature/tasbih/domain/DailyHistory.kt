package uz.xumoyun.feature.tasbih.domain

import java.time.LocalDate

/** Bir sana bo'yicha jami hisob. */
data class DayTotal(val date: LocalDate, val total: Int)

/** Bazadagi bitta yozuvning sanasi va hisobi (Room entity'sidan mustaqil). */
data class DailyCount(val date: String, val count: Int)

/** Kunlik tarix agregatsiyasi (TZ 5: kunlik tarix). Sana — ISO (yyyy-MM-dd). */
object DailyHistory {
    /** [today] dan boshlab orqaga [days] kun, eng yangisi birinchi; hisobi yo'q kunlar 0. */
    fun build(rows: List<DailyCount>, today: LocalDate, days: Int = 7): List<DayTotal> {
        val byDate = sumByDate(rows)
        return (0 until days).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayTotal(date, byDate[date.toString()] ?: 0)
        }
    }

    /** Berilgan kunning jami hisobi. */
    fun totalFor(rows: List<DailyCount>, date: LocalDate): Int =
        rows.filter { it.date == date.toString() }.sumOf { it.count }

    /** Sana bo'yicha yig'indi. */
    fun sumByDate(rows: List<DailyCount>): Map<String, Int> =
        rows.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.count } }
}
