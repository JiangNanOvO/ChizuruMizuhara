package com.astraflow.Chizuru.lyric

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal class KuwoStreamTee(
    private val input: InputStream,
    private val onPayload: (ByteArray) -> Unit
) : InputStream() {

    private val sink = ByteArrayOutputStream(64 * 1024)
    private var stopped = false
    private var decided = false

    private fun feed(buffer: ByteArray, offset: Int, length: Int) {
        if (stopped || length <= 0) return
        if (sink.size() + length > MAX) {
            stop()
            return
        }
        sink.write(buffer, offset, length)
        if (!decided && sink.size() >= HEAD_SIZE) {
            decided = true
            if (!looksLikeLyricPayloadHead(sink.toByteArray())) stop()
        }
    }

    private fun stop() {
        stopped = true
        sink.reset()
    }

    private fun finish() {
        if (stopped) return
        stopped = true
        val bytes = sink.toByteArray()
        sink.reset()

        if (bytes.size >= MIN) runCatching { onPayload(bytes) }
    }

    override fun read(): Int {
        val value = input.read()
        if (value < 0) {
            finish()
        } else {
            feed(byteArrayOf(value.toByte()), 0, 1)
        }
        return value
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = input.read(b, off, len)
        if (read < 0) finish() else feed(b, off, read)
        return read
    }

    override fun skip(n: Long): Long = input.skip(n)

    override fun available(): Int = input.available()

    override fun close() {
        try {
            input.close()
        } finally {
            finish()
        }
    }

    private companion object {
        const val MAX = 1_500_000
        const val MIN = 32
        const val HEAD_SIZE = 32
    }
}
