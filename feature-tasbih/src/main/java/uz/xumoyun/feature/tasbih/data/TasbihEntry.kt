package uz.xumoyun.feature.tasbih.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tasbeh seansi: bir sana, bir zikr, shu seansdagi hisob va maqsad. Reset yoki zikr almashganda
 * yangi qator ochiladi — eski qatorlar o'chmaydi (kunlik tarix shulardan yig'iladi).
 */
@Entity(tableName = "tasbih_entry")
data class TasbihEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO sana, yyyy-MM-dd. */
    val date: String,
    /** [uz.xumoyun.feature.tasbih.domain.Dhikr.name]. */
    val dhikrKey: String,
    val count: Int,
    val target: Int,
)
