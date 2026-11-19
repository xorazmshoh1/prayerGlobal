package uz.xumoyun.feature.hifz.plan.domain

import java.time.LocalDate

/**
 * Kunlik dars (TZ 5-bo'lim 12-band, 7-bo'lim). Hammasi global oyat raqamlari, tartib bo'yicha.
 * - [sabaq]: rejadagi yangi (hali progress yozuvi yo'q) oyatlar, kunlik limit bo'yicha.
 * - [sabqi]: oxirgi 7 kunda (bugundan oldingi 7 kun) SABAQ seanslarida o'rganilgan oyatlar.
 * - [manzil]: dueDate <= bugun bo'lgan LEARNING/MEMORIZED oyatlar; eng ko'pi 1 juz.
 */
data class DailyLesson(val sabaq: List<Int>, val sabqi: List<Int>, val manzil: List<Int>)

/** Sabqi oynasi: bugundan oldingi necha kun (bugun kirmaydi; bugungi yangi oyatlar Sabaq'da). */
const val SABQI_WINDOW_DAYS = 7

/**
 * Bugungi darsni tuzadi. Sof funksiya: vaqt [today] orqali beriladi (TimeSource.today()).
 *
 * @param planAyahIds rejadagi oyatlar (global id, tartib bo'yicha) — HifzPlan.ayahIds yoki quran.db dan.
 * @param dailyNewAyahs kunlik yangi oyatlar limiti (>= 1).
 * @param newAyahsToday bugun allaqachon o'rganilgan yangi oyatlar soni; Sabaq shundan keyingi qoldiq bilan cheklanadi.
 * @param progress oyat id → holat. Rejadan tashqaridagi yozuvlar e'tiborga olinmaydi.
 * @param sessions seanslar tarixi; faqat [ReviewType.SABAQ] va Sabqi oynasiga tushganlari olinadi.
 * @param juzOf global oyat id → juz raqami (1..30). Manzil faqat bitta juz doirasida tuziladi.
 */
fun buildDailyLesson(
    today: LocalDate,
    planAyahIds: List<Int>,
    dailyNewAyahs: Int,
    newAyahsToday: Int,
    progress: Map<Int, AyahProgress>,
    sessions: List<ReviewSession>,
    juzOf: (Int) -> Int,
): DailyLesson {
    require(dailyNewAyahs >= 1) { "kunlik yangi oyatlar: $dailyNewAyahs" }
    require(newAyahsToday >= 0) { "bugungi yangi oyatlar: $newAyahsToday" }
    val planSet = planAyahIds.toHashSet()

    // Sabaq: hali progress yozuvi yo'q oyatlar, rejadagi tartibda; bugun qilinganini ayiramiz.
    val sabaqLimit = (dailyNewAyahs - newAyahsToday).coerceAtLeast(0)
    val sabaq = planAyahIds.asSequence()
        .filter { progress[it] == null }
        .take(sabaqLimit)
        .toList()

    // Sabqi: oxirgi 7 kunda o'rganilgan (SABAQ seanslari) va hozir rejada bo'lgan oyatlar.
    val sabqiFrom = today.minusDays(SABQI_WINDOW_DAYS.toLong())
    val sabqiTo = today.minusDays(1)
    val sabqi = sessions
        .asSequence()
        .filter { it.type == ReviewType.SABAQ && !it.date.isBefore(sabqiFrom) && !it.date.isAfter(sabqiTo) }
        .flatMap { (it.ayahFrom..it.ayahTo).asSequence() }
        .filter { it in planSet && progress[it] != null }
        .distinct()
        .sorted()
        .toList()

    // Manzil: takror muddati kelgan (dueDate <= bugun) LEARNING/MEMORIZED oyatlar.
    val due = planAyahIds.filter { id ->
        val p = progress[id] ?: return@filter false
        p.state != MemoState.NEW && !p.dueDate.isAfter(today)
    }
    val manzil = if (due.isEmpty()) {
        emptyList()
    } else {
        // Eng muddati o'tgan oyat qaysi juzda bo'lsa, shu juzning barcha muddati kelganlari (1 juz chegarasi).
        val oldest = due.minWith(compareBy<Int>({ progress.getValue(it).dueDate }, { it }))
        val juz = juzOf(oldest)
        due.filter { juzOf(it) == juz }.sorted()
    }

    return DailyLesson(sabaq = sabaq, sabqi = sabqi, manzil = manzil)
}
