package com.astraflow.MysticSky.capability.neriplayer

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import com.astraflow.MysticSky.lyric.ModuleLogger

object NeriPlayerHook {

    private const val TAG = "MysticSky-Neri"

    const val PACKAGE = "moe.ouom.neriplayer"

    private const val LYRICON_MANAGER = "moe.ouom.neriplayer.lyrics.lyricon.LyriconManager"
    private const val PLAYER_MANAGER = "moe.ouom.neriplayer.core.player.PlayerManager"

    fun install(module: XposedModule, classLoader: ClassLoader, logger: ModuleLogger) {
        runCatching {
            val lyricon = Class.forName(LYRICON_MANAGER, false, classLoader)
            val playerManager = runCatching {
                Class.forName(PLAYER_MANAGER, false, classLoader)
            }.getOrNull()

            val setEnabled = lyricon.getDeclaredMethod("setEnabled", Boolean::class.javaPrimitiveType)
            module.hook(setEnabled)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val original = chain.args.getOrNull(0) as? Boolean
                    openGate(playerManager, logger)
                    ensureInitialized(lyricon, logger)
                    if (original != true) {
                        logger.info("音理音理原本是关闭的（原值=" + original + "），已强制开启")
                    }
                    chain.proceed(arrayOf<Any?>(true))
                }

            logger.info("音理音理适配已安装：自动打开它的外部歌词输出")
        }.onFailure {
            logger.warn("音理音理适配安装失败：${it.message}")
        }
    }

    private fun openGate(playerManager: Class<*>?, logger: ModuleLogger) {
        playerManager ?: return
        runCatching {
            val instance = playerManager.getField("INSTANCE").get(null)
            val field = playerManager.getDeclaredField("lyriconEnabled")
            field.isAccessible = true
            if (!field.getBoolean(instance)) {
                field.setBoolean(instance, true)
                logger.info("已打开音理音理的门控 lyriconEnabled")
            }
        }.onFailure {
            logger.warn("门控写入失败：${it.message}")
        }
    }

    private fun ensureInitialized(lyricon: Class<*>, logger: ModuleLogger) {
        runCatching {
            val instance = lyricon.getField("INSTANCE").get(null)
            val isInitialized = lyricon.getDeclaredMethod("isInitialized")
            if (isInitialized.invoke(instance) == true) return
            val context = currentApplication() ?: return
            lyricon.getDeclaredMethod("initialize", android.content.Context::class.java)
                .invoke(instance, context)
            logger.info("已替音理音理初始化 LyricON 提供者")
        }.onFailure {
            logger.warn("初始化失败：${it.message}")
        }
    }

    private fun currentApplication(): android.app.Application? = try {
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as? android.app.Application
    } catch (t: Throwable) {
        null
    }
}
