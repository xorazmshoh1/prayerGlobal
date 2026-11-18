package uz.xumoyun.namoz.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.xumoyun.feature.prayer.data.LocationDao
import uz.xumoyun.feature.prayer.data.SavedLocation

/** Tasbeh: har kun va har zikr bo'yicha hisob. */
@Entity(tableName = "tasbeh_day", primaryKeys = ["date", "dhikr"])
data class TasbehDay(val date: String, val dhikr: String, val count: Int)

@Dao
interface TasbehDao {
    @Query("SELECT * FROM tasbeh_day WHERE date = :date")
    fun observeDay(date: String): Flow<List<TasbehDay>>

    @Query("SELECT count FROM tasbeh_day WHERE date = :date AND dhikr = :dhikr")
    suspend fun count(date: String, dhikr: String): Int?

    @Upsert
    suspend fun upsert(day: TasbehDay)
}

/** Ilova bazasi: joylar (feature-prayer entity'si) va tasbeh (4-bosqichda feature-tasbih'ga ko'chadi). */
@Database(entities = [SavedLocation::class, TasbehDay::class], version = 1, exportSchema = true)
abstract class NamozDatabase : RoomDatabase() {
    abstract fun locations(): LocationDao
    abstract fun tasbeh(): TasbehDao
}
