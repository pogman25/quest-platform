package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.CaseIntroduction
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.components.SectionCard
import ru.pogman.questplatform.domain.PlayerRun
import ru.pogman.questplatform.theme.QuestTheme
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun PlayerHomeScreen(
    run: PlayerRun?,
    action: ActionState,
    onBriefing: () -> Unit,
    onPhoto: () -> Unit,
    onResult: () -> Unit,
) {
    ScreenContent {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.player_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                stringResource(R.string.player_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (run == null) {
            CaseIntroduction()
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.game_waiting_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.game_waiting_description),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
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
                    Text(run.quest.reward ?: "·", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        stringResource(
                            if (run.quest.solvedAt == null) R.string.progress_zero
                            else R.string.progress_one
                        )
                    )
                }
            }
            if (run.startedAt != null && run.finishedAt == null) {
                SectionCard(run.quest.title) {
                    Text(
                        stringResource(
                            if (run.quest.openedAt == null) R.string.quest_ready
                            else R.string.quest_in_progress
                        )
                    )
                    Button(onClick = onPhoto, enabled = !action.busy) {
                        Text(stringResource(R.string.open_photo_action))
                    }
                }
            }
            if (run.finishedAt != null) {
                Button(onClick = onResult, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.results_action))
                }
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
    QuestTheme {
        PlayerHomeScreen(
            run = null,
            action = ActionState(),
            onBriefing = {},
            onPhoto = {},
            onResult = {},
        )
    }
}
