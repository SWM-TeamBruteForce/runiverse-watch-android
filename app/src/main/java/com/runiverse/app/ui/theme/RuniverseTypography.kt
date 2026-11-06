package com.runiverse.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Typography
import com.runiverse.app.R

val SUIT = FontFamily(
    Font(R.font.suit_medium, FontWeight.Medium),
    Font(R.font.suit_semibold, FontWeight.SemiBold),
    Font(R.font.suit_bold, FontWeight.Bold),
)

/** 글자 잉크가 기준선 위아래로 차지하는 높이(em)다. 시안 간격은 보이는 글자 픽셀 사이로 잰다. */
internal data class InkExtent(val aboveBaselineEm: Float, val belowBaselineEm: Float)

/** SUIT 숫자·라틴 단위 — 숫자 높이(cap height)는 폰트 파일 OS/2 표의 값이다. */
internal val SUIT_NUMERAL_INK = InkExtent(aboveBaselineEm = 0.722f, belowBaselineEm = 0f)

/** SUIT 한글 — 숫자보다 위로 더 올라가고 기준선 아래로도 내려간다(SemiBold 안내 문구 잉크를 잰 값). */
internal val SUIT_HANGUL_INK = InkExtent(aboveBaselineEm = 0.814f, belowBaselineEm = 0.078f)

/** SUIT 라틴 꼬리 글자(`bpm`의 p 등) — 기준선 아래로 내려간다(Medium 단위 잉크를 잰 값). */
internal val SUIT_DESCENDER_INK = InkExtent(aboveBaselineEm = 0.722f, belowBaselineEm = 0.216f)

private const val DESCENDER_LETTERS = "gjpqy"

/**
 * 글자 내용으로 잉크 높이를 고른다 — 간격을 보이는 글자 사이로 재려면 한글·꼬리 글자는 숫자와 높이가 다르다.
 * 여러 종류가 섞이면 위·아래 각각 가장 큰 쪽을 쓴다.
 */
internal fun suitInkFor(text: String): InkExtent {
    val kinds = buildList {
        add(SUIT_NUMERAL_INK)
        if (text.any { it in '가'..'힣' }) add(SUIT_HANGUL_INK)
        if (text.any { it in DESCENDER_LETTERS }) add(SUIT_DESCENDER_INK)
    }
    return InkExtent(kinds.maxOf { it.aboveBaselineEm }, kinds.maxOf { it.belowBaselineEm })
}

private const val TABULAR_NUMERALS = "tnum"

/** 글자 크기표다. 자리마다 따로 두지 않고 큰 숫자·수치·읽는 글자·보조 글자 네 단계만 쓴다. */
private class WatchFontSizes(val bigNumber: Dp, val value: TextUnit, val reading: TextUnit)

private val LARGE_SCREEN_FONT_SIZES = WatchFontSizes(bigNumber = 53.dp, value = 18.sp, reading = 14.sp)
private val SMALL_SCREEN_FONT_SIZES = WatchFontSizes(bigNumber = 47.dp, value = 17.sp, reading = 13.sp)
private val SUPPORTING_FONT_SIZE = 12.sp

// 숫자 자리에 드는 상태 글자는 옆 숫자와 같은 크기면 튀어 보여 조금 줄인다
private const val STATUS_TO_NUMBER_RATIO = 0.93f

/** 1초마다 바뀌는 내 수치에만 쓴다 — 10초마다 오는 파티원 수는 원래 폭이 글자 간격이 고르다. */
fun TextStyle.tabularNumerals(): TextStyle = copy(fontFeatureSettings = TABULAR_NUMERALS)

/**
 * 시안의 자리별 글자 크기를 Wear 글자 슬롯에 둔다. 자리마다 크기는 하나로 고정한다.
 * 큰 수(페이스·심박) numeralExtraLarge, 수치 numeralSmall, 사진 아래 격차 numeralExtraSmall,
 * 안내 문구 bodyMedium, 사진 아래 줄의 상태 글자 bodySmall, 단위·라벨 labelSmall, 사진 자리 이름 첫 글자 labelMedium이다.
 */
@Composable
internal fun runiverseTypography(): Typography {
    val sizes = if (isSmallWatchScreen()) SMALL_SCREEN_FONT_SIZES else LARGE_SCREEN_FONT_SIZES
    // 20sp를 넘는 큰 수는 글자 설정을 따라 커지면 둥근 화면 밖으로 나가서 고정한다
    val bigNumberSize = with(LocalDensity.current) { sizes.bigNumber.toSp() }
    return remember(sizes, bigNumberSize) {
        Typography(
            defaultFontFamily = SUIT,
            numeralExtraLarge = TextStyle(fontSize = bigNumberSize, fontWeight = FontWeight.Bold),
            numeralSmall = TextStyle(fontSize = sizes.value, fontWeight = FontWeight.SemiBold),
            numeralExtraSmall = TextStyle(fontSize = sizes.reading, fontWeight = FontWeight.SemiBold),
            bodyMedium = TextStyle(fontSize = sizes.reading, fontWeight = FontWeight.SemiBold),
            bodySmall = TextStyle(fontSize = (sizes.reading.value * STATUS_TO_NUMBER_RATIO).sp, fontWeight = FontWeight.SemiBold),
            labelSmall = TextStyle(fontSize = SUPPORTING_FONT_SIZE, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = SUPPORTING_FONT_SIZE, fontWeight = FontWeight.Bold),
        )
    }
}
