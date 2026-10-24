package com.astraflow.Chizuru.ui.page

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * 插件的界面:从桌面图标、LSPosed 的模块页或星流的星河岛页打开。
 * 内容是 Compose 页面(见 [ChizuruApp]),四个标签页:首页、链接、歌词、关于。
 */
open class ChizuruShellActivity : ComponentActivity() {

    /** 打开时落在哪一个标签页。 */
    protected open val initialTab: Int get() = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val tab = intent?.getIntExtra(EXTRA_TAB, initialTab) ?: initialTab
        setContent { ChizuruApp(tab) }
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_HOME = 0
        const val TAB_LINK = 1
        const val TAB_LYRIC = 2
        const val TAB_ABOUT = 3
    }
}
