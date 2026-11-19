package uz.xumoyun.namoz.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.xumoyun.core.ui.components.SectionTitle
import uz.xumoyun.namoz.R

/** TZ 12-bo'lim: "Litsenziyalar va manbalar" — Tanzil (havola bilan), Adhan, Amiri, GeoNames, qorilar. */
@Composable
fun LicensesScreen() {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.licenses_intro),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )

        SectionTitle(stringResource(R.string.licenses_quran_title))
        LicenseBlock(
            name = stringResource(R.string.licenses_tanzil_name),
            text = stringResource(R.string.licenses_tanzil_text),
            linkLabel = stringResource(R.string.licenses_tanzil_link),
            url = "https://tanzil.net/download",
        )
        LicenseBlock(
            name = stringResource(R.string.licenses_amiri_name),
            text = stringResource(R.string.licenses_amiri_text),
            linkLabel = stringResource(R.string.licenses_source),
            url = "https://github.com/aliftype/amiri",
        )

        SectionTitle(stringResource(R.string.licenses_libraries_title))
        LicenseBlock(
            name = stringResource(R.string.licenses_adhan_name),
            text = stringResource(R.string.licenses_adhan_text),
            linkLabel = stringResource(R.string.licenses_source),
            url = "https://github.com/batoulapps/Adhan",
        )

        SectionTitle(stringResource(R.string.licenses_data_title))
        LicenseBlock(
            name = stringResource(R.string.licenses_geonames_name),
            text = stringResource(R.string.licenses_geonames_text),
            linkLabel = stringResource(R.string.licenses_source),
            url = "https://www.geonames.org/",
        )
        LicenseBlock(
            name = stringResource(R.string.licenses_reciters_name),
            text = stringResource(R.string.licenses_reciters_text),
            linkLabel = null,
            url = null,
        )
    }
}

@Composable
private fun LicenseBlock(name: String, text: String, linkLabel: String?, url: String?) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, style = MaterialTheme.typography.titleSmall)
        Text(text, style = MaterialTheme.typography.bodySmall)
        if (linkLabel != null && url != null) {
            OutlinedButton(onClick = { openUrl(context, url) }) { Text(linkLabel) }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // Brauzer yo'q: havola ko'rsatilgan, jim o'tamiz.
    }
}
