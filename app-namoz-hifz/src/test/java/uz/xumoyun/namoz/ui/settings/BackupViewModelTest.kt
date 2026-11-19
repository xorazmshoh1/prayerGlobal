package uz.xumoyun.namoz.ui.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import uz.xumoyun.namoz.backup.BackupError
import uz.xumoyun.namoz.backup.BackupService
import uz.xumoyun.namoz.backup.FakeBackupStore
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val time = FixedTimeSource(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun exportWritesJsonAndReportsExported() = runTest {
        val store = FakeBackupStore(contents = FakeBackupStore.sample())
        val vm = BackupViewModel(BackupService(store, time))
        var written = ""
        vm.exportTo { written = it }.join()
        assertTrue(written.contains("namoz-hifz-backup"))
        assertEquals(BackupStatus.Exported, vm.status.first())
    }

    @Test fun restoreReportsSummary() = runTest {
        val source = FakeBackupStore(contents = FakeBackupStore.sample())
        val text = BackupService(source, time).export()
        val target = FakeBackupStore()
        val vm = BackupViewModel(BackupService(target, time))
        vm.restoreFrom { text }.join()
        val status = vm.status.first()
        assertTrue(status is BackupStatus.Restored)
        assertEquals(source.readAll().prefs.size, (status as BackupStatus.Restored).summary.prefs)
    }

    @Test fun badFileReportsErrorAndKeepsData() = runTest {
        val store = FakeBackupStore(contents = FakeBackupStore.sample())
        val before = store.readAll()
        val vm = BackupViewModel(BackupService(store, time))
        vm.restoreFrom { "garbage" }.join()
        assertEquals(BackupStatus.Failed(BackupError.NOT_A_BACKUP), vm.status.first())
        assertEquals(before, store.readAll())
    }

    @Test fun ioFailureIsReportedAsIo() = runTest {
        val vm = BackupViewModel(BackupService(FakeBackupStore(), time))
        vm.exportTo { throw java.io.IOException("disk") }.join()
        assertEquals(BackupStatus.Failed(BackupError.IO), vm.status.first())
        vm.clearStatus()
        assertEquals(BackupStatus.Idle, vm.status.first())
    }
}
