package uz.xumoyun.feature.hifz.audio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import uz.xumoyun.feature.hifz.R
import uz.xumoyun.feature.hifz.audio.domain.StorageMath
import uz.xumoyun.feature.hifz.audio.domain.SuraRow
import uz.xumoyun.feature.hifz.audio.domain.SuraStatus

/** TZ 6-bo'lim, 12-ekran: qori tanlash, sura bo'yicha yuklash/o'chirish, egallangan joy (MB). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioManagementRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AudioManagementViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audio_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.audio_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            when {
                state.configError -> Text(stringResource(R.string.audio_config_error))
                state.quranMissing -> Text(stringResource(R.string.audio_no_quran))
                else -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.reciters, key = { it.key }) { reciter ->
                            FilterChip(
                                selected = reciter.key == state.selectedKey,
                                onClick = { vm.selectReciter(reciter.key) },
                                label = { Text(reciter.name) },
                            )
                        }
                    }
                    val selected = state.reciters.firstOrNull { it.key == state.selectedKey }
                    if (selected?.todo != null) {
                        Text(
                            stringResource(R.string.audio_unverified),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.audio_wifi_only))
                        Switch(checked = state.wifiOnly, onCheckedChange = vm::setWifiOnly)
                    }
                    Text(
                        stringResource(R.string.audio_total, StorageMath.format(state.totalBytes)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.rows, key = { it.surah }) { row ->
                            val name = state.surahs.getOrNull(row.surah - 1)?.nameTranslit ?: "${row.surah}"
                            SuraRowItem(
                                name = name,
                                row = row,
                                onDownload = { vm.download(row.surah) },
                                onDelete = { vm.delete(row.surah) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuraRowItem(name: String, row: SuraRow, onDownload: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("${row.surah}. $name", style = MaterialTheme.typography.titleSmall)
        Text(statusText(row), style = MaterialTheme.typography.bodySmall)
        if (row.ayahCount > 0 && row.status != SuraStatus.DOWNLOADED) {
            LinearProgressIndicator(
                progress = { row.finishedAyahs.toFloat() / row.ayahCount },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (row.status == SuraStatus.NOT_DOWNLOADED || row.status == SuraStatus.FAILED) {
                Button(onClick = onDownload) { Text(stringResource(R.string.audio_download)) }
            }
            val hasProgress = row.finishedAyahs > 0 || row.status != SuraStatus.NOT_DOWNLOADED
            if (hasProgress) {
                OutlinedButton(onClick = onDelete) { Text(stringResource(R.string.audio_delete)) }
            }
        }
    }
}

@Composable
private fun statusText(row: SuraRow): String = when (row.status) {
    SuraStatus.NOT_DOWNLOADED -> stringResource(R.string.audio_status_not_downloaded)
    SuraStatus.QUEUED -> stringResource(R.string.audio_status_queued)
    SuraStatus.DOWNLOADING -> stringResource(R.string.audio_status_downloading, row.finishedAyahs, row.ayahCount)
    SuraStatus.DOWNLOADED -> stringResource(R.string.audio_status_downloaded, StorageMath.format(row.bytes))
    SuraStatus.FAILED -> stringResource(R.string.audio_status_failed)
}
