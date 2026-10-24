package com.astraflow.Chizuru.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astraflow.Chizuru.ui.kit.icons.AstraGlyphs
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.CupertinoText
import zone.ien.hig.theme.CupertinoTheme
import kotlin.math.abs

/*
 * 页面的排法(用户定的草图「方向 A」,照 iOS 26 应用商店):零件全是上游的(字号、颜色、按钮、圆角都取上游),
 * 这里只管怎么摆:大标题下面的块标题、圆角大卡片、带说明的功能行、两列小卡片。设置类的内容照常用上游分组列表。
 */

/**
 * 块标题(照应用商店):粗体大字;[onOpen] 不为空时标题后面跟一个灰色「›」,点标题打开这一块的全部内容
 * (用户 2026-10-04 嫌右边单独的「全部」不好看,选了标题后加「›」)。
 */
fun LazyListScope.astraBlockTitle(
    title: String,
    key: Any,
    onOpen: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) = item(key = key) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = AstraDimens.pageGutter + 2.dp, end = AstraDimens.pageGutter + 2.dp, top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            if (onOpen != null) Modifier.clickable(onClickLabel = "全部", role = Role.Button, onClick = onOpen) else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CupertinoText(
                title,
                style = CupertinoTheme.typography.title2,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (onOpen != null) {
                // 小一号的灰色箭头(用户 2026-10-04 嫌和标题差不多高「太大了」)
                CupertinoIcon(
                    AstraGlyphs.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(BlockTitleChevron),
                    tint = CupertinoTheme.colorScheme.secondaryLabel,
                )
            }
        }
        if (action != null) {
            Spacer(Modifier.weight(1f))
            action()
        }
    }
}

/** 块标题后面「›」的大小。 */
private val BlockTitleChevron = 12.dp

/** 圆角大卡片:与上游分组同样的左右留白、圆角和底色;[onClick] 不为空时整张卡可点。 */
@Composable
fun AstraCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    horizontalPadding: Dp = AstraDimens.pageGutter,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth()
            .clip(CupertinoTheme.shapes.extraLarge)
            .background(Astra.colors.card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

/** 圆角方块图标:浅色为彩色玻璃,深色为深灰底与彩色图案;尺寸与图形占比沿用设置行的小方块。 */
@Composable
fun AstraIconTile(icon: ImageVector, color: Color, size: Dp, modifier: Modifier = Modifier) {
    val dark = CupertinoTheme.colorScheme.isDark
    val shape = astraCardShape(size * 0.25f)
    val gray = color.red == color.green || abs(color.red - color.blue) < 8f / 255f
    val glyphTop = if (dark) color.mixWith(Color.White, if (gray) 0.55f else 0.38f) else Color.White
    val glyphBottom = if (dark) (if (gray) color.mixWith(Color.White, 0.22f) else color) else Color.White.copy(alpha = 0.9f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .drawWithCache {
                val edge = this.size.width
                val background = if (dark) Brush.verticalGradient(listOf(Color(0xFF3A3A3C), Color(0xFF161618))) else
                    Brush.verticalGradient(0f to color.mixWith(Color.White, 0.3f), 0.55f to color, 1f to color.mixWith(Color.Black, 0.1f))
                val sheen = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = if (dark) 0.06f else 0.22f), Color.Transparent),
                    center = Offset(edge * 0.22f, -edge * 0.12f), radius = edge * 0.95f,
                )
                val rim = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (dark) 0.32f else 0.55f),
                    (if (dark) 0.35f else 0.3f) to Color.Transparent,
                    0.8f to Color.Transparent,
                    1f to Color.White.copy(alpha = if (dark) 0.08f else 0.2f),
                )
                val outline = shape.createOutline(this.size, layoutDirection, this)
                onDrawWithContent {
                    drawRect(background)
                    drawRect(sheen)
                    drawContent()
                    drawOutline(outline, rim, style = Stroke(edge * 0.032f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CupertinoIcon(
            icon, contentDescription = null, tint = Color.White,
            modifier = Modifier.size(size * 0.625f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val brush = Brush.verticalGradient(listOf(glyphTop, glyphBottom))
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush, blendMode = BlendMode.SrcIn)
                    }
                },
        )
    }
}

/** 与图标方案的出图程序使用同一种 RGB 混色。 */
private fun Color.mixWith(other: Color, fraction: Float) = Color(
    red = red + (other.red - red) * fraction,
    green = green + (other.green - green) * fraction,
    blue = blue + (other.blue - blue) * fraction,
)

/**
 * 应用商店式的一行:左边方块图标([iconSize];[leading] 不为空时换成它),名字(可带「星尘」标记)和一行灰色说明,右边 [trailing];
 * 不是最后一行时下面有一条从文字左边开始的分隔线。[onClick] 为空时这一行只显示、不能点。
 */
@Composable
fun AstraAppRow(
    icon: ImageVector?,
    color: Color,
    title: String,
    summary: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 44.dp,
    badge: String? = null,
    divider: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            leading != null -> leading()
            icon != null -> AstraIconTile(icon, color, iconSize)
        }
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CupertinoText(title, style = CupertinoTheme.typography.headline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (badge != null) AstraBadge(badge)
                    }
                    if (summary != null) {
                        CupertinoText(
                            summary,
                            style = CupertinoTheme.typography.footnote,
                            color = CupertinoTheme.colorScheme.secondaryLabel,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                trailing()
            }
            if (divider) AstraHairline()
        }
    }
}

/** 细分隔线:上游分组里的分隔线颜色。 */
@Composable
fun AstraHairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(CupertinoTheme.colorScheme.separator),
    )
}

/** 卡片上方大图区里左上角的小字(例如「星河岛」「系统 · 新功能」)。 */
@Composable
fun AstraEyebrow(text: String, color: Color, modifier: Modifier = Modifier) {
    CupertinoText(text, modifier = modifier, style = CupertinoTheme.typography.footnote, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
}
