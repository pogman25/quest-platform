package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.LazyScreenContent
import ru.pogman.questplatform.components.OrganizerGameCard
import ru.pogman.questplatform.domain.GameSummary
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun OrganizerGamesScreen(
    games: List<GameSummary>,
    action: ActionState,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onHistory: () -> Unit,
    onPassword: () -> Unit,
    onNoiseMeter: () -> Unit,
) {
    var deleting by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val handleQuery: (String) -> Unit = { query = it.take(80) }
    val visible = games.filter { it.title.contains(query.trim(), ignoreCase = true) }
    val handleAskDelete: (String) -> Unit = { deleting = it }
    val handleCancelDelete = { deleting = null }
    val handleDelete = {
        deleting?.let(onDelete)
        deleting = null
    }

    LazyScreenContent {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    stringResource(R.string.organizer_games_title),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(stringResource(R.string.organizer_games_description))
                Button(
                    onClick = onCreate,
                    enabled = !action.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.create_game_action))
                }
                OutlinedButton(onClick = onNoiseMeter, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.noise_meter_action))
                }
                ActionMessage(action)
                OutlinedTextField(
                    query,
                    handleQuery,
                    Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_games_label)) },
                )
            }
        }
        items(visible, key = { it.id }) { game ->
            OrganizerGameCard(game, !action.busy, onOpen, handleAskDelete)
        }
        if (games.isEmpty()) {
            item { Text(stringResource(R.string.games_empty)) }
        } else if (visible.isEmpty()) {
            item { Text(stringResource(R.string.search_games_empty)) }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onHistory) { Text(stringResource(R.string.history_title)) }
                TextButton(onClick = onPassword) {
                    Text(stringResource(R.string.password_change_title))
                }
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
