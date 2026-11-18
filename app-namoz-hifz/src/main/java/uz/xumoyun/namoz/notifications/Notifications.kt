package uz.xumoyun.namoz.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import uz.xumoyun.namoz.MainActivity
import uz.xumoyun.namoz.R
import uz.xumoyun.feature.prayer.data.AlertSound
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.ui.prayerName
import java.time.Instant
import java.time.ZoneId

object Notifications {
    private const val CHANNEL_AZAN = "prayer_azan"
    private const val CHANNEL_BEEP = "prayer_beep"
    private const val CHANNEL_SILENT = "prayer_silent"
    private const val CHANNEL_REMINDER = "prayer_reminder"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        // Azon ovozi: res/raw/azan.ogg qo'shilganda shu yerga "android.resource://<paket>/raw/azan" ulanadi.
        // Hozircha tizimning signal (alarm) ovozi ishlatiladi.
        val azan = NotificationChannel(CHANNEL_AZAN, context.getString(R.string.channel_azan), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), attrs)
        }
        val beep = NotificationChannel(CHANNEL_BEEP, context.getString(R.string.channel_beep), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attrs)
        }
        val silent = NotificationChannel(CHANNEL_SILENT, context.getString(R.string.channel_silent), NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(null, null)
            enableVibration(false)
        }
        val reminder = NotificationChannel(CHANNEL_REMINDER, context.getString(R.string.channel_reminder), NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannels(listOf(azan, beep, silent, reminder))
    }

    fun show(context: Context, prayer: Prayer, prayerTime: Instant, reminder: Boolean, sound: AlertSound, minutesBefore: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val channel = when {
            reminder -> CHANNEL_REMINDER
            sound == AlertSound.AZAN -> CHANNEL_AZAN
            sound == AlertSound.BEEP -> CHANNEL_BEEP
            else -> CHANNEL_SILENT
        }
        val name = context.getString(prayerName(prayer))
        val time = uz.xumoyun.feature.prayer.ui.TimeFormat.format(context, prayerTime.atZone(ZoneId.systemDefault()))
        val title = if (reminder) context.getString(R.string.notif_reminder_title, name, minutesBefore)
        else context.getString(R.string.notif_title, name)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_text, name, time))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(prayer.ordinal + if (reminder) 100 else 0, notification)
    }
}
