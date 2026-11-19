package uz.xumoyun.feature.ramadan.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.ramadan.R
import uz.xumoyun.feature.ramadan.data.RamadanPrefs
import uz.xumoyun.feature.ramadan.domain.RamadanCalc
import uz.xumoyun.feature.ramadan.domain.RamadanPhase
import uz.xumoyun.feature.ramadan.domain.RamadanSchedulePort
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Ramazon vidjeti (TZ 11-bo'lim): bugungi saharlik va iftor, Ramazon kuni. Ma'lumot Hilt EntryPoint orqali. */
class RamadanWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context, RamadanWidgetEntryPoint::class.java)
        val now = entry.time().now()
        val zoneAndTimes = runCatching {
            val offset = entry.common().hijriAdjustDays.first()
            val cal = entry.hijri().calendar(offset)
            val today = entry.time().today()
            val start = RamadanCalc.upcomingStart(today, cal)
            val settings = entry.prefs().settings.first()
            val sched = entry.schedule().days(kotlinx.coroutines.flow.flowOf(today), 1).first()
            val t = sched?.days?.firstOrNull { it.date == today }
            WidgetData(
                phase = start?.let { RamadanCalc.phase(today, it) },
                day = start?.let { RamadanCalc.dayNumber(today, it) },
                daysLeft = start?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) },
                suhoor = t?.let { RamadanCalc.imsak(it.fajr, settings.imsakOffsetMin) },
                iftar = t?.maghrib,
                zone = sched?.zone,
            )
        }.getOrNull()
        provideContent { Content(context, zoneAndTimes, now) }
    }

    @Composable
    private fun Content(context: Context, data: WidgetData?, now: Instant) {
        val background = ColorProvider(Color(0xFF0F5C4D))
        val white = ColorProvider(Color.White)
        val gold = ColorProvider(Color(0xFFE2C062))
        Column(GlanceModifier.fillMaxSize().background(background).cornerRadius(16.dp).padding(12.dp)) {
            when {
                data == null || data.phase == null -> Text(context.getString(R.string.widget_ramadan_no_location), style = TextStyle(color = white))
                data.phase == RamadanPhase.BEFORE -> Text(
                    context.getString(R.string.ramadan_starts_in, data.daysLeft?.toInt() ?: 0),
                    style = TextStyle(color = gold, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                data.phase == RamadanPhase.AFTER -> Text(context.getString(R.string.ramadan_next_year), style = TextStyle(color = white))
                else -> {
                    Text(context.getString(R.string.ramadan_day_title, data.day ?: 0), style = TextStyle(color = gold, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Text(
                        context.getString(R.string.suhoor) + " " + time(context, data.suhoor, data.zone),
                        style = TextStyle(color = white, fontSize = 14.sp),
                    )
                    Text(
                        context.getString(R.string.iftar) + " " + time(context, data.iftar, data.zone),
                        style = TextStyle(color = white, fontSize = 14.sp),
                    )
                }
            }
        }
    }

    private fun time(context: Context, instant: Instant?, zone: ZoneId?): String {
        if (instant == null) return "—"
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(instant.atZone(zone ?: ZoneId.systemDefault()))
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { RamadanWidget().updateAll(context) }
        }
    }
}

/** Vidjet ma'lumoti (sof klass emas — faqat ko'rsatish uchun). */
private data class WidgetData(
    val phase: RamadanPhase?,
    val day: Int?,
    val daysLeft: Long?,
    val suhoor: Instant?,
    val iftar: Instant?,
    val zone: ZoneId?,
)

class RamadanWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RamadanWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RamadanWidgetEntryPoint {
    fun time(): TimeSource
    fun hijri(): HijriProvider
    fun common(): CommonSettings
    fun prefs(): RamadanPrefs
    fun schedule(): RamadanSchedulePort
}
