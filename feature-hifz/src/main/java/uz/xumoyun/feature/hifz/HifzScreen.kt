package uz.xumoyun.feature.hifz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.xumoyun.core.ui.components.EmptyState

/**
 * Hifz tabi. Hozircha bo'sh ekran va mus'hafga o'tish tugmasi; bugungi dars (Sabaq / Sabqi / Manzil)
 * va progress 8-bosqichda qo'shiladi (TZ 6 va 11-bo'limlar).
 */
@Composable
fun HifzRoute(
    modifier: Modifier = Modifier,
    onOpenMushaf: () -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            icon = Icons.Rounded.AutoStories,
            title = stringResource(R.string.hifz_title),
            message = stringResource(R.string.hifz_coming_soon),
        )
        Button(onClick = onOpenMushaf) {
            Text(stringResource(R.string.mushaf_open))
        }
    }
}
