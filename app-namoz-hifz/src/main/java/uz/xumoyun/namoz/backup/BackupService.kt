package uz.xumoyun.namoz.backup

import uz.xumoyun.core.common.TimeSource
import javax.inject.Inject

/** Saqlagich (Room + DataStore) bilan ishlash shartnomasi. Room tomoni [RoomBackupStore] da. */
interface BackupStore {
    /** Ilovaning hozirgi baza sxemasi versiyasi. */
    fun schemaVersion(): Int
    suspend fun readAll(): BackupContents

    /** Hamma jadvallarni va DataStore'ni almashtiradi (bitta tranzaksiyada jadvallar uchun). */
    suspend fun replaceAll(contents: BackupContents)
}

data class BackupSummary(val tables: Int, val rows: Int, val prefs: Int)

/** Eksport va import: JSON matn ko'rinishida. Fayl I/O (SAF) UI qatlamida. */
class BackupService @Inject constructor(
    private val store: BackupStore,
    private val time: TimeSource,
) {
    /** Hamma jadvallar va sozlamalar — JSON matn. Ma'lumot qurilmadan chiqmaydi (faqat tanlangan faylga). */
    suspend fun export(): String {
        val contents = store.readAll()
        val file = BackupFile(
            schemaVersion = store.schemaVersion(),
            exportedAt = time.now().toEpochMilli(),
            prefs = contents.prefs,
            tables = BackupFormat.TABLES.associateWith { contents.tables[it].orEmpty() },
        )
        return BackupFormat.encode(file)
    }

    /** Tekshiradi va hamma ma'lumotni zaxiradagi bilan almashtiradi. Xato bo'lsa hech narsa o'zgarmaydi. */
    suspend fun restore(text: String): BackupSummary {
        val file = BackupFormat.decode(text, store.schemaVersion())
        val contents = BackupContents(
            tables = BackupFormat.TABLES.associateWith { file.tables[it].orEmpty() },
            prefs = file.prefs,
        )
        store.replaceAll(contents)
        return BackupSummary(
            tables = BackupFormat.TABLES.size,
            rows = contents.tables.values.sumOf { it.size },
            prefs = contents.prefs.size,
        )
    }
}
