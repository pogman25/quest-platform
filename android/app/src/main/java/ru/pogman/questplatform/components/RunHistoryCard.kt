package ru.pogman.questplatform.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.QuestRunSummary
import ru.pogman.questplatform.domain.RunSummary

private val logDate = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

@Composable
fun RunHistoryCard(
    run: RunSummary,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onRestart: (RunSummary) -> Unit,
    onResetQuest: (RunSummary, QuestRunSummary) -> Unit,
) {
    var expanded by rememberSaveable(run.id) { mutableStateOf(false) }
    val handleExpand = { expanded = !expanded }
    val handleSelect = { onSelect(run.id) }
    val handleRestart = { onRestart(run) }

    SectionCard(run.teamName) {
        Text(run.gameTitle)
        Text(logDate.format(Instant.ofEpochMilli(run.createdAt).atZone(ZoneId.systemDefault())))
        Text(
            stringResource(
                when {
                    run.finishedAt != null -> R.string.run_finished
                    run.startedAt != null -> R.string.run_in_progress
                    else -> R.string.run_prepared
                }
            )
        )
        if (run.startedAt != null) {
            ElapsedTime(run.startedAt, run.finishedAt, stringResource(R.string.total_time_label))
        }
        run.quests.forEach { quest ->
            val handleResetQuest = { onResetQuest(run, quest) }
            Text("${quest.position}. ${quest.title}")
            quest.noiseCount?.let { Text(stringResource(R.string.silent_noise_count, it)) }
            if (quest.openedAt != null)
                ElapsedTime(
                    quest.openedAt,
                    quest.solvedAt,
                    stringResource(R.string.quest_time_label),
                )
            if (run.startedAt != null && quest.type != null) {
                TextButton(
                    onClick = handleResetQuest,
                    enabled =
                        enabled &&
                            (quest.openedAt != null ||
                                quest.solvedAt != null ||
                                (quest.noiseCount ?: 0) > 0),
                ) {
                    Text(stringResource(R.string.reset_quest_action))
                }
            }
        }
        Text(stringResource(R.string.attempt_statistics, run.attempts, run.incorrectAttempts))
        if (run.log.isNotEmpty()) {
            TextButton(onClick = handleExpand) {
                Text(
                    stringResource(
                        if (expanded) R.string.hide_attempts_action
                        else R.string.show_attempts_action
                    )
                )
            }
            if (expanded) {
                run.log.forEach { attempt ->
                    val time =
                        logDate.format(
                            Instant.ofEpochMilli(attempt.submittedAt).atZone(ZoneId.systemDefault())
                        )
                    val result =
                        stringResource(
                            if (attempt.correct) R.string.attempt_correct
                            else R.string.attempt_incorrect
                        )
                    Text("$time · ${attempt.questTitle} · ${attempt.answer} · $result")
                }
            }
        }
        Button(onClick = handleSelect, enabled = enabled) {
            Text(stringResource(R.string.select_run_action))
        }
        if (run.startedAt != null || run.attempts > 0) {
            TextButton(onClick = handleRestart, enabled = enabled) {
                Text(stringResource(R.string.restart_run_action))
            }
        }
    }
}
