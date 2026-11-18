package uz.xumoyun.core.common.notifications

/*
 * Bildirishnoma kanallari infratuzilmasi (TZ 8-bo'lim), Android'siz ta'rif.
 * Har bir modul o'z kanallarini [NotificationChannelSource] sifatida beradi (Hilt @IntoSet),
 * ilova ishga tushganda core-data'dagi NotificationChannelRegistrar hammasini yaratadi.
 * Hozir: 5 ta namoz kanali va eslatmalar (feature-prayer), "Hifz eslatmasi" (feature-hifz).
 * Keyin: "Saharlik", "Iftor" (feature-ramadan, 11-bosqich), "Islomiy sanalar" (feature-calendar, 10-bosqich) —
 * faqat yangi manba qo'shiladi, registrar o'zgarmaydi.
 */

enum class ChannelImportance { HIGH, DEFAULT, LOW }

/** Kanal ovozi. Android 8+ da kanal yaratilgandan keyin ovozini ilova o'zgartira olmaydi — yangi ID kerak. */
sealed interface ChannelSound {
    /** Tizimning standart bildirishnoma ovozi. */
    data object SystemNotification : ChannelSound

    /** Tizimning standart signal (budilnik) ovozi. */
    data object SystemAlarm : ChannelSound

    /** Ovozsiz. */
    data object None : ChannelSound

    /** res/raw dagi fayl (masalan azon, 30 soniyadan qisqa). */
    data class Raw(val rawResId: Int) : ChannelSound
}

data class ChannelGroupSpec(val id: String, val nameRes: Int)

data class ChannelSpec(
    val id: String,
    /** Kanal nomi — modulning string resursi (tizim sozlamalarida ko'rinadi). */
    val nameRes: Int,
    val importance: ChannelImportance,
    val sound: ChannelSound,
    val vibrate: Boolean = true,
    val groupId: String? = null,
)

/** Bitta modulning kanallari. */
interface NotificationChannelSource {
    val groups: List<ChannelGroupSpec> get() = emptyList()
    val channels: List<ChannelSpec>

    /** Eski versiyalarda yaratilgan va endi ishlatilmaydigan kanallar — o'chiriladi. */
    val obsoleteChannelIds: List<String> get() = emptyList()
}

/** Barcha manbalar birlashmasi; ID'lar takrorlanmasligini va guruhlar mavjudligini tekshiradi. */
data class ChannelCatalog(
    val groups: List<ChannelGroupSpec>,
    val channels: List<ChannelSpec>,
    val obsoleteChannelIds: List<String>,
) {
    companion object {
        fun of(sources: Collection<NotificationChannelSource>): ChannelCatalog {
            val groups = sources.flatMap { it.groups }
            val channels = sources.flatMap { it.channels }
            val obsolete = sources.flatMap { it.obsoleteChannelIds }.distinct()

            duplicates(groups.map { it.id }).let { require(it.isEmpty()) { "Takroriy kanal guruhi: $it" } }
            duplicates(channels.map { it.id }).let { require(it.isEmpty()) { "Takroriy kanal ID: $it" } }
            val groupIds = groups.map { it.id }.toSet()
            channels.filter { it.groupId != null && it.groupId !in groupIds }.let { missing ->
                require(missing.isEmpty()) { "Kanal guruhi topilmadi: ${missing.map { it.id to it.groupId }}" }
            }
            val active = channels.map { it.id }.toSet()
            obsolete.filter { it in active }.let { require(it.isEmpty()) { "Faol kanal eskirgan deb belgilangan: $it" } }
            return ChannelCatalog(groups, channels, obsolete)
        }

        private fun duplicates(ids: List<String>): Set<String> =
            ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    }
}
