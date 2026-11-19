package uz.xumoyun.feature.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.common.hijri.HijriCalendar
import uz.xumoyun.core.common.hijri.HijriYmd
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.calendar.domain.CalendarGrid
import uz.xumoyun.feature.calendar.domain.CalendarPrefsStore
import uz.xumoyun.feature.calendar.domain.DateConversion
import uz.xumoyun.feature.calendar.domain.DateConverter
import uz.xumoyun.feature.calendar.domain.EventOccurrence
import uz.xumoyun.feature.calendar.domain.MonthGrid
import uz.xumoyun.feature.calendar.domain.PrayerTimetable
import uz.xumoyun.feature.calendar.domain.PrayerTimetablePort
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

enum class CalendarMode { GRID, TIMETABLE }

data class CalendarUiState(
    val month: YearMonth,
    val mode: CalendarMode,
    /** true — hijriy sana asosiy (katakda katta), milodiy kichik. */
    val hijriPrimary: Boolean,
    val grid: MonthGrid,
    val events: List<EventOccurrence>,
    /** 30 kunlik jadval; joy tanlanmagan bo'lsa null. */
    val timetable: PrayerTimetable?,
    val today: HijriYmd,
    val todayDate: LocalDate,
    val conversion: DateConversion?,
    val conversionError: Boolean,
)

/**
 * Taqvim tabi holati (TZ 6-bo'lim, 3-ekran). Oylik setka, islomiy sanalar ro'yxati, asosiy tizim, 30 kunlik
 * namoz jadvali va sana konvertori. Hijriy hisob [HijriProvider.calendar] orqali (±kun tuzatish bilan).
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val hijri: HijriProvider,
    common: CommonSettings,
    private val prefs: CalendarPrefsStore,
    private val prayers: PrayerTimetablePort,
    private val time: TimeSource,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(time.today()))
    private val mode = MutableStateFlow(CalendarMode.GRID)
    private val conversion = MutableStateFlow<DateConversion?>(null)
    private val conversionError = MutableStateFlow(false)
    private val tableStart = MutableStateFlow(time.today())

    private val offset = common.hijriAdjustDays.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val base = combine(month, mode, offset, prefs.hijriPrimary) { m, md, adjust, primary ->
        val cal = hijri.calendar(adjust)
        val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val grid = CalendarGrid.build(m, firstDay, time.today(), cal)
        BaseState(m, md, primary, grid, CalendarGrid.events(grid), cal.ymd(time.today()), time.today())
    }

    val state: StateFlow<CalendarUiState> = combine(
        base,
        prayers.timetable(tableStart),
        conversion,
        conversionError,
    ) { b, table, conv, convErr ->
        CalendarUiState(
            month = b.month, mode = b.mode, hijriPrimary = b.primary, grid = b.grid, events = b.events,
            timetable = table, today = b.today, todayDate = b.todayDate, conversion = conv, conversionError = convErr,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(
        month = month.value, mode = CalendarMode.GRID, hijriPrimary = true,
        grid = MonthGrid(month.value, emptyList()), events = emptyList(), timetable = null,
        today = HijriYmd(1448, 1, 1), todayDate = time.today(), conversion = null, conversionError = false,
    ))

    private data class BaseState(
        val month: YearMonth,
        val mode: CalendarMode,
        val primary: Boolean,
        val grid: MonthGrid,
        val events: List<EventOccurrence>,
        val today: HijriYmd,
        val todayDate: LocalDate,
    )

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun thisMonth() { month.value = YearMonth.from(time.today()) }
    fun setMode(value: CalendarMode) { mode.value = value }

    fun setHijriPrimary(value: Boolean) { viewModelScope.launch { prefs.setHijriPrimary(value) } }

    /** Milodiy sanani hijriyga o'girish (sana tanlash oynasidan). */
    fun convertGregorian(date: LocalDate) {
        conversionError.value = false
        conversion.value = DateConverter.fromGregorian(date, hijri.calendar(offset.value))
    }

    /** Hijriy sanani milodiyga o'girish. Noto'g'ri yoki topilmasa xato ko'rsatiladi. */
    fun convertHijri(year: Int, monthNo: Int, day: Int) {
        val result = DateConverter.fromHijri(year, monthNo, day, hijri.calendar(offset.value))
        conversion.value = result
        conversionError.value = result == null
    }

    fun clearConversion() {
        conversion.value = null
        conversionError.value = false
    }
}
