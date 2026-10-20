# 安全说明

这份文档回答三个问题 **它要什么权限**、**它把数据发哪**、**验证**。

结论 **谜天没有联网权限 也没有任何一行发起网络请求的代码**
拿不到的网络 哪来上传

-----

## 一、权限是星流 SDK 要求的

```
com.astraflow.tool.island.permission.PUBLISH_ACTIVITY 星流SDK用来向星河岛投送卡片
com.astraflow.Chizuru.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION  构建工具自动加的
```

**没有 `INTERNET`**
```bash
aapt2 dump badging app-release.apk | grep uses-permission
```
没有 `INTERNET` 意味着就算是代码里藏了上传逻辑 调用也会被系统拒绝
`SecurityException`
`READ_SMS`、`READ_CONTACTS`、`ACCESS_FINE_LOCATION`、`CAMERA`、`RECORD_AUDIO`、
`READ_PHONE_STATE`、`QUERY_ALL_PACKAGES`

## 二、数据流向

数据从哪来去哪 会不会离开设备
-----
剪贴板文字 system_server的`ClipboardService`只在**本机**判断有没有链接
链接url上一步编成`PendingIntent`交给星河岛
同时手机号地址上岛也是差不多的原理
歌词播放器自己的缓存 按LyricON协议交给星流显示
设置页面你自己的开关存本地`SharedPreferences`

   剪贴板**只在设备上做匹配**原文不会写入日志
   日志`/data/local/tmp/mitian_crash.log`便于你反馈问题时排查 只记录**钩子**之类的
  **不记录原文也不记录链接**

## 三、代码层查过什么

`app/src/main/kotlin`全量扫描 以下本身一个都没有

- 发起网络请求：`HttpURLConnection` / `OkHttpClient` / `Socket` / `InetAddress` / `WebView.loadUrl`
- 动态加载代码：`DexClassLoader` / `PathClassLoader` / `InMemoryDexClassLoader`
- 隐私相关：相机 / 麦克风 / 定位 / 短信 / 通讯录 / 设备号

**执行命令root 但只有一处代码、且能不开`capability/root/RootShell` 用
`ProcessBuilder("su", "-c", …)` 执行命令 全部命令如下没有第二路径

  独立歌词提供者 `id`、`dumpsys media_session`、`cat '<歌词缓存文件>'`、`ls -t '<目录>'` 判断有无root、读当前播放状态、读播放器自己的歌词缓存
  音理音理适配 `cp '<设置文件>' '<备份>'`、`base64 -d` 写入设置文件 写入一个设置值

两条都受开关控制 不打开两个功能 一行 `su` 都不会执行 命令都带超时即销毁

代码里能看到的 `Class.forName()` 全部是 Xposed 模块的正常形态用来定位
**宿主应用** 例如 `okhttp3.OkHttpClient`、`com.facebook.react.bridge.PromiseImpl`、`com.android.server.clipboard.ClipboardService`**挂钩子**必须做的事

**唯一的非只读适配 音频输出状态(仅酷我の汽水音乐)**

`LocalLyricProvider.installAudioOutputBoost()`在`cn.kuwo.player`、`com.luna.music`
两个进程内挂两个方法 让它们返回true

类#方法 原行为 适配后 为什么
`android.media.AudioManager#isBluetoothA2dpOn()`真实蓝牙状态`true`歌词能力开启时 
这两个播放器只在**检测到正在用无线音频输出**时才报歌名 歌手 否则 MediaSession是空的 星河岛拿不到歌名 歌词胶囊对它们完全不可用`android.bluetooth.BluetoothAdapter#isEnabled()`真实蓝牙开关状态同上


**不连接、不开启、不搜索任何蓝牙设备** 不会申请任何蓝牙权限
`BLUETOOTH``BLUETOOTH_CONNECT`都没有

除上述外 模块对播放器**只读**挂的都是读返回值或参数的钩子 不改写

## 四、依赖库审计

| 库 | 版本 | 审计结果 |
| --- | --- | --- |
| `astraisland-sdk`（星流官方库） | 0.1.0 | 包内 **无 URL**、无网络 API、无命令执行、无动态加载；只有 Binder 调用 |
| `io.github.proify.lyricon:provider` / `lyric:model` | 0.1.70 | 同上，**无 URL**、无网络 API |
| `kotlinx-serialization` / `kotlinx-coroutines` | 1.11.0 / 1.9.x | JetBrains 官方库，无网络代码 |
| `androidx.core` / `profileinstaller` / `startup-runtime` | 1.17.0 等 | Google 官方库，由 LyricON 间接引入。其中 `androidx.core` 含 `Socket`/`getImei` 等**兼容包装类**（AndroidX 的 API 表面），谜天没有调用它们 |

> 第三方 SDK 是编译好的库，能查的是包内字符串、符号与 API 表面。
> 上面两个关键库在这方面是干净的；若你更保守，可以只启用歌词能力而不勾「系统框架」作用域。

## 五、进程边界

会运行在两类进程里 各自只做该做的事

**自己的进程** 只负责界面设置以及把卡片投给星河岛
**被勾选作用域的进程**播放器 / `system_server`
播放器只读歌词缓存 只读地嗅探它刚收到的响应
`system_server`只读剪贴板有没有链接 
**不改写剪贴板、系统行为**

## 六、加固的地方

**链接投递入口`LinkInboxProvider`加了调用校验**只接受system
否则任何第三方App都可以调用这个Provider
让本身弹出长得像官方 实际指向恶意网站的卡片
**用户链接**日志只写收到一条链接投递不写本身

## 七、用户自己复核

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

Chizuru是**通过星流第三方接入的开源插件** 与星流官方没有隶属关系
歌词雏形来自星河开源项目星穹AstraGalaxy MIT 版权声明已保留
Chizuru代码是MIT许可 星流接入库按PolyForm Noncommercial1.0.0只能非商业使用
详见 [NOTICE.md](NOTICE.md)。
