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

fun evaluateAnswer(type: QuestType, config: QuestConfig, answer: QuestAnswer): AnswerVerdict {
    val correct =
        when (type) {
            QuestType.PHOTO ->
                config is PhotoQuestConfig &&
                    answer is QuestAnswer.Text &&
                    matchesPhotoAnswer(answer.value, config.answers)
            QuestType.STENCIL ->
                config is WordQuestConfig &&
                    answer is QuestAnswer.Text &&
                    matchesPhotoAnswer(answer.value, listOf(config.word))
            QuestType.SILENT ->
                config is SilentQuestConfig &&
                    answer is QuestAnswer.Text &&
                    answer.value == config.code
            QuestType.QR ->
                config is QrQuestConfig &&
                    answer is QuestAnswer.Token &&
                    answer.value.trim() == config.token
            QuestType.NFC -> {
                if (config !is NfcQuestConfig || answer !is QuestAnswer.Token)
                    return AnswerVerdict.INCORRECT
                val tag =
                    config.tags.find { it.token == answer.value.trim() }
                        ?: return AnswerVerdict.UNKNOWN_TAG
                tag.id == config.correctId
            }
            QuestType.OBJECTS,
            QuestType.TREE -> {
                if (
                    config !is ChoiceQuestConfig ||
                        answer !is QuestAnswer.Selection ||
                        answer.ids.distinct().size != answer.ids.size
                )
                    return AnswerVerdict.INCORRECT
                if (type == QuestType.OBJECTS)
                    answer.ids.size == 3 && answer.ids.toSet() == config.answers.toSet()
                else answer.ids.size == 4 && answer.ids == config.answers
            }
        }
    return if (correct) AnswerVerdict.CORRECT else AnswerVerdict.INCORRECT
}

fun QuestConfig.answerDescription(answer: QuestAnswer): String =
    when (answer) {
        is QuestAnswer.Text -> answer.value.take(500)
        is QuestAnswer.Token ->
            if (this is NfcQuestConfig)
                tags.find { it.token == answer.value.trim() }?.label.orEmpty()
            else answer.value.take(500)
        is QuestAnswer.Selection ->
            if (this is ChoiceQuestConfig)
                answer.ids.joinToString(" → ") { id -> cells.find { it.id == id }?.label ?: id }
            else answer.ids.joinToString()
    }

fun elapsedSeconds(start: Long?, end: Long?, now: Long): Long? = start?.let {
    ((end ?: now) - it).coerceAtLeast(0) / 1_000
}

fun formatDuration(seconds: Long): String {
    val duration = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(Locale.ROOT, duration / 60, duration % 60)
}
