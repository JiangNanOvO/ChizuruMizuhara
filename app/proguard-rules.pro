# 谜天 Mitian - R8 rules
-keep class com.astraflow.Chizuru.xposed.** { *; }
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

# ── LSPosed 模块运行必需（框架按类名加载入口，名字不能被改）──
-keep class com.astraflow.Chizuru.xposed.** { *; }
-keep class com.astraflow.Chizuru.app.** { *; }
-keepclassmembers class * extends io.github.libxposed.api.XposedModule { public *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# AIDL / Binder / Parcelable 协议类（星河岛 SDK、内容提供者）
-keep class * implements android.os.Parcelable { *; }
-keep class * implements android.os.IInterface { *; }
-dontwarn android.os.**
