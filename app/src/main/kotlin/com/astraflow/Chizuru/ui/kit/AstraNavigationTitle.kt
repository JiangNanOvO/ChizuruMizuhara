/*
 * Copyright (c) 2023-2024. Compose Cupertino project and open source contributors.
 * Copyright (c) 2025. Scott Lanoue.
 * Copyright (c) 2026. IENGROUND of IENLAB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modified by AstraFlow: use unclipped window positions for the vertical title
// offset so a horizontal page transition cannot change navigation title visibility.
// Source: zone.ien.hig:hig-android:27.0.1-alpha05, CupertinoTopBars.kt (Apache-2.0).
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.astraflow.Chizuru.ui.kit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zone.ien.hig.LocalContentColor
import zone.ien.hig.LocalNavigationTitleProgress
import zone.ien.hig.LocalNavigationTitleVisible
import zone.ien.hig.LocalScaffoldCoordinates
import zone.ien.hig.LocalScaffoldInsets
import zone.ien.hig.LocalTopBarHeight
import zone.ien.hig.ProvideTextStyle
import zone.ien.hig.section.CupertinoSectionDefaults
import zone.ien.hig.theme.CupertinoTheme

private const val NavTitleMaxFontScale = 1.1f
private val NavTitleMaxFontScaleDistance = 150.dp

/** 上游大标题的坐标修正版；字号、留白、缩放、淡入淡出及顶部栏动画仍沿用上游。 */
@Composable
internal fun AstraNavigationTitle(
    modifier: Modifier = Modifier,
    maxFontScale: Float = NavTitleMaxFontScale,
    maxFontScaleDistance: Dp = NavTitleMaxFontScaleDistance,
    paddingValues: PaddingValues = CupertinoSectionDefaults.PaddingValues,
    subtitle: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    require(maxFontScale >= 1) {
        "maxFontScale must be >= 1."
    }
    val navigationTitleVisibility = LocalNavigationTitleVisible.current
    val navigationTitleProgress = LocalNavigationTitleProgress.current
    var visible by navigationTitleVisibility
    DisposableEffect(navigationTitleVisibility) {
        onDispose {
            navigationTitleVisibility.value = false
            navigationTitleProgress.value = 1f
        }
    }

    val density = LocalDensity.current
    val scaffoldCoordinates by LocalScaffoldCoordinates.current
    val topBarHeightPx = (LocalTopBarHeight.current.value ?: 0f)
    val topAppBarExists = topBarHeightPx > 0//Float.MIN_VALUE

    var offsetDifference by remember { mutableStateOf(0f) }
    var actualTopBarHeight by remember { mutableStateOf(0f) }

    val maxSizeIncreaseDistancePx = density.run { maxFontScaleDistance.toPx() }

    val insets = LocalScaffoldInsets.current?.getTop(density) ?: 0
    val fontIncrease by remember(
        maxSizeIncreaseDistancePx,
        maxFontScale,
        topBarHeightPx,
        insets,
    ) {
        derivedStateOf {
            val d = offsetDifference + topBarHeightPx - insets
            if (d >= 0) {
                1f
            } else {
                1f + (-d / maxSizeIncreaseDistancePx).coerceIn(0f, 1f) * (maxFontScale - 1)
            }
        }
    }

    val font = CupertinoTheme.typography.largeTitle.copy(fontWeight = FontWeight.Bold)
    val subtitleFont = CupertinoTheme.typography.subhead

    val titleAlpha by remember(topAppBarExists, navigationTitleProgress) {
        derivedStateOf {
            if (!topAppBarExists) 1f else 1f - navigationTitleProgress.value
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = titleAlpha,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "NavigationTitleAlpha"
    )

    Column(
        modifier
            .padding(paddingValues)
            .alpha(animatedAlpha)
            .onGloballyPositioned {
                actualTopBarHeight = it.size.height.toFloat()
                // 横向移出窗口后，裁切边界会变成空矩形；位置本身仍保留正确的纵向坐标。
                val scaffoldTop = (scaffoldCoordinates?.positionInWindow()?.y ?: 0f)

                offsetDifference = (topBarHeightPx - it.positionInWindow().y) + scaffoldTop
                val fadeDistance = density.run { 100.dp.toPx() }
                val fadeStart = density.run { 50.dp.toPx() }
                navigationTitleProgress.value =
                    if (topAppBarExists) {
                        ((offsetDifference - actualTopBarHeight + fadeStart) / fadeDistance)
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                visible = !topAppBarExists || offsetDifference < it.size.height
            },
    ) {
        ProvideTextStyle(value = font.copy(fontSize = font.fontSize * fontIncrease)) {
            title()
        }
        ProvideTextStyle(value = subtitleFont.copy(fontSize = subtitleFont.fontSize * fontIncrease)) {
            CompositionLocalProvider(
                LocalContentColor provides CupertinoTheme.colorScheme.secondaryLabel,
            ) {
                subtitle?.invoke()
            }
        }
    }
}
