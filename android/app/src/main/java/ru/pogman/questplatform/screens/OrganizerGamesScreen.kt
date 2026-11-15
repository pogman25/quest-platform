package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.OrganizerGameCard
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.domain.GameSummary
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun OrganizerGamesScreen(
    games: List<GameSummary>,
    action: ActionState,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onPrepare: (String) -> Unit,
    onDelete: (String) -> Unit,
    onHistory: () -> Unit,
    onPassword: () -> Unit,
) {
    var deleting by remember { mutableStateOf<String?>(null) }
    val handleAskDelete: (String) -> Unit = { deleting = it }
    val handleCancelDelete = { deleting = null }
    val handleDelete = {
        deleting?.let(onDelete)
        deleting = null
    }

    ScreenContent {
        Text(
            stringResource(R.string.organizer_games_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(stringResource(R.string.organizer_games_description))
        Button(onClick = onCreate, enabled = !action.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.create_game_action))
        }
        ActionMessage(action)
        games.forEach { game ->
            OrganizerGameCard(game, !action.busy, onEdit, onPrepare, handleAskDelete)
        }
        if (games.isEmpty()) {
            Text(stringResource(R.string.games_empty))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onHistory) { Text(stringResource(R.string.history_title)) }
            TextButton(onClick = onPassword) {
                Text(stringResource(R.string.password_change_title))
            }
        }
    }

    if (deleting != null) {
        AlertDialog(
            onDismissRequest = handleCancelDelete,
            title = { Text(stringResource(R.string.delete_game_title)) },
            text = { Text(stringResource(R.string.delete_game_description)) },
            confirmButton = {
                TextButton(onClick = handleDelete) { Text(stringResource(R.string.delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = handleCancelDelete) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}
