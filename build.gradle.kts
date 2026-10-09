buildscript {
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        mavenCentral()
    }
    dependencies {

        classpath("com.android.tools.build:gradle:9.3.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

extra["compileSdk"] = 37

extra["compileSdkVersion"] = 37
extra["targetSdkVersion"] = 36
