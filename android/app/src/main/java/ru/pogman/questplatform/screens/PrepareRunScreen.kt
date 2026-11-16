package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.domain.GameSummary
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun PrepareRunScreen(game: GameSummary, action: ActionState, onPrepare: (String, String) -> Unit) {
    var team by rememberSaveable(game.id) { mutableStateOf("") }
    var code by rememberSaveable(game.id) { mutableStateOf("") }
    val handleTeam: (String) -> Unit = { team = it.take(80) }
    val handleCode: (String) -> Unit = {
        code = it.filter { char -> char in '0'..'9' }.take(game.questCount)
    }
    val handlePrepare = { onPrepare(team, code) }
    LaunchedEffect(game.questCount) { code = code.take(game.questCount) }

    ScreenContent {
        Text(game.title, style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.prepare_description))
        OutlinedTextField(
            team,
            handleTeam,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.team_name_label)) },
            singleLine = true,
            enabled = !action.busy,
        )
        OutlinedTextField(
            code,
            handleCode,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.lock_code_label)) },
            supportingText = { Text(stringResource(R.string.lock_code_hint, game.questCount)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = !action.busy,
        )
        ActionMessage(action)
        Button(
            onClick = handlePrepare,
            enabled = !action.busy && team.isNotBlank() && code.length == game.questCount,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.handover_action))
        }
    }
}
