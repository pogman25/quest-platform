package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ElapsedTime
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.components.SectionCard
import ru.pogman.questplatform.domain.PlayerRun
import ru.pogman.questplatform.domain.QuestType

@Composable
fun ResultScreen(run: PlayerRun, onReturn: () -> Unit) {
    ScreenContent {
        Text(
            stringResource(R.string.congratulations_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(run.teamName, style = MaterialTheme.typography.titleLarge)
        SectionCard(stringResource(R.string.final_code_label)) {
            Text(
                run.finalCode.orEmpty(),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.testTag("finalCode"),
            )
            Text(stringResource(R.string.open_lock_hint))
        }
        ElapsedTime(run.startedAt, run.finishedAt, stringResource(R.string.total_time_label))
        run.quests
            .find { it.type == QuestType.SILENT }
            ?.let { quest ->
                SectionCard(quest.title) {
                    Text(stringResource(R.string.silent_noise_count, quest.noiseCount))
                    ElapsedTime(
                        quest.openedAt,
                        quest.solvedAt,
                        stringResource(R.string.quest_time_label),
                    )
                }
            }
        SectionCard(stringResource(R.string.slowest_quest_label)) {
            run.slowestQuest?.let { quest ->
                Text(quest.title)
                ElapsedTime(
                    quest.openedAt,
                    quest.solvedAt,
                    stringResource(R.string.quest_time_label),
                )
            }
        }
        Button(onClick = onReturn, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.notebook_action))
        }
    }
}
