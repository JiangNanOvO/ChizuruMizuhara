package com.astraflow.Chizuru.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import zone.ien.hig.CupertinoButton
import zone.ien.hig.CupertinoButtonDefaults
import zone.ien.hig.CupertinoButtonSize
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.CupertinoText

/*
 * 按钮直接用上游 compose-hig 的按钮(样子、按下效果照上游);这里只把星流的四种用法对到上游的配色上。
 */

enum class AstraButtonStyle {
    /** 实心主题色:主要操作。 */
    Filled,
    /** 浅灰底、主题色字:次要操作、列表右侧的「开启」。 */
    Tinted,
    /** 无底色、主题色字。 */
    Plain,
    /** 浅灰底、红字:删除、注销这类操作。 */
    Destructive,
}

enum class AstraButtonSize(internal val hig: CupertinoButtonSize) {
    /** 列表行右侧的小胶囊。 */
    Small(CupertinoButtonSize.Small),
    /** 卡片里的按钮。 */
    Medium(CupertinoButtonSize.Regular),
    /** 页面底部的大按钮:按文字长短收紧,居中。 */
    Large(CupertinoButtonSize.Large),
}

/** 大按钮的最小宽度,字少时不缩成小圆块。 */
private val LargeButtonMinWidth = 160.dp

/** 胶囊按钮。 */
@Composable
fun AstraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AstraButtonStyle = AstraButtonStyle.Filled,
    size: AstraButtonSize = AstraButtonSize.Medium,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = Astra.colors
    val buttonColors = when (style) {
        AstraButtonStyle.Filled -> CupertinoButtonDefaults.filledButtonColors(
            contentColor = colors.onAccent,
            containerColor = colors.accentFill,
        )
        AstraButtonStyle.Tinted -> CupertinoButtonDefaults.grayButtonColors()
        AstraButtonStyle.Plain -> CupertinoButtonDefaults.plainButtonColors()
        AstraButtonStyle.Destructive -> CupertinoButtonDefaults.grayButtonColors(contentColor = colors.danger)
    }
    val button: @Composable (Modifier) -> Unit = { m ->
        CupertinoButton(
            onClick = onClick,
            modifier = m,
            enabled = enabled,
            size = size.hig,
            colors = buttonColors,
        ) {
            if (leadingIcon != null) {
                CupertinoIcon(leadingIcon, null, Modifier.padding(end = 6.dp).size(18.dp))
            }
            CupertinoText(text, maxLines = 1)
        }
    }
    // 按钮一律按文字长短收紧,不跟着所占的位置拉宽(页面给了整行或半行时,按钮在那块地方里居中);大按钮占一整行居中
    Box(if (size == AstraButtonSize.Large) modifier.fillMaxWidth() else modifier, contentAlignment = Alignment.Center) {
        button(if (size == AstraButtonSize.Large) Modifier.widthIn(min = LargeButtonMinWidth) else Modifier)
    }
}

/** 列表右侧表示功能开关状态的胶囊:开着是实心「已开启」,关着是浅灰「开启」。 */
@Composable
fun AstraStatePill(
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AstraButton(
        text = if (on) "已开启" else "开启",
        onClick = onClick,
        modifier = modifier,
        style = if (on) AstraButtonStyle.Filled else AstraButtonStyle.Tinted,
        size = AstraButtonSize.Small,
        enabled = enabled,
    )
}

/** 小标记,例如「星尘」。 */
@Composable
fun AstraBadge(text: String, modifier: Modifier = Modifier) {
    AstraText(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Astra.colors.fill)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        style = Astra.type.caption2,
        color = Astra.colors.labelSecondary,
        maxLines = 1,
    )
}
