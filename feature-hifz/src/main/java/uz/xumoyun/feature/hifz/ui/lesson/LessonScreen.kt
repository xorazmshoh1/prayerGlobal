package uz.xumoyun.feature.hifz.ui.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import uz.xumoyun.core.ui.components.EmptyState
import uz.xumoyun.feature.hifz.R
import uz.xumoyun.feature.hifz.domain.AyahText
import uz.xumoyun.feature.hifz.domain.QuranCatalog
import uz.xumoyun.feature.hifz.plan.domain.LessonItem
import uz.xumoyun.feature.hifz.plan.domain.LessonState
import uz.xumoyun.feature.hifz.plan.domain.Quality
import uz.xumoyun.feature.hifz.plan.domain.ReviewType
import uz.xumoyun.feature.hifz.plan.domain.arabicWords
import uz.xumoyun.feature.hifz.ui.QuranFont

/** Dars rejimi (TZ 6-bo'lim, ekran 5). Chiqishda qisman baholangan seanslar saqlanadi. */
@Composable
fun LessonRoute(onDone: () -> Unit) {
    val vm: LessonViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = { LessonTopBar(onBack = { vm.close(onDone) }) },
    ) { padding ->
        val body = Modifier.fillMaxSize().padding(padding).statusBarsPadding()
        when {
            state.loading -> Box(body, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.failed || !state.quranAvailable -> Box(body, contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.hifz_title),
                    message = stringResource(if (state.failed) R.string.hifz_load_failed else R.string.hifz_quran_missing),
                )
            }
            state.noPlan -> Box(body, contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.hifz_no_plan_title),
                    message = stringResource(R.string.hifz_no_plan_message),
                )
            }
            state.session.items.isEmpty() -> Box(body, contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.lesson_title),
                    message = stringResource(R.string.lesson_empty),
                )
            }
            state.session.finished -> Box(body, contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.lesson_done_title),
                    message = stringResource(R.string.lesson_done_message, state.session.ratedCount),
                    actionLabel = stringResource(R.string.lesson_finish),
                    onAction = { vm.close(onDone) },
                )
            }
            else -> ActiveLesson(state, vm, body)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.lesson_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.audio_back))
            }
        },
    )
}

@Composable
private fun ActiveLesson(state: LessonUiState, vm: LessonViewModel, modifier: Modifier) {
    val session = state.session
    val item: LessonItem = session.current ?: return
    val ayah: AyahText? = state.texts[item.ayahId]

    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.lesson_position, session.index + 1, session.items.size),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                typeLabel(item.type),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Oyat matni: ekranning taxminan 70%i. Matn o'ngdan chapga, markazda.
        Box(Modifier.weight(0.7f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AyahBody(ayah, session, state)
            }
        }

        // Pleyer paneli
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.move(forward = false) }, enabled = session.index > 0) {
                Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.lesson_previous))
            }
            FilledIconButton(onClick = { vm.togglePlay() }, modifier = Modifier.size(56.dp)) {
                Icon(
                    if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (state.playing) R.string.lesson_pause else R.string.lesson_play),
                )
            }
            IconButton(onClick = { vm.move(forward = true) }, enabled = session.index < session.items.lastIndex) {
                Icon(Icons.Rounded.SkipNext, stringResource(R.string.lesson_next))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Stepper(
                label = stringResource(R.string.lesson_repeat, state.repeatTimes),
                onLess = { vm.changeRepeat(-1) },
                onMore = { vm.changeRepeat(1) },
            )
            Stepper(
                label = stringResource(R.string.lesson_speed, "%.2f".format(state.speed)),
                onLess = { vm.changeSpeed(up = false) },
                onMore = { vm.changeSpeed(up = true) },
            )
        }
        if (state.audioMissing) {
            Text(
                stringResource(R.string.lesson_audio_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        // Yashirish va so'z ochish
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { vm.toggleHidden() }) {
                Text(stringResource(if (session.hidden) R.string.lesson_show_text else R.string.lesson_hide_text))
            }
            val total = ayah?.let { arabicWords(it.text).size } ?: 0
            TextButton(
                onClick = { vm.revealWord() },
                enabled = session.hidden && session.revealedWords < total,
            ) {
                Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.lesson_reveal_word))
            }
        }

        // Baholash: eng pastda
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.rate(Quality.EASY) }, enabled = ayah != null, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.lesson_rate_easy))
            }
            FilledTonalButton(onClick = { vm.rate(Quality.HARD) }, enabled = ayah != null, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.lesson_rate_hard))
            }
            FilledTonalButton(onClick = { vm.rate(Quality.FORGOT) }, enabled = ayah != null, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.lesson_rate_forgot))
            }
        }
    }
}

@Composable
private fun AyahBody(ayah: AyahText?, session: LessonState, state: LessonUiState) {
    if (ayah == null) {
        Text("—", style = MaterialTheme.typography.headlineMedium)
        return
    }
    val words = arabicWords(ayah.text)
    val shown = if (!session.hidden) {
        ayah.text
    } else {
        words.mapIndexed { i, w -> if (i < session.revealedWords) w else "•••" }.joinToString(" ")
    }
    val fontSize = QuranCatalog.FONT_DEFAULT_SP
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = shown,
            fontFamily = QuranFont.family(),
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.8f).sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            QuranCatalog.ayahMarker(ayah.numberInSurah),
            fontFamily = QuranFont.family(),
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.tertiary,
        )
        val surahName = state.surahs.firstOrNull { it.id == ayah.surahId }?.nameTranslit.orEmpty()
        Text(
            "$surahName · ${ayah.numberInSurah}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Stepper(label: String, onLess: () -> Unit, onMore: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onLess) { Icon(Icons.Rounded.Remove, stringResource(R.string.lesson_less)) }
        Text(label, style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = onMore) { Icon(Icons.Rounded.Add, stringResource(R.string.lesson_more)) }
    }
}

@Composable
private fun typeLabel(type: ReviewType): String = stringResource(
    when (type) {
        ReviewType.SABAQ -> R.string.hifz_card_sabaq
        ReviewType.SABQI -> R.string.hifz_card_sabqi
        ReviewType.MANZIL -> R.string.hifz_card_manzil
    },
)
