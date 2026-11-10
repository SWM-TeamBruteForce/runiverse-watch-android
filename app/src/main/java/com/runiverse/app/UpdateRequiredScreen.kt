package com.runiverse.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.runiverse.app.run.common.currentRunPageSlots
import com.runiverse.app.run.common.currentScreenWidth
import com.runiverse.app.run.component.AlignedText
import com.runiverse.app.run.component.BOTTOM_PILL_BOTTOM_PADDING
import com.runiverse.app.run.component.ControlButton
import com.runiverse.app.run.component.ControlIcon
import com.runiverse.app.run.component.RunDimensions
import com.runiverse.app.run.component.bottomPillWidth
import com.runiverse.app.run.component.contentBottomAt
import com.runiverse.app.run.component.contentTopAt
import com.runiverse.app.ui.component.PreviewFrame

@Composable
internal fun UpdateRequiredScreen(openStore: () -> Boolean) {
    var isStoreOpenFailed by remember { mutableStateOf(false) }
    ScreenScaffold {
        UpdateRequiredContent(isStoreOpenFailed, onUpdate = { isStoreOpenFailed = !openStore() })
    }
}

@Composable
private fun UpdateRequiredContent(isStoreOpenFailed: Boolean, onUpdate: () -> Unit) {
    val slots = currentRunPageSlots()
    val style = MaterialTheme.typography.bodyMedium
    val halfGroupGap = with(LocalDensity.current) { RunDimensions.GROUP_GAP.toPx() } / 2
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // 두 줄이 같은 글자라 높이가 같아, 큰 숫자 가운데에서 반씩 띄우면 묶음 가운데가 맞는다
        AlignedText(
            stringResource(R.string.update_required),
            style,
            MaterialTheme.colorScheme.onSurface,
            Modifier.contentBottomAt(slots.center - halfGroupGap),
        )
        AlignedText(
            stringResource(if (isStoreOpenFailed) R.string.update_open_store_failed else R.string.update_cannot_connect),
            style,
            if (isStoreOpenFailed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            Modifier.contentTopAt(slots.center + halfGroupGap),
        )
        ControlButton(
            stringResource(R.string.update_open_store),
            ControlIcon.UP_ARROW,
            MaterialTheme.colorScheme.outlineVariant,
            bottomPillWidth(currentScreenWidth()),
            isEnabled = true,
            Modifier.align(Alignment.BottomCenter).padding(bottom = BOTTOM_PILL_BOTTOM_PADDING),
            onClick = onUpdate,
        )
    }
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@WearPreviewFontScales
@Composable
private fun UpdateRequiredScreenPreview() = PreviewFrame {
    UpdateRequiredContent(isStoreOpenFailed = false, onUpdate = {})
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@WearPreviewFontScales
@Composable
private fun UpdateRequiredScreenStoreOpenFailedPreview() = PreviewFrame {
    UpdateRequiredContent(isStoreOpenFailed = true, onUpdate = {})
}
