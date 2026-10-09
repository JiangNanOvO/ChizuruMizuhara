# 安全说明 · 谜天

这份文档回答三个问题：**它要什么权限**、**它把数据发到哪去**、**你怎么自己验证**。

结论先放前面：**谜天没有联网权限，也没有任何一行发起网络请求的代码。**
它拿不到的网络，就不可能上传。

---

## 一、权限：只有一条，而且是星流 SDK 要求的

```
com.astraflow.tool.island.permission.PUBLISH_ACTIVITY   星流 SDK 用来向星河岛投送卡片
com.astraflow.MysticSky.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION   构建工具自动加的（仅本应用内广播）
```

**没有 `INTERNET`**。这一点可以直接验证：

```bash
aapt2 dump badging app-release.apk | grep uses-permission
```

没有 `INTERNET` 意味着：即使代码里藏了上传逻辑，调用时也会被系统直接拒绝
（`SecurityException`），而不是"偷偷发出去"。同时也没有
`READ_SMS`、`READ_CONTACTS`、`ACCESS_FINE_LOCATION`、`CAMERA`、`RECORD_AUDIO`、
`READ_PHONE_STATE`、`QUERY_ALL_PACKAGES` 等等任何敏感权限。

## 二、数据流向

| 数据 | 从哪来 | 去哪 | 离开设备？ |
| --- | --- | --- | --- |
| 剪贴板文字 | system_server 的 `ClipboardService` | 只在**本机**判断有没有链接，有则通过 Binder 交给谜天自己的进程 | ❌ 不出设备 |
| 链接 URL | 上一步 | 编成 `PendingIntent` 交给星河岛（系统进程），点卡片时由**浏览器**打开 | ❌ 打开动作走浏览器 |
| 歌词文本 | 播放器自己的内存/缓存 | 按 LyricON 协议交给星流显示 | ❌ 不出设备 |
| 设置项 | 你自己的开关 | 存本地 `SharedPreferences` | ❌ |

- **打开网页**用的是浏览器的网络，不是谜天 —— 所以谜天不需要 `INTERNET` 也能跳转。
- 剪贴板**只在设备上做正则匹配**，原文不会写入任何日志或文件。
- 日志（`/data/local/tmp/mitian_crash.log`，便于你反馈问题时排查）只记录**钩子安装失败**之类的
  技术信息，**不记录剪贴板原文，也不记录链接内容**。

## 三、代码层面查过什么

对 `app/src/main/kotlin` 全量扫描，以下调用**一个都没有**：

- 发起网络请求：`HttpURLConnection` / `OkHttpClient` / `Socket` / `InetAddress` / `WebView.loadUrl`
- 执行外部命令：`Runtime.getRuntime().exec` / `ProcessBuilder` / `su -c`
- 动态加载代码：`DexClassLoader` / `PathClassLoader` / `InMemoryDexClassLoader`
- 隐私相关：相机 / 麦克风 / 定位 / 短信 / 通讯录 / 设备号

代码里能看到的 `Class.forName(...)` 全部是 **Xposed 模块的正常形态** —— 用来定位
*宿主应用*（播放器）的类，例如 `okhttp3.OkHttpClient`、`com.facebook.react.bridge.PromiseImpl`、
`com.android.server.clipboard.ClipboardService`。这是"挂钩子"必须要做的事，
挂上去只读、不改写、不拦截。

## 四、依赖库审计

| 库 | 版本 | 审计结果 |
| --- | --- | --- |
| `astraisland-sdk`（星流官方接入库） | 0.1.0 | 包内 **无 URL**、无网络 API、无命令执行、无动态加载；只有 Binder 调用 |
| `io.github.proify.lyricon:provider` / `lyric:model` | 0.1.70 | 同上，**无 URL**、无网络 API |
| `kotlinx-serialization` / `kotlinx-coroutines` | 1.11.0 / 1.9.x | JetBrains 官方库，无网络代码 |
| `androidx.core` / `profileinstaller` / `startup-runtime` | 1.17.0 等 | Google 官方库，由 LyricON 间接引入。其中 `androidx.core` 含 `Socket`/`getImei` 等**兼容包装类**（AndroidX 的 API 表面），谜天没有调用它们 |

> 第三方 SDK 是编译好的库，能查的是包内字符串、符号与 API 表面。
> 上面两个关键库在这方面是干净的；若你更保守，可以只启用歌词能力而不勾「系统框架」作用域。

## 五、进程边界

谜天会运行在两类进程里，各自只做该做的事：

- **谜天自己的进程**：只负责界面、设置、以及把卡片投给星河岛。
- **被勾选作用域的宿主进程**（播放器 / `system_server`）：
  - 播放器里：只读地读歌词缓存、只读地嗅探它刚收到的网络响应体；
  - `system_server` 里：只读地看剪贴板有没有链接，**不改写剪贴板、不拦任何系统行为**。

## 六、已加固的地方

- **链接投递入口（`LinkInboxProvider`）加了调用方校验**：只接受 `system`（uid 1000）
  和谜天自己。否则任何第三方 App 都可以调用这个导出的 Provider，
  让谜天弹出一张**长得像官方**、实际指向钓鱼网站的卡片。
- **不记录用户链接**：投递日志只写「收到一条链接投递」，不写 URL 本身。
- 导出组件只有：启动器入口（`LauncherAlias`/`HomeActivity`）、
  LSPosed 的「模块信息」页（`SettingsActivity`）、以及上面那个带调用方校验的 Provider。

## 七、你可以自己复核

```bash
# 1. 看权限（应该只有星流那一条）
aapt2 dump badging app-release.apk | grep uses-permission

# 2. 看有没有 native 库（应该一个都没有）
unzip -l app-release.apk | grep '\.so$'

# 3. 看包里所有 URL（应该只有 astraflow.cc / github.com / example.com）
unzip -p app-release.apk classes.dex | strings | grep -E 'https?://[a-z0-9]'

# 4. 看导出组件
aapt2 dump xmltree --file AndroidManifest.xml app-release.apk | grep -A2 'E: provider'
```

## 八、许可与来源

谜天是**第三方接入应用**，与星流官方没有隶属关系。
歌词部分移植自开源项目星河 AstraGalaxy（MIT），版权声明已保留。
谜天自己的代码是 MIT；星流接入库按 PolyForm Noncommercial 1.0.0 只能非商业使用，
详见 [NOTICE.md](NOTICE.md)。
