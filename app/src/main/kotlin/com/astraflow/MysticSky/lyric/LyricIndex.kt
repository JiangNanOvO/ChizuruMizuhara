package com.astraflow.MysticSky.lyric

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal object LyricIndex {

    private const val FILE_NAME = "mitian_lyric_index.json"

    private const val MAX_ENTRIES = 4000

    private const val SAVE_INTERVAL_MS = 60_000L

    private class Entry(
        val path: String,
        val mtime: Long,
        val size: Long,
        val title: String,
        val artist: String,
        val id: String,
        val lastBegin: Long
    )

    private val loaded = AtomicBoolean(false)
    private val byPath = ConcurrentHashMap<String, Entry>()
    private val dirty = AtomicBoolean(false)
    private var lastSave = 0L

    fun remember(context: Context, file: File, lyric: LocalLyric) {
        val entry = Entry(
            path = file.absolutePath,
            mtime = file.lastModified(),
            size = file.length(),
            title = LocalLyricFinder.normalize(lyric.title),
            artist = LocalLyricFinder.normalize(lyric.artist),
            id = LocalLyricFinder.normalize(lyric.id),
            lastBegin = lyric.lines.lastOrNull()?.begin ?: 0L
        )
        byPath[entry.path] = entry
        dirty.set(true)
        maybeSave(context)
    }

    fun lookup(
        context: Context,
        wantTitle: String,
        wantArtist: String,
        mediaId: String?
    ): List<File> {
        load(context)
        if (byPath.isEmpty()) return emptyList()
        val mid = LocalLyricFinder.normalize(mediaId)
        val entries = byPath.values.toList()

        val ordered = ArrayList<Entry>(entries.size)
        if (mid.isNotEmpty()) ordered += entries.filter { it.id.isNotEmpty() && it.id == mid }
        if (wantTitle.isNotEmpty()) {
            ordered += entries.filter {
                it.title.isNotEmpty() && it.title == wantTitle &&
                    (wantArtist.isEmpty() || it.artist == wantArtist)
            }
            ordered += entries.filter { it.title.isNotEmpty() && it.title == wantTitle }
            ordered += entries.filter { it.title.isNotEmpty() && it.title.contains(wantTitle) }
        }

        val out = LinkedHashSet<File>()
        for (entry in ordered) {
            val file = File(entry.path)
            if (!file.isFile || file.lastModified() != entry.mtime) {
                byPath.remove(entry.path)
                continue
            }
            out.add(file)
            if (out.size >= 4) break
        }
        return out.toList()
    }

    private fun load(context: Context) {
        if (!loaded.compareAndSet(false, true)) return
        val file = indexFile(context)
        if (!file.isFile) return
        runCatching {
            val array = JSONArray(file.readText())
            for (i in 0 until array.length()) {
                val row = array.optJSONArray(i) ?: continue
                val entry = Entry(
                    path = row.optString(0),
                    mtime = row.optLong(1),
                    size = row.optLong(2),
                    title = row.optString(3),
                    artist = row.optString(4),
                    id = row.optString(5),
                    lastBegin = row.optLong(6)
                )
                if (entry.path.isNotEmpty()) byPath[entry.path] = entry
            }
        }
    }

    private fun maybeSave(context: Context) {
        val now = System.currentTimeMillis()
        if (!dirty.get() || now - lastSave < SAVE_INTERVAL_MS) return
        lastSave = now
        dirty.set(false)

        if (byPath.size > MAX_ENTRIES) {
            val keep = byPath.values.sortedByDescending { it.mtime }.take(MAX_ENTRIES)
            byPath.clear()
            for (entry in keep) byPath[entry.path] = entry
        }

        val array = JSONArray()
        for (entry in byPath.values) {
            array.put(
                JSONArray().apply {
                    put(entry.path)
                    put(entry.mtime)
                    put(entry.size)
                    put(entry.title)
                    put(entry.artist)
                    put(entry.id)
                    put(entry.lastBegin)
                }
            )
        }
        runCatching { indexFile(context).writeText(array.toString()) }
    }

    private fun indexFile(context: Context): File = File(context.cacheDir, FILE_NAME)
}
