package com.runiverse.app.run

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.runiverse.app.R
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.ui.theme.MY_LANE_COLOR_SLOT
import com.runiverse.app.ui.theme.SUIT_HANGUL_INK
import com.runiverse.app.ui.theme.laneColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val FADED_BUTTON_ALPHA = 0.55f
private const val ICON_TO_FONT_RATIO = 0.62f
private const val PAUSE_ICON_BAR_FRACTION = 0.28f
private const val PAUSE_ICON_LEFT_BAR_FRACTION = 0.1f
private const val PAUSE_ICON_RIGHT_BAR_FRACTION = 0.62f

// 제재 방은 10km, 초대 방은 42.195km까지다 — SUIT 숫자는 0, 4 순으로 넓다
private const val WIDEST_PENALTY_LINE_REMAINING = "4.0km"
private const val WIDEST_GOAL_REMAINING = "40.0km"
private const val HOLD_TO_FINISH_MS = 1200
private val HOLD_LONGER_NOTICE = 1500.milliseconds

// 한 값으로 합치면 192dp에서 버튼이 화면 끝에 닿는다
private val PILL_WIDTH = 120.dp
private val PILL_WIDTH_SCALED_FONT = 140.dp
private val PILL_HEIGHT = 48.dp
private val PILL_GAP = 8.dp
private val ICON_TO_LABEL_GAP = 6.dp
private val PILL_FIT_EDGE_MARGIN = 2.dp
private val LIST_BOTTOM_PADDING = 24.dp

private val HINT_START_TOP = 34.dp
private val HINT_MAX_SHIFT = 24.dp
private val FIRST_BUTTON_TOP_AT_DEFAULT_FONT = 74.dp
private val HINT_TO_BUTTON_GAP = 5.dp

private val CLOCK_TOP_PADDING = 2.dp
private val CLOCK_TO_COVER_END = 2.dp
private val CLOCK_FADE_HEIGHT = 12.dp

private val COMBO_ROW_OUTLINE_COLOR = Color(0xFF333333)
private val COMBO_ROW_OUTLINE_WIDTH = 1.dp
private val COMBO_LABEL_START = 16.dp
private val COMBO_SWITCH_END = 14.dp
private val COMBO_SWITCH_WIDTH = 28.dp
private val COMBO_SWITCH_HEIGHT = 16.dp
private val COMBO_SWITCH_THUMB_INSET = 3.dp

private enum class ControlIcon { PAUSE, PLAY, STOP }

@Composable
internal fun ControlsPage(
    run: RunState,
    isPhoneReachable: Boolean,
    commandState: RunCommandState,
    commands: RunCommands,
    isComboEffectOn: Boolean,
    onComboEffectChange: (Boolean) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    // 스크롤 중에도 시계를 남기려고 스크롤 상태를 연결하지 않는다
    ScreenScaffold(scrollIndicator = { ScrollIndicator(listState, Modifier.align(Alignment.CenterEnd)) }) {
        ControlsPageContent(
            run,
            isPhoneReachable,
            commandState,
            commands,
            isComboEffectOn,
            onComboEffectChange,
            listState,
        )
    }
}

@Composable
private fun ControlsPageContent(
    run: RunState,
    isPhoneReachable: Boolean,
    commandState: RunCommandState,
    commands: RunCommands,
    isComboEffectOn: Boolean,
    onComboEffectChange: (Boolean) -> Unit,
    listState: TransformingLazyColumnState,
) {
    val slots = currentControlsPageSlots()
    val pillWidth = if (LocalDensity.current.fontScale > 1f) PILL_WIDTH_SCALED_FONT else PILL_WIDTH
    val isPaused = run.status == RunStatus.PAUSED
    val isLocked = areControlsLocked(run, isPhoneReachable, commandState)
    val isDimmed = areControlsDimmed(run, isPhoneReachable, commandState)
    val buttonAlpha = if (isDimmed) RunDimensions.STALE_CONTENT_ALPHA else 1f
    val screenWidth = LocalWindowInfo.current.containerSize.width.toFloat()
    val isScrolled = listState.canScrollBackward
    val (finishButtonFits, comboRowFits) = with(LocalDensity.current) {
        val step = (PILL_HEIGHT + PILL_GAP).toPx()
        val fits = { top: Float ->
            pillFitsRoundScreen(screenWidth, PILL_FIT_EDGE_MARGIN.toPx(), pillWidth.toPx(), PILL_HEIGHT.toPx(), top)
        }
        fits(slots.firstButtonTop + step) to fits(slots.firstButtonTop + step * 2)
    }
    // 화면 끝에 걸린 칸이 잘못 눌리지 않게 스크롤 전엔 막는다
    val isFinishButtonReachable = finishButtonFits || isScrolled
    val isComboRowReachable = (finishButtonFits && comboRowFits) || isScrolled
    val finishButtonTopToBottom = with(LocalDensity.current) {
        screenWidth - slots.firstButtonTop - (PILL_HEIGHT + PILL_GAP).toPx()
    }

    Box(Modifier.fillMaxSize()) {
        TransformingLazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = LIST_BOTTOM_PADDING),
            verticalArrangement = Arrangement.spacedBy(PILL_GAP, Alignment.Top),
        ) {
            item { ControlsHintLines(run, isPhoneReachable, slots) }
            item {
                ControlButton(
                    stringResource(if (isPaused) R.string.controls_resume else R.string.controls_pause),
                    if (isPaused) ControlIcon.PLAY else ControlIcon.PAUSE,
                    MaterialTheme.colorScheme.outlineVariant,
                    pillWidth,
                    isEnabled = !isLocked,
                    Modifier.alpha(buttonAlpha),
                    onClick = { if (isPaused) commands.resume() else commands.pause() },
                )
            }
            item {
                FinishButton(
                    finishButtonStage(run, commandState),
                    pillWidth,
                    isEnabled = !isLocked && isFinishButtonReachable,
                    Modifier
                        .fadedTowardScreenBottom(!isFinishButtonReachable, finishButtonTopToBottom)
                        .alpha(buttonAlpha),
                    onFinish = { commands.finish(isFinishForced(run)) },
                )
            }
            item {
                ComboEffectRow(
                    isComboEffectOn,
                    pillWidth,
                    isEnabled = isComboRowReachable,
                    Modifier.alpha(if (isComboRowReachable) 1f else 0f),
                    onToggle = onComboEffectChange,
                )
            }
        }
        ClockFade(Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun currentControlsPageSlots(): ControlsPageSlots {
    val style = MaterialTheme.typography.bodyMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val screenWidth = LocalWindowInfo.current.containerSize.width.toFloat()
    val widestFirstHints = listOf(
        stringResource(R.string.controls_to_penalty_line, WIDEST_PENALTY_LINE_REMAINING),
        stringResource(R.string.controls_to_goal, WIDEST_GOAL_REMAINING),
    )
    val widestSecondHint = stringResource(R.string.controls_penalty_if_short)
    return remember(widestFirstHints, widestSecondHint, style, density, screenWidth) {
        with(density) {
            val widestFirstHint = widestFirstHints.maxBy { measurer.measure(it, style).size.width }
            controlsPageSlots(
                listOf(widestFirstHint, widestSecondHint),
                screenWidth,
                edgeMargin = RunDimensions.TEXT_EDGE_MARGIN.toPx(),
                lineAdvance = hintLineAdvance(style.fontSize.toPx()),
                hintStartTop = HINT_START_TOP.toPx(),
                hintMaxShift = HINT_MAX_SHIFT.toPx(),
                firstButtonTopAtDefaultFont = FIRST_BUTTON_TOP_AT_DEFAULT_FONT.toPx(),
                hintToButtonGap = HINT_TO_BUTTON_GAP.toPx(),
                isFontScaled = fontScale > 1f,
                widthOf = { measurer.measure(it, style).size.width.toFloat() },
            )
        }
    }
}

private fun Density.hintLineAdvance(fontSizePx: Float): Float =
    fontSizePx * (SUIT_HANGUL_INK.aboveBaselineEm + SUIT_HANGUL_INK.belowBaselineEm) + RunDimensions.LINE_GAP.toPx()

// 안내 길이와 상관없이 버튼 자리가 고정되게 칸 높이를 잡는다
@Composable
private fun ControlsHintLines(run: RunState, isPhoneReachable: Boolean, slots: ControlsPageSlots) {
    val hints = controlsHintTexts(controlsHint(run, isPhoneReachable))
    val style = MaterialTheme.typography.bodyMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val screenWidth = LocalWindowInfo.current.containerSize.width.toFloat()
    val lines = remember(hints, slots, style, density, screenWidth) {
        with(density) {
            placeHintLines(
                hints,
                slots.hintTop,
                hintLineAdvance(style.fontSize.toPx()),
                widthAt = { usableWidthAt(it, screenWidth, RunDimensions.TEXT_EDGE_MARGIN.toPx()) },
                widthOf = { measurer.measure(it, style).size.width.toFloat() },
            )
        }
    }
    val height = with(density) { slots.firstButtonTop.toDp() } - PILL_GAP
    val firstHintColor = MaterialTheme.colorScheme.onSurface
    val secondHintColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.TopCenter) {
        lines.forEach { line ->
            AlignedText(
                line.text,
                style,
                if (line.hintIndex == 0) firstHintColor else secondHintColor,
                Modifier.contentTopAt(line.top),
            )
        }
    }
}

@Composable
private fun controlsHintTexts(hint: ControlsHint): List<String> = listOfNotNull(
    hint.first?.let { controlsFirstHintText(it, hint.remainingMeters) },
    hint.second?.let { controlsSecondHintText(it) },
)

@Composable
private fun controlsFirstHintText(first: ControlsFirstHint, remainingMeters: Int?): String = when (first) {
    ControlsFirstHint.PHONE_CHECKING -> stringResource(R.string.run_phone_checking)
    ControlsFirstHint.SERVER_CHECKING -> stringResource(R.string.run_server_checking)
    ControlsFirstHint.DISTANCE_CHECKING -> stringResource(R.string.controls_distance_checking)
    ControlsFirstHint.TO_PENALTY_LINE ->
        stringResource(R.string.controls_to_penalty_line, remainingDistanceText(remainingMeters ?: 0))
    ControlsFirstHint.TO_GOAL -> stringResource(R.string.controls_to_goal, remainingDistanceText(remainingMeters ?: 0))
}

@Composable
private fun remainingDistanceText(meters: Int): String {
    val distance = formatRemainingDistance(meters)
    return distance.value + (distance.unit?.let { distanceUnitText(it) } ?: "")
}

@Composable
private fun controlsSecondHintText(second: ControlsSecondHint): String = stringResource(
    when (second) {
        ControlsSecondHint.CONTROLS_AFTER_RECONNECT -> R.string.controls_after_reconnect
        ControlsSecondHint.RECORDING -> R.string.controls_recording
        ControlsSecondHint.KEEP_RUNNING_IF_SHORT -> R.string.controls_keep_running_if_short
        ControlsSecondHint.PENALTY_IF_SHORT -> R.string.controls_penalty_if_short
        ControlsSecondHint.PENALTY_IF_FINISHED -> R.string.controls_penalty_if_finished
        ControlsSecondHint.MAY_NOT_COMPLETE -> R.string.controls_may_not_complete
    },
)

@Composable
private fun FinishButton(
    stage: FinishButtonStage,
    width: Dp,
    isEnabled: Boolean,
    modifier: Modifier,
    onFinish: () -> Unit,
) {
    val background = laneColor(MY_LANE_COLOR_SLOT)
    when (stage) {
        FinishButtonStage.HOLD -> HoldToFinishButton(background, width, isEnabled, modifier, onFinish)
        FinishButtonStage.TAP -> ControlButton(
            stringResource(R.string.controls_finish),
            ControlIcon.STOP,
            background,
            width,
            isEnabled,
            modifier,
            onFinish,
        )
        FinishButtonStage.FINISHING -> ControlButton(
            stringResource(R.string.controls_finishing),
            icon = null,
            background,
            width,
            isEnabled = false,
            modifier,
            onClick = {},
        )
        FinishButtonStage.CHECKING -> ControlButton(
            stringResource(R.string.controls_finish_checking),
            icon = null,
            background,
            width,
            isEnabled = false,
            modifier,
            onClick = {},
        )
    }
}

@Composable
private fun ControlButton(
    label: String,
    icon: ControlIcon?,
    background: Color,
    width: Dp,
    isEnabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(width, PILL_HEIGHT)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick),
    ) {
        ButtonContent(label, icon, MaterialTheme.colorScheme.onBackground)
    }
}

// 달리는 중에 손가락이 스쳐 기록이 끝나지 않게 한다
@Composable
private fun HoldToFinishButton(
    background: Color,
    width: Dp,
    isEnabled: Boolean,
    modifier: Modifier,
    onFinish: () -> Unit,
) {
    val currentOnFinish by rememberUpdatedState(onFinish)
    var isPressing by remember { mutableStateOf(false) }
    var showsHoldLonger by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val isFilling = isPressing && isEnabled

    LaunchedEffect(isFilling) {
        if (isFilling) {
            progress.snapTo(0f)
            launch { progress.animateTo(1f, tween(HOLD_TO_FINISH_MS, easing = LinearEasing)) }
            // 시스템 애니메이션을 끄면 채움이 바로 끝나서 누른 시간은 따로 잰다
            delay(HOLD_TO_FINISH_MS.toLong())
            currentOnFinish()
            isPressing = false
            progress.snapTo(0f)
        } else {
            progress.animateTo(0f, tween(remainingMs(progress.value), easing = LinearEasing))
        }
    }
    LaunchedEffect(showsHoldLonger) {
        if (showsHoldLonger) {
            delay(HOLD_LONGER_NOTICE)
            showsHoldLonger = false
        }
    }
    // 화면이 꺼지면 프레임이 멈춰, 그대로 두면 켜졌을 때 이어서 찬다
    LifecycleStartEffect(Unit) {
        onStopOrDispose { isPressing = false }
    }

    val label = stringResource(
        when {
            isPressing -> R.string.controls_keep_pressing
            showsHoldLonger -> R.string.controls_hold_longer
            else -> R.string.controls_hold_to_finish
        },
    )
    val icon = if (showsHoldLonger && !isPressing) null else ControlIcon.STOP
    val holdLabel = stringResource(R.string.controls_hold_to_finish)
    Box(
        modifier
            .size(width, PILL_HEIGHT)
            .clip(CircleShape)
            .background(background)
            // 쓸기로 넘어가면 up이 null이다 — 짧게 누른 것으로 보지 않는다
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    showsHoldLonger = false
                    isPressing = true
                    try {
                        val up = waitForUpOrCancellation()
                        if (isPressing && up != null) showsHoldLonger = true
                    } finally {
                        isPressing = false
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = holdLabel
                if (isEnabled) {
                    onLongClick(holdLabel) {
                        currentOnFinish()
                        true
                    }
                } else {
                    disabled()
                }
            },
    ) {
        ButtonContent(label, icon, MaterialTheme.colorScheme.onBackground)
        Box(
            Modifier
                .matchParentSize()
                .clearAndSetSemantics {}
                .drawWithContent {
                    clipRect(right = size.width * progress.value) { this@drawWithContent.drawContent() }
                }
                .background(MaterialTheme.colorScheme.onBackground),
        ) {
            ButtonContent(label, icon, MaterialTheme.colorScheme.background)
        }
    }
}

private fun remainingMs(remainingFraction: Float): Int = (HOLD_TO_FINISH_MS * remainingFraction).toInt()

@Composable
private fun ButtonContent(label: String, icon: ControlIcon?, color: Color) {
    val style = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current
    val iconSize = with(density) { style.fontSize.toDp() } * ICON_TO_FONT_RATIO
    val pillCenter = with(density) { PILL_HEIGHT.toPx() } / 2
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(ICON_TO_LABEL_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Canvas(Modifier.size(iconSize)) { drawControlIcon(icon, color) }
        AlignedText(label, style, color, Modifier.contentCenterAt(pillCenter))
    }
}

private fun DrawScope.drawControlIcon(icon: ControlIcon, color: Color) {
    when (icon) {
        ControlIcon.PAUSE -> {
            val barSize = Size(size.width * PAUSE_ICON_BAR_FRACTION, size.height)
            drawRect(color, topLeft = Offset(size.width * PAUSE_ICON_LEFT_BAR_FRACTION, 0f), size = barSize)
            drawRect(color, topLeft = Offset(size.width * PAUSE_ICON_RIGHT_BAR_FRACTION, 0f), size = barSize)
        }
        ControlIcon.PLAY -> drawPath(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, size.height / 2)
                lineTo(0f, size.height)
                close()
            },
            color,
        )
        ControlIcon.STOP -> drawRect(color)
    }
}

// 종료 버튼을 찾을 수 있게 숨기지 않고 흐리게 보여 준다
private fun Modifier.fadedTowardScreenBottom(isFaded: Boolean, distanceToScreenBottom: Float): Modifier {
    if (!isFaded) return this
    return drawWithContent {
        drawContent()
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), endY = distanceToScreenBottom))
    }.alpha(FADED_BUTTON_ALPHA)
}

@Composable
private fun ComboEffectRow(
    isOn: Boolean,
    width: Dp,
    isEnabled: Boolean,
    modifier: Modifier,
    onToggle: (Boolean) -> Unit,
) {
    val style = MaterialTheme.typography.bodyMedium
    val pillCenter = with(LocalDensity.current) { PILL_HEIGHT.toPx() } / 2
    Row(
        modifier
            .size(width, PILL_HEIGHT)
            .clip(CircleShape)
            .border(COMBO_ROW_OUTLINE_WIDTH, COMBO_ROW_OUTLINE_COLOR, CircleShape)
            .toggleable(value = isOn, enabled = isEnabled, role = Role.Switch, onValueChange = onToggle)
            .padding(start = COMBO_LABEL_START, end = COMBO_SWITCH_END),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlignedText(
            stringResource(R.string.controls_combo_effect),
            style,
            MaterialTheme.colorScheme.onSurface,
            Modifier.contentCenterAt(pillCenter),
        )
        ComboSwitch(isOn)
    }
}

@Composable
private fun ComboSwitch(isOn: Boolean) {
    val trackColor = if (isOn) laneColor(MY_LANE_COLOR_SLOT) else MaterialTheme.colorScheme.outlineVariant
    val thumbColor = MaterialTheme.colorScheme.onBackground
    Canvas(Modifier.size(COMBO_SWITCH_WIDTH, COMBO_SWITCH_HEIGHT).clip(CircleShape).background(trackColor)) {
        val center = size.height / 2
        drawCircle(
            thumbColor,
            radius = center - COMBO_SWITCH_THUMB_INSET.toPx(),
            center = Offset(if (isOn) size.width - center else center, center),
        )
    }
}

// 시계는 글자 설정을 따라 커져서 그 크기로 덮는다
@Composable
private fun ClockFade(modifier: Modifier) {
    val background = MaterialTheme.colorScheme.background
    val clockFontSize = with(LocalDensity.current) { MaterialTheme.typography.arcMedium.fontSize.toDp() }
    val coverHeight = CLOCK_TOP_PADDING + clockFontSize + CLOCK_TO_COVER_END
    val height = coverHeight + CLOCK_FADE_HEIGHT
    val coveredFraction = coverHeight / height
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    coveredFraction to background,
                    1f to background.copy(alpha = 0f),
                ),
            ),
    )
}

private val PREVIEW_COMMANDS = RunCommands(pause = {}, resume = {}, finish = {})

@WearPreviewLargeRound
@WearPreviewSmallRound
@WearPreviewFontScales
@Composable
private fun ControlsPageRunningPreview() = RunPreviewFrame {
    ControlsPageContent(
        RunPreviewSamples.party,
        isPhoneReachable = true,
        RunCommandState(),
        PREVIEW_COMMANDS,
        isComboEffectOn = true,
        onComboEffectChange = {},
        rememberTransformingLazyColumnState(),
    )
}

@WearPreviewLargeRound
@Composable
private fun ControlsPagePausedPreview() = RunPreviewFrame {
    ControlsPageContent(
        RunPreviewSamples.party.copy(status = RunStatus.PAUSED),
        isPhoneReachable = true,
        RunCommandState(),
        PREVIEW_COMMANDS,
        isComboEffectOn = true,
        onComboEffectChange = {},
        rememberTransformingLazyColumnState(),
    )
}

@WearPreviewLargeRound
@Composable
private fun ControlsPagePhoneLostPreview() = RunPreviewFrame {
    ControlsPageContent(
        RunPreviewSamples.party,
        isPhoneReachable = false,
        RunCommandState(),
        PREVIEW_COMMANDS,
        isComboEffectOn = true,
        onComboEffectChange = {},
        rememberTransformingLazyColumnState(),
    )
}

@WearPreviewLargeRound
@Composable
private fun ControlsPageFinishingPreview() = RunPreviewFrame {
    ControlsPageContent(
        RunPreviewSamples.party,
        isPhoneReachable = true,
        RunCommandState(isLocked = true, isFinishPending = true),
        PREVIEW_COMMANDS,
        isComboEffectOn = true,
        onComboEffectChange = {},
        rememberTransformingLazyColumnState(),
    )
}
