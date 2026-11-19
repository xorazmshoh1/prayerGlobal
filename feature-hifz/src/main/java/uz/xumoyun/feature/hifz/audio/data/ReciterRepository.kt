package uz.xumoyun.feature.hifz.audio.data

import uz.xumoyun.core.data.AssetSource
import uz.xumoyun.feature.hifz.audio.domain.ReciterCatalog
import uz.xumoyun.feature.hifz.audio.domain.ReciterConfig
import javax.inject.Inject
import javax.inject.Singleton

/** Qorilar ro'yxati: assets/reciters.json dan bir marta o'qiladi. */
@Singleton
class ReciterRepository @Inject constructor(private val assets: AssetSource) {
    val reciters: List<ReciterConfig> by lazy { ReciterCatalog.parse(assets.readText(FILE)) }

    fun find(key: String): ReciterConfig? = reciters.firstOrNull { it.key == key }

    private companion object {
        const val FILE = "reciters.json"
    }
}
