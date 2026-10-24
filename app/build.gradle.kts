import java.util.Properties

plugins {
    // AGP 9 自带 Kotlin 支持,只加 Compose 编译器插件
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 星流插件 Chizuru:LyricON 歌词提供者 + 链接/手机号/地址上岛 + 可选 root 独立供词。
// 界面部分原样用星流那一套上游部件(compose-hig、AndroidLiquidGlass),不再自己画 Cupertino 样子。

val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(key: String): String? =
    (localProps.getProperty(key) ?: System.getenv(key))?.trim()?.takeIf { it.isNotEmpty() }

val customStoreFile = secret("mitian.storeFile")

android {
    namespace = "com.astraflow.Chizuru"
    compileSdk = (project.findProperty("mysticsky.compileSdk") as String?)?.toIntOrNull() ?: 37

    defaultConfig {
        applicationId = "com.astraflow.JiangNan.Chizuru"
        // 界面部件上游 compose-hig 要求的最低版本;星河岛本身也只在安卓 15 以上跑
        minSdk = 29
        targetSdk = 36
        versionCode = versionProps.getProperty("versionCode").trim().toInt()
        versionName = versionProps.getProperty("versionName").trim()
    }

    signingConfigs {
        create("release") {
            if (customStoreFile != null) {
                storeFile = file(customStoreFile)
                storePassword = secret("mitian.storePassword")
                keyAlias = secret("mitian.keyAlias")
                keyPassword = secret("mitian.keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            } else {
                println("[Chizuru] 未配置私有签名：release 产物将是未签名的。")
                println("[Chizuru] 在 local.properties 里写 mitian.storeFile / mitian.storePassword / mitian.keyAlias / mitian.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 开启 R8 死代码消除 + 资源压缩（规则见 app/proguard-rules.pro）
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (customStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java", "src/main/kotlin")
        }
    }

    packaging {
        resources {
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/versions/**"
            excludes += "META-INF/*.version"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {

    compileOnly("io.github.libxposed:api:102.0.0")

    implementation("io.github.proify.lyricon:provider:0.1.70")
    implementation("io.github.proify.lyricon.lyric:model:0.1.70")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation(files("libs/astraisland-sdk-0.1.0.aar"))

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")

    // 界面全部原样用上游 compose-hig(页面骨架、顶部栏、分组列表、开关、弹层、确认框),玻璃用 backdrop
    implementation("zone.ien.hig:hig:27.0.1-alpha05")
    implementation("zone.ien.hig:hig-icons-extended:27.0.1-alpha05")
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")
    implementation("zone.ien.capsule:capsule:2.1.4")

    testImplementation("junit:junit:4.13.2")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}