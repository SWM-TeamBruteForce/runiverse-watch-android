package com.runiverse.app.run

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.GestureInclusion
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.VerticalPagerScaffold
import com.runiverse.app.phone.RunState
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val OVERVIEW_PAGE_INDEX = 0
private const val MY_RECORD_PAGE_INDEX = 1
private const val PAGE_COUNT = 2
private const val OVERVIEW_ROW_INDEX = 0
private const val OVERVIEW_ROW_KEY = "overview"
private const val INACTIVE_ROW_DOT_ALPHA = 0.35f
private val ROW_DOT_DIAMETER = 3.dp
private val ROW_DOT_SPACING = 6.dp
private val ROW_DOTS_END_INSET = 12.5.dp

@Composable
internal fun RunScreen(run: RunState, isPhoneReachable: Boolean, heartRateBpm: Int?, birthday: String?) {
    val pagerState = rememberPagerState(initialPage = OVERVIEW_PAGE_INDEX) { PAGE_COUNT }
    val rowState = rememberPagerState(initialPage = OVERVIEW_ROW_INDEX) { run.players.size + 1 }
    val scope = rememberCoroutineScope()
    val isOnOverview = pagerState.currentPage == OVERVIEW_PAGE_INDEX && rowState.currentPage == OVERVIEW_ROW_INDEX
    // 첫 화면에서 뒤로가면 러닝을 끝내지 않고 시계 화면으로 나가기만 한다 — 끝내는 길은 종료 버튼 하나다
    BackHandler(enabled = !isOnOverview) {
        scope.launch {
            pagerState.animateScrollToPage(OVERVIEW_PAGE_INDEX)
            rowState.animateScrollToPage(OVERVIEW_ROW_INDEX)
        }
    }

    val birthDate = remember(birthday) { parseBirthday(birthday) }
    val zone = heartRateZoneOrNull(heartRateBpm, birthDate, LocalDate.now())

    // 장 사이는 화면이 통째로 밀리는 보통 넘기기다 — AnimatedPage의 확대·축소는 쓰지 않는다
    HorizontalPagerScaffold(pagerState, pageIndicator = null) {
        HorizontalPager(pagerState, gestureInclusion = overviewRowGestures(pagerState, rowState)) { page ->
            when (page) {
                OVERVIEW_PAGE_INDEX -> VerticalPagerScaffold(
                    rowState,
                    pageIndicator = if (run.players.isEmpty()) null else ({ RowDots(rowState) }),
                    pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimationSpec,
                ) {
                    // 명단이 바뀌어도 보던 사람의 장에 남는다
                    VerticalPager(
                        rowState,
                        key = { row -> run.players.getOrNull(row - 1)?.userId ?: OVERVIEW_ROW_KEY },
                    ) { row ->
                        ScreenScaffold {
                            val player = run.players.getOrNull(row - 1)
                            if (player == null) {
                                RunOverviewPage(run, isPhoneReachable, heartRateBpm, zone)
                            } else {
                                PartyPage(run, player, isPhoneReachable)
                            }
                        }
                    }
                }
                MY_RECORD_PAGE_INDEX -> ScreenScaffold { MyRecordPage(run, isPhoneReachable, heartRateBpm, zone) }
            }
        }
    }
}

// 파티원 장은 첫 화면에서 세로로만 오간다
// userScrollEnabled로 막으면 첫 화면으로 돌아온 직후 첫 좌우 쓸기가 무시된다 — 라이브러리가 다음 터치에서야 감지를 다시 켠다
@Composable
private fun overviewRowGestures(pagerState: PagerState, rowState: PagerState): GestureInclusion {
    val defaultGestures = PagerDefaults.gestureInclusion(pagerState)
    return remember(defaultGestures, rowState) {
        object : GestureInclusion {
            override fun ignoreGestureStart(offset: Offset, layoutCoordinates: LayoutCoordinates): Boolean =
                rowState.currentPage != OVERVIEW_ROW_INDEX ||
                    defaultGestures.ignoreGestureStart(offset, layoutCoordinates)
        }
    }
}

// 라이브러리 기본 표시는 화면 끝을 따라 그려져 진행 링과 겹친다 — 링 안쪽에 점으로 그린다
@Composable
private fun RowDots(rowState: PagerState) {
    val color = MaterialTheme.colorScheme.onBackground
    val count = rowState.pageCount
    Canvas(
        Modifier.padding(end = ROW_DOTS_END_INSET)
            .size(ROW_DOT_DIAMETER, ROW_DOT_SPACING * (count - 1) + ROW_DOT_DIAMETER),
    ) {
        val radius = size.width / 2
        repeat(count) { index ->
            drawCircle(
                color,
                radius,
                Offset(radius, radius + ROW_DOT_SPACING.toPx() * index),
                alpha = if (index == rowState.currentPage) 1f else INACTIVE_ROW_DOT_ALPHA,
            )
        }
    }
}
