package com.astraflow.Chizuru

object Module {

    const val PACKAGE_NAME: String = "com.astraflow.Chizuru"

    const val LYRICON_PROVIDER_PACKAGE: String = PACKAGE_NAME

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

fun redactUrl(raw: String?): String {
    if (raw.isNullOrBlank()) return "(空)"
    return runCatching {
        val uri = java.net.URI(raw)
        val host = uri.host
        when {
            host.isNullOrBlank() -> "(非标准链接)"
            uri.path.isNullOrEmpty() || uri.path == "/" -> host
            else -> "$host/…（长度 ${raw.length}）"
        }
    }.getOrDefault("(无法解析)")
}
