package uz.xumoyun.namoz.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.core.common.ThemeMode
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.ui.AppLocale
import uz.xumoyun.core.ui.components.ChoiceRow
import uz.xumoyun.core.ui.components.SectionTitle
import uz.xumoyun.core.ui.components.SettingsRow
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.namoz.R
import uz.xumoyun.feature.prayer.R as PrayerR
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.HighLatRule
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.namoz.ui.SupportedLanguages
import uz.xumoyun.feature.prayer.ui.highLatName
import uz.xumoyun.feature.prayer.ui.madhabName
import uz.xumoyun.feature.prayer.ui.methodName
import uz.xumoyun.feature.prayer.ui.prayerName

private enum class Dialog { LANGUAGE, METHOD, MADHAB, HIGH_LAT, THEME, ADJUST, HIJRI }

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onLocations: () -> Unit,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onLicenses: () -> Unit = {},
    backup: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = state.config
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<Dialog?>(null) }
    val currentLang = AppLocale.current()

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))

        SectionTitle(stringResource(R.string.section_location))
        SettingsRow(stringResource(PrayerR.string.saved_locations), value = config?.location?.name, icon = Icons.Rounded.LocationOn, onClick = onLocations)

        SectionTitle(stringResource(R.string.section_calculation))
        SettingsRow(
            stringResource(R.string.calc_method),
            value = config?.let {
                val name = stringResource(methodName(it.input.method))
                if (it.methodIsAuto) stringResource(R.string.auto_value, name) else name
            },
            icon = Icons.Rounded.Calculate,
            onClick = { dialog = Dialog.METHOD },
        )
        SettingsRow(
            stringResource(R.string.asr_madhab),
            value = config?.let {
                val name = stringResource(madhabName(it.input.madhab))
                if (it.madhabIsAuto) stringResource(R.string.auto_value, name) else name
            },
            icon = Icons.Rounded.WbTwilight,
            onClick = { dialog = Dialog.MADHAB },
        )
        SettingsRow(
            stringResource(R.string.high_lat_rule),
            value = config?.let { stringResource(highLatName(it.input.highLatRule)) },
            icon = Icons.Rounded.Public,
            onClick = { dialog = Dialog.HIGH_LAT },
        )
        SettingsRow(stringResource(R.string.time_adjustments), icon = Icons.Rounded.Schedule, onClick = { dialog = Dialog.ADJUST })
        SettingsRow(
            stringResource(R.string.hijri_adjust),
            value = signed(state.hijriAdjustDays),
            icon = Icons.Rounded.CalendarMonth,
            onClick = { dialog = Dialog.HIJRI },
        )

        SectionTitle(stringResource(R.string.section_tracker))
        SettingsSwitchRow(
            title = stringResource(R.string.qada_auto_title),
            hint = stringResource(R.string.qada_auto_hint),
            checked = state.autoQada,
            onChange = viewModel::setAutoQada,
        )

        SectionTitle(stringResource(R.string.section_app))
        SettingsRow(stringResource(R.string.notifications), icon = Icons.Rounded.Notifications, onClick = onNotifications)
        SettingsRow(
            stringResource(R.string.language),
            value = SupportedLanguages.firstOrNull { it.first == currentLang }?.second ?: stringResource(R.string.system_default),
            icon = Icons.Rounded.Language,
            onClick = { dialog = Dialog.LANGUAGE },
        )
        SettingsRow(stringResource(R.string.theme), value = stringResource(themeName(state.theme)), icon = Icons.Rounded.DarkMode, onClick = { dialog = Dialog.THEME })
        SettingsRow(stringResource(R.string.help_notifications), icon = Icons.AutoMirrored.Rounded.HelpOutline, onClick = onHelp)

        SectionTitle(stringResource(R.string.section_data))
        BackupRows(backup)
        SettingsSwitchRow(
            title = stringResource(R.string.crash_reports_title),
            hint = stringResource(R.string.crash_reports_hint),
            checked = state.crashReporting,
            onChange = viewModel::setCrashReporting,
        )

        SectionTitle(stringResource(R.string.section_about))
        SettingsRow(stringResource(R.string.licenses_title), icon = Icons.Rounded.Info, onClick = onLicenses)
        SettingsRow(stringResource(R.string.privacy_policy), icon = Icons.Rounded.Policy, onClick = {
            openUrl(context, context.getString(R.string.privacy_policy_url))
        })
        SettingsRow(stringResource(R.string.rate_app), icon = Icons.Rounded.Star, onClick = {
            openUrl(context, "market://details?id=${context.packageName}", "https://play.google.com/store/apps/details?id=${context.packageName}")
        })
        Text(
            stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }

    val close = { dialog = null }
    when (dialog) {
        Dialog.LANGUAGE -> ChoiceDialog(stringResource(R.string.language), close) {
            ChoiceRow(stringResource(R.string.system_default), currentLang == null) { AppLocale.set(null); close() }
            SupportedLanguages.forEach { (tag, name) ->
                ChoiceRow(name, currentLang == tag) { viewModel.setLanguage(tag); AppLocale.set(tag); close() }
            }
        }
        Dialog.METHOD -> ChoiceDialog(stringResource(R.string.calc_method), close) {
            config?.let { c ->
                ChoiceRow(stringResource(R.string.auto_value, stringResource(methodName(c.autoMethod))), c.methodIsAuto) { viewModel.setMethod(null); close() }
                CalcMethod.entries.forEach { m ->
                    ChoiceRow(stringResource(methodName(m)), !c.methodIsAuto && c.input.method == m) { viewModel.setMethod(m); close() }
                }
            }
        }
        Dialog.MADHAB -> ChoiceDialog(stringResource(R.string.asr_madhab), close) {
            config?.let { c ->
                ChoiceRow(stringResource(R.string.auto_value, stringResource(madhabName(c.autoMadhab))), c.madhabIsAuto) { viewModel.setMadhab(null); close() }
                AsrMadhab.entries.forEach { m ->
                    ChoiceRow(stringResource(madhabName(m)), !c.madhabIsAuto && c.input.madhab == m) { viewModel.setMadhab(m); close() }
                }
            }
        }
        Dialog.HIGH_LAT -> ChoiceDialog(stringResource(R.string.high_lat_rule), close) {
            HighLatRule.entries.forEach { r ->
                ChoiceRow(stringResource(highLatName(r)), config?.input?.highLatRule == r) { viewModel.setHighLat(r); close() }
            }
        }
        Dialog.THEME -> ChoiceDialog(stringResource(R.string.theme), close) {
            ThemeMode.entries.forEach { t ->
                ChoiceRow(stringResource(themeName(t)), state.theme == t) { viewModel.setTheme(t); close() }
            }
        }
        Dialog.HIJRI -> ChoiceDialog(stringResource(R.string.hijri_adjust), close) {
            (-CommonSettings.MAX_HIJRI_ADJUST..CommonSettings.MAX_HIJRI_ADJUST).forEach { d ->
                ChoiceRow(signed(d), state.hijriAdjustDays == d) { viewModel.setHijriOffset(d); close() }
            }
        }
        Dialog.ADJUST -> ChoiceDialog(stringResource(R.string.time_adjustments), close) {
            Text(stringResource(R.string.time_adjustments_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            Prayer.entries.forEach { p ->
                val value = config?.prefs?.adjustments?.get(p) ?: 0
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(prayerName(p)), Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.setAdjustment(p, value - 1) }) { Text("−") }
                    Text(signed(value), Modifier.width(48.dp).padding(horizontal = 4.dp), style = TabularNumbers, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    OutlinedButton(onClick = { viewModel.setAdjustment(p, value + 1) }) { Text("+") }
                }
            }
        }
        null -> Unit
    }
}

@Composable
fun ChoiceDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content()
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

private fun themeName(t: ThemeMode) = when (t) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun signed(v: Int) = if (v > 0) "+$v" else "$v"

private fun openUrl(context: android.content.Context, url: String, fallback: String? = null) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        fallback?.let { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
    }
}

/** Yoqish/o'chirish qatori: sarlavha, ixtiyoriy izoh va kalit. */
@Composable
internal fun SettingsSwitchRow(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            hint?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
