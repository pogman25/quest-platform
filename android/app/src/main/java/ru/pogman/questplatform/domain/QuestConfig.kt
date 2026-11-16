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
) : QuestConfig

@Serializable data class ChoiceCell(val id: String, val label: String, val symbol: String)

@Serializable
data class ChoiceQuestConfig(
    override val instruction: String,
    val cells: List<ChoiceCell>,
    val answers: List<String>,
) : QuestConfig

@Serializable
data class WordQuestConfig(override val instruction: String, val word: String) : QuestConfig

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
