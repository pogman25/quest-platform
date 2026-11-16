package ru.pogman.questplatform.domain

data class PlayerQuest(
    val type: QuestType,
    val position: Int,
    val instruction: String,
    val photoUri: String?,
    val cells: List<ChoiceItem>,
    val openedAt: Long?,
    val solvedAt: Long?,
    val reward: String?,
) {
    val title: String
        get() = type.title
}

data class PlayerRun(
    val id: String,
    val gameTitle: String,
    val teamName: String,
    val story: String,
    val rules: String,
    val startedAt: Long?,
    val finishedAt: Long?,
    val quests: List<PlayerQuest>,
    val gameId: String,
) {
    val finalCode: String?
        get() =
            if (finishedAt != null && quests.isNotEmpty() && quests.all { it.reward != null })
                quests.sortedBy { it.position }.joinToString("") { it.reward.orEmpty() }
            else null

    val slowestQuest: PlayerQuest?
        get() =
            if (finishedAt != null)
                quests
                    .sortedBy { it.position }
                    .maxByOrNull { elapsedSeconds(it.openedAt, it.solvedAt, finishedAt) ?: 0 }
            else null
}

data class QuestRunSummary(
    val title: String,
    val position: Int,
    val openedAt: Long?,
    val solvedAt: Long?,
)

data class RunSummary(
    val id: String,
    val teamName: String,
    val gameTitle: String,
    val createdAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?,
    val attempts: Int,
    val incorrectAttempts: Int,
    val quests: List<QuestRunSummary>,
    val log: List<AnswerAttempt>,
)

data class AnswerAttempt(
    val questTitle: String,
    val answer: String,
    val correct: Boolean,
    val submittedAt: Long,
)
