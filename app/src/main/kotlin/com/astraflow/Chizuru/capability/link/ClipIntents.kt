package com.astraflow.Chizuru.capability.link

enum class ClipKind(val wire: String) {
    LINK("link"),
    PHONE("phone"),
    ADDRESS("address");

    companion object {
        fun of(wire: String?): ClipKind = when (wire) {
            PHONE.wire -> PHONE
            ADDRESS.wire -> ADDRESS
            else -> LINK
        }
    }
}

data class ClipIntent(val kind: ClipKind, val payload: String)

object ClipIntents {

    private val PHONE_MOBILE = Regex("""(?<!\d)(?:\+?86[\s-]?)?1[3-9]\d{9}(?!\d)""")

    private val PHONE_LANDLINE = Regex("""(?<!\d)0\d{2,3}[\s-]?\d{7,8}(?!\d)""")

    private val PHONE_SERVICE = Regex("""(?<!\d)(?:400|800)[\s-]?\d{3}[\s-]?\d{4}(?!\d)""")

    private val ADDRESS_PARTS = listOf(
        "省", "市", "自治区", "自治州", "区", "县", "镇", "乡", "村", "街道", "路", "街", "巷",
        "大道", "胡同", "小区", "花园", "公寓", "大厦", "广场", "中心", "号楼", "号楼", "栋", "单元",
        "室", "楼", "层", "号"
    )

    private val SENTENCE_TAIL = Regex("""[。！？!?~～]""")

    private const val MAX_ADDRESS_LENGTH = 120

    private const val MIN_ADDRESS_LENGTH = 6

    fun classify(text: CharSequence?): ClipIntent? {
        val raw = text?.toString()?.trim().orEmpty()
        if (raw.length < 3) return null

        val link = LinkPatterns.firstLink(raw)
        if (link != null) return ClipIntent(ClipKind.LINK, link)

        phoneOf(raw)?.let { return ClipIntent(ClipKind.PHONE, it) }

        addressOf(raw)?.let { return ClipIntent(ClipKind.ADDRESS, it) }

        return null
    }

    fun phoneOf(raw: String): String? {
        val compact = raw.replace(" ", "")
        if (compact.length > 32) return null
        val hit = PHONE_MOBILE.find(raw)
            ?: PHONE_LANDLINE.find(raw)
            ?: PHONE_SERVICE.find(raw)
            ?: return null
        val digits = hit.value.count { it.isDigit() }
        if (digits < 7) return null
        if (countDigits(raw) > digits + 4) return null
        return hit.value.trim()
    }

    fun addressOf(raw: String): String? {
        if (raw.length < MIN_ADDRESS_LENGTH || raw.length > MAX_ADDRESS_LENGTH) return null
        if (SENTENCE_TAIL.containsMatchIn(raw)) return null
        if (raw.count { it == '\n' } > 1) return null
        if (!raw.any { it in '\u4e00'..'\u9fff' }) return null
        var hits = 0
        for (part in ADDRESS_PARTS) {
            if (raw.contains(part)) hits++
        }
        if (hits < 2) return null
        val hasRegion = raw.contains("省") || raw.contains("市") || raw.contains("区") ||
            raw.contains("县") || raw.contains("镇") || raw.contains("乡")
        val hasStreet = raw.contains("路") || raw.contains("街") || raw.contains("巷") ||
            raw.contains("大道") || raw.contains("道")
        val hasNumber = Regex("""\d{1,4}\s*(?:号|弄|栋|单元|室|楼|层)""").containsMatchIn(raw)
        if (!hasRegion && !hasStreet && !hasNumber) return null
        return raw
    }

    fun summaryOf(intent: ClipIntent): String = when (intent.kind) {
        ClipKind.LINK -> LinkPatterns.hostOf(intent.payload)
        ClipKind.PHONE -> intent.payload
        ClipKind.ADDRESS -> cityOf(intent.payload)
    }

    private fun cityOf(address: String): String {
        val city = Regex("""[\u4e00-\u9fff]{2,8}市""").find(address)?.value
        val district = Regex("""[\u4e00-\u9fff]{2,8}[区县]""").find(address)?.value
        return city ?: district ?: address.take(8)
    }

    private fun countDigits(text: String): Int = text.count { it.isDigit() }
}
