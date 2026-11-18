package uz.xumoyun.feature.prayer

import android.location.Location
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import uz.xumoyun.core.data.AssetSource
import uz.xumoyun.core.data.HijriDate
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.prayer.data.LocationDao
import uz.xumoyun.feature.prayer.data.LocationProvider
import uz.xumoyun.feature.prayer.data.SavedLocation
import uz.xumoyun.feature.prayer.domain.City
import uz.xumoyun.feature.prayer.domain.TestData
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.util.Locale

/** Haqiqiy assets fayllarini diskdan o'qiydi (ishchi papka — feature-prayer). */
object FileAssets : AssetSource {
    override fun readText(path: String): String = TestData.asset(path)
}

fun tempDataStore(): DataStore<Preferences> {
    val dir = Files.createTempDirectory("ds").toFile()
    return PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { File(dir, "settings.preferences_pb") },
    )
}

class FakeLocationDao(initial: List<SavedLocation> = emptyList()) : LocationDao {
    val items = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<SavedLocation>> = items
    override suspend fun get(id: Long) = items.value.firstOrNull { it.id == id }
    override suspend fun first() = items.value.firstOrNull()
    override suspend fun insert(location: SavedLocation): Long {
        val id = (items.value.maxOfOrNull { it.id } ?: 0) + 1
        items.value = items.value + location.copy(id = id)
        return id
    }
    override suspend fun delete(id: Long) { items.value = items.value.filterNot { it.id == id } }
}

class FakeHijri : HijriProvider {
    val offsets = mutableListOf<Int>()
    override fun of(date: LocalDate, offsetDays: Int, locale: Locale): HijriDate {
        offsets += offsetDays
        return HijriDate(1 + offsetDays, 9, 1448, "${1 + offsetDays} Ramadan 1448")
    }
}

/** Joylashuv ruxsati yo'q (yoki joy aniqlanmaydi) — shahar qidiruviga o'tiladi. */
class FakeLocationProvider(var permission: Boolean = false) : LocationProvider {
    var requests = 0
    override fun hasPermission() = permission
    override suspend fun currentLocation(): Location? { requests++; return null }
    override fun toCity(location: Location) = City("x", "", 0.0, 0.0, "UTC")
}

val Tashkent = SavedLocation(id = 1, name = "Tashkent", countryCode = "UZ", latitude = 41.2995, longitude = 69.2401, timeZone = "Asia/Tashkent")
val Jakarta = SavedLocation(id = 2, name = "Jakarta", countryCode = "ID", latitude = -6.2088, longitude = 106.8456, timeZone = "Asia/Jakarta")
