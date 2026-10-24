package com.astraflow.Chizuru.ui.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.LocalContentColor
import zone.ien.hig.CupertinoText

/** 星流界面统一的尺寸,与上游 compose-hig 的分组列表一致(左右留白、行内留白、行高、卡片圆角都照上游)。改这里会影响全部页面。 */
object AstraDimens {
    /** 页面左右留白。 */
    val pageGutter = 18.dp
    /** 分组卡片的圆角。 */
    val groupCorner = 24.dp
    /** 列表行的最小高度、左右内边距。 */
    val rowMinHeight = 45.dp
    val rowPadding = 18.dp
    /** 设置行左侧小图标、功能行左侧大图标。 */
    val rowIcon = 32.dp
    val featureIcon = 46.dp
    /** 分组之外的块之间的距离(上游分组自己上下各留 12)。 */
    val groupGap = 12.dp
    /** 底栏高度与它离屏幕底边(手势条上方)的距离。 */
    val tabBarHeight = 64.dp
    val tabBarBottomGap = 10.dp
    /** 页面最后要给底栏让出的高度。 */
    val tabBarClearance = tabBarHeight + tabBarBottomGap + 24.dp
}

/** 分组卡片、大卡片用的连续曲率圆角。 */
fun astraCardShape(corner: Dp = AstraDimens.groupCorner): Shape = RoundedRectangle(corner)

/** 文字。默认用正文字号和主文字色。 */
@Composable
fun AstraText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Astra.type.body,
    color: Color = Astra.colors.label,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    CupertinoText(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign ?: TextAlign.Unspecified,
        overflow = overflow,
        maxLines = maxLines,
        style = style,
    )
}

/** 图标,按 [tint] 上色;不给时跟着所在上游部件的文字颜色。 */
@Composable
fun AstraIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
) {
    CupertinoIcon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

/**
 * 可以点的行、卡片:按住时底色变深(照 iOS 列表的按下效果),不加水波纹。
 * [pressedColor] 为空时用主题的按下底色。[interactionSource] 给了时用它报按压,
 * 行里的开关可以拿同一个来源,行点完开关变了也有触感。
 */
fun Modifier.astraPressable(
    enabled: Boolean = true,
    shape: Shape? = null,
    restColor: Color = Color.Transparent,
    pressedColor: Color? = null,
    role: Role? = Role.Button,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val target = if (pressed && enabled) pressedColor ?: Astra.colors.cardPressed else restColor
    val color by animateColorAsState(target, tween(if (pressed) 0 else 220), label = "press")
    val bg = if (shape != null) Modifier.background(color, shape) else Modifier.background(color)
    this
        .then(bg)
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}
