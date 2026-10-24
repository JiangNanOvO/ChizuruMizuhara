package com.astraflow.Chizuru.ui.kit

import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.backdrops.LayerBackdrop

/**
 * 当前页面内容录下来的「背景」。浮在页面上的玻璃部件(底栏、顶部栏、圆形按钮)从这里取背后的画面来模糊和折射。
 * 页面在内容外层用 `Modifier.layerBackdrop(backdrop)` 录下画面,再用这个提供给浮在上面的玻璃部件;
 * 内容里面的部件拿不到(自己被录进画面里,不能再取这份画面)。
 */
val LocalAstraBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
