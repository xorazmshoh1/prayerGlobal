package uz.xumoyun.namoz.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.core.ui.AppLocale
import uz.xumoyun.core.ui.components.ChoiceRow
import uz.xumoyun.core.ui.components.EmptyState
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.ui.NamozPreview
import uz.xumoyun.namoz.ui.SupportedLanguages
import uz.xumoyun.feature.prayer.ui.madhabName
import uz.xumoyun.feature.prayer.ui.methodName
import uz.xumoyun.feature.prayer.ui.LocationsScreen
import uz.xumoyun.namoz.ui.settings.SettingsViewModel

/** Onboarding: til → joylashuv → avtomatik tanlangan usul → bildirishnoma ruxsati. */
@Composable
fun OnboardingRoute(viewModel: SettingsViewModel, onFinished: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.finishOnboarding(); onFinished()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth().padding(16.dp))
        Box(Modifier.weight(1f)) {
            when (step) {
                0 -> LanguageStep(current = AppLocale.current()) { tag ->
                    if (tag != null) { viewModel.setLanguage(tag); AppLocale.set(tag) }
                    step = 1
                }
                1 -> Column {
                    Text(stringResource(R.string.onboarding_location_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp))
                    LocationsScreen(showSaved = false, onPicked = { step = 2 })
                }
                2 -> {
                    val c = state.config
                    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.onboarding_method_title), style = MaterialTheme.typography.headlineSmall)
                        if (c != null) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(c.location.name, style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.calc_method) + ": " + stringResource(methodName(c.input.method)))
                                    Text(stringResource(R.string.asr_madhab) + ": " + stringResource(madhabName(c.input.madhab)))
                                }
                            }
                            Text(stringResource(R.string.onboarding_method_hint), style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.next)) }
                    }
                }
                else -> Column(Modifier.padding(16.dp)) {
                    EmptyState(
                        icon = Icons.Rounded.NotificationsActive,
                        title = stringResource(R.string.onboarding_notif_title),
                        message = stringResource(R.string.onboarding_notif_text),
                        actionLabel = stringResource(R.string.allow),
                        onAction = {
                            if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else { viewModel.finishOnboarding(); onFinished() }
                        },
                    )
                    TextButton(onClick = { viewModel.finishOnboarding(); onFinished() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.later))
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageStep(current: String?, onPick: (String?) -> Unit) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.onboarding_language_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ChoiceRow(stringResource(R.string.system_default), current == null) { onPick(null) }
        SupportedLanguages.forEach { (tag, name) -> ChoiceRow(name, current == tag) { onPick(tag) } }
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageStepPreview() { NamozPreview { LanguageStep(current = "uz") {} } }
