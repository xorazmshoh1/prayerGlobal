package uz.xumoyun.feature.hifz.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uz.xumoyun.core.ui.components.EmptyState
import uz.xumoyun.feature.hifz.R
import uz.xumoyun.feature.hifz.domain.AyahText
import uz.xumoyun.feature.hifz.domain.QuranCatalog

/**
 * Sura yoki juz o'qish ekrani. [onAddToHifz] — oyatdan hifzga qo'shish callback'i; hifz yadrosi
 * 7-bosqichda ulanadi. Hozircha ekran "keyingi yangilanishda" xabarini ko'rsatadi.
 */
@Composable
fun ReaderRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToHifz: (AyahText) -> Unit = {},
    vm: ReaderViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ReaderScreen(
        state = state,
        onBack = onBack,
        onFontChange = vm::changeFont,
        onAddToHifz = onAddToHifz,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onBack: () -> Unit,
    onFontChange: (Boolean) -> Unit,
    onAddToHifz: (AyahText) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val title = when {
        state.surah != null -> state.surah.nameTranslit
        state.juz != null -> stringResource(R.string.mushaf_juz_label, state.juz)
        else -> stringResource(R.string.mushaf_title)
    }
    val hifzSoon = stringResource(R.string.mushaf_hifz_soon)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.mushaf_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onFontChange(false) }) {
                        Icon(Icons.Rounded.Remove, stringResource(R.string.mushaf_font_smaller))
                    }
                    IconButton(onClick = { onFontChange(true) }) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.mushaf_font_larger))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                !state.available -> EmptyState(
                    icon = Icons.Rounded.AutoStories,
                    title = stringResource(R.string.mushaf_unavailable_title),
                    message = stringResource(R.string.mushaf_unavailable),
                    modifier = Modifier.fillMaxSize(),
                )
                state.failed -> EmptyState(
                    icon = Icons.Rounded.AutoStories,
                    title = title,
                    message = stringResource(R.string.mushaf_read_failed),
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    items(state.ayahs, key = { it.globalId }) { ayah ->
                        AyahItem(
                            ayah = ayah,
                            fontSp = state.fontSp,
                            onAdd = {
                                onAddToHifz(ayah)
                                scope.launch { snackbar.showSnackbar(hifzSoon) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahItem(ayah: AyahText, fontSp: Float, onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Qur'on matni doim o'ngdan chapga (arab yozuvi); oyat belgisi matn oxirida.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = ayah.text + " " + QuranCatalog.ayahMarker(ayah.numberInSurah),
                fontFamily = QuranFont.family(),
                fontSize = fontSp.sp,
                lineHeight = (fontSp * 1.8f).sp,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onAdd) {
                Icon(Icons.Rounded.BookmarkAdd, null, Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.mushaf_add_hifz))
            }
        }
        HorizontalDivider()
    }
}
