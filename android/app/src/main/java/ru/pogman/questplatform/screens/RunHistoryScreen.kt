package ru.pogman.questplatform.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.RunHistoryCard
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.domain.RunSummary
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun RunHistoryScreen(runs: List<RunSummary>, action: ActionState, onSelect: (String) -> Unit) {
    ScreenContent {
        ActionMessage(action)
        if (runs.isEmpty()) {
            Text(stringResource(R.string.history_empty))
        }
        runs.forEach { run ->
            RunHistoryCard(run, !action.busy, onSelect)
        }
    }
}
