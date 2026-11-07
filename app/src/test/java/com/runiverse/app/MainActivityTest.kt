package com.runiverse.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.runiverse.app.phone.PhoneLink
import com.runiverse.app.phone.PhoneMessage
import com.runiverse.app.phone.PhoneState
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.phone.WatchMessage
import com.runiverse.app.phone.WatchStateUpdated
import com.runiverse.app.phone.WatchSync
import com.runiverse.app.phone.WatchSynced
import com.runiverse.app.run.RunPreviewSamples
import com.runiverse.app.run.formatElapsedTime
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// 닫기 애니메이션 200ms보다 넉넉히 돌린다
private const val SWIPE_SETTLE_MS = 1_000L
private const val SWIPE_SETTLE_FRAME_COUNT = 60

@RunWith(RobolectricTestRunner::class)
@Config(application = FakeRuniverseApp::class, qualifiers = "w226dp-h226dp-round-watch-xhdpi")
class MainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app get() = composeRule.activity.application as FakeRuniverseApp
    private var nextStateSequence = 1L

    // 오프라인 솔로는 방 번호가 없어 SYNC 없이 바로 받는다
    private val soloRun = RunPreviewSamples.solo.copy(runningRoomId = null)
    private val appName get() = composeRule.activity.getString(R.string.app_name)

    @After
    fun cancelTestScope() {
        app.testScope.cancel()
    }

    @Test
    fun `러닝이 없으면 대기 화면을 보인다`() {
        // when
        answerSync(run = null)

        // then
        composeRule.onNodeWithText(appName).assertExists()
    }

    @Test
    fun `러닝이 시작되면 러닝 화면을 보인다`() {
        // given
        answerSync(run = null)

        // when
        receiveState(soloRun)
        composeRule.waitForIdle()

        // then
        composeRule.onNodeWithText(formatElapsedTime(soloRun.me.elapsedSeconds)).assertExists()
    }

    @Test
    fun `러닝이 끝나면 결과 화면을 보인다`() {
        // given
        answerSync(run = null)

        // when
        receiveState(soloRun.copy(status = RunStatus.FINISHED, result = RunPreviewSamples.result))
        composeRule.waitForIdle()

        // then
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.run_summary_completed)).assertExists()
    }

    @Test
    fun `대기 화면을 끝까지 쓸면 앱을 닫는다`() {
        // given
        answerSync(run = null)

        // when
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        // then
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `쓸어 닫는 도중 러닝이 시작되면 앱을 닫지 않고 러닝 화면을 보인다`() {
        // given
        answerSync(run = null)
        composeRule.onNodeWithText(appName).assertExists()
        composeRule.mainClock.autoAdvance = false
        composeRule.onRoot().performTouchInput { swipeRight() }

        // when -> 손을 뗀 직후라 닫기 애니메이션(200ms)이 아직 안 끝났다
        receiveState(soloRun)
        composeRule.mainClock.advanceTimeBy(SWIPE_SETTLE_MS)
        composeRule.waitForIdle()

        // then
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText(formatElapsedTime(soloRun.me.elapsedSeconds)).assertExists()
    }

    @Test
    fun `폰에서 결과를 닫으면 대기 화면을 보이지 않고 앱을 닫는다`() {
        // given
        val completed = composeRule.activity.getString(R.string.run_summary_completed)
        val phoneChecking = composeRule.activity.getString(R.string.run_phone_checking)
        answerSync(run = null)
        receiveState(soloRun.copy(status = RunStatus.FINISHED, result = RunPreviewSamples.result))
        composeRule.waitForIdle()
        composeRule.onNodeWithText(completed).assertExists()

        // when
        receiveState(run = null)
        composeRule.waitForIdle()

        // then
        assertTrue(composeRule.activity.isFinishing)
        composeRule.onRoot().assertExists()
        composeRule.onNodeWithText(completed).assertDoesNotExist()
        composeRule.onNodeWithText(appName).assertDoesNotExist()
        composeRule.onNodeWithText(phoneChecking).assertDoesNotExist()
    }

    @Test
    fun `대기 화면을 쓸어 닫을 때 제자리로 돌아와 다시 보이지 않는다`() {
        // given
        answerSync(run = null)
        composeRule.mainClock.autoAdvance = false

        // when
        composeRule.onRoot().performTouchInput { swipeRight() }
        val lefts = List(SWIPE_SETTLE_FRAME_COUNT) {
            composeRule.mainClock.advanceTimeByFrame()
            appNameLeftOrNull()
        }

        // then
        val shownLefts = lefts.takeWhile { it != null }.filterNotNull()
        assertTrue(shownLefts.isNotEmpty())
        assertEquals(shownLefts.sorted(), shownLefts)
        assertTrue(lefts.drop(shownLefts.size).all { it == null })
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `러닝 첫 화면에서는 뒤로가기를 가로채지 않는다`() {
        // given
        answerSync(run = null)
        receiveState(soloRun)

        // when
        composeRule.waitForIdle()

        // then -> 가로채지 않아야 시계 화면으로 나간다
        assertFalse(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }

    @Test
    fun `조작 장에서 뒤로가면 러닝 첫 화면으로 돌아온다`() {
        // given
        answerSync(run = null)
        receiveState(soloRun)
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        assertTrue(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())

        // when
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        // then
        assertFalse(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        assertFalse(composeRule.activity.isFinishing)
    }

    private fun appNameLeftOrNull(): Float? =
        composeRule.onAllNodesWithText(appName).fetchSemanticsNodes().firstOrNull()?.boundsInRoot?.left

    private fun answerSync(run: RunState?) {
        composeRule.waitForIdle()
        val syncId = app.sent.filterIsInstance<WatchSync>().last().syncId
        receive(WatchSynced(syncId, phoneState(run), birthday = null, photos = emptyList()))
        composeRule.waitForIdle()
    }

    // SYNC 중에 받은 상태는 조용히 버려져, 시험이 엉뚱한 이유로 통과하거나 실패하지 않게 막는다
    private fun receiveState(run: RunState?) {
        check(!app.phoneLink.state.value.isSyncing) { "SYNC 중이라 상태를 버린다" }
        receive(WatchStateUpdated(phoneState(run)))
    }

    private fun receive(message: PhoneMessage) {
        composeRule.runOnUiThread { app.phoneLink.onReceived(message) }
    }

    private fun phoneState(run: RunState?) = PhoneState(
        phoneInstanceId = "phone",
        stateSequence = nextStateSequence++,
        statusVersion = 1,
        run = run,
    )
}

// 수신 등록·진동을 돌리지 않고, 실제 PhoneLink로 보낸 메시지만 기록한다
// Android가 리플렉션으로 만들어 private로 둘 수 없다
class FakeRuniverseApp : RuniverseApp() {
    val testScope = MainScope()
    val sent = mutableListOf<WatchMessage>()
    override val phoneLink = PhoneLink(testScope, send = { sent.add(it) })

    override fun onCreate() = Unit
}
