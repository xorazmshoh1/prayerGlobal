package uz.xumoyun.feature.tasbih.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbihDao {
    /** [fromDate] (yyyy-MM-dd) dan boshlab barcha seanslar. ISO sana matn sifatida tartiblanadi. */
    @Query("SELECT * FROM tasbih_entry WHERE date >= :fromDate ORDER BY date DESC, id ASC")
    fun observeSince(fromDate: String): Flow<List<TasbihEntry>>

    @Insert
    suspend fun insert(entry: TasbihEntry): Long

    @Query("UPDATE tasbih_entry SET count = :count, target = :target WHERE id = :id")
    suspend fun updateCount(id: Long, count: Int, target: Int)
}
