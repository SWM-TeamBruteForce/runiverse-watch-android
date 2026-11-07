package com.runiverse.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.runiverse.app.run.RunPreviewFrame

@Composable
internal fun IdleScreen(isPhoneReachable: Boolean) {
    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isPhoneReachable) {
                Text(text = stringResource(R.string.app_name))
            } else {
                Text(
                    text = stringResource(R.string.run_phone_checking),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun IdleScreenPhoneLostPreview() = RunPreviewFrame {
    IdleScreen(isPhoneReachable = false)
}
