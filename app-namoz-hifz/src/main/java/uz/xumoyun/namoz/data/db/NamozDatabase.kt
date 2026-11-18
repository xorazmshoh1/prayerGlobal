package uz.xumoyun.namoz.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

enum class LocationLabel { NONE, HOME, WORK, TRAVEL }

@Entity(tableName = "saved_location")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val label: LocationLabel = LocationLabel.NONE,
    val fromGps: Boolean = false,
)

@Dao
interface LocationDao {
    @Query("SELECT * FROM saved_location ORDER BY id")
    fun observeAll(): Flow<List<SavedLocation>>

    @Query("SELECT * FROM saved_location WHERE id = :id")
    suspend fun get(id: Long): SavedLocation?

    @Query("SELECT * FROM saved_location ORDER BY id LIMIT 1")
    suspend fun first(): SavedLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: SavedLocation): Long

    @Query("DELETE FROM saved_location WHERE id = :id")
    suspend fun delete(id: Long)
}

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

@Database(entities = [SavedLocation::class, TasbehDay::class], version = 1, exportSchema = true)
abstract class NamozDatabase : RoomDatabase() {
    abstract fun locations(): LocationDao
    abstract fun tasbeh(): TasbehDao
}
