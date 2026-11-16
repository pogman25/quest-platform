package ru.pogman.questplatform.data

internal fun decodeNdefText(payload: ByteArray): String? {
    if (payload.isEmpty()) return null
    val status = payload[0].toInt() and 0xff
    val start = 1 + (status and 0x3f)
    if (start >= payload.size) return null
    val charset = if (status and 0x80 != 0) Charsets.UTF_16 else Charsets.UTF_8
    return String(payload, start, payload.size - start, charset)
}
