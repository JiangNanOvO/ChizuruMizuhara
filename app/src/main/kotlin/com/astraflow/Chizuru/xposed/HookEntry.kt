package com.astraflow.Chizuru.xposed

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import com.astraflow.Chizuru.Module
import com.astraflow.Chizuru.capability.link.LinkHook
import com.astraflow.Chizuru.capability.neriplayer.NeriPlayerHook
import com.astraflow.Chizuru.lyric.Constants
import com.astraflow.Chizuru.lyric.HookCrashLog
import com.astraflow.Chizuru.lyric.LocalLyricProvider
import com.astraflow.Chizuru.lyric.ModuleLogger
import com.astraflow.Chizuru.lyric.MitianLyricProvider
import com.astraflow.Chizuru.settings.ModulePrefs
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

class HookEntry : XposedModule() {

    private val logger = ModuleLogger(this)

    private val lyricInstalled = lyricInstalledGate

    private val linkInstalled = linkInstalledGate

    @Volatile
    private var isSystemServer = false

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        isSystemServer = param.isSystemServer
        if (isSystemServer) {
            logSys("Mitian loaded in system_server")
            return
        }
        if (!loadedLoggedGate.compareAndSet(false, true)) return
        logger.info(
            "Mitian loaded: process=" + param.processName +
                ", framework=" + frameworkName + " " + frameworkVersion +
                " (" + frameworkVersionCode + "), api=" + apiVersion
        )
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        if (!linkInstalled.compareAndSet(false, true)) {
            logSys("链接助手已装过，跳过")
            return
        }
        runCatching {
            LinkHook(module = this, classLoader = param.classLoader).install()
        }.onFailure { logSys("链接助手挂载失败: ${it.message}") }
    }

    override fun onPackageReady(param: PackageReadyParam) {
        val packageName = param.packageName
        if (!param.isFirstPackage) return
        if (packageName == Module.PACKAGE_NAME) return
        if (isSystemFramework(packageName)) return

        if (!isEnabled() || !isLyricEnabled()) {
            logger.info("Module or lyric disabled in settings, skip $packageName")
            return
        }
        if (!lyricInstalled.compareAndSet(false, true)) return

        logger.info("Package ready: $packageName, process=" + currentProcessName())

        if (packageName == NeriPlayerHook.PACKAGE) {
            if (isNeriAdapt()) {
                NeriPlayerHook.install(this, param.classLoader, logger)
            } else {
                logger.info("音理音理适配未开启，跳过安装")
            }
            return
        }

        try {
            if (packageName == Constants.PLAYER_PACKAGE_NAME) {
                MitianLyricProvider(this, logger, param.classLoader).installHooks()
                return
            }
            LocalLyricProvider(
                module = this,
                logger = logger,
                classLoader = param.classLoader,
                hostPackage = packageName,
                processName = currentProcessName(),
                recipe = Constants.recipeOf(packageName)
            ).installHooks()
        } catch (throwable: Throwable) {
            logger.error("Failed to install hooks for $packageName", throwable)
            HookCrashLog.record("installHooks:$packageName", throwable)
        }
    }

    private fun isEnabled(): Boolean = remoteBool(ModulePrefs.KEY_ENABLED, true)

    private fun isLyricEnabled(): Boolean = remoteBool(ModulePrefs.KEY_LYRIC, true)

    private fun isNeriAdapt(): Boolean = remoteBool(ModulePrefs.KEY_NERI_ADAPT, false)

    private fun remoteBool(key: String, def: Boolean): Boolean = runCatching {
        getRemotePreferences(ModulePrefs.NAME).getBoolean(key, def)
    }.getOrDefault(def)

    private fun isSystemFramework(packageName: String): Boolean =
        packageName == "android" || packageName == "system" || packageName == "system_server"

    private fun currentProcessName(): String = runCatching {
        java.io.File("/proc/self/cmdline").readText().trimEnd('\u0000')
    }.getOrDefault("")

    private fun logSys(msg: String) {
        runCatching { log(Log.INFO, "Mitian", msg) }
    }

    private companion object {
        val loadedLoggedGate = AtomicBoolean(false)
        val lyricInstalledGate = AtomicBoolean(false)
        val linkInstalledGate = AtomicBoolean(false)
    }
}
