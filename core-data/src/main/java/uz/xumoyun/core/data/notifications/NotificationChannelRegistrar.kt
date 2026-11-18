package uz.xumoyun.core.data.notifications

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import uz.xumoyun.core.common.notifications.ChannelCatalog
import uz.xumoyun.core.common.notifications.ChannelImportance
import uz.xumoyun.core.common.notifications.ChannelSound
import uz.xumoyun.core.common.notifications.ChannelSpec
import uz.xumoyun.core.common.notifications.NotificationChannelSource

/**
 * Modullar bergan kanallarni yaratadi (Android 8+; pastroqda NotificationManagerCompat hech narsa qilmaydi).
 * Har ishga tushishda chaqiriladi: mavjud kanalning nomi yangilanadi, ovozi/muhimligi esa foydalanuvchi
 * sozlamasi bo'lib qoladi (Android cheklovi).
 */
object NotificationChannelRegistrar {

    fun register(context: Context, sources: Collection<NotificationChannelSource>) {
        val catalog = ChannelCatalog.of(sources)
        val manager = NotificationManagerCompat.from(context)
        catalog.obsoleteChannelIds.forEach { manager.deleteNotificationChannel(it) }
        catalog.groups.forEach { group ->
            manager.createNotificationChannelGroup(
                NotificationChannelGroupCompat.Builder(group.id).setName(context.getString(group.nameRes)).build(),
            )
        }
        manager.createNotificationChannelsCompat(catalog.channels.map { it.toCompat(context) })
    }

    private fun ChannelSpec.toCompat(context: Context): NotificationChannelCompat {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val uri: Uri? = when (val s = sound) {
            ChannelSound.SystemNotification -> Settings.System.DEFAULT_NOTIFICATION_URI
            ChannelSound.SystemAlarm -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ChannelSound.None -> null
            is ChannelSound.Raw -> Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${s.rawResId}")
        }
        return NotificationChannelCompat.Builder(id, importance.toAndroid())
            .setName(context.getString(nameRes))
            .setGroup(groupId)
            .setSound(uri, if (uri == null) null else attributes)
            .setVibrationEnabled(vibrate)
            .build()
    }

    private fun ChannelImportance.toAndroid(): Int = when (this) {
        ChannelImportance.HIGH -> NotificationManagerCompat.IMPORTANCE_HIGH
        ChannelImportance.DEFAULT -> NotificationManagerCompat.IMPORTANCE_DEFAULT
        ChannelImportance.LOW -> NotificationManagerCompat.IMPORTANCE_LOW
    }
}

/** Kanal manbalari to'plami: har feature modul `@IntoSet` bilan qo'shadi; hech biri bo'lmasa ham bo'sh to'plam. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationChannelsModule {
    @Multibinds abstract fun channelSources(): Set<NotificationChannelSource>
}
