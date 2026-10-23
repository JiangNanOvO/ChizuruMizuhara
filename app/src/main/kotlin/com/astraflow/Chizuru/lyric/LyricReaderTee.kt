package com.astraflow.Chizuru.lyric

import java.io.Reader

internal class LyricReaderTee(
    private val input: Reader,
    private val onPayload: (String) -> Unit
) : Reader() {

    private val sink = StringBuilder(64 * 1024)
    private var stopped = false
    private var decided = false

    private fun feed(buffer: CharArray, offset: Int, length: Int) {
        if (stopped || length <= 0) return
        if (sink.length + length > MAX) {
            stop()
            return
        }
        sink.appendRange(buffer, offset, offset + length)
        if (!decided && sink.length >= HEAD) {
            decided = true
            val head = sink.substring(0, minOf(sink.length, 96))
            if (!looksLikeLyricPayloadHead(head.toByteArray(Charsets.ISO_8859_1))) stop()
        }
    }

    private fun stop() {
        stopped = true
        sink.setLength(0)
    }

    private fun finish() {
        if (stopped) return
        stopped = true
        if (sink.length < MIN) {
            sink.setLength(0)
            return
        }
        val text = sink.toString()
        sink.setLength(0)
        runCatching { onPayload(text) }
    }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        val n = input.read(cbuf, off, len)
        if (n > 0) feed(cbuf, off, n) else if (n < 0) finish()
        return n
    }

    override fun close() {
        finish()
        input.close()
    }

    private companion object {
        private const val MAX = 1_500_000
        private const val MIN = 64
        private const val HEAD = 64
    }
}
