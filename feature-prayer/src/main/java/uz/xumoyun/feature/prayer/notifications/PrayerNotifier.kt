package uz.xumoyun.feature.prayer.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uz.xumoyun.feature.prayer.R
import uz.xumoyun.feature.prayer.domain.AlarmKind
import uz.xumoyun.feature.prayer.domain.AlertMode
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerChannelIds
import uz.xumoyun.feature.prayer.ui.TimeFormat
import uz.xumoyun.feature.prayer.ui.prayerName
import java.time.ZonedDateTime

/** Namoz bildirishnomasi: namoz kanaliga (azon) yoki eslatmalar kanaliga. */
internal object PrayerNotifier {

    // Ruxsat NotificationAccess.enabled() bilan tekshiriladi (POST_NOTIFICATIONS, Android 13+).
    @SuppressLint("MissingPermission")
    fun show(
        context: Context,
        prayer: Prayer,
        kind: AlarmKind,
        mode: AlertMode,
        prayerTime: ZonedDateTime,
        minutesLeft: Int,
    ) {
        if (mode == AlertMode.OFF || !NotificationAccess.enabled(context)) return

        val name = context.getString(prayerName(prayer))
        val title = when {
            kind == AlarmKind.BEFORE -> context.getString(R.string.notif_reminder_title, name, minutesLeft)
            prayer == Prayer.SUNRISE -> context.getString(R.string.notif_sunrise_title)
            else -> context.getString(R.string.notif_title, name)
        }
        val text = context.getString(R.string.notif_text, name, TimeFormat.format(context, prayerTime))
        val id = PrayerChannelIds.notificationId(prayer, kind)
        val atTime = kind == AlarmKind.AT_TIME && prayer.isPrayer

        val builder = NotificationCompat.Builder(context, PrayerChannelIds.forAlarm(prayer, kind))
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (atTime) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setWhen(prayerTime.toInstant().toEpochMilli())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setSilent(mode == AlertMode.SILENT)
        openAppIntent(context, id)?.let(builder::setContentIntent)
        // Android 7.x da kanal yo'q: ovoz va tebranish bildirishnomaning o'zida.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && mode == AlertMode.SOUND) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }
        // Eslatma namoz vaqti kirganda o'z-o'zidan yo'qoladi.
        if (kind == AlarmKind.BEFORE) builder.setTimeoutAfter(minutesLeft * 60_000L)

        val manager = NotificationManagerCompat.from(context)
        if (kind == AlarmKind.AT_TIME) manager.cancel(PrayerChannelIds.notificationId(prayer, AlarmKind.BEFORE))
        try {
            manager.notify(id, builder.build())
        } catch (_: SecurityException) {
            // Ruxsat shu orada olib qo'yilgan.
        }
    }

    /** Ilova moduliga bog'lanmaslik uchun ishga tushirish intent'i (MainActivity) paket bo'yicha olinadi. */
    private fun openAppIntent(context: Context, requestCode: Int): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, requestCode, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
