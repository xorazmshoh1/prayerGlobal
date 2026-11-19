package uz.xumoyun.namoz.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import uz.xumoyun.feature.prayer.data.LocationDao
import uz.xumoyun.feature.prayer.data.SavedLocation
import uz.xumoyun.feature.tasbih.data.TasbihDao
import uz.xumoyun.feature.tasbih.data.TasbihEntry

/**
 * Ilova bazasi: joylar (feature-prayer) va tasbeh seanslari (feature-tasbih).
 * v2: tasbeh_day o'rniga tasbih_entry (seanslar); eski ma'lumot [MIGRATION_1_2] orqali ko'chiriladi.
 */
@Database(entities = [SavedLocation::class, TasbihEntry::class], version = 2, exportSchema = true)
abstract class NamozDatabase : RoomDatabase() {
    abstract fun locations(): LocationDao
    abstract fun tasbeh(): TasbihDao
}
