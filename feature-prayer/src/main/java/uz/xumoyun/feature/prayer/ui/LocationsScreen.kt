package uz.xumoyun.feature.prayer.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.feature.prayer.R
import uz.xumoyun.feature.prayer.data.LocationLabel
import uz.xumoyun.feature.prayer.domain.City

/**
 * Joylar: saqlanganlar ro'yxati (uy, ish, safar), joylashuv (faqat ACCESS_COARSE_LOCATION) va shahar qidirish.
 * Ruxsat rad etilsa yoki joy aniqlanmasa — assets'dagi GeoNames shaharlaridan qidiruv taklif qilinadi.
 * [onPicked] — joy tanlangandan keyin (onboarding'da keyingi qadamga o'tish uchun).
 */
@Composable
fun LocationsScreen(
    showSaved: Boolean = true,
    onPicked: () -> Unit = {},
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var label by rememberSaveable { mutableStateOf(LocationLabel.NONE) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(granted, label, onFound = onPicked)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showSaved && state.saved.isNotEmpty()) {
            item { Text(stringResource(R.string.saved_locations), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp)) }
            items(state.saved, key = { it.id }) { loc ->
                val selected = loc.id == state.selectedId
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { viewModel.select(loc.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.MyLocation.takeIf { loc.fromGps } ?: Icons.Rounded.Search,
                        null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(loc.name, style = MaterialTheme.typography.bodyLarge)
                        val sub = listOfNotNull(
                            loc.label.takeIf { it != LocationLabel.NONE }?.let { stringResource(labelName(it)) },
                            loc.countryCode.takeIf { it.isNotBlank() },
                        ).joinToString(" · ")
                        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.saved.size > 1) {
                        IconButton(onClick = { viewModel.delete(loc.id) }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete)) }
                    }
                }
            }
            item { Spacer(Modifier.size(16.dp)) }
        }

        item {
            Text(stringResource(R.string.add_location), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LocationLabel.entries) { l ->
                    FilterChip(selected = label == l, onClick = { label = l }, label = { Text(stringResource(labelName(l))) })
                }
            }
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                enabled = !state.locating,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                if (state.locating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.MyLocation, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.use_gps))
            }
            val gpsMessage = when (state.gps) {
                GpsResult.DENIED -> R.string.location_denied
                GpsResult.FAILED -> R.string.gps_failed
                else -> null
            }
            if (gpsMessage != null) {
                Text(stringResource(gpsMessage), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            Text(stringResource(R.string.gps_optional), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::search,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text(stringResource(R.string.search_city)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        items(state.results, key = { "${it.name}-${it.countryCode}-${it.latitude}" }) { city ->
            CityRow(city) {
                viewModel.addCity(city, label)
                onPicked()
            }
        }
    }
}

@Composable
private fun CityRow(city: City, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(city.name, style = MaterialTheme.typography.bodyLarge)
            Text("${city.countryCode} · ${city.timeZone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
