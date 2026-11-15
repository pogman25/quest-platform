package ru.pogman.questplatform.domain

import kotlinx.serialization.Serializable

@Serializable
data class PhotoQuestConfig(
    val instruction: String,
    val photoRef: String,
    val answers: List<String>,
)

data class Game(
    val id: String,
    val title: String,
    val story: String,
    val rules: String,
    val photo: PhotoQuestConfig,
)

data class GameSummary(val id: String, val title: String)
