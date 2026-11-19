package uz.xumoyun.feature.ramadan.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

/** Ramazon kunlari uchun Bomdod/Shom vaqtlari (feature-prayer'dan ilova orqali). */
data class RamadanSchedule(val city: String, val zone: ZoneId, val days: List<RamadanDayTimes>)

/** [start] dan boshlab [count] kunlik vaqtlar; joy tanlanmagan bo'lsa null. feature-ramadan feature-prayer'ga bog'lanmaydi. */
interface RamadanSchedulePort {
    fun days(start: Flow<LocalDate>, count: Int): Flow<RamadanSchedule?>
}

/** Ro'za qazosi (QadaCounter, FAST) — feature-prayer'dagi kuzatuvchi orqali ilova ulaydi. */
interface RamadanQadaPort {
    fun fastQada(): Flow<Int>
    suspend fun adjustFast(delta: Int)
}
