import java.util.Properties

plugins {
    id("com.android.application")
}

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

val compileSdkVersion: Int =
    (project.findProperty("mysticsky.compileSdk") as String?)?.toIntOrNull() ?: 36

android {
    namespace = "com.astraflow.Chizuru"
    compileSdk = compileSdkVersion

    defaultConfig {
        applicationId = "com.astraflow.Chizuru"
        minSdk = 27
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

    testImplementation("junit:junit:4.13.2")
}
