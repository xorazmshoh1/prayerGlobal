package uz.xumoyun.feature.hifz.ui.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.feature.hifz.domain.AyahText
import uz.xumoyun.feature.hifz.domain.QuranSource
import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.HifzRepository
import uz.xumoyun.feature.hifz.plan.domain.LessonState
import uz.xumoyun.feature.hifz.plan.domain.ReviewType
import uz.xumoyun.feature.hifz.plan.domain.QuranLayout
import uz.xumoyun.feature.hifz.plan.domain.buildDailyLesson
import uz.xumoyun.feature.hifz.plan.domain.lessonItemsOf
import uz.xumoyun.feature.hifz.plan.domain.lessonSessions
import uz.xumoyun.feature.hifz.plan.domain.next
import uz.xumoyun.feature.hifz.plan.domain.previous
import uz.xumoyun.feature.hifz.plan.domain.rate
import uz.xumoyun.feature.hifz.plan.domain.revealNextWord
import uz.xumoyun.feature.hifz.plan.domain.toggleHidden
import uz.xumoyun.feature.hifz.plan.domain.arabicWords
import uz.xumoyun.feature.hifz.audio.domain.PlaybackSpeed
import uz.xumoyun.feature.hifz.plan.domain.reviewAyah
import uz.xumoyun.feature.hifz.domain.SurahInfo

data class LessonUiState(
    val loading: Boolean = true,
    val quranAvailable: Boolean = true,
    val failed: Boolean = false,
    val noPlan: Boolean = false,
    val surahs: List<SurahInfo> = emptyList(),
    val session: LessonState = LessonState(emptyList()),
    /** Navbatdagi oyatlarning matni (global id bo'yicha). */
    val texts: Map<Int, AyahText> = emptyMap(),
    val repeatTimes: Int = 1,
    val speed: Float = PlaybackSpeed.DEFAULT,
    val playing: Boolean = false,
    val audioMissing: Boolean = false,
    val persisted: Boolean = false,
)

/**
 * Dars rejimi. [ARG_TYPE] (SavedStateHandle) — "SABAQ", "SABQI", "MANZIL" yoki yo'q (hammasi).
 * Har baho darhol progress'ga yoziladi; seanslar dars tugaganda yoki chiqishda yoziladi.
 */
@HiltViewModel
class LessonViewModel @Inject constructor(
    private val repository: HifzRepository,
    private val quran: QuranSource,
    private val audio: LessonAudio,
    private val time: TimeSource,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val typeFilter: ReviewType? = savedState.get<String>(ARG_TYPE)?.let { runCatching { ReviewType.valueOf(it) }.getOrNull() }
    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    private var startedAt: Instant = time.now()
    private var today: LocalDate = time.today()
    private var progress: MutableMap<Int, AyahProgress> = HashMap()
    private var layout: QuranLayout? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        try {
            if (!quran.isAvailable()) {
                _state.update { it.copy(loading = false, quranAvailable = false) }
                return
            }
            val surahs = quran.surahs()
            val juzStarts = quran.juzStarts()
            if (surahs.isEmpty() || juzStarts.isEmpty() || juzStarts.first() != 1) {
                _state.update { it.copy(loading = false, quranAvailable = false) }
                return
            }
            val q = QuranLayout(surahs, juzStarts)
            layout = q
            val plan = repository.observeActivePlan().first()
            if (plan == null) {
                _state.update { it.copy(loading = false, noPlan = true, surahs = surahs) }
                return
            }
            today = time.today()
            startedAt = time.now()
            progress = repository.observeProgress().first().toMutableMap()
            val sessions = repository.sessionsBetween(today.minusDays(SABQI_LOOKBACK), today)
            val newToday = sessions.filter { it.date == today && it.type == ReviewType.SABAQ }.sumOf { it.ayahTo - it.ayahFrom + 1 }
            val lesson = buildDailyLesson(
                today = today,
                planAyahIds = plan.ayahIds,
                dailyNewAyahs = plan.dailyNewAyahs,
                newAyahsToday = newToday,
                progress = progress,
                sessions = sessions,
                juzOf = q::juzOf,
            )
            val items = lessonItemsOf(lesson).filter { typeFilter == null || it.type == typeFilter }
            val texts = if (items.isEmpty()) emptyMap() else quran.ayahsByIds(items.map { it.ayahId }).associateBy { it.globalId }
            _state.update {
                it.copy(
                    loading = false,
                    surahs = surahs,
                    session = LessonState(items),
                    texts = texts,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, failed = true) }
        }
    }

    /** Joriy oyatni baholaydi (5 oson, 3 qiyin, 1 unutdim), progress'ni saqlaydi. Dars tugasa seanslar yoziladi. */
    fun rate(quality: Int) {
        val s = _state.value
        val cur = s.session.current ?: return
        val updated = reviewAyah(
            ayahGlobalId = cur.ayahId,
            previous = progress[cur.ayahId],
            quality = quality,
            today = today,
            now = time.now(),
        )
        progress[cur.ayahId] = updated
        val next = s.session.rate(quality)
        audio.pause()
        _state.update { it.copy(session = next, playing = false) }
        viewModelScope.launch { repository.saveProgress(listOf(updated)) }
        if (next.finished) viewModelScope.launch { persist() }
    }

    /** Oldingi yoki keyingi oyatga o'tadi (baholamasdan). */
    fun move(forward: Boolean) {
        val s = _state.value.session
        audio.pause()
        _state.update { it.copy(session = if (forward) s.next() else s.previous(), playing = false) }
    }

    fun toggleHidden() = _state.update { it.copy(session = it.session.toggleHidden()) }

    fun revealWord() {
        val s = _state.value
        val text = s.session.current?.let { s.texts[it.ayahId]?.text } ?: return
        _state.update { it.copy(session = it.session.revealNextWord(arabicWords(text).size)) }
    }

    fun changeRepeat(delta: Int) = _state.update {
        it.copy(repeatTimes = (it.repeatTimes + delta).coerceIn(REPEAT_MIN, REPEAT_MAX))
    }

    fun changeSpeed(up: Boolean) {
        val steps = PlaybackSpeed.steps
        val current = steps.indexOfFirst { kotlin.math.abs(it - _state.value.speed) < 0.001f }.let { if (it < 0) steps.indexOf(PlaybackSpeed.DEFAULT) else it }
        val idx = (current + if (up) 1 else -1).coerceIn(0, steps.lastIndex)
        val speed = steps[idx]
        audio.setSpeed(speed)
        _state.update { it.copy(speed = speed) }
    }

    fun togglePlay() {
        val s = _state.value
        if (s.playing) {
            audio.pause()
            _state.update { it.copy(playing = false) }
            return
        }
        val text = s.session.current?.let { s.texts[it.ayahId] } ?: return
        val started = audio.play(text.surahId, text.numberInSurah, s.repeatTimes, s.speed)
        _state.update { it.copy(playing = started, audioMissing = !started) }
    }

    /** Chiqish: qisman baholangan seanslar saqlanadi, keyin [onDone] (navigatsiya) chaqiriladi. */
    fun close(onDone: () -> Unit) {
        viewModelScope.launch {
            persist()
            audio.pause()
            onDone()
        }
    }

    private suspend fun persist() {
        val s = _state.value
        if (s.persisted || s.session.ratedCount == 0) {
            _state.update { it.copy(persisted = true) }
            return
        }
        val duration = Duration.between(startedAt, time.now()).seconds.coerceAtLeast(0).toInt()
        repository.saveSessions(lessonSessions(today, s.session.items, s.session.ratings, duration))
        _state.update { it.copy(persisted = true) }
    }

    override fun onCleared() {
        audio.release()
    }

    companion object {
        const val ARG_TYPE = "type"
        const val REPEAT_MIN = 1
        const val REPEAT_MAX = 10
        const val SABQI_LOOKBACK = 8L
    }
}
