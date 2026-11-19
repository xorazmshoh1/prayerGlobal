package uz.xumoyun.feature.hifz.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.core.ui.components.EmptyState
import uz.xumoyun.feature.hifz.R
import uz.xumoyun.feature.hifz.domain.QuranCatalog
import uz.xumoyun.feature.hifz.domain.Revelation
import uz.xumoyun.feature.hifz.domain.SurahInfo

@Composable
fun MushafRoute(
    onBack: () -> Unit,
    onSurah: (Int) -> Unit,
    onJuz: (Int) -> Unit,
    modifier: Modifier = Modifier,
    vm: MushafViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    MushafScreen(
        state = state,
        onQueryChange = vm::onQueryChange,
        onTabChange = vm::onTabChange,
        onBack = onBack,
        onSurah = onSurah,
        onJuz = onJuz,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafScreen(
    state: MushafUiState,
    onQueryChange: (String) -> Unit,
    onTabChange: (MushafTab) -> Unit,
    onBack: () -> Unit,
    onSurah: (Int) -> Unit,
    onJuz: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mushaf_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.mushaf_back))
                    }
                },
            )
        },
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
                    title = stringResource(R.string.mushaf_title),
                    message = stringResource(R.string.mushaf_read_failed),
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Column(Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = state.tab.ordinal) {
                        Tab(
                            selected = state.tab == MushafTab.SURAHS,
                            onClick = { onTabChange(MushafTab.SURAHS) },
                            text = { Text(stringResource(R.string.mushaf_tab_surahs)) },
                        )
                        Tab(
                            selected = state.tab == MushafTab.JUZ,
                            onClick = { onTabChange(MushafTab.JUZ) },
                            text = { Text(stringResource(R.string.mushaf_tab_juz)) },
                        )
                    }
                    when (state.tab) {
                        MushafTab.SURAHS -> SurahTab(state.query, state.surahs, onQueryChange, onSurah)
                        MushafTab.JUZ -> JuzTab(onJuz)
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahTab(
    query: String,
    surahs: List<SurahInfo>,
    onQueryChange: (String) -> Unit,
    onSurah: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.mushaf_search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (surahs.isEmpty()) {
            Text(
                text = stringResource(R.string.mushaf_no_results),
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(surahs, key = { it.id }) { surah ->
                SurahRow(surah, onClick = { onSurah(surah.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SurahRow(surah: SurahInfo, onClick: () -> Unit) {
    val revelation = stringResource(
        if (surah.revelation == Revelation.MECCAN) R.string.mushaf_meccan else R.string.mushaf_medinan,
    )
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = surah.id.toString(), modifier = Modifier.size(width = 40.dp, height = 24.dp))
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(surah.nameTranslit, style = MaterialTheme.typography.titleMedium)
            Text(
                "${stringResource(R.string.mushaf_ayah_count, surah.ayahCount)} · $revelation",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = surah.nameArabic,
            fontFamily = QuranFont.family(),
            fontSize = 22.sp,
        )
    }
}

@Composable
private fun JuzTab(onJuz: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(QuranCatalog.juzNumbers.toList()) { juz ->
            Row(
                Modifier.fillMaxWidth().clickable { onJuz(juz) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(stringResource(R.string.mushaf_juz_label, juz), style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()
        }
    }
}
