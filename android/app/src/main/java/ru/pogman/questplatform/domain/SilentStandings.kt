package ru.pogman.questplatform.domain

data class SilentStanding(
    val runId: String,
    val team: String,
    val count: Int,
    val durationSeconds: Long,
    val place: Int,
)

data class SilentCompetition(val title: String, val standings: List<SilentStanding>)

fun silentCompetitions(runs: List<RunSummary>): List<SilentCompetition> {
    val completed = runs.mapNotNull { run ->
        val quest =
            run.quests.find {
                it.noiseCount != null &&
                    it.noiseRulesKey != null &&
                    it.openedAt != null &&
                    it.solvedAt != null
            } ?: return@mapNotNull null
        Triple(run, quest, (quest.solvedAt!! - quest.openedAt!!).coerceAtLeast(0) / 1000)
    }
    return completed
        .groupBy { it.first.gameId to it.second.noiseRulesKey }
        .values
        .map { group ->
            val sorted =
                group.sortedWith(
                    compareBy({ it.second.noiseCount }, { it.third }, { it.first.createdAt })
                )
            var place = 1
            val standings = sorted.mapIndexed { index, (run, quest, duration) ->
                if (
                    index > 0 &&
                        (sorted[index - 1].second.noiseCount != quest.noiseCount ||
                            sorted[index - 1].third != duration)
                )
                    place = index + 1
                SilentStanding(run.id, run.teamName, quest.noiseCount!!, duration, place)
            }
            SilentCompetition(group.first().first.gameTitle, standings)
        }
}
