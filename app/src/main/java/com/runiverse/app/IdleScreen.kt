package com.runiverse.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

// TODO: 결과 요약 — 끝난 러닝(FINISHED)도 결과 화면을 만들 때까지 여기로 온다
@Composable
internal fun IdleScreen() {
    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.app_name))
        }
    }
}
