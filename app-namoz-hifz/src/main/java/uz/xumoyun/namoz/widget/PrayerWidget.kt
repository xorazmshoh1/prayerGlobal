package uz.xumoyun.namoz.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import uz.xumoyun.namoz.MainActivity
import uz.xumoyun.namoz.R
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.Schedule
import uz.xumoyun.feature.prayer.ui.TimeFormat
import uz.xumoyun.feature.prayer.ui.prayerName
import java.time.LocalDate
import java.time.ZonedDateTime

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun repository(): PrayerRepository
}

private data class WidgetData(
    val city: String,
    val nextName: String,
    val nextTime: String,
    val rows: List<Triple<String, String, Boolean>>,
)

/** Bosh ekran vidjeti: bugungi vaqtlar va keyingi namoz (Jetpack Glance). */
class PrayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).repository()
        val config = repo.currentConfig()
        val data = config?.let {
            val now = ZonedDateTime.now(it.zone)
            val today = it.day(LocalDate.now(it.zone))
            val next = Schedule.next(now, today, it.day(today.date.plusDays(1)))
            WidgetData(
                city = it.location.name,
                nextName = context.getString(prayerName(next.prayer)),
                nextTime = TimeFormat.format(context, next.time),
                rows = Prayer.entries.map { p ->
                    Triple(context.getString(prayerName(p)), TimeFormat.format(context, today[p]), p == next.prayer)
                },
            )
        }
        provideContent { Content(context, data) }
    }

    @Composable
    private fun Content(context: Context, data: WidgetData?) {
        val bg = ColorProvider(Color(0xFF00695C))
        val white = ColorProvider(Color.White)
        val gold = ColorProvider(Color(0xFFC9A227))
        Column(
            GlanceModifier.fillMaxSize().background(bg).cornerRadius(16.dp).padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            if (data == null) {
                Text(context.getString(R.string.widget_no_location), style = TextStyle(color = white))
                return@Column
            }
            Text(data.city, style = TextStyle(color = white, fontSize = 13.sp))
            Text(
                context.getString(R.string.widget_next, data.nextName, data.nextTime),
                style = TextStyle(color = gold, fontSize = 17.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(6.dp))
            data.rows.forEach { (name, time, isNext) ->
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val style = TextStyle(color = if (isNext) gold else white, fontSize = 13.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal)
                    Text(name, style = style, modifier = GlanceModifier.defaultWeight())
                    Text(time, style = style)
                }
            }
        }
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
