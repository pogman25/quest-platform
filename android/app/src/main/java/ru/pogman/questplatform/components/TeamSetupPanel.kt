package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.Game
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun TeamSetupPanel(
    game: Game,
    team: String,
    code: String,
    action: ActionState,
    onTeamChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPrepare: (String, String) -> Unit,
) {
    val handlePrepare = { onPrepare(team, code) }

    ScreenContent {
        Text(game.title, style = MaterialTheme.typography.headlineLarge)
        SectionCard(stringResource(R.string.team_setup_title)) {
            Text(stringResource(R.string.prepare_description))
            OutlinedTextField(
                team,
                onTeamChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.team_name_label)) },
                singleLine = true,
                enabled = !action.busy,
            )
            OutlinedTextField(
                code,
                onCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.lock_code_label)) },
                supportingText = {
                    Text(stringResource(R.string.lock_code_hint, game.quests.size))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !action.busy,
            )
            ActionMessage(action)
            Button(
                onClick = handlePrepare,
                enabled = !action.busy && team.isNotBlank() && code.length == game.quests.size,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.handover_action))
            }
        }
    }
}
