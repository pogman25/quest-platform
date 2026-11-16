package ru.pogman.questplatform.domain

import kotlinx.serialization.Serializable

@Serializable
data class PhotoQuestConfig(
    override val instruction: String,
    val photoRef: String,
    val answers: List<String>,
) : QuestConfig

data class Game(
    val id: String,
    val title: String,
    val story: String,
    val rules: String,
    val quests: List<GameQuest>,
)

data class GameSummary(val id: String, val title: String, val questCount: Int)
