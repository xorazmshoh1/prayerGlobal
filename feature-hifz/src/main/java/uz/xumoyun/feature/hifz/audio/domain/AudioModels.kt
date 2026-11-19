package uz.xumoyun.feature.hifz.audio.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Sura va oyat chegaralari (Tanzil). Eng uzun sura — 286 oyat (Baqara). */
const val MAX_AYAHS_PER_SURAH = 286

/**
 * Qori konfiguratsiyasi (`assets/reciters.json`). URL shabloni va qori ID'si kodga qattiq yozilmaydi.
 * [todo] bo'sh bo'lmasa, qiymat hali tasdiqlanmagan (TZ 4-bo'lim, ochiq savol).
 */
@Serializable
data class ReciterConfig(
    val key: String,
    val name: String,
    val urlTemplate: String,
    val format: String = "mp3",
    val bitrateKbps: Int = 64,
    val todo: String? = null,
)

@Serializable
private data class ReciterFile(val reciters: List<ReciterConfig>)

/** Yuklangan sura haqida yozuv (AudioDownload jadvali). */
data class DownloadedSura(
    val reciterKey: String,
    val surah: Int,
    val bytes: Long,
    val downloadedAt: Long,
)

object ReciterCatalog {
    private val keyPattern = Regex("^[a-z0-9_]{1,40}$")
    private val allowedFormats = setOf("mp3")
    private val anyPlaceholder = Regex("\\{[^}]*\\}")
    private val json = Json { ignoreUnknownKeys = true }

    /** Kalit yo'l qismiga kiradi, shuning uchun faqat kichik lotin harf, raqam va "_". */
    fun isValidKey(key: String): Boolean = keyPattern.matches(key)

    /** Konfiguratsiyani o'qiydi va tekshiradi. Noto'g'ri fayl [IllegalArgumentException] beradi. */
    fun parse(text: String): List<ReciterConfig> {
        val list = json.decodeFromString(ReciterFile.serializer(), text).reciters
        require(list.isNotEmpty()) { "reciters: ro'yxat bo'sh" }
        require(list.map { it.key }.distinct().size == list.size) { "reciters: key takrorlangan" }
        list.forEach(::validate)
        return list
    }

    private fun validate(r: ReciterConfig) {
        require(isValidKey(r.key)) { "reciter '${r.key}': key noto'g'ri" }
        require(r.name.isNotBlank()) { "reciter '${r.key}': nomi bo'sh" }
        require(r.format in allowedFormats) { "reciter '${r.key}': format ${r.format} qo'llanmaydi" }
        require(r.bitrateKbps in 8..320) { "reciter '${r.key}': bitrate ${r.bitrateKbps}" }
        require(r.urlTemplate.startsWith("https://")) { "reciter '${r.key}': URL https bo'lishi shart" }
        require("{surah3}" in r.urlTemplate && "{ayah3}" in r.urlTemplate) {
            "reciter '${r.key}': shablonda {surah3} va {ayah3} bo'lishi shart"
        }
        val unknown = anyPlaceholder.findAll(r.urlTemplate).map { it.value }.toSet() -
            setOf("{surah3}", "{ayah3}")
        require(unknown.isEmpty()) { "reciter '${r.key}': noma'lum o'rin: $unknown" }
    }
}

/** Oyat audiosining URL'ini shablondan quradi. */
object AyahUrl {
    fun build(reciter: ReciterConfig, surah: Int, ayah: Int): String {
        require(surah in 1..114) { "sura: $surah" }
        require(ayah in 1..MAX_AYAHS_PER_SURAH) { "oyat: $ayah" }
        return reciter.urlTemplate
            .replace("{surah3}", pad3(surah))
            .replace("{ayah3}", pad3(ayah))
    }

    internal fun pad3(n: Int): String = n.toString().padStart(3, '0')
}
