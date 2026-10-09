package com.astraflow.MysticSky.lyric

internal object PlayerTitle {

    private val CREDIT_PREFIX = listOf(
        "作词", "作曲", "编曲", "制作", "混音", "母带", "录音", "和声", "监制", "统筹",
        "吉他", "贝斯", "键盘", "弦乐", "出品", "发行", "策划", "鸣谢", "词曲",
        "词：", "曲：", "词:", "曲:", "OP", "SP", "本作品", "未经许可", "未经授权",
        "版权", "原唱", "翻唱", "后期", "美术", "设计", "配唱", "人声", "企划"
    )

    private val SEPARATORS = listOf(" - ", "–", "—", "-", "－", "_")

    internal class Resolved(
        val name: String?,
        val artist: String?,
        val sameSong: Boolean,
        val suspectTitle: Boolean
    )

    fun resolve(
        title: String?,
        artist: String?,
        currentName: String?,
        currentArtist: String?,
        lyricLines: Collection<String>,
        songFirstArtistLast: Boolean,
        trustArtistStructure: Boolean = false
    ): Resolved {
        val rawTitle = title?.trim().orEmpty()
        val rawArtist = artist?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
        val suspect = isSuspectTitle(rawTitle, lyricLines)

        val curName = normalize(currentName)
        val curArtist = normalize(currentArtist)
        val parts = splitArtist(rawArtist)
        val structureHoldsCurrentSong = parts != null &&
            (matches(curName, parts.first) || matches(curName, parts.second) ||
                matches(curArtist, parts.first) || matches(curArtist, parts.second))

        if (parts != null && (suspect || structureHoldsCurrentSong || trustArtistStructure)) {
            val (a, b) = parts
            val ordered = when {
                matches(curName, a) -> a to b
                matches(curName, b) -> b to a
                matches(curArtist, a) -> b to a
                matches(curArtist, b) -> a to b
                songFirstArtistLast -> a to b
                else -> b to a
            }
            val name = ordered.first.trim().ifEmpty { null } ?: currentName
            val singer = ordered.second.trim().ifEmpty { null } ?: currentArtist
            return Resolved(name, singer, sameAs(currentName, name), suspectTitle = suspect)
        }

        if (rawTitle.isNotEmpty() && !suspect) {
            return Resolved(rawTitle, rawArtist, sameAs(currentName, rawTitle), suspectTitle = false)
        }

        

        if (currentName == null) {

            return Resolved(
                rawTitle.ifEmpty { null },
                rawArtist,
                sameSong = false,
                suspectTitle = suspect
            )
        }
        return Resolved(
            currentName,
            currentArtist ?: rawArtist,
            sameSong = sameAs(currentName, rawTitle),
            suspectTitle = true
        )
    }

    fun isSuspectTitle(title: String?, lyricLines: Collection<String> = emptyList()): Boolean {
        val t = title?.trim().orEmpty()
        if (t.isEmpty()) return true
        val key = normalize(t)
        if (key.isEmpty()) return true

        

        
        if (CREDIT_PREFIX.any { t.startsWith(it) }) return true
        if (t.startsWith("【") || t.startsWith("［") || t.startsWith("[")) return true
        if (t.startsWith("（") && t.endsWith("）")) return true
        return false
    }

    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val sb = StringBuilder(text.length)
        for (c in text.lowercase()) if (c.isLetterOrDigit()) sb.append(c)
        return sb.toString()
    }

    private fun matches(current: String, candidate: String?): Boolean {
        val c = normalize(candidate)
        return current.isNotEmpty() && c.isNotEmpty() && (c == current || c.contains(current) || current.contains(c))
    }

    private fun sameAs(current: String?, candidate: String?): Boolean {
        val a = normalize(current)
        val b = normalize(candidate)
        return a.isNotEmpty() && a == b
    }

    private fun splitArtist(artist: String?): Pair<String, String>? {
        if (artist.isNullOrBlank()) return null
        for (sep in SEPARATORS) {
            val idx = artist.indexOf(sep)
            if (idx <= 0) continue
            val left = artist.substring(0, idx).trim()
            val right = artist.substring(idx + sep.length).trim()
            if (left.length < 2 || right.length < 2) continue
            if (left.length > 60 || right.length > 60) continue
            if (left.none { it.isLetterOrDigit() } || right.none { it.isLetterOrDigit() }) continue
            return left to right
        }
        return null
    }
}
