package uz.xumoyun.feature.calendar.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import uz.xumoyun.core.common.hijri.HijriCalendar
import uz.xumoyun.core.common.hijri.HijriYmd
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriDate
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.calendar.domain.CalendarPrefsStore
import uz.xumoyun.feature.calendar.domain.FakeUmmAlQura
import uz.xumoyun.feature.calendar.domain.IslamicEvent
import uz.xumoyun.feature.calendar.domain.PrayerDayRow
import uz.xumoyun.feature.calendar.domain.PrayerTimetable
import uz.xumoyun.feature.calendar.domain.PrayerTimetablePort
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val zone = ZoneId.of("UTC")
    private val time = FixedTimeSource(Instant.parse("2027-02-10T10:00:00Z"), zone)
    private val hijriProvider = object : HijriProvider {
        override fun of(date: LocalDate, offsetDays: Int, locale: java.util.Locale) = HijriDate(1, 9, 1448, "")
        override fun calendar(offsetDays: Int): HijriCalendar = HijriCalendar(FakeUmmAlQura, offsetDays)
    }
    private val prefs = object : CalendarPrefsStore {
        override val hijriPrimary: MutableStateFlow<Boolean> = MutableStateFlow(true)
        override suspend fun setHijriPrimary(value: Boolean) { hijriPrimary.value = value }
    }
    private val port = object : PrayerTimetablePort {
        override fun timetable(start: Flow<LocalDate>): Flow<PrayerTimetable?> = start.map { from ->
            PrayerTimetable("Tashkent", (0 until 30).map { PrayerDayRow(from.plusDays(it.toLong()), emptyList()) })
        }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun store(): DataStore<Preferences> {
        val file = File(Files.createTempDirectory("cal").toFile(), "s.preferences_pb")
        return PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)) { file }
    }

    private fun vm(common: CommonSettings = CommonSettings(store())) = CalendarViewModel(hijriProvider, common, prefs, port, time)

    @Test fun showsFebruary2027GridWithRamadanStart() = runTest {
        val vm = vm()
        val s = vm.state.first { it.grid.weeks.isNotEmpty() }
        assertEquals(YearMonth.of(2027, 2), s.month)
        assertEquals(LocalDate.of(2027, 2, 10), s.todayDate)
        assertTrue(s.events.any { it.event == IslamicEvent.RAMADAN_1 })
    }

    @Test fun nextMonthMovesToMarchAndKeepsEvents() = runTest {
        val vm = vm()
        vm.nextMonth()
        val s = vm.state.first { it.month == YearMonth.of(2027, 3) }
        assertTrue(s.events.any { it.event == IslamicEvent.EID_AL_FITR })
    }

    @Test fun timetableHasThirtyDays() = runTest {
        val vm = vm()
        vm.setMode(CalendarMode.TIMETABLE)
        val s = vm.state.first { it.timetable != null }
        assertEquals(30, s.timetable?.rows?.size)
    }

    @Test fun hijriToGregorianConversionAndErrors() = runTest {
        val vm = vm()
        vm.convertHijri(1448, 9, 1)
        val ok = vm.state.first { it.conversion != null }
        assertEquals(LocalDate.of(2027, 2, 8), ok.conversion?.gregorian)
        assertFalse(ok.conversionError)

        vm.convertHijri(1448, 13, 1)
        val bad = vm.state.first { it.conversionError }
        assertNotNull(bad)
        assertEquals(null, bad.conversion)
    }

    @Test fun primarySystemIsPersisted() = runTest {
        val vm = vm()
        vm.setHijriPrimary(false)
        assertFalse(vm.state.first { !it.hijriPrimary }.hijriPrimary)
    }

    @Test fun offsetSettingChangesTodayHijriDay() = runTest {
        val common = CommonSettings(store())
        common.setHijriAdjustDays(1)
        val vm = vm(common)
        val s = vm.state.first { it.grid.weeks.isNotEmpty() }
        // 2027-02-10 da +1 tuzatish: Umm al-Qura 2027-02-09 = 2-Ramazon, lekin 1-kun 02-09 bo'lgani uchun 2.
        assertEquals(2, s.today.day)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class IslamicEventsViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun defaultsAndUpdatesArePersisted() = runTest {
        val file = File(Files.createTempDirectory("ev").toFile(), "e.preferences_pb")
        val ds = PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)) { file }
        val prefs = object : uz.xumoyun.feature.calendar.domain.EventPrefsStore {
            override val settings = ds.data.map { p ->
                uz.xumoyun.feature.calendar.domain.IslamicEvent.entries.associateWith { e ->
                    uz.xumoyun.feature.calendar.domain.EventSetting(
                        enabled = p[androidx.datastore.preferences.core.booleanPreferencesKey("${e.key}_on")] ?: e.enabledByDefault,
                        remindDayBefore = p[androidx.datastore.preferences.core.booleanPreferencesKey("${e.key}_remind")] ?: false,
                    )
                }
            }
            override suspend fun set(event: IslamicEvent, setting: uz.xumoyun.feature.calendar.domain.EventSetting) {
                ds.edit {
                    it[androidx.datastore.preferences.core.booleanPreferencesKey("${event.key}_on")] = setting.enabled
                    it[androidx.datastore.preferences.core.booleanPreferencesKey("${event.key}_remind")] = setting.remindDayBefore
                }
            }
        }
        val vm = IslamicEventsViewModel(prefs)
        val initial = vm.rows.first { it.isNotEmpty() }
        assertFalse(initial.first { it.event == IslamicEvent.RAJAB_27 }.setting.enabled)

        vm.setEnabled(IslamicEvent.RAJAB_27, true)
        vm.setRemindDayBefore(IslamicEvent.RAMADAN_1, true)
        val updated = vm.rows.first { rows -> rows.first { it.event == IslamicEvent.RAJAB_27 }.setting.enabled }
        assertTrue(updated.first { it.event == IslamicEvent.RAMADAN_1 }.setting.remindDayBefore)
    }
}
