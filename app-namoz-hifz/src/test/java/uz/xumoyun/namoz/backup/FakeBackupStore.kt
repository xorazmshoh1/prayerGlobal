package uz.xumoyun.namoz.backup

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/** Xotiradagi zaxira saqlagichi: Room/DataStore'siz round-trip sinovi uchun. */
class FakeBackupStore(
    var schema: Int = 5,
    var contents: BackupContents = emptyContents(),
) : BackupStore {
    override fun schemaVersion(): Int = schema
    override suspend fun readAll(): BackupContents = contents
    override suspend fun replaceAll(contents: BackupContents) {
        this.contents = contents
    }

    companion object {
        fun emptyContents() = BackupContents(BackupFormat.TABLES.associateWith { emptyList() }, emptyList())

        /** Har turdagi qiymat va hamma jadvallar bor namunaviy ma'lumot. */
        fun sample(): BackupContents {
            val tables = BackupFormat.TABLES.associateWith { emptyList<Map<String, JsonElement>>() }.toMutableMap()
            tables["saved_location"] = listOf(
                mapOf(
                    "id" to JsonPrimitive(1L), "name" to JsonPrimitive("Tashkent"),
                    "countryCode" to JsonPrimitive("UZ"), "latitude" to JsonPrimitive(41.2995),
                    "longitude" to JsonPrimitive(69.2401), "timeZone" to JsonPrimitive("Asia/Tashkent"),
                    "label" to JsonPrimitive("HOME"), "fromGps" to JsonPrimitive(0L),
                ),
            )
            tables["prayer_log"] = listOf(
                mapOf("date" to JsonPrimitive("2026-11-19"), "prayer" to JsonPrimitive("FAJR"), "status" to JsonPrimitive("MISSED"), "updatedAt" to JsonPrimitive(1700000000123L)),
                mapOf("date" to JsonPrimitive("2026-11-19"), "prayer" to JsonPrimitive("ASR"), "status" to JsonPrimitive("PRAYED"), "updatedAt" to JsonPrimitive(2L)),
            )
            tables["qada_counter"] = listOf(
                mapOf("prayer" to JsonPrimitive("FAJR"), "remaining" to JsonPrimitive(366L), "initialEstimate" to JsonPrimitive(366L), "dailyGoal" to JsonPrimitive(2L), "updatedAt" to JsonPrimitive(3L)),
            )
            tables["qada_event"] = listOf(
                mapOf("id" to JsonPrimitive(1L), "prayer" to JsonPrimitive("FAJR"), "delta" to JsonPrimitive(-1L), "source" to JsonPrimitive("MANUAL"), "createdAt" to JsonPrimitive(4L)),
            )
            tables["tasbih_entry"] = listOf(
                mapOf("id" to JsonPrimitive(1L), "date" to JsonPrimitive("2026-11-19"), "dhikrKey" to JsonPrimitive("subhanallah"), "count" to JsonPrimitive(33L), "target" to JsonPrimitive(99L)),
            )
            tables["audio_download"] = listOf(
                mapOf("reciterKey" to JsonPrimitive("r1"), "surahId" to JsonPrimitive(1L), "bytes" to JsonPrimitive(123456789L), "downloadedAt" to JsonPrimitive(5L)),
            )
            tables["hifz_plan"] = listOf(
                mapOf(
                    "id" to JsonPrimitive(1L), "scope" to JsonPrimitive("JUZ"), "fromAyah" to JsonPrimitive(1L), "toAyah" to JsonPrimitive(6236L),
                    "dailyNewAyahs" to JsonPrimitive(5L), "reminderAfterPrayer" to JsonNull, "reminderOffsetMin" to JsonPrimitive(15L),
                    "startDate" to JsonPrimitive(6L), "isActive" to JsonPrimitive(1L),
                ),
            )
            tables["ayah_progress"] = listOf(
                mapOf(
                    "ayahGlobalId" to JsonPrimitive(1L), "state" to JsonPrimitive("LEARNING"), "easeFactor" to JsonPrimitive(2.5),
                    "intervalDays" to JsonPrimitive(6L), "repetitions" to JsonPrimitive(2L), "dueDate" to JsonPrimitive(7L),
                    "lastRating" to JsonPrimitive(5L), "lastReviewedAt" to JsonNull,
                ),
            )
            tables["review_session"] = listOf(
                mapOf("id" to JsonPrimitive(1L), "date" to JsonPrimitive(8L), "type" to JsonPrimitive("SABAQ"), "ayahFrom" to JsonPrimitive(1L), "ayahTo" to JsonPrimitive(5L), "durationSec" to JsonPrimitive(300L), "avgRating" to JsonPrimitive(4.25)),
            )
            val prefs = listOf(
                PrefEntry("onboarding_done", PrefType.BOOL, JsonPrimitive(true)),
                PrefEntry("hijri_offset", PrefType.INT, JsonPrimitive(1)),
                PrefEntry("last_seen", PrefType.LONG, JsonPrimitive(9_000_000_000L)),
                PrefEntry("ratio", PrefType.DOUBLE, JsonPrimitive(0.75)),
                PrefEntry("language", PrefType.STRING, JsonPrimitive("uz")),
                PrefEntry("tags", PrefType.STRING_SET, JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("1")))),
            )
            return BackupContents(tables, prefs)
        }
    }
}
