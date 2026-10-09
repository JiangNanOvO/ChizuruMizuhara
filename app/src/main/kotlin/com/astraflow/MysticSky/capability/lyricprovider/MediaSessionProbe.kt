package com.astraflow.MysticSky.capability.lyricprovider

object MediaSessionProbe {

    data class NowPlaying(
        val pkg: String,
        val title: String,
        val artist: String?,
        val positionMs: Long,
        val playing: Boolean
    ) {

        fun signature(): String = "$pkg|$title|${artist.orEmpty()}"
    }

    fun probe(): NowPlaying? {
        val dump = com.astraflow.MysticSky.capability.root.RootShell
            .exec("dumpsys media_session 2>/dev/null", timeoutMs = 12_000)
            ?: return null
        return parse(dump)
    }

    fun parse(dump: String): NowPlaying? {
        var pkg: String? = null
        var active = false
        var title: String? = null
        var artist: String? = null
        var position = 0L
        var stateCode = -1
        var best: NowPlaying? = null

        fun flush() {
            val p = pkg
            val t = title
            if (p != null && t != null && active) {

                if (stateCode != 1) {
                    best = NowPlaying(p, t, artist, position, stateCode == 3)
                }
            }
            pkg = null; active = false; title = null; artist = null; position = 0L; stateCode = -1
        }

        for (raw in dump.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("MediaSession ") && line.contains("/MediaSession/") -> {
                    flush()
                }
                line.startsWith("package=") -> pkg = line.removePrefix("package=").trim()
                line.startsWith("active=") -> active = line.removePrefix("active=").trim() == "true"
                line.startsWith("state=PlaybackState {") -> {
                    val body = line.substringAfter("{")
                    Regex("state=(\\w+)\\((\\d+)\\)").find(body)?.let { stateCode = it.groupValues[2].toInt() }
                    Regex("position=(-?\\d+)").find(body)?.let { position = it.groupValues[1].toLong() }
                }
                line.startsWith("metadata:") && line.contains("description=") -> {

                    val rest = line.substringAfter("description=").trim()
                    val parts = splitMetadata(rest)
                    title = parts.getOrNull(0)?.takeIf { it.isNotBlank() && it != "null" }
                    artist = parts.getOrNull(1)?.takeIf { it.isNotBlank() && it != "null" }
                }
            }
        }
        flush()
        return best
    }

    private fun splitMetadata(rest: String): List<String> {
        val parts = rest.split(", ").toMutableList()
        if (parts.size >= 3) {
            val album = parts.removeAt(parts.size - 1)
            val artist = parts.removeAt(parts.size - 1)
            val title = parts.joinToString(", ")
            return listOf(title, artist, album)
        }
        return parts
    }
}
