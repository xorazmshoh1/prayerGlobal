package uz.xumoyun.namoz

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uz.xumoyun.namoz.data.db.TasbehDao
import uz.xumoyun.namoz.data.db.TasbehDay
import java.io.File
import java.nio.file.Files

fun tempDataStore(): DataStore<Preferences> {
    val dir = Files.createTempDirectory("ds").toFile()
    return PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { File(dir, "settings.preferences_pb") },
    )
}

class FakeTasbehDao : TasbehDao {
    val rows = MutableStateFlow<List<TasbehDay>>(emptyList())
    override fun observeDay(date: String) = rows.map { list -> list.filter { it.date == date } }
    override suspend fun count(date: String, dhikr: String) = rows.value.firstOrNull { it.date == date && it.dhikr == dhikr }?.count
    override suspend fun upsert(day: TasbehDay) {
        rows.value = rows.value.filterNot { it.date == day.date && it.dhikr == day.dhikr } + day
    }
}
