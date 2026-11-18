package uz.xumoyun.feature.prayer.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class LocationLabel { NONE, HOME, WORK, TRAVEL }

/**
 * Saqlangan joy (TZ 7-bo'lim, LocationEntity). Jadval ilovaning Room bazasida
 * (app-namoz-hifz: NamozDatabase) — feature modul faqat entity va DAO ni beradi.
 */
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
