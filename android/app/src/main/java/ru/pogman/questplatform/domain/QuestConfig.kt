package ru.pogman.questplatform.domain

import kotlinx.serialization.Serializable

sealed interface QuestConfig {
    val instruction: String
}

@Serializable data class NfcTagConfig(val id: String, val label: String, val token: String)

@Serializable
data class NfcQuestConfig(
    override val instruction: String,
    val tags: List<NfcTagConfig>,
    val correctId: String,
) : QuestConfig

@Serializable
data class QrQuestConfig(
    override val instruction: String,
    val photoRef: String,
    val token: String,
    val sizeMm: Int = 150,
    val fit: PictureFit = PictureFit.COVER,
) : QuestConfig

@Serializable
enum class PictureFit {
    COVER,
    CONTAIN,
}

@Serializable
data class ChoiceCell(
    val id: String,
    val label: String,
    val symbol: String,
    val photoRef: String? = null,
)

data class ChoiceItem(
    val id: String,
    val label: String,
    val symbol: String,
    val imageUri: String? = null,
)

fun ChoiceCell.forDisplay(photoUri: (String) -> String) =
    ChoiceItem(id, label, symbol, photoRef?.let(photoUri))

data class PhotoTarget(val type: QuestType, val cellId: String? = null)

@Serializable
data class ChoiceQuestConfig(
    override val instruction: String,
    val cells: List<ChoiceCell>,
    val answers: List<String>,
) : QuestConfig

@Serializable
// Defaults also read old word-only saves. Keep them stable; new saves encode print settings.
data class WordQuestConfig(
    override val instruction: String,
    val word: String,
    val widthMm: Int = 168,
    val correctOverlay: Int = 3,
    val strokeSeed: Long = 20261116L,
    val strokeVersion: Int = 3,
) : QuestConfig

data class GameQuest(val type: QuestType, val config: QuestConfig)

fun Game.updateQuest(type: QuestType, transform: (QuestConfig) -> QuestConfig): Game =
    copy(
        quests = quests.map { if (it.type == type) it.copy(config = transform(it.config)) else it }
    )

fun Game.photoConfig(): PhotoQuestConfig? =
    quests.find { it.type == QuestType.PHOTO }?.config as? PhotoQuestConfig

sealed interface QuestAnswer {
    data class Text(val value: String) : QuestAnswer

    data class Token(val value: String) : QuestAnswer

    data class Selection(val ids: List<String>) : QuestAnswer
}

enum class AnswerVerdict {
    CORRECT,
    INCORRECT,
    UNKNOWN_TAG,
}
