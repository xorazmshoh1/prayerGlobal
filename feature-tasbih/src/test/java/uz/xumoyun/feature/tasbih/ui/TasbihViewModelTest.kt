package uz.xumoyun.feature.tasbih.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import uz.xumoyun.feature.tasbih.data.TasbihDao
import uz.xumoyun.feature.tasbih.data.TasbihEntry
import uz.xumoyun.feature.tasbih.data.TasbihSettings
import uz.xumoyun.feature.tasbih.domain.Dhikr
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TasbihViewModelTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private lateinit var time: FixedTimeSource

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        time = FixedTimeSource(at(2026, 11, 17, 10), zone)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun at(y: Int, m: Int, d: Int, h: Int) = ZonedDateTime.of(y, m, d, h, 0, 0, 0, zone).toInstant()

    private class FakeDao : TasbihDao {
        val rows = MutableStateFlow<List<TasbihEntry>>(emptyList())
        private var nextId = 1L

        override fun observeSince(fromDate: String): Flow<List<TasbihEntry>> =
            rows.map { list -> list.filter { it.date >= fromDate }.sortedWith(compareByDescending<TasbihEntry> { it.date }.thenBy { it.id }) }

        override suspend fun insert(entry: TasbihEntry): Long {
            val id = nextId++
            rows.value = rows.value + entry.copy(id = id)
            return id
        }

        override suspend fun updateCount(id: Long, count: Int, target: Int) {
            rows.value = rows.value.map { if (it.id == id) it.copy(count = count, target = target) else it }
        }
    }

    private fun settings(): TasbihSettings {
        val dir = Files.createTempDirectory("ds").toFile()
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { File(dir, "settings.preferences_pb") },
        )
        return TasbihSettings(store)
    }

    @Test fun tapReachesTargetAndPersistsSession() = runTest {
        val dao = FakeDao()
        val vm = TasbihViewModel(dao, settings(), time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        repeat(32) { assertFalse(vm.tap()) }
        assertTrue(vm.tap())
        val row = dao.rows.value.single()
        assertEquals(33, row.count)
        assertEquals(33, row.target)
        assertEquals("SUBHANALLAH", row.dhikrKey)
        assertEquals("2026-11-17", row.date)
    }

    @Test fun resetKeepsSavedHistory() = runTest {
        val dao = FakeDao()
        val vm = TasbihViewModel(dao, settings(), time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        repeat(5) { vm.tap() }
        vm.reset()
        vm.tap(); vm.tap()
        advanceUntilIdle()
        assertEquals(listOf(5, 2), dao.rows.value.map { it.count })
        assertEquals(2, vm.state.value.count)
        assertEquals(7, vm.state.value.todayTotal)
    }

    @Test fun switchingDhikrStartsNewRow() = runTest {
        val dao = FakeDao()
        val vm = TasbihViewModel(dao, settings(), time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        repeat(3) { vm.tap() }
        vm.select(Dhikr.ALLAHU_AKBAR)
        vm.tap()
        advanceUntilIdle()
        assertEquals(listOf("SUBHANALLAH", "ALLAHU_AKBAR"), dao.rows.value.map { it.dhikrKey })
        assertEquals(1, vm.state.value.count)
        assertEquals(4, vm.state.value.todayTotal)
    }

    @Test fun selectingSameDhikrKeepsSession() = runTest {
        val vm = TasbihViewModel(FakeDao(), settings(), time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        vm.tap(); vm.tap()
        vm.select(Dhikr.SUBHANALLAH)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.count)
    }

    @Test fun targetChangePersistsAndDrivesReach() = runTest {
        val s = settings()
        val dao = FakeDao()
        val vm = TasbihViewModel(dao, s, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.setTarget(99)
        assertEquals(99, vm.state.first { it.target == 99 }.target)
        repeat(98) { assertFalse(vm.tap()) }
        assertTrue(vm.tap())
        assertEquals(99, dao.rows.value.single().target)
    }

    @Test fun invalidTargetIsIgnored() = runTest {
        val s = settings()
        val vm = TasbihViewModel(FakeDao(), s, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.setTarget(0)
        vm.setTarget(100_001)
        advanceUntilIdle()
        assertEquals(33, s.target.first())
    }

    @Test fun midnightStartsNewRowForNewDay() = runTest {
        val dao = FakeDao()
        val vm = TasbihViewModel(dao, settings(), time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        time.instant = at(2026, 11, 17, 23)
        vm.tap(); vm.tap()
        time.instant = at(2026, 11, 18, 0)
        vm.tap()
        advanceUntilIdle()
        assertEquals(listOf("2026-11-17" to 2, "2026-11-18" to 1), dao.rows.value.map { it.date to it.count })
        assertEquals(1, vm.state.value.count)
    }

    @Test fun historyShowsSevenDaysFromSavedRows() = runTest {
        val dao = FakeDao()
        dao.rows.value = listOf(
            TasbihEntry(1, "2026-11-15", "SALAWAT", 40, 33),
            TasbihEntry(2, "2026-11-17", "SUBHANALLAH", 12, 33),
            TasbihEntry(3, "2026-11-17", "SALAWAT", 8, 33),
            TasbihEntry(4, "2026-11-01", "SALAWAT", 500, 33),
        )
        val vm = TasbihViewModel(dao, settings(), time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.history.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        val history = vm.history.value
        assertEquals(7, history.size)
        assertEquals(20, history[0].total)
        assertEquals(0, history[1].total)
        assertEquals(40, history[2].total)
        assertEquals(20, vm.state.value.todayTotal)
    }
}
