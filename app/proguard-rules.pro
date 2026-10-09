# 谜天 Mitian - R8 rules
-keep class com.astraflow.MysticSky.xposed.** { *; }
-keep class io.github.libxposed.api.** { *; }

# lyricon provider library: keep entry points used reflectively
-keep class io.github.proify.lyricon.provider.** { *; }
-keep class io.github.proify.lyricon.lyric.model.** { *; }
-dontwarn io.github.proify.**
-dontwarn org.jetbrains.annotations.**

# 星河岛接入库（com.astraisland.sdk）：里面的 PendingIntent / Binder 代理
# 与宿主按协议互通，名字不能被压缩
-keep class com.astraisland.sdk.** { *; }
-keep class com.astraisland.protocol.** { *; }
-dontwarn com.astraisland.**
