package uz.xumoyun.namoz.backup

import android.database.Cursor
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import uz.xumoyun.namoz.data.db.DB_SCHEMA_VERSION
import uz.xumoyun.namoz.data.db.NamozDatabase
import javax.inject.Inject

/**
 * [BackupStore] ning Room + DataStore amalga oshirilishi. Jadvallar [BackupFormat.TABLES] bo'yicha xom SQL
 * orqali o'qiladi (ustun nomlari bilan). Almashtirish bitta SQLite tranzaksiyasida; sozlamalar undan keyin yoziladi.
 */
class RoomBackupStore @Inject constructor(
    private val db: NamozDatabase,
    private val prefs: DataStore<Preferences>,
) : BackupStore {

    override fun schemaVersion(): Int = DB_SCHEMA_VERSION

    override suspend fun readAll(): BackupContents = withContext(Dispatchers.IO) {
        val sqlite = db.openHelper.readableDatabase
        val tables = BackupFormat.TABLES.associateWith { readTable(sqlite, it) }
        BackupContents(tables = tables, prefs = readPrefs())
    }

    override suspend fun replaceAll(contents: BackupContents) {
        withContext(Dispatchers.IO) {
            val sqlite = db.openHelper.writableDatabase
            sqlite.beginTransaction()
            try {
                BackupFormat.TABLES.reversed().forEach { sqlite.execSQL("DELETE FROM `$it`") }
                BackupFormat.TABLES.forEach { table ->
                    contents.tables[table].orEmpty().forEach { row -> insertRow(sqlite, table, row) }
                }
                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }
        }
        writePrefs(contents.prefs)
    }

    private fun readTable(sqlite: SupportSQLiteDatabase, table: String): List<Map<String, JsonElement>> =
        sqlite.query("SELECT * FROM `$table`").use { c ->
            val names = c.columnNames
            buildList {
                while (c.moveToNext()) {
                    val row = LinkedHashMap<String, JsonElement>(names.size)
                    names.forEachIndexed { i, name -> row[name] = c.toJson(i) }
                    add(row)
                }
            }
        }

    private fun insertRow(sqlite: SupportSQLiteDatabase, table: String, row: Map<String, JsonElement>) {
        if (row.isEmpty()) return
        val columns = row.keys.toList()
        val sql = "INSERT INTO `$table` (" + columns.joinToString(",") { "`$it`" } +
            ") VALUES (" + columns.joinToString(",") { "?" } + ")"
        sqlite.execSQL(sql, columns.map { bind(row.getValue(it)) }.toTypedArray())
    }

    private suspend fun readPrefs(): List<PrefEntry> = prefs.data.first().asMap().mapNotNull { (key, value) ->
        val name = key.name
        when (value) {
            is Boolean -> PrefEntry(name, PrefType.BOOL, JsonPrimitive(value))
            is Int -> PrefEntry(name, PrefType.INT, JsonPrimitive(value))
            is Long -> PrefEntry(name, PrefType.LONG, JsonPrimitive(value))
            is Float -> PrefEntry(name, PrefType.DOUBLE, JsonPrimitive(value.toDouble()))
            is Double -> PrefEntry(name, PrefType.DOUBLE, JsonPrimitive(value))
            is String -> PrefEntry(name, PrefType.STRING, JsonPrimitive(value))
            is Set<*> -> PrefEntry(name, PrefType.STRING_SET, JsonArray(value.map { JsonPrimitive(it.toString()) }))
            else -> null
        }
    }.sortedBy { it.name }

    private suspend fun writePrefs(entries: List<PrefEntry>) {
        prefs.edit { p ->
            p.clear()
            entries.forEach { e ->
                val raw = (e.value as? JsonPrimitive)?.content
                when (e.type) {
                    PrefType.BOOL -> raw?.let { p[booleanPreferencesKey(e.name)] = it.toBoolean() }
                    PrefType.INT -> raw?.toIntOrNull()?.let { p[intPreferencesKey(e.name)] = it }
                    PrefType.LONG -> raw?.toLongOrNull()?.let { p[longPreferencesKey(e.name)] = it }
                    PrefType.DOUBLE -> raw?.toDoubleOrNull()?.let { p[doublePreferencesKey(e.name)] = it }
                    PrefType.STRING -> raw?.let { p[stringPreferencesKey(e.name)] = it }
                    PrefType.STRING_SET -> (e.value as? JsonArray)?.let { arr ->
                        p[stringSetPreferencesKey(e.name)] = arr.mapNotNull { (it as? JsonPrimitive)?.content }.toSet()
                    }
                }
            }
        }
    }

    private companion object {
        fun Cursor.toJson(i: Int): JsonElement = when (getType(i)) {
            Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(getLong(i))
            Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(getDouble(i))
            Cursor.FIELD_TYPE_STRING -> JsonPrimitive(getString(i))
            else -> JsonNull
        }

        /** Matn qiymati matn bo'lib qoladi; raqamli matn son sifatida (ustun turi bo'yicha) yoziladi. */
        fun bind(value: JsonElement): Any? {
            if (value is JsonNull) return null
            val p = value as? JsonPrimitive ?: return null
            if (p.isString) return p.content
            return p.content.toLongOrNull() ?: p.content.toDoubleOrNull() ?: p.content
        }
    }
}
