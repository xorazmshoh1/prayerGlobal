package uz.xumoyun.namoz.backup

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** DataStore qiymati turi. Eksportda va importda bir xil ishlatiladi. */
@Serializable
enum class PrefType { BOOL, INT, LONG, DOUBLE, STRING, STRING_SET }

/** Bitta DataStore kaliti: nomi, turi va JSON qiymati (STRING_SET — JSON massiv). */
@Serializable
data class PrefEntry(val name: String, val type: PrefType, val value: JsonElement)

/**
 * Zaxira fayli (TZ 13-bosqich, 17-funksiya). [tables] — jadval nomi → qatorlar; har qator — ustun nomi → qiymat
 * (JsonNull, son yoki matn). Jadval va ustun nomlari shu fayl ichida saqlanadi, shuning uchun sxema
 * o'zgarganda ham qatorlar ustun nomi bo'yicha tiklanadi.
 */
@Serializable
data class BackupFile(
    val format: String = BackupFormat.FORMAT,
    val version: Int = BackupFormat.VERSION,
    val schemaVersion: Int,
    val exportedAt: Long,
    val prefs: List<PrefEntry> = emptyList(),
    val tables: Map<String, List<Map<String, JsonElement>>> = emptyMap(),
)

/** Ichki (jadval/prefs) tarkib — saqlagich bilan o'qish/yozish uchun. */
data class BackupContents(
    val tables: Map<String, List<Map<String, JsonElement>>>,
    val prefs: List<PrefEntry>,
)

/** Xato turlari: UI har biriga alohida matn ko'rsatadi. */
enum class BackupError { NOT_A_BACKUP, NEWER_VERSION, NEWER_SCHEMA, INVALID, IO }

class BackupException(val error: BackupError, message: String = error.name) : Exception(message)

object BackupFormat {
    const val FORMAT = "namoz-hifz-backup"
    /** Fayl formati versiyasi. Yangi maydon qo'shilsa oshiriladi; eski o'qiladigan bo'lib qoladi. */
    const val VERSION = 1

    /** Zaxiraga kiradigan barcha user.db jadvallari (TZ 13-bosqich: LocationEntity, PrayerLog, Qada*, Hifz*, Tasbih, Audio). */
    val TABLES: List<String> = listOf(
        "saved_location", "prayer_log", "qada_counter", "qada_event",
        "tasbih_entry", "audio_download",
        "hifz_plan", "ayah_progress", "review_session",
    )

    private val IDENT = Regex("^[A-Za-z_][A-Za-z0-9_]{0,63}$")

    @OptIn(ExperimentalSerializationApi::class)
    val json: Json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        // format/version standart qiymatli: ularni fayldan chiqarib tashlamaslik uchun majburiy.
        encodeDefaults = true
        explicitNulls = true
    }

    /**
     * Faylni o'qiydi va tekshiradi: format, versiya, sxema, jadval/ustun nomlari, qiymat turlari.
     * [currentSchema] — ilovadagi bazaning versiyasi; undan yangi zaxira rad etiladi.
     */
    fun decode(text: String, currentSchema: Int): BackupFile {
        // Format belgisi standart qiymatga ishonilmasdan, faylning o'zidan tekshiriladi.
        val root = try {
            json.parseToJsonElement(text) as? JsonObject ?: throw IllegalArgumentException("not an object")
        } catch (e: Exception) {
            throw BackupException(BackupError.NOT_A_BACKUP, e.message ?: "parse")
        }
        val format = (root["format"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (format != FORMAT) throw BackupException(BackupError.NOT_A_BACKUP, format ?: "no format")
        val file = try {
            json.decodeFromJsonElement(BackupFile.serializer(), root)
        } catch (e: Exception) {
            throw BackupException(BackupError.INVALID, e.message ?: "decode")
        }
        if (file.version > VERSION) throw BackupException(BackupError.NEWER_VERSION, file.version.toString())
        if (file.schemaVersion > currentSchema) throw BackupException(BackupError.NEWER_SCHEMA, file.schemaVersion.toString())
        validate(file)
        return file
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    private fun validate(file: BackupFile) {
        file.tables.forEach { (table, rows) ->
            if (table !in TABLES) throw BackupException(BackupError.INVALID, "table $table")
            rows.forEach { row ->
                row.forEach { (column, value) ->
                    if (!IDENT.matches(column)) throw BackupException(BackupError.INVALID, "column $column")
                    if (value !is JsonNull && value !is JsonPrimitive) throw BackupException(BackupError.INVALID, "value of $table.$column")
                }
            }
        }
        file.prefs.forEach { p ->
            if (p.name.isBlank() || p.name.length > 200) throw BackupException(BackupError.INVALID, "pref name")
            val ok = when (p.type) {
                PrefType.BOOL -> (p.value as? JsonPrimitive)?.booleanOrNull != null
                PrefType.INT, PrefType.LONG -> (p.value as? JsonPrimitive)?.longOrNull != null
                PrefType.DOUBLE -> (p.value as? JsonPrimitive)?.doubleOrNull != null
                PrefType.STRING -> (p.value as? JsonPrimitive)?.isString == true
                PrefType.STRING_SET -> (p.value as? JsonArray)?.all { (it as? JsonPrimitive)?.isString == true } == true
            }
            if (!ok) throw BackupException(BackupError.INVALID, "pref ${p.name}")
        }
    }

    /** Matn qiymatini tekshirmasdan qaytaradi (testlar va ko'rsatish uchun). */
    fun textOf(value: JsonElement): String? = (value as? JsonPrimitive)?.contentOrNull
}
