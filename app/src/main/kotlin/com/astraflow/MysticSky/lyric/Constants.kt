package com.astraflow.MysticSky.lyric

object Constants {

    const val PROVIDER_PACKAGE_NAME: String = "com.astraflow.MysticSky"

    const val VERBOSE_BRIDGE: Boolean = false

    const val LX_PACKAGE: String = "cn.toside.music.mobile"

    const val LUNA_PACKAGE: String = "com.luna.music"

    const val KUWO_PACKAGE: String = "cn.kuwo.player"

    const val QQ_PACKAGE: String = "com.tencent.qqmusic"

    val ARTIST_STRUCTURE_PACKAGES: Set<String> = setOf(
        "com.kugou.android",
        "com.kugou.android.lite",
        QQ_PACKAGE
    )

    val BLUETOOTH_BOOST_PACKAGES: Set<String> = setOf(KUWO_PACKAGE, LUNA_PACKAGE)

    const val PREFS_NAME: String = "mitian_settings"
    const val KEY_ENABLED: String = "module_enabled"
    const val KEY_HIDE_ICON: String = "hide_launcher_icon"

    const val PLAYER_PACKAGE_NAME: String = "com.music"
    const val SONG_LYRIC_FILE: String = "songLyric.json"
    const val NOW_PLAYING_FILE: String = "nowPlaying.json"

    internal val LOCAL_RECIPES: List<LocalRecipe> = listOf(
        LocalRecipe(
            packageName = "com.kugou.android",
            displayName = "酷狗音乐",
            sources = kugouSources()
        ),
        LocalRecipe(
            packageName = "com.kugou.android.lite",
            displayName = "酷狗概念版",
            sources = kugouSources()
        ),
        LocalRecipe(
            packageName = "cn.wenyu.bodian",
            displayName = "波点音乐",
            sources = listOf(
                LocalSource(BaseDir.CACHE, "lyric", LyricFormat.LRCX, listOf("lrcx", "lrc")),
                LocalSource(BaseDir.EXTERNAL_CACHE, "lyric", LyricFormat.LRCX, listOf("lrcx", "lrc"))
            )
        ),
        LocalRecipe(
            packageName = "com.tencent.qqmusic",
            displayName = "QQ 音乐",
            sources = listOf(
                LocalSource(BaseDir.EXTERNAL_FILES, "qqmusic/qrc", LyricFormat.QRC, listOf("qrc")),
                LocalSource(BaseDir.FILES, "qqmusic/qrc", LyricFormat.QRC, listOf("qrc"))
            )
        ),

        

        
        LocalRecipe(
            packageName = KUWO_PACKAGE,
            displayName = "酷我音乐",
            sources = listOf(
                LocalSource(BaseDir.FILES, "lyric", LyricFormat.KRC, listOf("krc", "lrc")),
                LocalSource(BaseDir.FILES, "lyrics", LyricFormat.KRC, listOf("krc", "lrc")),
                LocalSource(BaseDir.CACHE, "lyric", LyricFormat.KRC, listOf("krc", "lrc")),
                LocalSource(BaseDir.CACHE, "lyrics", LyricFormat.KRC, listOf("krc", "lrc")),
                LocalSource(BaseDir.FILES, "krc", LyricFormat.KRC, listOf("krc", "lrc")),
                LocalSource(BaseDir.EXTERNAL_FILES, "kwmusic/lyric", LyricFormat.LRC, listOf("lrc", "krc")),
                LocalSource(BaseDir.EXTERNAL_FILES, "kwmusic/krc", LyricFormat.KRC, listOf("krc", "lrc"))
            )
        ),
        LocalRecipe(
            packageName = "com.netease.cloudmusic",
            displayName = "网易云音乐",
            sources = listOf(
                LocalSource(BaseDir.EXTERNAL_FILES, "LrcCache", LyricFormat.NETEASE),
                LocalSource(BaseDir.FILES, "LrcCache", LyricFormat.NETEASE)
            )
        ),
        LocalRecipe(
            packageName = "com.heytap.music",
            displayName = "OPPO 音乐",
            sources = listOf(
                LocalSource(BaseDir.FILES, "lyric", LyricFormat.LRC, listOf("alm3ll", "lrc")),
                LocalSource(BaseDir.EXTERNAL_FILES, "lyric", LyricFormat.LRC, listOf("alm3ll", "lrc")),
                LocalSource(BaseDir.CACHE, "lyric", LyricFormat.LRC, listOf("alm3ll", "lrc"))
            )
        ),

        
        LocalRecipe(
            packageName = LUNA_PACKAGE,
            displayName = "汽水音乐",
            sources = emptyList()
        )
    )

    private fun kugouSources(): List<LocalSource> = listOf(
        LocalSource(BaseDir.EXTERNAL_FILES, "kugou/lyrics", LyricFormat.KRC, listOf("krc")),
        LocalSource(BaseDir.EXTERNAL_FILES, "kugou/temp_lyrics", LyricFormat.KRC, listOf("krc")),
        LocalSource(BaseDir.FILES, "kugou/lyrics", LyricFormat.KRC, listOf("krc")),
        LocalSource(BaseDir.FILES, "kugou/temp_lyrics", LyricFormat.KRC, listOf("krc"))
    )

    val LOCAL_PLAYER_PACKAGES: Set<String> = LOCAL_RECIPES.map { it.packageName }.toSet()

    fun recipeOf(packageName: String): LocalRecipe? =
        LOCAL_RECIPES.firstOrNull { it.packageName == packageName }

    const val ICON: String =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 108 108\">" +
            "<rect width=\"108\" height=\"108\" rx=\"24\" fill=\"#0B1026\"/>" +
            "<path d=\"M20 62 L34 52 L42 60 L88 34 L74 68 L62 66 L50 82 Z\" fill=\"#7CE7FF\"/>" +
            "<circle cx=\"46\" cy=\"38\" r=\"4\" fill=\"#FFE9A8\"/>" +
            "<circle cx=\"30\" cy=\"74\" r=\"2.6\" fill=\"#9C8CFF\"/>" +
            "<circle cx=\"84\" cy=\"70\" r=\"2.2\" fill=\"#7CF5B0\"/>" +
            "<circle cx=\"66\" cy=\"26\" r=\"1.8\" fill=\"#FFFFFF\"/>" +
            "</svg>"
}
