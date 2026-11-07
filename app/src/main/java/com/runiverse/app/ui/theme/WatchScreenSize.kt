package com.runiverse.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 글자 폭이 dp라 화면도 dp로 가른다 — px로 가르면 454px·2.0(227dp) 화면이 작은 화면이 된다
// 갤럭시 44mm 226dp는 큰 화면, 40mm(약 206dp)는 작은 화면이다
private val SMALL_SCREEN_BELOW = 215.dp

internal fun isSmallScreenWidth(width: Dp): Boolean = width < SMALL_SCREEN_BELOW

@Composable
internal fun isSmallWatchScreen(): Boolean =
    isSmallScreenWidth(with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() })
