package uz.xumoyun.namoz.backup

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import uz.xumoyun.core.common.FixedTimeSource
import java.time.Instant
import java.time.ZoneOffset

/** TZ 7-bo'lim: "eksport qilib import qilganda barcha jadvallar bir xil qoladi". */
class BackupRoundTripTest {
    private val time = FixedTimeSource(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC)

    @Test fun exportThenRestoreIntoEmptyStoreKeepsEveryTableAndPref() = runTest {
        val source = FakeBackupStore(schema = 5, contents = FakeBackupStore.sample())
        val text = BackupService(source, time).export()

        val target = FakeBackupStore(schema = 5)
        val summary = BackupService(target, time).restore(text)

        assertEquals(source.readAll(), target.readAll())
        assertEquals(BackupFormat.TABLES.size, summary.tables)
        assertEquals(FakeBackupStore.sample().tables.values.sumOf { it.size }, summary.rows)
        assertEquals(6, summary.prefs)
    }

    @Test fun secondRoundTripIsByteIdentical() = runTest {
        val store = FakeBackupStore(schema = 5, contents = FakeBackupStore.sample())
        val first = BackupService(store, time).export()
        BackupService(store, time).restore(first)
        val second = BackupService(store, time).export()
        assertEquals(first, second)
    }

    @Test fun emptyDatabaseRoundTrips() = runTest {
        val source = FakeBackupStore()
        val text = BackupService(source, time).export()
        val target = FakeBackupStore(contents = FakeBackupStore.sample())
        BackupService(target, time).restore(text)
        assertEquals(FakeBackupStore.emptyContents(), target.readAll())
    }

    @Test fun formatValuesSurviveEncodeDecode() {
        val file = BackupFile(
            schemaVersion = 5, exportedAt = 1L,
            prefs = listOf(PrefEntry("tags", PrefType.STRING_SET, JsonArray(listOf(JsonPrimitive("x"))))),
            tables = mapOf("qada_event" to listOf(mapOf("delta" to JsonPrimitive(-1L), "note" to JsonPrimitive("1"), "avg" to JsonPrimitive(1.0)))),
        )
        val back = BackupFormat.decode(BackupFormat.encode(file), currentSchema = 5)
        assertEquals(file, back)
    }

    @Test fun rejectsForeignFileWithoutTouchingStore() = runTest {
        val store = FakeBackupStore(contents = FakeBackupStore.sample())
        val before = store.readAll()
        expectError(BackupError.NOT_A_BACKUP) { BackupService(store, time).restore("{\"hello\":1}") }
        expectError(BackupError.NOT_A_BACKUP) { BackupService(store, time).restore("not json") }
        assertEquals(before, store.readAll())
    }

    @Test fun rejectsNewerVersionAndNewerSchema() = runTest {
        val store = FakeBackupStore(schema = 5)
        val future = BackupFormat.encode(BackupFile(version = BackupFormat.VERSION + 1, schemaVersion = 5, exportedAt = 0))
        expectError(BackupError.NEWER_VERSION) { BackupService(store, time).restore(future) }
        val newerDb = BackupFormat.encode(BackupFile(schemaVersion = 6, exportedAt = 0))
        expectError(BackupError.NEWER_SCHEMA) { BackupService(store, time).restore(newerDb) }
    }

    @Test fun rejectsUnknownTableAndUnsafeColumnName() = runTest {
        val store = FakeBackupStore()
        val badTable = BackupFormat.encode(BackupFile(schemaVersion = 5, exportedAt = 0, tables = mapOf("sqlite_master" to emptyList())))
        expectError(BackupError.INVALID) { BackupService(store, time).restore(badTable) }
        val badColumn = BackupFormat.encode(
            BackupFile(schemaVersion = 5, exportedAt = 0, tables = mapOf("qada_event" to listOf(mapOf("x; DROP TABLE t" to JsonPrimitive(1L))))),
        )
        expectError(BackupError.INVALID) { BackupService(store, time).restore(badColumn) }
    }

    @Test fun rejectsNestedValueAndWrongPrefType() = runTest {
        val store = FakeBackupStore()
        val nested = "{\"format\":\"namoz-hifz-backup\",\"version\":1,\"schemaVersion\":5,\"exportedAt\":0," +
            "\"tables\":{\"qada_event\":[{\"delta\":{\"a\":1}}]}}"
        expectError(BackupError.INVALID) { BackupService(store, time).restore(nested) }
        val wrongType = BackupFormat.encode(
            BackupFile(schemaVersion = 5, exportedAt = 0, prefs = listOf(PrefEntry("language", PrefType.BOOL, JsonPrimitive("uz")))),
        )
        expectError(BackupError.INVALID) { BackupService(store, time).restore(wrongType) }
    }

    @Test fun exportCarriesFormatAndVersionMarkers() = runTest {
        val text = BackupService(FakeBackupStore(contents = FakeBackupStore.sample()), time).export()
        assertTrue(text.contains("\"format\":\"namoz-hifz-backup\""))
        assertTrue(text.contains("\"version\":1"))
    }

    @Test fun fileWithoutFormatMarkerIsRejected() = runTest {
        val noFormat = "{\"schemaVersion\":5,\"exportedAt\":0,\"tables\":{}}"
        expectError(BackupError.NOT_A_BACKUP) { BackupService(FakeBackupStore(), time).restore(noFormat) }
    }

    @Test fun exportOmitsUnknownTablesAndContainsSchemaVersion() = runTest {
        val store = FakeBackupStore(schema = 5, contents = FakeBackupStore.sample())
        val file = BackupFormat.decode(BackupService(store, time).export(), currentSchema = 5)
        assertEquals(5, file.schemaVersion)
        assertEquals(BackupFormat.TABLES.toSet(), file.tables.keys)
        assertTrue(file.exportedAt > 0)
    }

    private suspend fun expectError(expected: BackupError, block: suspend () -> Unit) {
        try {
            block()
            fail("expected $expected")
        } catch (e: BackupException) {
            assertEquals(expected, e.error)
        }
    }
}
