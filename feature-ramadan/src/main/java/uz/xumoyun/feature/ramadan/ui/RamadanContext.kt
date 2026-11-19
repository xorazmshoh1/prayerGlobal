package uz.xumoyun.feature.ramadan.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.common.hijri.HijriCalendar
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.ramadan.domain.RamadanCalc
import java.time.Instant
import java.time.LocalDate

/** Ramazon konteksti: joriy yoki keyingi Ramazon boshi, hijriy tuzatish va bugungi sana. */
data class RamadanContext(
    val start: LocalDate,
    val offset: Int,
    val today: LocalDate,
    val now: Instant,
    val hijri: HijriCalendar,
)

/** Har daqiqada va hijriy tuzatish o'zgarganda qayta hisoblanadi. Ramazon boshi topilmasa chiqarilmaydi. */
fun ramadanContext(hijri: HijriProvider, common: CommonSettings, time: TimeSource): Flow<RamadanContext> =
    combine(ticker(), common.hijriAdjustDays) { _, offset -> offset }
        .mapNotNull { offset ->
            val today = time.today()
            val cal = hijri.calendar(offset)
            RamadanCalc.upcomingStart(today, cal)?.let { start ->
                RamadanContext(start, offset, today, time.now(), cal)
            }
        }

private fun ticker(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(TICK_MS)
    }
}

private const val TICK_MS = 60_000L
