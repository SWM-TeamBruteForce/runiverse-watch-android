package com.runiverse.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.runiverse.app.run.common.currentRunPageSlots
import com.runiverse.app.run.component.AlignedText
import com.runiverse.app.run.component.ContentBoundsColumn
import com.runiverse.app.run.component.RunDimensions
import com.runiverse.app.run.component.boxContentBounds
import com.runiverse.app.run.component.contentBottomAt
import com.runiverse.app.run.component.contentTopAt
import com.runiverse.app.ui.component.PreviewFrame

// 시안의 36dp는 192dp 화면에서 위 가운데 시계와 붙어, 시안에서 고른 24dp로 줄였다
private val MARK_HEIGHT = 24.dp

@Composable
internal fun IdleScreen(isPhoneReachable: Boolean) {
    ScreenScaffold {
        IdleContent(isPhoneReachable)
    }
}

// 글자 두 줄은 업데이트 필요 화면과 같은 자리라, 폰이 버전을 거절해 넘어가도 글자가 제자리에서 바뀐다
@Composable
private fun IdleContent(isPhoneReachable: Boolean) {
    val slots = currentRunPageSlots()
    val style = MaterialTheme.typography.bodyMedium
    val halfGroupGap = with(LocalDensity.current) { RunDimensions.GROUP_GAP.toPx() } / 2
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        ContentBoundsColumn(
            contentGap = RunDimensions.GROUP_GAP,
            modifier = Modifier.contentBottomAt(slots.center - halfGroupGap),
        ) {
            Image(
                painterResource(R.drawable.ic_runiverse_mark),
                stringResource(R.string.app_name),
                Modifier.boxContentBounds().height(MARK_HEIGHT),
            )
            AlignedText(
                stringResource(if (isPhoneReachable) R.string.idle_start_on_phone else R.string.run_phone_checking),
                style,
                MaterialTheme.colorScheme.onSurface,
            )
        }
        if (isPhoneReachable) {
            AlignedText(
                stringResource(R.string.idle_run_screen_shown_automatically),
                style,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.contentTopAt(slots.center + halfGroupGap),
            )
        }
    }
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@WearPreviewFontScales
@Composable
private fun IdleScreenPreview() = PreviewFrame {
    IdleContent(isPhoneReachable = true)
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@WearPreviewFontScales
@Composable
private fun IdleScreenPhoneLostPreview() = PreviewFrame {
    IdleContent(isPhoneReachable = false)
}
