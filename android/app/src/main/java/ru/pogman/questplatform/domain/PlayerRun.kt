package ru.pogman.questplatform.domain

data class PlayerQuest(
    val title: String,
    val instruction: String,
    val photoUri: String,
    val openedAt: Long?,
    val solvedAt: Long?,
    val reward: String?,
)

data class PlayerRun(
    val id: String,
    val gameTitle: String,
    val teamName: String,
    val story: String,
    val rules: String,
    val startedAt: Long?,
    val finishedAt: Long?,
    val quest: PlayerQuest,
) {
    val finalCode: String?
        get() = quest.reward.takeIf { finishedAt != null }
}

data class RunSummary(
    val id: String,
    val teamName: String,
    val gameTitle: String,
    val createdAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?,
    val attempts: Int,
    val incorrectAttempts: Int,
    val questOpenedAt: Long?,
    val questSolvedAt: Long?,
    val log: List<AnswerAttempt>,
)

data class AnswerAttempt(val answer: String, val correct: Boolean, val submittedAt: Long)
