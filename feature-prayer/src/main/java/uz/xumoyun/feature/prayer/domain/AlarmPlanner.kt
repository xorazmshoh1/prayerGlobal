package uz.xumoyun.feature.prayer.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Signal turi: namoz vaqtining o'zida yoki undan oldin eslatma. */
enum class AlarmKind { AT_TIME, BEFORE }

/** AlarmManager'ga qanday qo'yiladi (TZ 8-bo'lim). */
sealed interface AlarmDelivery {
    /** SCHEDULE_EXACT_ALARM bor (yoki Android 11 va past): setExactAndAllowWhileIdle. */
    data object Exact : AlarmDelivery

    /** Ruxsat yo'q: setWindow(start, length). */
    data class Window(val start: Instant, val length: Duration) : AlarmDelivery
}

data class PlannedAlarm(
    /** PendingIntent request kodi: (kun, namoz, tur) bo'yicha barqaror — qayta rejalashtirish eskisini almashtiradi. */
    val requestCode: Int,
    /** Hisoblash kuni (DayTimes.date). Yuqori kengliklarda Xufton ertangi kalendar kuniga o'tishi mumkin. */
    val date: LocalDate,
    val prayer: Prayer,
    val kind: AlarmKind,
    val mode: AlertMode,
    val prayerTime: Instant,
    val triggerAt: Instant,
    val delivery: AlarmDelivery,
)

/** Vidjetni keyingi vaqt kirganda yangilash (uyg'otmaydigan signal). */
data class PlannedRefresh(val at: Instant, val delivery: AlarmDelivery)

data class AlarmPlan(
    val alarms: List<PlannedAlarm>,
    /** Bekor qilinadigan eski kodlar (o'chirilgan namoz, eslatma olib tashlangan, o'tgan kunlar). */
    val staleCodes: List<Int>,
    val refresh: PlannedRefresh?,
)

/**
 * Qaysi signallar qo'yilishini hisoblaydi — Android'siz, soxta soat bilan unit test qilinadi.
 *
 * - Oyna: hozirdan [HORIZON] (2 kun) ichidagi vaqtlar. Kunlik WorkManager va har vaqt kirganda
 *   qayta hisoblanadi, shuning uchun oyna doim oldinga suriladi.
 * - Aniq signal ruxsati yo'q bo'lsa [AlarmDelivery.Window]: Android 12+ da oyna kamida 10 daqiqa
 *   (tizim kichigini 10 ga cho'zadi). Eslatma ±5 daqiqa ([trigger − 5, trigger + 5]); namoz vaqtidagi
 *   bildirishnoma esa vaqt kirmasdan chiqmasligi uchun [vaqt, vaqt + 10].
 */
object AlarmPlanner {
    val HORIZON: Duration = Duration.ofDays(2)
    val WINDOW: Duration = Duration.ofMinutes(10)
    private val HALF_WINDOW: Duration = WINDOW.dividedBy(2)

    /** Signal juda kech kelsa (masalan Doze), bildirishnoma ko'rsatilmaydi. */
    val LATE_LIMIT: Duration = Duration.ofMinutes(30)

    private const val CODE_DAY_CYCLE = 1000
    private const val CODES_PER_DAY = 16

    /** Vidjetni yangilash signalining kodi (namoz kodlari 0..15999). */
    const val REFRESH_REQUEST_CODE = 20_000

    fun requestCode(date: LocalDate, prayer: Prayer, kind: AlarmKind): Int =
        Math.floorMod(date.toEpochDay(), CODE_DAY_CYCLE.toLong()).toInt() * CODES_PER_DAY + prayer.ordinal * 2 + kind.ordinal

    fun codesFor(date: LocalDate): List<Int> =
        Prayer.entries.flatMap { p -> AlarmKind.entries.map { requestCode(date, p, it) } }

    /** Hisoblanadigan kunlar: kecha (Xufton yarim tundan keyin bo'lishi mumkin) … indinga. */
    fun planningDates(today: LocalDate): List<LocalDate> = (-1L..2L).map(today::plusDays)

    /** Avvalgi rejalardan qolgan bo'lishi mumkin bo'lgan kunlar (joy/vaqt mintaqasi o'zgarishi bilan). */
    fun cleanupDates(today: LocalDate): List<LocalDate> = (-3L..3L).map(today::plusDays)

    fun plan(
        now: Instant,
        zone: ZoneId,
        alerts: Map<Prayer, PrayerAlert>,
        exactAllowed: Boolean,
        dayTimes: (LocalDate) -> DayTimes,
    ): AlarmPlan {
        val today = now.atZone(zone).toLocalDate()
        val end = now.plus(HORIZON)
        val days = planningDates(today).map(dayTimes)

        val alarms = days.flatMap { day ->
            Prayer.entries.flatMap { prayer ->
                alarmsFor(day, prayer, alerts[prayer] ?: PrayerAlert.default(prayer), now, end, exactAllowed)
            }
        }.sortedBy { it.triggerAt }

        val used = alarms.mapTo(HashSet()) { it.requestCode }
        val stale = cleanupDates(today).flatMap(::codesFor).filterNot { it in used }

        val refreshAt = days.flatMap { d -> d.ordered.map { it.second.toInstant() } }.filter { it.isAfter(now) }.minOrNull()
        val refresh = refreshAt?.let { PlannedRefresh(it, delivery(AlarmKind.AT_TIME, it, now, exactAllowed)) }
        return AlarmPlan(alarms, stale, refresh)
    }

    /** Joy tanlanmagan: hamma signal bekor qilinadi. */
    fun cancelAll(now: Instant, zone: ZoneId): AlarmPlan =
        AlarmPlan(emptyList(), cleanupDates(now.atZone(zone).toLocalDate()).flatMap(::codesFor), null)

    private fun alarmsFor(
        day: DayTimes,
        prayer: Prayer,
        alert: PrayerAlert,
        now: Instant,
        end: Instant,
        exact: Boolean,
    ): List<PlannedAlarm> {
        if (alert.mode == AlertMode.OFF) return emptyList()
        val time = day[prayer].toInstant()
        val result = ArrayList<PlannedAlarm>(2)
        fun add(kind: AlarmKind, trigger: Instant) {
            if (trigger.isAfter(now) && !trigger.isAfter(end)) {
                result += PlannedAlarm(
                    requestCode(day.date, prayer, kind), day.date, prayer, kind, alert.mode,
                    time, trigger, delivery(kind, trigger, now, exact),
                )
            }
        }
        add(AlarmKind.AT_TIME, time)
        val before = PrayerAlert.clampMinutes(alert.minutesBefore)
        if (before > 0) add(AlarmKind.BEFORE, time.minus(Duration.ofMinutes(before.toLong())))
        return result
    }

    fun delivery(kind: AlarmKind, trigger: Instant, now: Instant, exact: Boolean): AlarmDelivery {
        if (exact) return AlarmDelivery.Exact
        return when (kind) {
            AlarmKind.AT_TIME -> AlarmDelivery.Window(trigger, WINDOW)
            AlarmKind.BEFORE -> {
                val start = maxOf(now, trigger.minus(HALF_WINDOW))
                AlarmDelivery.Window(start, Duration.between(start, trigger.plus(HALF_WINDOW)))
            }
        }
    }

    /** Signal kelganda: bildirishnoma hali ma'noliymi (eslatma — namoz kirmaguncha, vaqt — [LATE_LIMIT] ichida). */
    fun shouldNotify(now: Instant, kind: AlarmKind, prayerTime: Instant): Boolean = when (kind) {
        AlarmKind.AT_TIME -> !now.isAfter(prayerTime.plus(LATE_LIMIT))
        AlarmKind.BEFORE -> now.isBefore(prayerTime)
    }

    /** Eslatma sarlavhasi uchun qolgan daqiqalar (yuqoriga yaxlitlangan, kamida 1). */
    fun minutesUntil(now: Instant, prayerTime: Instant): Int {
        val seconds = Duration.between(now, prayerTime).seconds
        return ((seconds + 59) / 60).toInt().coerceAtLeast(1)
    }
}

/** Kanal va bildirishnoma ID'lari (Android'siz; kanallarning o'zi notifications/PrayerChannels.kt da). */
object PrayerChannelIds {
    const val GROUP = "prayer_times"

    /** Namoz oldidan eslatmalar va Quyosh chiqishi. */
    const val REMINDER = "prayer_reminder"

    /** 1–2-bosqichdagi umumiy kanallar — 3-bosqichda har namozga alohida kanal bilan almashtirildi. */
    val OBSOLETE = listOf("prayer_azan", "prayer_beep", "prayer_silent")

    /** 5 ta namoz kanali: prayer_fajr, prayer_dhuhr, prayer_asr, prayer_maghrib, prayer_isha. */
    fun prayerChannel(prayer: Prayer): String {
        require(prayer.isPrayer) { "Quyosh chiqishi uchun alohida kanal yo'q" }
        return "prayer_" + prayer.name.lowercase()
    }

    val prayerChannels: List<String> get() = Prayer.entries.filter { it.isPrayer }.map(::prayerChannel)

    fun forAlarm(prayer: Prayer, kind: AlarmKind): String =
        if (kind == AlarmKind.AT_TIME && prayer.isPrayer) prayerChannel(prayer) else REMINDER

    private const val NOTIFICATION_BASE = 1000

    /** Bildirishnoma ID'si: har namoz va tur uchun bittadan (yangisi eskisini almashtiradi). */
    fun notificationId(prayer: Prayer, kind: AlarmKind): Int = NOTIFICATION_BASE + prayer.ordinal * 2 + kind.ordinal
}
