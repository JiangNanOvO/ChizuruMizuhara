package com.astraflow.Chizuru.ui.page

/** 系统「应用信息」里的入口:直接落在「关于」标签页。 */
class AboutActivity : ChizuruShellActivity() {

    override val initialTab: Int get() = TAB_ABOUT
}
