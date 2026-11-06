package com.runiverse.app.run

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.runiverse.app.phone.RunState
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val OVERVIEW_PAGE_INDEX = 0
private const val MY_RECORD_PAGE_INDEX = 1
private const val PAGE_COUNT = 2

@Composable
internal fun RunScreen(run: RunState, isPhoneReachable: Boolean, heartRateBpm: Int?, birthday: String?) {
    val pagerState = rememberPagerState(initialPage = OVERVIEW_PAGE_INDEX) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    // 첫 화면에서 뒤로가면 러닝을 끝내지 않고 시계 화면으로 나가기만 한다 — 끝내는 길은 종료 버튼 하나다
    BackHandler(enabled = pagerState.currentPage != OVERVIEW_PAGE_INDEX) {
        scope.launch { pagerState.animateScrollToPage(OVERVIEW_PAGE_INDEX) }
    }

    val birthDate = remember(birthday) { parseBirthday(birthday) }
    val zone = heartRateZoneOrNull(heartRateBpm, birthDate, LocalDate.now())

    // 장 사이는 화면이 통째로 밀리는 보통 넘기기다 — AnimatedPage의 확대·축소는 쓰지 않는다
    HorizontalPagerScaffold(pagerState, pageIndicator = null) {
        HorizontalPager(pagerState) { page ->
            ScreenScaffold {
                when (page) {
                    OVERVIEW_PAGE_INDEX -> RunOverviewPage(run, isPhoneReachable, heartRateBpm, zone)
                    MY_RECORD_PAGE_INDEX -> MyRecordPage(run, isPhoneReachable, heartRateBpm, zone)
                }
            }
        }
    }
}
