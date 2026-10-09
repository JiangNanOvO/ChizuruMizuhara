package com.astraflow.Chizuru.capability.link

object LinkPatterns {

    private const val COMMON_TOP_LEVEL_DOMAINS =
        "com|cn|net|org|edu|gov|io|ai|co|info|biz|me|tv|cc|app|dev|tech|site|online|" +
            "xyz|top|vip|shop|store|club|cloud|pro|mobi|asia|uk|jp|de|fr|ru|au|ca|" +
            "us|hk|tw|sg|kr|in|br|it|nl|es"

    private val PATTERN_EMAIL = Regex(
        pattern = """(?i)(?<![a-z0-9._%+\-])[a-z0-9](?:[a-z0-9._%+\-]{0,62}[a-z0-9])?@(?:[a-z0-9](?:[a-z0-9\-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}(?![a-z0-9_\-])""",
    )

    private val PATTERN_URL = Regex(
        pattern = """(?i)(?<![a-z0-9_])https?://[^\s<>"“”‘’（）【】《》。，；：！？、]+""",
    )

    private val PATTERN_DOMAIN = Regex(
        pattern = """(?i)(?<![@/:a-z0-9_\-])(?:[a-z0-9](?:[a-z0-9\-]{0,61}[a-z0-9])?\.)+(?:$COMMON_TOP_LEVEL_DOMAINS)(?::[0-9]{1,5})?(?:[/?#][^\s<>"“”‘’（）【】《》。，；：！？、]*)?(?![a-z0-9_\-]|\.[a-z0-9])""",
    )

    private val FILE_LIKE = Regex(
        """\.(?:kt|kts|java|xml|json|md|txt|log|apk|zip|jar|so|dex|pro|gradle|properties|ts|js|tsx|jsx|py|c|cpp|h|rs|go|rb|css|html|htm|yml|yaml|conf|cfg|ini|sh|bat|png|jpg|jpeg|gif|webp|mp3|mp4|flac|aac|lrc|pdf|doc|docx|xls|xlsx|ppt|pptx)$""",
        RegexOption.IGNORE_CASE
    )

    private const val SCAN_LIMIT = 4096

    fun firstLink(text: CharSequence?): String? {
        if (text.isNullOrBlank()) return null
        val sample = text.trim().let {
            if (it.length > SCAN_LIMIT) it.substring(0, SCAN_LIMIT) else it
        }

        PATTERN_URL.find(sample)?.let { m ->
            return polish(m.value, hadScheme = true)
        }

        PATTERN_DOMAIN.find(sample)?.let { m ->
            val raw = m.value
            if (looksLikeFile(raw)) return null
            return polish(raw, hadScheme = false)
        }
        return null
    }

    fun hasLink(text: CharSequence?): Boolean = firstLink(text) != null

    fun hostOf(url: String): String = runCatching {
        java.net.URI(url).host?.removePrefix("www.") ?: url
    }.getOrDefault(url)

    @Suppress("unused")
    fun hasEmail(text: CharSequence?): Boolean =
        !text.isNullOrBlank() && PATTERN_EMAIL.containsMatchIn(text)

    private fun polish(raw: String, hadScheme: Boolean): String {
        var url = raw.trim()
        if (!hadScheme) {

            
            val firstCn = url.indexOfFirst { it.code in 0x4E00..0x9FFF }
            val slash = url.indexOf('/')
            val pathStart = if (slash < 0) url.length else slash
            if (firstCn > pathStart) url = url.substring(0, firstCn)
        }
        url = url.trimEnd(
            '.', ',', ';', ':', '!', '?',
            ')', ']', '}', '>',
            '。', '，', '；', '：', '！', '？', '、',
            '"', '\'', '）', '】', '》', '｜', '|'
        )
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
            url = "https://$url"
        }
        return url
    }

    private fun looksLikeFile(raw: String): Boolean =
        FILE_LIKE.containsMatchIn(raw) &&
            !raw.contains('/') && !raw.contains('?') && !raw.contains('#')
}
