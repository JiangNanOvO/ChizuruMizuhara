# compose-hig 27.0.1 API 参考

给 chizuru-astraflow 的 Compose 重写用。所有签名取自上游 **zone.ien.hig:hig 27.0.1-alpha05.1** 的
`hig-27.0.1-alpha05.1-sources.jar` / `hig-core-27.0.1-alpha05.1-sources.jar`（chizuru 依赖的是
`zone.ien.hig:hig:27.0.1-alpha05`，两者内容相同），并用 `classes.jar` 里的真实 class 用 javap 交叉核对过
（例如 CupertinoScaffold 有 4 个重载、CupertinoAlertDialog 的 9 个参数顺序、section 的 9 个参数）。

- 标记 **【实验】** = 声明处带 `@ExperimentalCupertinoApi`，调用方必须有 opt-in。
- 标记 **【稳定】** = 不需要 opt-in。
- 参数名、类型、顺序、默认值都按源码照抄；`= ...` 后面是默认值。

---

## 0. 一次性约定（先看这个）

### OptIn

整个文件加一行最省事（上游自己的页面也是这么干的）：

```kotlin
@file:OptIn(ExperimentalCupertinoApi::class)

import zone.ien.hig.ExperimentalCupertinoApi
```

`ExperimentalCupertinoApi` 的 level 是 **WARNING**（不是 ERROR），漏了只是警告；但仓库里一般都 opt-in。

### 坐标（包）

| 位置 | 包 |
| --- | --- |
| 骨架、顶栏、导航栏、按钮、弹层、确认框、搜索框、开关、文字、图标、Surface、Divider | `zone.ien.hig` |
| 分组列表 | `zone.ien.hig.section` |
| 主题（CupertinoTheme / ColorScheme / Typography / Shapes / CupertinoColors） | `zone.ien.hig.theme` |
| rememberDefaultBackdrop（以及 internal 的 CupertinoGlassDefaults） | `zone.ien.hig.utils` |
| 图标 | `zone.ien.hig.icons` / `.icons.filled` / `.icons.outlined` |
| 玻璃（kyant backdrop 2.0.1） | `com.kyant.backdrop.*` |

依赖（chizuru 已有）：

```kotlin
implementation("zone.ien.hig:hig:27.0.1-alpha05")
implementation("zone.ien.hig:hig-icons-extended:27.0.1-alpha05")
implementation("io.github.kyant0:backdrop:2.0.1")
```

> 注意：分组列表的 `section`、`switch`、`link` 等都是 **顶层扩展函数**，必须单独 import，
> 例如 `import zone.ien.hig.section.section`、`import zone.ien.hig.section.switch`；
> 确认框的 `cancel` / `default` 是 `zone.ien.hig` 包的扩展函数，也要单独 import
> （见 `SettingsScreen.kt` 的 import 列表）。

---

## 1. 页面骨架 CupertinoScaffold

### CupertinoScaffold 【实验】

源码里一共 **4 个重载**。正常页面只会用到第 1 个；另外 3 个多出的是
`bottomBarBackdropEnabled: Boolean`（无默认值）和/或 `hasSoftBottomEdge: Boolean`（无默认值），
不显式传就不会被选中，**不用管**。

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = CupertinoScaffoldDefaults.containerColor,
    contentColor: Color = CupertinoScaffoldDefaults.contentColor,
    contentWindowInsets: WindowInsets = CupertinoScaffoldDefaults.contentWindowInsets,
    appBarsBlurAlpha: Float = CupertinoScaffoldDefaults.AppBarsBlurAlpha,
    appBarsBlurRadius: Dp = CupertinoScaffoldDefaults.AppBarsBlurRadius,
    hasNavigationTitle: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
)
```

- 用法要点：`content` 的参数是 `PaddingValues`（不是 `innerPadding`），
  一般写成 `contentPadding = PaddingValues(top = pv.calculateTopPadding(), bottom = pv.calculateBottomPadding() + 24.dp)` 喂给 `LazyColumn`。
- `hasNavigationTitle = true` 才能在列表里放 `CupertinoNavigationTitle`（大标题随滚动收进顶栏）。
- 顶栏/底栏的模糊只作用于"滚到栏后面的内容"，需要滚动容器上打 `.layerBackdrop(backdrop)`（见 §13）。

其它重载（只为查证，写代码不用）：

```kotlin
// 2) 多一个 bottomBarBackdropEnabled（必需，无默认值）
fun CupertinoScaffold(modifier, topBar, bottomBar, bottomBarBackdropEnabled: Boolean, snackbarHost, floatingActionButton, ...)
// 3) 多一个 hasSoftBottomEdge（必需，无默认值）
fun CupertinoScaffold(modifier, topBar, bottomBar, snackbarHost, floatingActionButton, ..., hasNavigationTitle, hasSoftBottomEdge: Boolean, content)
// 4) 两个都多
fun CupertinoScaffold(modifier, topBar, bottomBar, bottomBarBackdropEnabled: Boolean, ..., hasSoftBottomEdge: Boolean, content)
```

### CupertinoScaffoldDefaults 【稳定】（object，全在 zone.ien.hig）

```kotlin
object CupertinoScaffoldDefaults {
    val AppBarsBlurAlpha: Float          // 0.5f；系统开"减少透明度"时 0.85f
    val AppBarsBlurRadius: Dp            // 40.dp；"减少透明度"时 50.dp

    val contentWindowInsets: WindowInsets   // @Composable get() = WindowInsets.systemBars
    val containerColor: Color               // @Composable @ReadOnlyComposable, systemBackground
    val contentColor: Color                 // @Composable @ReadOnlyComposable, label
}

@kotlin.jvm.JvmInline
value class FabPosition internal constructor(private val value: Int) {
    companion object { val Center = FabPosition(0); val End = FabPosition(1) }
}
```

### CupertinoNavigationTitle 【稳定】

```kotlin
@Composable
fun CupertinoNavigationTitle(
    modifier: Modifier = Modifier,
    maxFontScale: Float = NavTitleMaxFontScale,
    maxFontScaleDistance: Dp = 150.dp,
    paddingValues: PaddingValues = CupertinoSectionDefaults.PaddingValues,
    subtitle: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
)
```

- 用法要点：**必须是列表第一个可滚动容器内的第一个元素**（上游 KDoc 明确写：一个容器只能有一个 title）。
  它靠这个约定自动控制 `CupertinoTopAppBar` 的标题显隐与背景透明度；违反约定行为不确定。
- 用了它，`CupertinoScaffold` 要传 `hasNavigationTitle = true`，`LazyColumn` 的 `contentPadding` 顶部留 `pv.calculateTopPadding()`。

### 其它同文件（【稳定】，一般不用）

```kotlin
@Composable fun Modifier.cupertinoScrollEdge(state: ScrollableState): Modifier   // 让顶栏在滚动边缘变透明
val LocalContainerColor: ProvidableCompositionLocal<Color>                      // 公开
```

---

## 2. 顶部栏 CupertinoTopAppBar

### CupertinoTopAppBar 【实验】

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoTopAppBar(
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    windowInsets: WindowInsets = LocalTopAppBarInsets.current ?: CupertinoTopAppBarDefaults.windowInsets,
    isCenterAligned: Boolean = true,
    isBackgroundAdaptive: Boolean = true,
    isBackgroundGradient: Boolean = false,
    backdrop: LayerBackdrop = LocalCupertinoBackdrop.current ?: rememberDefaultBackdrop(),
    colors: CupertinoTopAppBarColors = CupertinoTopAppBarDefaults.topAppBarColors(),
)
```

- 用法要点：`title` 是**第一个**位置参数（必填，没有默认值）。
- `isBackgroundAdaptive = true`（默认）会定期把背后的画面拍下来算平均亮度——**在主线程上取图，会掉帧**。
  星流在设置页显式传了 `isBackgroundAdaptive = false`，chizuru 建议照做。
- `backdrop` 传在外面 `rememberLayerBackdrop()` 拿的那个，否则退化成自己画一块底色（不模糊页面真实内容）。
- `actions` 是 `RowScope.() -> Unit`，里面可以直接用 `Row` 的成员。

### CupertinoTopAppBarDefaults 【实验】（object）

```kotlin
object CupertinoTopAppBarDefaults {
    val windowInsets: WindowInsets   // @Composable get() = systemBars.only(Horizontal + Top)

    @Composable @ReadOnlyComposable
    fun topAppBarColors(
        lightGradientColor: Color = lightColorScheme().tertiarySystemBackground,
        darkGradientColor: Color = darkColorScheme().tertiarySystemBackground,
        navigationIconContentColor: Color = CupertinoTheme.colorScheme.accent,
        lightTitleContentColor: Color = lightColorScheme().label,
        darkTitleContentColor: Color = darkColorScheme().label,
        lightSubtitleContentColor: Color = lightColorScheme().secondaryLabel,
        darkSubtitleContentColor: Color = darkColorScheme().secondaryLabel,
        actionIconContentColor: Color = CupertinoTheme.colorScheme.accent,
    ): CupertinoTopAppBarColors
}
```

### CupertinoTopAppBarColors 【稳定】

```kotlin
@Stable
class CupertinoTopAppBarColors internal constructor(...) {
    @Composable fun gradientColor(isDark: Boolean = CupertinoTheme.colorScheme.isDark): State<Color>
    @Composable fun titleContentColor(isDark: Boolean = CupertinoTheme.colorScheme.isDark): State<Color>
    @Composable fun subtitleContentColor(isDark: Boolean = CupertinoTheme.colorScheme.isDark): State<Color>
}
```

> 构造函数是 `internal`，只能通过 `CupertinoTopAppBarDefaults.topAppBarColors(...)` 得到。

### 其它顶栏相关【实验】

```kotlin
@Composable @ExperimentalCupertinoApi
fun LazyListState.isTopBarTransparent(topPadding: Dp = 0.dp): Boolean

@Composable @ExperimentalCupertinoApi
fun cupertinoTranslucentTopBarColor(color: Color, isTranslucent: Boolean, isTransparent: Boolean): Color
```

---

## 3. 底部导航 CupertinoNavigationBar

### CupertinoNavigationBar 【实验】（两个重载）

**普通用法用第 1 个（没有 `prominentTabIndex`）**：栏自己画那条滑动的选择胶囊，items 不需要 index。
第 2 个多一个 `prominentTabIndex: Int?`（**必需，无默认值**），是"中间大图标 / 可折叠"形态。

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoNavigationBar(
    modifier: Modifier = Modifier,
    colors: CupertinoNavigationBarColors = CupertinoNavigationBarDefaults.colors(),
    windowInsets: WindowInsets = CupertinoNavigationBarDefaults.windowInsets,
    backdrop: LayerBackdrop,                 // 必填，没有默认值
    selectedTabIndex: () -> Int,             // 注意是 lambda，不是 Int
    onTabSelected: (index: Int) -> Unit,
    tabsCount: Int,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    trailingAction: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
)

// 第 2 个：多 prominentTabIndex（必需）与 isCollapsed（有默认值）
fun CupertinoNavigationBar(
    modifier: Modifier = Modifier,
    colors: CupertinoNavigationBarColors = CupertinoNavigationBarDefaults.colors(),
    windowInsets: WindowInsets = CupertinoNavigationBarDefaults.windowInsets,
    backdrop: LayerBackdrop,
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    tabsCount: Int,
    prominentTabIndex: Int?,
    isCollapsed: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    trailingAction: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
)
```

- 用法要点：`backdrop`、`selectedTabIndex`、`onTabSelected`、`tabsCount` 四个都要传；
  `selectedTabIndex` 传 `{ state }`（选择胶囊要做动画，读的是实时值）。
- 只有带 `prominentTabIndex` 的那个重载会向 items 提供选中状态；不带时用**无 index 的 item 重载**。

### CupertinoNavigationBarItem

```kotlin
// 普通 item（不需要 index；配合没有 prominentTabIndex 的导航栏）
@Composable
fun RowScope.CupertinoNavigationBarItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)

// 带 index 的 item（配合 prominentTabIndex / isCollapsed 的重载；index 必须从 0 连续）
@Composable
fun RowScope.CupertinoNavigationBarItem(
    index: Int,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```

两个都**【稳定】**。`label` 是 `@Composable (() -> Unit)?`（不是 String）。

### CupertinoNavigationBarDefaults 【实验】

```kotlin
@ExperimentalCupertinoApi @Immutable
object CupertinoNavigationBarDefaults {
    val containerColor: Color            // @Composable @ReadOnlyComposable, CupertinoGlassDefaults.tint
    val windowInsets: WindowInsets = WindowInsets(left = 16.dp, right = 16.dp)
    val BottomPadding: Dp = 20.dp

    @Composable @ReadOnlyComposable
    fun colors(
        accentColor: Color = CupertinoTheme.colorScheme.accent,
        containerColor: Color = CupertinoGlassDefaults.tint,
        selectedIconColor: Color = CupertinoTheme.colorScheme.accent,
        selectedTextColor: Color = CupertinoTheme.colorScheme.accent,
        unselectedIconColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        unselectedTextColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        disabledIconColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        disabledTextColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
    ): CupertinoNavigationBarColors
}
```

### CupertinoNavigationBarColors 【实验】

`@Stable class CupertinoNavigationBarColors internal constructor(...)`，字段全是 `internal`，
外部只能通过 `CupertinoNavigationBarDefaults.colors(...)` 造。

---

## 4. 按钮

### CupertinoButton 【实验】

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: CupertinoButtonSize = CupertinoButtonSize.Regular,
    colors: CupertinoButtonColors = filledButtonColors(),   // = CupertinoButtonDefaults.filledButtonColors()
    border: BorderStroke? = null,
    shape: Shape = size.shape(CupertinoTheme.shapes),
    contentPadding: PaddingValues = size.contentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
)
```

- 用法要点：`content` 是 `RowScope.() -> Unit`（可以放多个元素，默认横向排）。
- `colors` 默认是 filled（实心主题色）。

### CupertinoIconButton 【实验】

```kotlin
@ExperimentalCupertinoApi @Composable
fun CupertinoIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CupertinoButtonColors = plainButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
)
```

### CupertinoButtonSize 【实验】（enum，4 项）

```kotlin
enum class CupertinoButtonSize(
    val shape: (Shapes) -> Shape,
    val textStyle: (Typography) -> TextStyle = { it.body },
    val contentPadding: PaddingValues,
) {
    Small,      // shape = CircleShape,       textStyle = subhead, padding = (12.dp, 6.dp)
    Regular,    // shape = ContinuousCapsule, textStyle = body,    padding = (16.dp, 10.dp)
    Large,      // shape = ContinuousCapsule, textStyle = body,    padding = (24.dp, 18.dp)
    ExtraLarge, // shape = CircleShape,       textStyle = body,    padding = (24.dp, 18.dp)
}
```

> `shape` 是 `(Shapes) -> Shape`，直接用会写成 `size.shape(CupertinoTheme.shapes)`。

### CupertinoButtonDefaults 【实验】（object）

四个工厂都是 `@Composable @ReadOnlyComposable`，**必须在 composable 里调用**：

```kotlin
object CupertinoButtonDefaults {
    @Composable @ReadOnlyComposable
    fun filledButtonColors(
        contentColor: Color = Color.Unspecified,             // 未指定时按背景亮度算白/黑
        containerColor: Color = CupertinoTheme.colorScheme.accent,
        disabledContentColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        disabledContainerColor: Color = CupertinoTheme.colorScheme.quaternarySystemFill,
        indicationColor: Color = Color.Unspecified,
    ): CupertinoButtonColors

    @Composable @ReadOnlyComposable
    fun tintedButtonColors(
        contentColor: Color = CupertinoTheme.colorScheme.accent,
        containerColor: Color = contentColor.copy(alpha = 0.2f),
        disabledContentColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        disabledContainerColor: Color = CupertinoTheme.colorScheme.quaternarySystemFill,
        indicationColor: Color = contentColor.copy(alpha = .15f),
    ): CupertinoButtonColors

    @Composable @ReadOnlyComposable
    fun grayButtonColors(
        contentColor: Color = CupertinoTheme.colorScheme.accent,
        containerColor: Color = CupertinoTheme.colorScheme.quaternarySystemFill,
        disabledContentColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        disabledContainerColor: Color = CupertinoTheme.colorScheme.quaternarySystemFill,
        indicationColor: Color = CupertinoColors.DefaultAlpha,
    ): CupertinoButtonColors

    @Composable @ReadOnlyComposable
    fun plainButtonColors(
        contentColor: Color = CupertinoTheme.colorScheme.accent,
        containerColor: Color = Color.Transparent,
        disabledContentColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        disabledContainerColor: Color = Color.Transparent,
        indicationColor: Color = Color.Transparent,
    ): CupertinoButtonColors

    // 以下四个都 @Deprecated，别用：
    // borderedProminentButtonColors -> filledButtonColors
    // borderedButtonColors          -> tintedButtonColors
    // borderlessButtonColors        -> plainButtonColors
    // borderedGrayButtonColors      -> grayButtonColors
}
```

### CupertinoButtonColors 【稳定】

```kotlin
@Immutable
class CupertinoButtonColors internal constructor(...) {
    @Composable fun containerColor(enabled: Boolean): State<Color>
    @Composable fun contentColor(enabled: Boolean): State<Color>
}
```

> `internal` 构造函数，只能用上面的工厂；两个 getter 返回 `State<Color>`，取要 `by`。

### CupertinoLiquidButton / CupertinoLiquidIconButton 【实验】（玻璃按钮）

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoLiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: CupertinoButtonSize = CupertinoButtonSize.Regular,
    colors: CupertinoLiquidButtonColors = glassButtonColors(),
    shape: Shape = size.shape(CupertinoTheme.shapes),
    contentPadding: PaddingValues = size.contentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    backdrop: Backdrop,                     // 必填
    isBackgroundAdaptive: Boolean = true,
    isInteractive: Boolean = true,
    content: @Composable RowScope.() -> Unit,
)

@ExperimentalCupertinoApi @Composable
fun CupertinoLiquidIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CupertinoLiquidButtonColors = glassButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    backdrop: Backdrop,                     // 必填
    isBackgroundAdaptive: Boolean = true,
    isInteractive: Boolean = true,
    content: @Composable () -> Unit,
)
```

- 用法要点：`backdrop` 类型是 `com.kyant.backdrop.Backdrop`（不是 LayerBackdrop），传 `rememberLayerBackdrop()` 的返回值即可。
- 图标按钮没有 `size`/`border`/`shape` 参数，固定圆形 48.dp。

### CupertinoLiquidButtonDefaults 【实验】（object）

```kotlin
object CupertinoLiquidButtonDefaults {
    @Composable @ReadOnlyComposable
    fun glassProminentButtonColors(          // 实心玻璃（主题色）
        lightTintColor: Color = CupertinoTheme.colorScheme.accent,
        lightSurfaceColor: Color = Color.Unspecified,
        lightContentColor: Color = CupertinoGlassDefaults.contentColor(lightTintColor, lightColorScheme().systemBackground),
        darkTintColor: Color = CupertinoTheme.colorScheme.accent,
        darkSurfaceColor: Color = Color.Unspecified,
        darkContentColor: Color = CupertinoGlassDefaults.contentColor(darkTintColor, darkColorScheme().systemBackground),
        disabledLightTintColor: Color = Color.Unspecified,
        disabledLightSurfaceColor: Color = lightColorScheme().systemFill,
        disabledLightContentColor: Color = lightColorScheme().tertiaryLabel,
        disabledDarkTintColor: Color = Color.Unspecified,
        disabledDarkSurfaceColor: Color = darkColorScheme().systemFill,
        disabledDarkContentColor: Color = darkColorScheme().tertiaryLabel,
        lightIndicationColor: Color = lightContentColor.copy(alpha = 0.2f),
        darkIndicationColor: Color = darkContentColor.copy(alpha = 0.2f),
    ): CupertinoLiquidButtonColors

    @Composable @ReadOnlyComposable
    fun glassButtonColors(                    // 透明玻璃（默认）
        lightTintColor: Color = Color.Unspecified,
        lightSurfaceColor: Color = Color.Unspecified,
        lightContentColor: Color = lightColorScheme().label,
        darkTintColor: Color = Color.Unspecified,
        darkSurfaceColor: Color = Color.Unspecified,
        darkContentColor: Color = darkColorScheme().label,
        disabledLightTintColor: Color = Color.Unspecified,
        disabledLightSurfaceColor: Color = Color.Unspecified,
        disabledLightContentColor: Color = lightColorScheme().tertiaryLabel,
        disabledDarkTintColor: Color = Color.Unspecified,
        disabledDarkSurfaceColor: Color = Color.Unspecified,
        disabledDarkContentColor: Color = darkColorScheme().tertiaryLabel,
        lightIndicationColor: Color = lightContentColor.copy(alpha = 0.2f),
        darkIndicationColor: Color = darkContentColor.copy(alpha = 0.2f),
    ): CupertinoLiquidButtonColors
}
```

### CupertinoNavigateBackButton 【实验】（返回按钮）

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoNavigateBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: CupertinoButtonSize = CupertinoButtonSize.Regular,
    shape: Shape = size.shape(CupertinoTheme.shapes),
    colors: CupertinoButtonColors = plainButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(8.dp, 4.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    icon: ImageVector = 按 LayoutDirection 自动选 ChevronBackward / ChevronForward,
    title: @Composable RowScope.() -> Unit,
)

@Composable
@ExperimentalCupertinoApi
fun CupertinoNavigateBackLiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CupertinoLiquidButtonColors = CupertinoLiquidButtonDefaults.glassButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    icon: ImageVector = 同上,
    backdrop: Backdrop,
    isBackgroundAdaptive: Boolean = true,
)
```

---

## 5. 弹层（BottomSheet）

### CupertinoBottomSheetScaffold 【实验】（两个重载）

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoBottomSheetScaffold(
    sheetContent: @Composable () -> Unit,                                  // 第一个位置参数
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = CupertinoScaffoldDefaults.contentWindowInsets,
    scaffoldState: CupertinoBottomSheetScaffoldState = rememberCupertinoBottomSheetScaffoldState(),
    colors: CupertinoBottomSheetScaffoldColors = CupertinoBottomSheetScaffoldDefaults.colors(),
    sheetShape: Shape = CupertinoBottomSheetDefaults.shape,
    sheetShadowElevation: Dp = CupertinoBottomSheetDefaults.ShadowElevation,
    sheetDragHandle: @Composable (() -> Unit)? = { CupertinoBottomSheetDefaults.DragHandle() },
    sheetSwipeEnabled: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    appBarsBlurAlpha: Float = CupertinoScaffoldDefaults.AppBarsBlurAlpha,
    appBarsBlurRadius: Dp = CupertinoScaffoldDefaults.AppBarsBlurRadius,
    hasNavigationTitle: Boolean = false,
    applyContentScaling: Boolean = CupertinoBottomSheetScaffoldDefaults.ApplyContentScaling,  // false
    content: @Composable (PaddingValues) -> Unit,
)

// 第二个重载：topBar/bottomBar 类型写成 (@Composable () -> Unit)?，并多一个
// bottomBarBackdropEnabled: Boolean（必需，无默认值）
```

- 用法要点：`content`（页面主体）是最后一个 lambda；`sheetContent` 是第一个参数，建议用具名实参。
  `sheetDragHandle = null` 去掉顶部的拖动小条。
- 主体/弹层里的页面骨架都用 `CupertinoScaffold` / `CupertinoBottomSheetContent`，标题栏共用 `CupertinoTopAppBar`。

### CupertinoBottomSheetScaffoldState 【实验】

```kotlin
@Stable
class CupertinoBottomSheetScaffoldState(val bottomSheetState: CupertinoSheetState)

@Composable
fun rememberCupertinoBottomSheetScaffoldState(
    bottomSheetState: CupertinoSheetState = rememberCupertinoSheetState(),
): CupertinoBottomSheetScaffoldState
```

### CupertinoBottomSheetScaffoldDefaults / Colors 【实验】

```kotlin
object CupertinoBottomSheetScaffoldDefaults {
    const val ApplyContentScaling = false

    @Composable
    fun colors(
        sheetContainerColor: Color = CupertinoBottomSheetDefaults.containerColor,
        sheetContentColor: Color = CupertinoBottomSheetDefaults.contentColor,
        containerColor: Color = CupertinoTheme.colorScheme.systemBackground,
        contentColor: Color = CupertinoTheme.colorScheme.label,
        scrimColor: Color = Color.Black.copy(alpha = 0.2f),
        scaledScaffoldBackgroundColor: Color = Color.Transparent,
    ): CupertinoBottomSheetScaffoldColors
}
```

### CupertinoBottomSheetContent 【实验】

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoBottomSheetContent(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalContainerColor.current.takeOrElse { CupertinoBottomSheetDefaults.containerColor },
    contentColor: Color = LocalContentColor.current.takeOrElse { CupertinoBottomSheetDefaults.contentColor },
    appBarsAlpha: Float = LocalAppBarsBlurAlpha.current,
    appBarsBlurRadius: Dp = LocalAppBarsBlurRadius.current,
    hasNavigationTitle: Boolean = false,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
)
```

- 用法要点：弹层内部的"页面骨架"，等于给 sheet 里的内容套一层内嵌 Scaffold（顶栏/底栏/窗口 inset 都对齐）。

### CupertinoBottomSheetDefaults 【实验】（object）

```kotlin
object CupertinoBottomSheetDefaults {
    val ShadowElevation: Dp = 0.dp
    val contentColor: Color    // @Composable @ReadOnlyComposable = label
    val containerColor: Color  // @Composable @ReadOnlyComposable = CupertinoGlassDefaults.panelTint
    val shape: Shape           // @Composable @ReadOnlyComposable = ContinuousRoundedRectangle(34.dp)

    @Composable
    fun DragHandle(
        modifier: Modifier = Modifier,
        width: Dp = 38.dp,
        height: Dp = 5.dp,
        shape: Shape = CircleShape,
        color: Color = CupertinoTheme.colorScheme.systemFill,
    )
}
```

### rememberCupertinoSheetState / CupertinoSheetState 【实验】

```kotlin
@Composable
fun rememberCupertinoSheetState(
    initialValue: CupertinoSheetValue = CupertinoSheetValue.Hidden,
    presentationStyle: PresentationStyle = PresentationStyle.Modal(),
    confirmValueChange: (CupertinoSheetValue) -> Boolean = { true },
): CupertinoSheetState

@Stable
class CupertinoSheetState(
    initialValue: CupertinoSheetValue = CupertinoSheetValue.Hidden,
    internal val presentationStyle: PresentationStyle = PresentationStyle.Modal(),
    internal val confirmValueChange: (CupertinoSheetValue) -> Boolean = { true },
) {
    val currentValue: CupertinoSheetValue
    val targetValue: CupertinoSheetValue
    val isVisible: Boolean
    fun requireOffset(): Float

    suspend fun show()                              // 展开到 PartiallyExpanded（有的话）或 Expanded
    suspend fun expand()                            // 展开到最大
    suspend fun partialExpand(detent: PresentationDetent)
    suspend fun hide()                              // 收起
    // internal suspend fun animateTo(...) / snapTo(...) / settle(...)
    // companion object { fun Saver(): Saver<CupertinoSheetState, *> }
}
```

- 用法要点：展开/收起都是 **suspend**，要 `scope.launch { state.show() }`；
  `currentValue` 只有在动画/拖动结束后才等于 `targetValue`。
- `partialExpand` 的 detent 必须先出现在 `PresentationStyle.Modal.detents` 里，否则抛 `IllegalArgumentException`。

### CupertinoSheetValue / PresentationStyle / PresentationDetent 【实验】（sealed interface）

```kotlin
sealed interface CupertinoSheetValue {
    data object Hidden : CupertinoSheetValue
    class PartiallyExpanded(val detent: PresentationDetent) : CupertinoSheetValue
    data object Expanded : CupertinoSheetValue
}

sealed interface PresentationStyle {
    data object Fullscreen : PresentationStyle
    class Modal(
        val detents: Set<PresentationDetent> = setOf(PresentationDetent.Large),
        val contentInteraction: PresentationContentInteraction = PresentationContentInteraction.Resize,
        val isBackgroundInteractive: (PresentationDetent) -> Boolean = { false },
        val dismissOnClickOutside: Boolean = true,
    ) : PresentationStyle
}

sealed interface PresentationDetent {
    fun calculate(density: Density, height: Float): Float
    data object Large : PresentationDetent                               // 永远最大
    data object Medium : PresentationDetent                              // = Fraction(.5f)
    class Height(height: Dp) : PresentationDetent                        // 固定高度
    class Fraction(fraction: Float) : PresentationDetent                 // 0f..1f，超出范围 require 失败
}

enum class PresentationContentInteraction { Scroll, Resize }
```

> `Height` 的 `height` 属性是 internal，但**构造函数是公开的**，外部 `PresentationDetent.Height(300.dp)` 可用。

---

## 6. 确认框（AlertDialog）

### CupertinoAlertDialog 【实验】

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    message: (@Composable () -> Unit)? = null,
    containerColor: Color = CupertinoDialogsDefaults.AlertContainerColor,
    shape: Shape = CupertinoDialogsDefaults.AlertShape,
    shadowElevation: Dp = CupertinoDialogsTokens.AlertDialogElevation,   // 0.dp（token 是 internal）
    properties: DialogProperties = DialogProperties(),
    buttonsOrientation: Orientation = CupertinoDialogsDefaults.ButtonOrientation, // Horizontal
    buttons: AlertDialogActionsScope.() -> Unit,                          // 尾随 lambda
)
```

- 用法要点：`buttons` 是最后一个参数、`AlertDialogActionsScope.() -> Unit`，尾随 lambda 写按钮；
  每个按钮是 `cancel({...}) { CupertinoText("取消") }` 这种形态（**点按只调回调，由调用方决定什么时候关**）。
- 上游这一版**没有内容槽（content slot）**：只有 title / message。
  （星流仓库的 `AstraLiquidAlertDialog.kt` 是基于它的派生文件，额外有 content 槽；chizuru 用上游原生版本时没有。）
- `onDismissRequest` 是"**已经**关闭之后"回调，不要在里面再改状态去关它（会重复）。

### 按钮 scope 与扩展

```kotlin
enum class AlertActionStyle { Default, Cancel, Destructive }

interface AlertDialogActionsScope {
    fun action(
        onClick: () -> Unit,
        style: AlertActionStyle = AlertActionStyle.Default,
        enabled: Boolean = true,
        title: @Composable () -> Unit,
    )
}

// 三个扩展函数（包 zone.ien.hig，要单独 import）：
fun AlertDialogActionsScope.default(onClick: () -> Unit, enabled: Boolean = true, title: @Composable () -> Unit)
fun AlertDialogActionsScope.destructive(onClick: () -> Unit, enabled: Boolean = true, title: @Composable () -> Unit)
fun AlertDialogActionsScope.cancel(onClick: () -> Unit, enabled: Boolean = true, title: @Composable () -> Unit)
```

- `cancel` 会排在其他按钮**下面**、底色不同、字加粗；`destructive` 是红字。
- 三个扩展都是**【稳定】**（本身不需要 opt-in，但调用处通常在 `@file:OptIn(ExperimentalCupertinoApi::class)` 文件里）。

### CupertinoDialogsDefaults 【实验】（object，部分）

```kotlin
object CupertinoDialogsDefaults {
    val ScrimColor: Color        // @Composable @ReadOnlyComposable
    val ButtonOrientation: Orientation = Orientation.Horizontal
    val ContainerColor: Color    // @Composable，面板底色 panelTint
    val AlertContainerColor: Color   // @Composable @ReadOnlyComposable，浅色白 0.7 / 深色 #1A1A1A 0.72
    val Shape: ContinuousRoundedRectangle        // 28.dp
    val AlertShape: ContinuousRoundedRectangle   // 34.dp
}
```

### CupertinoActionSheet 【实验】

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoActionSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    message: (@Composable () -> Unit)? = null,
    containerColor: Color = CupertinoDialogsDefaults.ContainerColor,
    secondaryContainerColor: Color = CupertinoTheme.colorScheme.tertiarySystemBackground,
    properties: DialogProperties = DialogProperties(),
    content: (@Composable () -> Unit)? = null,
    buttons: AlertDialogActionsScope.() -> Unit,
)
```

### CupertinoLiquidAlertDialog 【实验】（玻璃确认框）

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoLiquidAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    message: (@Composable () -> Unit)? = null,
    containerColor: Color = CupertinoLiquidDialogsDefaults.ContainerColor,
    shape: Shape = CupertinoLiquidDialogsDefaults.Shape,
    shadowElevation: Dp = CupertinoLiquidDialogsTokens.AlertDialogElevation,
    properties: DialogProperties = DialogProperties(),
    backdrop: Backdrop,                    // 必填
    buttonsOrientation: Orientation = CupertinoLiquidDialogsDefaults.ButtonOrientation,
    buttons: AlertDialogActionsScope.() -> Unit,
)
```

---

## 7. 搜索框 CupertinoSearchTextField

### CupertinoSearchTextField 【实验】

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    state: CupertinoSearchTextFieldState = rememberCupertinoSearchTextFieldState(),
    colors: CupertinoTextFieldColors = CupertinoSearchTextFieldDefaults.colors(),
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = CupertinoSearchTextFieldDefaults.PaddingValues,
    shape: Shape = CupertinoSearchTextFieldDefaults.shape,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = remember { KeyboardOptions(imeAction = ImeAction.Search) },
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    placeholder: @Composable () -> Unit = { CupertinoText("Search") },
    cancelButton: @Composable (() -> Unit)? = {
        CupertinoSearchTextFieldDefaults.cancelButton(
            onValueChange = onValueChange,
            interactionSource = interactionSource,
        )
    },
    leadingIcon: @Composable () -> Unit = { CupertinoSearchTextFieldDefaults.leadingIcon() },
    trailingIcon: @Composable () -> Unit = {},
)
```

- 用法要点：折叠靠**嵌套滚动**，要把 `state.nestedScrollConnection` 挂到下面的滚动容器
  （`Modifier.nestedScroll(state.nestedScrollConnection)`）；不挂就不会随滚动收起。
- 默认尾部按钮文案是英文 "Cancel"，要中文就自己传 `cancelButton = { ... }`。

### rememberCupertinoSearchTextFieldState 【稳定】

```kotlin
@Composable
fun rememberCupertinoSearchTextFieldState(
    initiallyExpanded: Boolean = true,
    scrollableState: ScrollableState? = null,
    collapse: (CupertinoSearchTextFieldState) -> Boolean = { !it.isFocused },
    blockScrollWhenFocusedAndEmpty: Boolean = true,
): CupertinoSearchTextFieldState
```

### CupertinoSearchTextFieldState 【稳定】（class，internal 构造函数）

```kotlin
val nestedScrollConnection: NestedScrollConnection
var progress: Float          // 0 = 展开，1 = 收起（private set）
var isFocused: Boolean       // private set
```

### CupertinoSearchTextFieldDefaults 【实验】（object）

```kotlin
object CupertinoSearchTextFieldDefaults {
    val shape: Shape          // @Composable @ReadOnlyComposable = ContinuousCapsule()
    val PaddingValues = PaddingValues(horizontal = 18.dp)

    @Composable
    fun leadingIcon(imageVector: ImageVector = CupertinoIcons.Outlined.MagnifyingGlass,
                    rotateWithLayoutDirection: Boolean = true)

    @Composable
    fun cancelButton(
        onValueChange: (String) -> Unit,
        colors: CupertinoButtonColors = plainButtonColors(),
        interactionSource: MutableInteractionSource,
        content: @Composable RowScope.() -> Unit = { CupertinoText("Cancel") },
    )

    @Composable @ReadOnlyComposable
    fun colors(
        focusedTextColor: Color = CupertinoTheme.colorScheme.label,
        unfocusedTextColor: Color = CupertinoTheme.colorScheme.label,
        disabledTextColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        errorTextColor: Color = CupertinoColors.systemRed,
        focusedContainerColor: Color = if (LocalCupertinoBackdrop.current != null) Color.Transparent else CupertinoGlassDefaults.tint,
        unfocusedContainerColor: Color = focusedContainerColor,
        disabledContainerColor: Color = unfocusedContainerColor.copy(alpha = ... * .65f),
        errorContainerColor: Color = disabledContainerColor,
        cursorColor: Color = CupertinoTheme.colorScheme.accent,
        errorCursorColor: Color = errorTextColor,
        selectionColors: TextSelectionColors = TextSelectionColors(cursorColor, cursorColor.copy(alpha = .25f)),
        focusedBorderColor: Color = if (LocalCupertinoBackdrop.current != null) Color.Transparent else CupertinoGlassDefaults.border,
        unfocusedBorderColor: Color = focusedBorderColor,
        disabledBorderColor: Color = ...,
        errorBorderColor: Color = errorTextColor,
        focusedLeadingIconColor: Color = CupertinoTheme.colorScheme.label,
        unfocusedLeadingIconColor: Color = focusedLeadingIconColor,
        disabledLeadingIconColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        errorLeadingIconColor: Color = focusedLeadingIconColor,
        focusedTrailingIconColor: Color = focusedLeadingIconColor,
        unfocusedTrailingIconColor: Color = focusedTrailingIconColor,
        disabledTrailingIconColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        errorTrailingIconColor: Color = focusedTrailingIconColor,
        focusedPlaceholderColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        unfocusedPlaceholderColor: Color = focusedPlaceholderColor,
        disabledPlaceholderColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        errorPlaceholderColor: Color = focusedPlaceholderColor,
    ): CupertinoTextFieldColors
}
```

---

## 8. 分组列表（section）

两个入口：`LazyListScope.section`（懒加载列表，推荐）和 `CupertinoSection`（非懒加载，用在普通 Column / 单独一张卡里）。

### LazyListScope.section 【实验】

```kotlin
@ExperimentalCupertinoApi
fun LazyListScope.section(
    style: SectionStyle? = null,                 // null = 用 LocalSectionStyle（默认 InsetGrouped）
    state: SectionState? = null,                 // 只有 Sidebar 样式 + 有 title 时用于折叠
    enterTransition: EnterTransition = CupertinoSectionDefaults.EnterTransition,
    exitTransition: ExitTransition = CupertinoSectionDefaults.ExitTransition,
    shape: ContinuousRoundedRectangle? = null,
    color: Color = Color.Unspecified,
    title: @Composable (LazyItemScope.() -> Unit)? = null,
    caption: @Composable (LazyItemScope.() -> Unit)? = null,
    content: LazySectionScope.() -> Unit,
)
```

- 用法要点：只能在 `LazyListScope`（`LazyColumn` / `LazyRow`）里调用；title / caption / 每一行都是**独立的 lazy item**。
- 组标题习惯写 `CupertinoText("运行状态".sectionTitle())` 让其跟随样式转大写（InsetGrouped/Grouped 会 uppercase；中文无影响）。

### LazyListScope.stickySection 【实验】

参数和 `section` 一样，只差一个：`title: @Composable (LazyItemScope.(PaddingValues) -> Unit)? = null`（标题带 padding 参数），用于吸顶组标题。

### CupertinoSection 【实验】（非懒加载）

```kotlin
@Composable
@ExperimentalCupertinoApi
fun CupertinoSection(
    modifier: Modifier = Modifier,
    style: SectionStyle = LocalSectionStyle.current,
    state: SectionState = rememberSectionState(canCollapse = true),
    enterTransition: EnterTransition = CupertinoSectionDefaults.EnterTransition,
    exitTransition: ExitTransition = CupertinoSectionDefaults.ExitTransition,
    shape: ContinuousRoundedRectangle = CupertinoSectionDefaults.shape(style),
    color: Color = if (style != SectionStyle.Sidebar && style.grouped) CupertinoSectionDefaults.Color else Color.Transparent,
    dividerPadding: PaddingValues = PaddingValues(start = CupertinoSectionDefaults.DividerPadding),
    contentPadding: PaddingValues = CupertinoSectionDefaults.paddingValues(
        style = style, includePaddingBetweenSections = true),
    title: (@Composable () -> Unit)? = null,
    caption: (@Composable () -> Unit)? = null,
    content: @Composable SectionScope.() -> Unit,
)
```

- 用法要点：`content` 的 receiver 是 `SectionScope`，所以里面可以直接写 `SectionItem(...)` / `SectionLink(...)`。
- 单独一张卡（不想带上下组间距）时传 `contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)`，星流 `AstraStandaloneLinkCard` 就是这么用的。

### SectionScope 的行（都是 SectionScope 的扩展）【实验】

```kotlin
@Composable fun SectionScope.SectionItem(          // 纯显示行
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = CupertinoSectionDefaults.PaddingValues,
    leadingContent: @Composable () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
)

@Composable fun SectionScope.SectionLink(          // 可点的行（默认右侧「›」）
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit = {},
    onClickLabel: String? = null,
    indication: Indication? = LocalIndication.current,
    interactionSource: MutableInteractionSource? = null,
    caption: @Composable () -> Unit = {},          // 箭头前的当前值
    chevron: @Composable () -> Unit = { CupertinoSectionDefaults.LabelChevron() },
    title: @Composable () -> Unit,
)

@Composable fun SectionScope.SidebarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    title: @Composable () -> Unit,
)

@Composable fun SectionScope.SectionTextField(     // 分组里直接放输入框
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable ((InteractionSource) -> Unit)? = 默认清除按钮,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    colors: CupertinoTextFieldColors? = null,
)

// 还有 SectionDropdownMenu / SectionDatePicker / SectionTimePicker（参数较多，见源码 SectionScope.kt）
```

**关键写法**：`SectionItem` / `SectionLink` 是 `SectionScope` 的成员扩展，不在 `CupertinoSection {}` 里时要先借一个 scope 对象（星流两份样例都是这个做法）：

```kotlin
private object Rows : SectionScope

LazyColumn {
    section(title = { CupertinoText("运行状态") }) {
        item(key = "enabled") {
            with(Rows) {
                SectionItem(
                    trailingContent = { CupertinoText("已启用", color = CupertinoTheme.colorScheme.secondaryLabel) },
                    title = { CupertinoText("在 LSPosed 中启用") },
                )
            }
        }
    }
}
```

### LazySectionScope（section {} 里的 receiver）【实验】

```kotlin
@Stable
sealed interface LazySectionScope {
    fun item(                                    // 自定义整行
        key: Any? = null,
        contentType: Any? = null,
        dividerPadding: Dp = CupertinoSectionDefaults.DividerPadding,
        content: @Composable (padding: PaddingValues) -> Unit,
    )

    fun sidebarItem(                             // 有默认实现
        selected: Boolean,
        onClick: () -> Unit,
        key: Any? = null,
        contentType: Any? = SidebarItemContentType,
        enabled: Boolean = true,
        leadingContent: @Composable () -> Unit = {},
        trailingContent: @Composable () -> Unit = {},
        title: @Composable () -> Unit,
    )
}
```

扩展函数（全部带 `@ExperimentalCupertinoApi`，除 `textField` 外）：

```kotlin
fun LazySectionScope.link(                       // 打开下一页的行
    onClick: () -> Unit,
    key: Any? = null,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    dividerPadding: Dp = if (icon != null) CupertinoSectionDefaults.DividerPaddingWithIcon
                        else CupertinoSectionDefaults.DividerPadding,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    caption: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = { CupertinoSectionDefaults.LabelChevron() },
    title: @Composable () -> Unit,
)

fun LazySectionScope.switch(                     // 开关行（最常用）
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    key: Any? = null,
    enabled: Boolean = true,
    colors: CupertinoSwitchColors? = null,       // null -> CupertinoSwitchDefaults.colors()
    icon: (@Composable () -> Unit)? = null,
    dividerPadding: Dp = if (icon != null) CupertinoSectionDefaults.DividerPaddingWithIcon
                        else CupertinoSectionDefaults.DividerPadding,
    interactionSource: MutableInteractionSource? = null,
    thumbContent: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
)

fun LazySectionScope.dropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    key: Any? = null,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    width: Dp = CupertinoDropdownMenuDefaults.SmallWidth,
    dividerPadding: Dp = ...,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    selectedLabel: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
    content: @Composable CupertinoMenuScope.() -> Unit,
)

fun LazySectionScope.datePicker(state: CupertinoDatePickerState, expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit, modifier: Modifier = Modifier,
    style: DatePickerStyle? = null, enabled: Boolean = true, icon: (@Composable () -> Unit)? = null,
    dividerPadding: Dp = ..., buttonColor: Color = Color.Unspecified,
    button: @Composable (buttonModifier: Modifier, titleModifier: Modifier, text: String) -> Unit = 默认,
    title: @Composable () -> Unit)

fun LazySectionScope.timePicker(state: CupertinoTimePickerState, expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, icon: @Composable (() -> Unit)? = null, dividerPadding: Dp = ...,
    buttonColor: Color = Color.Unspecified,
    button: @Composable (buttonModifier: Modifier, titleModifier: Modifier, text: String) -> Unit = 默认,
    title: @Composable () -> Unit)

// 没有 @ExperimentalCupertinoApi 标注
fun LazySectionScope.textField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable ((InteractionSource) -> Unit)? = 默认清除按钮,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    dividerPadding: Dp = CupertinoSectionDefaults.DividerPadding,
    colors: CupertinoTextFieldColors? = null,
)
```

- 用法要点：`switch` 的 `title` 是 `@Composable () -> Unit`，开关本体由上游画在行尾；不要自己再放 `CupertinoSwitch`。
- `key` 一定要给唯一值，否则开关状态在滚动/重组时会错位。

### CupertinoSectionDefaults 【实验】（object）

```kotlin
object CupertinoSectionDefaults {
    val PaddingValues: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 8.dp)   // 行的内边距
    val DividerPadding: Dp = 18.dp
    val DividerPaddingWithIcon: Dp = 18.dp + 45.dp + 6.dp                                    // 有左图标时用
    val SidebarItemPaddingValues: PaddingValues = PaddingValues(start = 10.dp)
    val Color: Color                 // @Composable @ReadOnlyComposable = secondarySystemGroupedBackground
    val EnterTransition: EnterTransition
    val ExitTransition: ExitTransition

    @Composable @ReadOnlyComposable
    fun paddingValues(style: SectionStyle, includePaddingBetweenSections: Boolean): PaddingValues

    @Composable @ReadOnlyComposable
    fun shape(style: SectionStyle = LocalSectionStyle.current): ContinuousRoundedRectangle

    @Composable @ReadOnlyComposable fun titleColor(style: SectionStyle): Color
    @Composable @ReadOnlyComposable fun titleTextStyle(style: SectionStyle = LocalSectionStyle.current): TextStyle
    @Composable @ReadOnlyComposable fun captionTextStyle(style: SectionStyle = LocalSectionStyle.current): TextStyle
    @Composable @ReadOnlyComposable fun captionColor(style: SectionStyle = LocalSectionStyle.current): Color
    @Composable @ReadOnlyComposable fun containerColor(style: SectionStyle): Color

    @Composable fun PickerButton(modifier: Modifier, expanded: Boolean,
        shape: Shape = CupertinoTheme.shapes.small,
        containerColor: Color = CupertinoTheme.colorScheme.quaternarySystemFill,
        activeContentColor: Color = CupertinoTheme.colorScheme.accent,
        contentColor: Color = CupertinoTheme.colorScheme.label,
        title: @Composable () -> Unit)

    @Composable fun LabelChevron()                                   // 行尾「›」
    @Composable fun TextFieldClearButton(visible: Boolean, onClick: () -> Unit)
}
```

### SectionStyle / LocalSectionStyle / SectionState

```kotlin
enum class SectionStyle(val inset: Boolean, val grouped: Boolean) {
    Sidebar(true, true), InsetGrouped(true, true), Grouped(false, true), Inset(true, false), Plain(false, false),
}

val LocalSectionStyle = compositionLocalOf { SectionStyle.InsetGrouped }

@Composable fun ProvideSectionStyle(style: SectionStyle, content: @Composable () -> Unit)
@Composable fun String.sectionTitle(style: SectionStyle = LocalSectionStyle.current): String
fun Modifier.sectionContainerBackground(style: SectionStyle? = null): Modifier

@Composable
fun rememberSectionState(initiallyCollapsed: Boolean = false, canCollapse: Boolean = true): SectionState

@Stable
class SectionState(initiallyCollapsed: Boolean, canCollapse: Boolean) {   // 构造函数没有默认值
    var isCollapsed: Boolean   // private set
    var canCollapse: Boolean
    fun toggle(); fun collapse(); fun expand()
    companion object { fun Saver(): Saver<SectionState, *> }
}
```

---

## 9. 开关 CupertinoSwitch

### CupertinoSwitch 【稳定】

```kotlin
@OptIn(InternalCupertinoApi::class)
@Composable
fun CupertinoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    thumbContent: @Composable (() -> Unit)? = null,
    colors: CupertinoSwitchColors = CupertinoSwitchDefaults.colors(),
    enabled: Boolean = true,
    backdrop: Backdrop = rememberDefaultBackdrop(),   // 有默认值
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```

- 用法要点：单独用才需要管 `backdrop`；用 `section { switch(...) }` 时不用（那个扩展内部处理）。
- 默认尺寸 51×31 dp。

### CupertinoSwitchDefaults 【稳定】（object）

```kotlin
object CupertinoSwitchDefaults {
    val Width: Dp = 51.dp
    val Height: Dp = 31.dp

    @Composable @ReadOnlyComposable
    fun colors(
        thumbColor: Color = Color.White,
        disabledThumbColor: Color = thumbColor,
        checkedTrackColor: Color = CupertinoColors.systemGreen,
        checkedIconColor: Color = CupertinoTheme.colorScheme.opaqueSeparator,
        uncheckedTrackColor: Color = CupertinoColors.Gray.copy(alpha = .33f),
        uncheckedIconColor: Color = checkedIconColor,
        disabledCheckedTrackColor: Color = checkedTrackColor.copy(alpha = .33f),
        disabledCheckedIconColor: Color = checkedIconColor,
        disabledUncheckedTrackColor: Color = uncheckedTrackColor,
        disabledUncheckedIconColor: Color = checkedIconColor,
    ): CupertinoSwitchColors
}
```

`CupertinoSwitchColors` 是 `@Immutable class ... internal constructor`，只能用工厂。

---

## 10. 文字 / 图标 / 表面

### CupertinoText 【稳定】（两个重载）

```kotlin
@Composable
fun CupertinoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
)

// AnnotatedString 版本同上，多一个 inlineContent: Map<String, InlineTextContent> = mapOf()
```

- 用法要点：`style` 默认 `LocalTextStyle.current`（`CupertinoTheme` 里被设成 `typography.body`），
  所以 `CupertinoTheme.typography.footnote` 之类直接传 `style = ...` 即可。

### ProvideTextStyle / LocalTextStyle / LocalContentColor 【稳定】

```kotlin
@Composable fun ProvideTextStyle(value: TextStyle, content: @Composable () -> Unit)
val LocalTextStyle: ProvidableCompositionLocal<TextStyle>          // zone.ien.hig
val LocalContentColor: ProvidableCompositionLocal<Color>           // zone.ien.hig
```

### CupertinoIcon 【稳定】（三个重载）

```kotlin
@Composable fun CupertinoIcon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current)
@Composable fun CupertinoIcon(bitmap: ImageBitmap, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current)
@Composable fun CupertinoIcon(painter: Painter, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current)

object CupertinoIconDefaults { val LargeSize = 24.dp; val MediumSize = 20.dp; val SmallSize = 14.dp }
```

- 用法要点：`contentDescription` **没有默认值**，必须显式传（`null` 表示装饰性图标）。
  图标没有内建尺寸，通常自己 `Modifier.size(20.dp)`。

### CupertinoSurface / Divider 【稳定】

```kotlin
@Composable
fun CupertinoSurface(modifier: Modifier = Modifier, shape: Shape = RectangleShape,
    color: Color = CupertinoTheme.colorScheme.systemBackground, shadowElevation: Dp = 0.dp,
    contentColor: Color = LocalContentColor.current, content: @Composable () -> Unit)

@Composable
fun CupertinoSurface(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RectangleShape, color: Color = CupertinoTheme.colorScheme.systemBackground,
    contentColor: Color = LocalContentColor.current, border: BorderStroke? = null,
    indication: Indication? = LocalIndication.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit)

@Composable fun CupertinoHorizontalDivider(modifier: Modifier = Modifier, thickness: Dp = CupertinoDividerDefaults.Thickness, color: Color = CupertinoDividerDefaults.color)
@Composable fun CupertinoVerticalDivider(modifier: Modifier = Modifier, thickness: Dp = CupertinoDividerDefaults.Thickness, color: Color = CupertinoDividerDefaults.color)
object CupertinoDividerDefaults { val Thickness = Dp.Hairline; val color: Color /* @Composable */ }
```

### 图标

```kotlin
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.outlined.ChevronForward    // 每个图标要单独 import 具体文件

CupertinoIcons.Default.X      // Default == Outlined
CupertinoIcons.Outlined.X
CupertinoIcons.Filled.X
```

> 图标是 `val CupertinoIcons.Outlined.X: ImageVector` 形式的**扩展属性**，**每个图标单独 import**
> （`import zone.ien.hig.icons.outlined.Bell`），不 import 时 `CupertinoIcons.Outlined.Bell` 解析不到。

**hig 主包自带（filled）**：Bell, Cart, CheckmarkCircle, Envelope, ExclamationmarkTriangle, Gearshape,
HandThumbsup, Heart, House, InfoCircle, Location, Lock, Paperplane, Person, PersonCropCircle,
PersonCropSquare, Phone, Play, PlusCircle, RectanglePortraitAndArrowForward, SquareAndArrowUp, Star, Trash, XmarkCircle

**hig 主包自带（outlined）**：ArrowClockwise, ArrowUturnLeft, ArrowUturnRight, Bell, Calendar, Cart, Checkmark,
CheckmarkCircle, ChevronBackward, ChevronDown, ChevronForward, ChevronUp, Ellipsis, Envelope,
ExclamationmarkTriangle, FaceSmiling, Gearshape, HandThumbsup, Heart, House, InfoCircle, ListBullet,
Location, Lock, MagnifyingGlass, Mappin, Paperplane, Pencil, Person, PersonCropCircle, PersonCropSquare,
Phone, Play, Plus, PlusCircle, RectanglePortraitAndArrowForward, SquareAndArrowUp, SquareAndPencil, Star,
Trash, WrenchAndScrewdriver, Xmark, XmarkCircle

**hig-icons-extended（chizuru 已依赖）**：outlined 484 个、filled 282 个，命名规则同上。
常用的一批（outlined）：

Airplane, Airpods, Alarm, AppleLogo, Appletv, ArrowDown, ArrowLeftArrowRight, ArrowUpArrowDown, At, Bag,
Bandage, Banknote, Barcode, Battery100, BedDouble, BellBadge, BellSlash, Bicycle, Bitcoinsign, Bolt, Book,
Bookmark, Brain, Briefcase, BubbleLeft, Building, Camera, ChartBar, ChartLineUptrendXyaxis, Checklist,
CheckmarkSeal, CheckmarkShield, CheckmarkSquare, Clock, Cloud, Command, Cpu, Creditcard, Crown, Cube,
DeleteLeft, Desktopcomputer, Display, Divide, Doc, DocText, Dollarsign, Drop, Ear, EnvelopeOpen, Eraser,
ExclamationmarkCircle, Externaldrive, Eye, EyeSlash, Faceid, Film, Flag, Flame, Folder, ForkKnife, Gear,
Gearshape2, Gift, GlobeDesk, Graduationcap, Hammer, HandRaised, HandTap, HandWave, Headphones, HeartCircle,
Hifispeaker, Homepod, Hourglass, Icloud, Ipad, Iphone, Key, Keyboard, Laptopcomputer, Leaf, Lightbulb,
Link, ListBulletIndent, LockOpen, LockSlash, Macwindow, Mail, Map, MappinSlash, Medal, Megaphone, Message,
Mic, MicSlash, Minus, MinusCircle, Moon, MusicNote, Network, Newspaper, NoteText, Number, Paintpalette,
Paperclip, Pause, Pawprint, PencilCircle, Percent, Person2, PersonCircle, Photo, Pill, Pin, PlayCircle,
PlusSquare, Power, Printer, Puzzlepiece, Qrcode, QuestionmarkCircle, QuoteOpening, RecordCircle, Repeat,
RotateLeft, RotateRight, Ruler, Safari, Scissors, Shield, Shuffle, SidebarLeft, SidebarRight, Simcard,
Snowflake, Sparkles, Speaker, SpeakerSlash, SquareOnSquare, SquareStack, Staroflife, Stop, StopCircle,
Suitcase, SunMax, Tag, Target, Terminal, TextBubble, Timer, TrashSlash, Trophy, Tv, Umbrella, Video,
VideoSlash, WalletPass, WandAndStars, Waveform, WebCamera, Wifi, WifiSlash, Wind, XmarkSeal, Zzz

（完整列表在 `D:\astra-workspace\compose-hig\hig-icons-extended\src\commonMain\kotlin\zone\ien\hig\icons\{filled,outlined}\`
下，一个文件一个图标，文件名就是图标名。）

---

## 11. 主题

### CupertinoTheme 【稳定】（函数）

```kotlin
@Composable
fun CupertinoTheme(
    colorScheme: ColorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    shapes: Shapes = Shapes(),
    typography: Typography = Typography(),
    content: @Composable () -> Unit,
)
```

- 用法要点：顺便设置 `SystemBarAppearance(colorScheme.isDark)`、`LocalTextStyle`、`LocalContentColor`、
  `LocalIndication`、`LocalHapticFeedback`。页面最外层套一次即可（星流是
  `CupertinoTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme())`）。

### CupertinoTheme 【稳定】（**object**，取当前值）

```kotlin
object CupertinoTheme {
    val colorScheme: ColorScheme     // @Composable @ReadOnlyComposable
    val typography: Typography       // @Composable @ReadOnlyComposable
    val shapes: Shapes               // @Composable @ReadOnlyComposable
}
```

> 同名一函数一 object 都在 `zone.ien.hig.theme`，靠"调用 vs 取值"区分，不会冲突。

### lightColorScheme / darkColorScheme 【稳定】

```kotlin
fun lightColorScheme(
    accent: Color = ..., label: Color = ..., secondaryLabel: Color = ..., tertiaryLabel: Color = ...,
    quaternaryLabel: Color = ..., link: Color = ..., placeholderText: Color = ..., separator: Color = ...,
    opaqueSeparator: Color = ..., systemBackground: Color = ..., secondarySystemBackground: Color = ...,
    tertiarySystemBackground: Color = ..., systemGroupedBackground: Color = ...,
    secondarySystemGroupedBackground: Color = ..., tertiarySystemGroupedBackground: Color = ...,
    systemFill: Color = ..., secondarySystemFill: Color = ..., tertiarySystemFill: Color = ...,
    quaternarySystemFill: Color = ..., lightText: Color = ..., darkText: Color = ...,
): ColorScheme

fun darkColorScheme(/* 同样 21 个参数，全部有默认值 */): ColorScheme
```

- 用法要点：全部参数都有默认值，一般 `darkColorScheme()` / `lightColorScheme()` 直接调；
  要改主题色就传 `accent = ...`（例如 `lightColorScheme(accent = CupertinoColors.systemBlue(false))`）。

### ColorScheme 【稳定】

```kotlin
@Immutable
class ColorScheme internal constructor(
    val isDark: Boolean,
    val accent: Color, val label: Color, val secondaryLabel: Color, val tertiaryLabel: Color,
    val quaternaryLabel: Color, val link: Color, val placeholderText: Color,
    val separator: Color, val opaqueSeparator: Color,
    val systemBackground: Color, val secondarySystemBackground: Color, val tertiarySystemBackground: Color,
    val systemGroupedBackground: Color, val secondarySystemGroupedBackground: Color,
    val tertiarySystemGroupedBackground: Color,
    val systemFill: Color, val secondarySystemFill: Color, val tertiarySystemFill: Color,
    val quaternarySystemFill: Color,
    val lightText: Color, val darkText: Color,
) {
    fun copy(/* 上面除 isDark 外的 21 个颜色，全部默认 = this.xxx */): ColorScheme
}
```

- 常用：`colorScheme.label` / `secondaryLabel` / `tertiaryLabel` / `accent` / `systemGroupedBackground` /
  `secondarySystemGroupedBackground` / `separator` / `quaternarySystemFill`。

### Typography 【稳定】

```kotlin
@Immutable
class Typography(
    val largeTitle: TextStyle = 34sp/41sp,
    val title1: TextStyle = 28sp/34sp,
    val title2: TextStyle = 22sp/28sp,
    val title3: TextStyle = 20sp/25sp,
    val headline: TextStyle = 17sp/22sp SemiBold,
    val body: TextStyle = 17sp/22sp,
    val callout: TextStyle = 16sp/21sp,
    val subhead: TextStyle = 15sp/20sp,
    val footnote: TextStyle = 13sp/18sp,
    val caption1: TextStyle = 12sp/16sp,
    val caption2: TextStyle = 11sp/13sp,
) {
    fun copy(/* 11 个字段，默认 = this.xxx */): Typography
}
```

### Shapes 【稳定】

```kotlin
@Stable
class Shapes(
    val extraSmall: ContinuousRoundedRectangle = ShapeDefaults.ExtraSmall,   // 4.dp
    val small: ContinuousRoundedRectangle = ShapeDefaults.Small,             // 8.dp
    val medium: ContinuousRoundedRectangle = ShapeDefaults.Medium,           // 12.dp
    val large: ContinuousRoundedRectangle = ShapeDefaults.Large,             // 16.dp
    val extraLarge: ContinuousRoundedRectangle = ShapeDefaults.ExtraLarge,   // 24.dp
) { fun copy(...): Shapes }
```

> 形状类型是 `com.kyant.capsule.ContinuousRoundedRectangle`（来自 `io.github.kyant0:capsule`，由 hig 传递依赖带进来），
> 不是 Compose 的 `RoundedCornerShape`。要传 `Shape` 的地方它都能用。

### CupertinoColors 【稳定】

```kotlin
object CupertinoColors {
    val DefaultAlpha: Color
    // 固定色
    val Black, Blue, Brown, Cyan, LightGray, DarkGray, Gray, Green, Magenta, Orange, Purple, Red, White, Yellow: Color
    // 随明暗
    fun systemRed(dark: Boolean): Color   // 同理 systemOrange … systemGray7
}
```

- 用法要点：不带参数的 `CupertinoColors.systemRed` 是固定值；要跟随主题用 `CupertinoColors.systemRed(isDark)`。
- 强调色**优先用** `CupertinoTheme.colorScheme.accent`，别硬编码 systemBlue。

---

## 12. 玻璃 / 背景工具（zone.ien.hig.utils）

```kotlin
@Composable
fun rememberDefaultBackdrop(): LayerBackdrop
// = rememberLayerBackdrop { drawRect(colorScheme.systemBackground); drawContent() }
// 用途：没给页面准备真实 backdrop 时，给顶栏/开关一个"自画底色"的兜底。

internal object CupertinoGlassDefaults   // ⚠️ internal，chizuru 用不了
// blurRadius 16.dp / refractionHeight 4.dp / refractionAmount 8.dp
// tint / panelTint / border / selection / contentColor(...) / opaqueMaterial 全是 internal
```

> **注意**：`CupertinoGlassDefaults` 在源码里是 `internal object`，跨模块**不可见**。
> 任务清单里写的 "zone.ien.hig.utils.CupertinoGlassDefaults" 实际不可用；玻璃底色只能用公开的
> `CupertinoSwitchDefaults` / `CupertinoNavigationBarDefaults` / `CupertinoDialogsDefaults` 或自己按主题色配。

辅助（【稳定】）：

```kotlin
object Accessibility                                    // zone.ien.hig
val Accessibility.isReduceTransparencyEnabled: Boolean   // 系统"减少透明度"
val Accessibility.isHighContrastEnabled: Boolean         // 系统"高对比度"
val Color.accessible: Color                              // @Composable，按当前明暗做对比度修正
```

---

## 13. kyant backdrop 2.0.1（io.github.kyant0:backdrop）

### 核心接口

```kotlin
package com.kyant.backdrop

interface Backdrop {
    val isCoordinatesDependent: Boolean
    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    )
}
```

### rememberLayerBackdrop / LayerBackdrop

```kotlin
package com.kyant.backdrop.backdrops

@Composable
fun rememberLayerBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    onDraw: ContentDrawScope.() -> Unit = { drawContent() },
): LayerBackdrop

@Stable
class LayerBackdrop internal constructor(
    val graphicsLayer: GraphicsLayer,
    internal val onDraw: ContentDrawScope.() -> Unit,
) : Backdrop {
    override val isCoordinatesDependent: Boolean = true
}

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier
```

- 用法要点：**在要被模糊的容器上打 `.layerBackdrop(backdrop)`**，容器内容会被录进 layer；
  然后把同一个 `backdrop` 传给 `CupertinoTopAppBar` / `CupertinoNavigationBar` / `CupertinoLiquidButton` 等。
  漏掉 `.layerBackdrop(...)` 的表现是玻璃里没有内容（只有底色）。

### 画玻璃的 modifier

```kotlin
package com.kyant.backdrop

fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: (() -> Highlight?)? = { Highlight.Default },      // 传 null 关掉
    shadow: (() -> Shadow?)? = { Shadow.Default },               // 传 null 关掉
    innerShadow: (() -> InnerShadow?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = { it() },
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
): Modifier

fun Modifier.drawPlainBackdrop(          // 不带 highlight / shadow / innerShadow
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = { it() },
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
): Modifier
```

- 用法要点：`shape` 是 **lambda**（`{ ContinuousCapsule() }`），不是 Shape 值；
  `onDrawSurface` 用来在上面铺一层 tint（`onDrawSurface = { drawRect(tint) }`）。

### 效果（BackdropEffectScope 的扩展）

```kotlin
package com.kyant.backdrop.effects

fun BackdropEffectScope.blur(radius: Float, edgeTreatment: TileMode = TileMode.Clamp)
fun BackdropEffectScope.lens(refractionHeight: Float, refractionAmount: Float,
                             depthEffect: Boolean = false, chromaticAberration: Boolean = false)
fun BackdropEffectScope.vibrancy()
fun BackdropEffectScope.colorFilter(colorFilter: ColorFilter)
fun BackdropEffectScope.opacity(alpha: Float)
fun BackdropEffectScope.colorControls(brightness: Float = 0f, contrast: Float = 1f, saturation: Float = 1f)
fun BackdropEffectScope.effect(effect: RenderEffect)
fun BackdropEffectScope.runtimeShaderEffect(...)
```

- 常用组合（上游惯用）：`{ vibrancy(); blur(24.dp.toPx()); lens(12.dp.toPx(), 22.dp.toPx(), depthEffect = true) }`。
- `lens` 只支持圆角矩形类 shape，其它 shape 会抛 `UnsupportedOperationException`。

### Highlight / Shadow / InnerShadow

```kotlin
package com.kyant.backdrop.highlight

@Immutable
data class Highlight(
    val width: Dp = 0.5.dp,
    val blurRadius: Dp = width / 2f,
    val alpha: Float = 1f,                 // 0..1
    val style: HighlightStyle = HighlightStyle.Default,
) { companion object { val Default: Highlight; val Ambient: Highlight; val Plain: Highlight } }

interface HighlightStyle {
    val color: Color; val blendMode: BlendMode
    fun DrawScope.createShader(shape: Shape, runtimeShaderCache: RuntimeShaderCache): RuntimeShader?
    data class Plain(color = Color.White.copy(alpha = 0.38f), blendMode = BlendMode.Plus)
    data class Default(color = Color.White.copy(alpha = 0.5f), blendMode = BlendMode.Plus,
                       angle: Float = 45f, falloff: Float = 1f)
    data class Ambient(intensity: Float = 0.38f)
    companion object { val Default; val Ambient; val Plain }
}

package com.kyant.backdrop.shadow

@Immutable data class Shadow(radius: Dp = 24.dp, offset: DpOffset = DpOffset(0.dp, radius / 6f),
    color: Color = Color.Black.copy(alpha = 0.1f), alpha: Float = 1f,
    blendMode: BlendMode = DrawScope.DefaultBlendMode) { companion object { val Default } }

@Immutable data class InnerShadow(radius: Dp = 24.dp, offset: DpOffset = DpOffset(0.dp, radius),
    color: Color = Color.Black.copy(alpha = 0.15f), alpha: Float = 1f,
    blendMode: BlendMode = DrawScope.DefaultBlendMode) { companion object { val Default } }

fun lerp(start: InnerShadow, stop: InnerShadow, fraction: Float): InnerShadow
```

### 其它 backdrop 工厂

```kotlin
package com.kyant.backdrop.backdrops

fun emptyBackdrop(): Backdrop

@Composable fun rememberBackdrop(backdrop: Backdrop, onDraw: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit): Backdrop
@Composable fun rememberCanvasBackdrop(onDraw: DrawScope.() -> Unit): Backdrop
@Composable fun rememberCombinedBackdrop(backdrop1: Backdrop, backdrop2: Backdrop): Backdrop
@Composable fun rememberCombinedBackdrop(backdrop1: Backdrop, backdrop2: Backdrop, backdrop3: Backdrop): Backdrop
@Composable fun rememberCombinedBackdrop(vararg backdrops: Backdrop): Backdrop
```

`BackdropEffectScope` 本身（效果里能读的东西）：

```kotlin
package com.kyant.backdrop

sealed interface BackdropEffectScope : Density, RuntimeShaderCache {
    val size: Size
    val layoutDirection: LayoutDirection
    val shape: Shape
    var padding: Float
    var renderEffect: RenderEffect?
}
```

---

## 14. 最小可编译骨架（对照 SettingsScreen.kt 的真实用法）

```kotlin
@file:OptIn(ExperimentalCupertinoApi::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import zone.ien.hig.CupertinoAlertDialog
import zone.ien.hig.CupertinoNavigationTitle
import zone.ien.hig.CupertinoScaffold
import zone.ien.hig.CupertinoText
import zone.ien.hig.CupertinoTopAppBar
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.cancel
import zone.ien.hig.default
import zone.ien.hig.section.LazySectionScope
import zone.ien.hig.section.SectionItem
import zone.ien.hig.section.SectionLink
import zone.ien.hig.section.SectionScope
import zone.ien.hig.section.section
import zone.ien.hig.section.switch
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.darkColorScheme
import zone.ien.hig.theme.lightColorScheme

private object Rows : SectionScope

@Composable
fun Page() {
    CupertinoTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        val backdrop = rememberLayerBackdrop()
        var showReset by remember { mutableStateOf(false) }
        var access by remember { mutableStateOf(false) }

        CupertinoScaffold(
            modifier = Modifier.fillMaxSize(),
            hasNavigationTitle = true,
            topBar = {
                CupertinoTopAppBar(
                    title = { CupertinoText("标题", maxLines = 1) },
                    isBackgroundAdaptive = false,     // 别在主线程截图算亮度
                    backdrop = backdrop,
                )
            },
        ) { pv ->
            LazyColumn(
                modifier = Modifier
                    .layerBackdrop(backdrop)          // 没有这行，顶栏玻璃里看不到内容
                    .fillMaxSize()
                    .background(CupertinoTheme.colorScheme.systemGroupedBackground),
                contentPadding = PaddingValues(
                    top = pv.calculateTopPadding(),
                    bottom = pv.calculateBottomPadding() + 24.dp,
                ),
            ) {
                item(key = "title") { CupertinoNavigationTitle { CupertinoText("标题") } }

                section(title = { CupertinoText("运行状态") }) {
                    item(key = "enabled") {
                        with(Rows) {
                            SectionItem(
                                trailingContent = { CupertinoText("已启用") },
                                title = { CupertinoText("在 LSPosed 中启用") },
                            )
                        }
                    }
                    switch(
                        checked = access,
                        onCheckedChange = { access = it },
                        key = "access",
                        title = { CupertinoText("接入系统流体云") },
                    )
                    item(key = "reset") {
                        with(Rows) {
                            SectionLink(onClick = { showReset = true }, chevron = {}) {
                                CupertinoText("恢复默认设置")
                            }
                        }
                    }
                }
            }
        }

        if (showReset) {
            CupertinoAlertDialog(
                onDismissRequest = { showReset = false },
                title = { CupertinoText("恢复默认设置") },
                message = { CupertinoText("会清空所有设置。") },
            ) {
                cancel({ showReset = false }) { CupertinoText("取消") }
                default({ showReset = false }) { CupertinoText("恢复") }
            }
        }
    }
}
```

---

## 15. 最容易出错的几处（写代码前先看）

1. **分组行要先借一个 `SectionScope`**。`SectionItem` / `SectionLink` 是 `SectionScope` 的扩展，
   在 `section { item { ... } }` 里没有 receiver，必须 `with(Rows) { SectionItem(...) }`
   （`private object Rows : SectionScope`）。而 `switch` / `link` 是 `LazySectionScope` 的扩展，
   直接在 `section {}` 里写，**两者的 receiver 不是同一个**。
2. **`selectedTabIndex` 是 lambda 不是 Int**，且 `backdrop` 没有默认值——
   `CupertinoNavigationBar(backdrop = backdrop, selectedTabIndex = { index }, onTabSelected = {...}, tabsCount = 3)`。
   另外只有传了 `prominentTabIndex` 的重载才会追踪选中态，那时 item 必须用带 `index:` 的重载。
3. **玻璃链是三步**：`rememberLayerBackdrop()` → 滚动容器 `.layerBackdrop(backdrop)` → 顶栏/导航栏/玻璃按钮
   传 `backdrop = backdrop`。少任何一步都不报错，只是玻璃里没有内容。
   而且 `CupertinoGlassDefaults` 是 `internal`，跨模块拿不到，别照抄上游源码里的用法。
4. **`ExperimentalCupertinoApi` 几乎覆盖全部部件**（Scaffold / TopAppBar / NavigationBar / Button / Sheet /
   AlertDialog / SearchTextField / section / CupertinoSection / 大部分 LazySectionScope 扩展），
   文件级 `@file:OptIn(ExperimentalCupertinoApi::class)` 一次解决。
5. **`CupertinoTopAppBar` 的 `title` 是第一个位置参数（必填）**；`CupertinoScaffold` 的 `content` 收到的是
   `PaddingValues`；`CupertinoNavigationTitle` 必须是列表第一个元素且 Scaffold 传了 `hasNavigationTitle = true`。
6. **图标要逐个 import**（`import zone.ien.hig.icons.outlined.ChevronForward`），
   `CupertinoIcon` 的 `contentDescription` 没有默认值，必须显式传。
7. **`CupertinoSwitch` 单独用时 `backdrop` 有默认值**（`rememberDefaultBackdrop()`，自画底色），
   要真玻璃就显式传页面那个 backdrop。
8. `section` / `switch` / `link` / `default` / `cancel` 都是**顶层扩展函数，要单独 import**，
   漏 import 时报的是 "unresolved reference" 而不是参数错误，容易误判成签名写错。

## 16. 未确认（别当结论）

- `CupertinoBottomSheetScaffold` 的 `sheetContent` 是否**必须**包一层 `CupertinoBottomSheetContent`：
  源码没有强制，上游 KDoc 只说"sheet 的内容"，也没找到可运行的示例（`compose-hig/example` 里没有源码）。
  按 API 推断：想让 sheet 内部也有顶栏/正确的窗口 inset 就包；不包应该也能显示，但 inset 要自己处理——**未验证**。
- `PresentationStyle.Modal(isBackgroundInteractive = ...)` 与 `dismissOnClickOutside` 的真机手感**未验证**。
- `CupertinoSearchTextField` 折叠依靠 `Modifier.nestedScroll(state.nestedScrollConnection)`，
  这是从源码声明推的用法，**未在 chizuru 工程里编译验证**。
- `CupertinoActionSheet` 的 `content` 槽与 `buttons` 的叠加排布**未验证**。
- "常用 extended 图标" 是按文件名校对的名字（都对得上文件），但没有逐个编译点过。
