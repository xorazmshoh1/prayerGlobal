package uz.xumoyun.feature.hifz

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uz.xumoyun.core.ui.components.EmptyState

/**
 * Hifz tabi. 1-bosqichda bo'sh ekran; bugungi dars (Sabaq / Sabqi / Manzil), progress halqasi
 * va "Darsni boshlash" tugmasi 8-bosqichda qo'shiladi (TZ 6 va 11-bo'limlar).
 */
@Composable
fun HifzRoute(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Rounded.AutoStories,
            title = stringResource(R.string.hifz_title),
            message = stringResource(R.string.hifz_coming_soon),
        )
    }
}
