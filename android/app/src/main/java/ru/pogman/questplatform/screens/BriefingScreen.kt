package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.components.SectionCard
import ru.pogman.questplatform.domain.PlayerRun
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun BriefingScreen(run: PlayerRun, action: ActionState, onStart: () -> Unit) {
    ScreenContent {
        Text(run.gameTitle, style = MaterialTheme.typography.headlineLarge)
        Text(run.teamName, style = MaterialTheme.typography.titleMedium)
        SectionCard(stringResource(R.string.story_label)) { Text(run.story) }
        SectionCard(stringResource(R.string.rules_label)) { Text(run.rules) }
        Text(stringResource(R.string.briefing_timer_hint))
        ActionMessage(action)
        Button(onClick = onStart, enabled = !action.busy, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (run.startedAt == null) R.string.start_run_action
                    else R.string.continue_run_action
                )
            )
        }
    }
}
