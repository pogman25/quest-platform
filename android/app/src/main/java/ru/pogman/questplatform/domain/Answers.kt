package ru.pogman.questplatform.domain

import java.text.Normalizer
import java.util.Locale

fun normalizeAnswer(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFC)
        .trim()
        .lowercase(Locale.forLanguageTag("ru"))
        .replace('ё', 'е')
        .replace(Regex("\\s+"), " ")

fun matchesPhotoAnswer(answer: String, accepted: List<String>): Boolean {
    val normalized = normalizeAnswer(answer)
    return normalized.isNotEmpty() && accepted.any { normalizeAnswer(it) == normalized }
}

fun elapsedSeconds(start: Long?, end: Long?, now: Long): Long? = start?.let {
    ((end ?: now) - it).coerceAtLeast(0) / 1_000
}

fun formatDuration(seconds: Long): String {
    val duration = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(Locale.ROOT, duration / 60, duration % 60)
}
