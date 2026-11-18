package uz.xumoyun.feature.prayer.widget

import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import uz.xumoyun.feature.prayer.R
import uz.xumoyun.feature.prayer.domain.WidgetModel
import uz.xumoyun.feature.prayer.domain.WidgetSnapshot
import uz.xumoyun.feature.prayer.notifications.prayerEntryPoint
import uz.xumoyun.feature.prayer.ui.TimeFormat
import uz.xumoyun.feature.prayer.ui.prayerName

/**
 * Bosh ekran vidjeti (TZ 5-bo'lim, 6-band): keyingi namoz, qolgan vaqt va bugungi 6 vaqt (Jetpack Glance).
 * Qolgan vaqt — RemoteViews Chronometer (teskari sanoq, API 24+): vidjet har soniya qayta chizilmaydi.
 * Vaqt kirganda PrayerAlarmScheduler'ning yangilash signali vidjetni yangilaydi.
 */
class PrayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = prayerEntryPoint(context)
        val config = entry.repository().currentConfig()
        val snapshot = config?.let {
            val now = entry.timeSource().now().atZone(it.zone)
            val today = it.day(now.toLocalDate())
            WidgetModel.build(now, it.location.name, today, it.day(today.date.plusDays(1)))
        }
        val elapsedNow = SystemClock.elapsedRealtime()
        provideContent { Content(context, snapshot, elapsedNow) }
    }

    @Composable
    private fun Content(context: Context, data: WidgetSnapshot?, elapsedNow: Long) {
        // TZ 11-bo'lim: primary #0F5C4D fonida urg'u (qorong'i tema varianti, kontrast uchun).
        val background = ColorProvider(Color(0xFF0F5C4D))
        val white = ColorProvider(Color.White)
        val accent = ColorProvider(Color(0xFFE2C062))
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val base = GlanceModifier.fillMaxSize().background(background).cornerRadius(16.dp).padding(12.dp)
        Column(if (launch != null) base.clickable(actionStartActivity(launch)) else base) {
            if (data == null) {
                Text(context.getString(R.string.widget_no_location), style = TextStyle(color = white))
                return@Column
            }
            val next = data.next
            Text(data.city, style = TextStyle(color = white, fontSize = 13.sp))
            Text(
                context.getString(R.string.widget_next, context.getString(prayerName(next.prayer)), TimeFormat.format(context, next.time)),
                style = TextStyle(color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold),
            )
            AndroidRemoteViews(countdown(context, elapsedNow + next.remaining.toMillis()))
            Spacer(GlanceModifier.height(6.dp))
            data.rows.forEach { (prayer, time) ->
                val isNext = prayer == next.prayer && time == next.time
                val style = TextStyle(
                    color = if (isNext) accent else white,
                    fontSize = 13.sp,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                )
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(context.getString(prayerName(prayer)), style = style, modifier = GlanceModifier.defaultWeight())
                    Text(TimeFormat.format(context, time), style = style)
                }
            }
        }
    }

    private fun countdown(context: Context, baseElapsed: Long) =
        RemoteViews(context.packageName, R.layout.widget_countdown).apply {
            setChronometer(R.id.widget_countdown, baseElapsed, null, true)
            setChronometerCountDown(R.id.widget_countdown, true)
        }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { PrayerWidget().updateAll(context) }
        }
    }
}

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()
}
