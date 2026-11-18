package uz.xumoyun.feature.prayer.notifications

import uz.xumoyun.core.common.notifications.ChannelGroupSpec
import uz.xumoyun.core.common.notifications.ChannelImportance
import uz.xumoyun.core.common.notifications.ChannelSound
import uz.xumoyun.core.common.notifications.ChannelSpec
import uz.xumoyun.core.common.notifications.NotificationChannelSource
import uz.xumoyun.feature.prayer.R
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerChannelIds
import uz.xumoyun.feature.prayer.ui.prayerName

/**
 * TZ 8-bo'lim: har bir namoz uchun alohida kanal (5 ta) — foydalanuvchi tizim sozlamasida har birining
 * ovozini/tebranishini alohida o'zgartira oladi — va eslatmalar kanali (namoz oldidan, Quyosh chiqishi).
 */
object PrayerChannels : NotificationChannelSource {

    /**
     * Azon ovozi kanal ovozi sifatida beriladi (30 soniyadan kam).
     * TODO(TZ-ochiq-savol): azon audio faylining litsenziyasi hal bo'lmagan — hozircha tizim bildirishnoma ovozi.
     * Fayl qo'shilganda ChannelSound.Raw(R.raw.azan) va YANGI kanal ID'lari kerak (Android kanal ovozini
     * yaratilgandan keyin o'zgartirmaydi): eski ID'lar PrayerChannelIds.OBSOLETE ga qo'shiladi.
     */
    private val AZAN_SOUND: ChannelSound = ChannelSound.SystemNotification

    override val groups = listOf(ChannelGroupSpec(PrayerChannelIds.GROUP, R.string.channel_group_prayers))

    override val channels: List<ChannelSpec> =
        Prayer.entries.filter { it.isPrayer }.map { prayer ->
            ChannelSpec(
                id = PrayerChannelIds.prayerChannel(prayer),
                nameRes = prayerName(prayer),
                importance = ChannelImportance.HIGH,
                sound = AZAN_SOUND,
                groupId = PrayerChannelIds.GROUP,
            )
        } + ChannelSpec(
            id = PrayerChannelIds.REMINDER,
            nameRes = R.string.channel_reminder,
            importance = ChannelImportance.DEFAULT,
            sound = ChannelSound.SystemNotification,
            groupId = PrayerChannelIds.GROUP,
        )

    override val obsoleteChannelIds = PrayerChannelIds.OBSOLETE
}
