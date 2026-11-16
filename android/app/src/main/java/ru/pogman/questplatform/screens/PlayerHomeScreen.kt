package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.*
import ru.pogman.questplatform.domain.*
import ru.pogman.questplatform.theme.QuestTheme
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun PlayerHomeScreen(
    run: PlayerRun?,
    action: ActionState,
    onBriefing: () -> Unit,
    onQuest: (QuestType) -> Unit,
    onResult: () -> Unit,
) {
    ScreenContent {
        Text(stringResource(R.string.player_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.player_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (run == null) {
            CaseIntroduction()
            SectionCard(stringResource(R.string.game_waiting_title)) {
                Text(stringResource(R.string.game_waiting_description))
            }
        } else {
            SectionCard(run.teamName) {
                Text(run.gameTitle)
                if (run.startedAt == null) {
                    Text(stringResource(R.string.player_ready))
                    Button(onClick = onBriefing, enabled = !action.busy) {
                        Text(stringResource(R.string.briefing_action))
                    }
                } else {
                    Text(
                        stringResource(R.string.code_progress_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        run.quests.sortedBy { it.position }.joinToString(" ") { it.reward ?: "·" },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        stringResource(
                            R.string.quest_progress,
                            run.quests.count { it.solvedAt != null },
                            run.quests.size,
                        )
                    )
                }
            }
            if (run.startedAt != null)
                run.quests.forEach { quest ->
                    PlayerQuestCard(quest, !action.busy, onQuest)
                }
            if (run.finishedAt != null)
                Button(onClick = onResult, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.results_action))
                }
        }
        ActionMessage(action)
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun PlayerHomePreview() {
    QuestTheme { PlayerHomeScreen(null, ActionState(), {}, {}, {}) }
}
