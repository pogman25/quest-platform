package ru.pogman.questplatform.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.RunHistoryCard
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.components.SilentStandingsCard
import ru.pogman.questplatform.domain.RunSummary
import ru.pogman.questplatform.domain.silentCompetitions
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun RunHistoryScreen(
    runs: List<RunSummary>,
    action: ActionState,
    onSelect: (String) -> Unit,
    onRestart: (String) -> Unit,
) {
    var restarting by remember { mutableStateOf<RunSummary?>(null) }
    val handleAskRestart: (RunSummary) -> Unit = { restarting = it }
    val handleCancelRestart = { restarting = null }
    val handleRestart = {
        restarting?.let { onRestart(it.id) }
        restarting = null
    }
    ScreenContent {
        ActionMessage(action)
        silentCompetitions(runs).forEach { SilentStandingsCard(it) }
        if (runs.isEmpty()) {
            Text(stringResource(R.string.history_empty))
        }
        runs.forEach { run ->
            RunHistoryCard(run, !action.busy, onSelect, handleAskRestart)
        }
    }
    restarting?.let { run ->
        AlertDialog(
            onDismissRequest = handleCancelRestart,
            title = { Text(stringResource(R.string.restart_run_title)) },
            text = { Text(stringResource(R.string.restart_run_description, run.teamName)) },
            confirmButton = {
                TextButton(onClick = handleRestart, enabled = !action.busy) {
                    Text(stringResource(R.string.restart_run_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = handleCancelRestart) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}
