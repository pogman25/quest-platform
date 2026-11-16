package ru.pogman.questplatform.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.pogman.questplatform.components.SilentSearchPanel
import ru.pogman.questplatform.domain.PlayerQuest
import ru.pogman.questplatform.viewmodel.SilentSearchState
import ru.pogman.questplatform.viewmodel.SilentSearchViewModel

@Composable
fun SilentSearchContent(
    viewModel: SilentSearchViewModel,
    runId: String,
    quest: PlayerQuest,
    active: Boolean,
) {
    val value by viewModel.state.collectAsStateWithLifecycle()
    val state =
        value.takeIf { it.runId == runId }?.copy(events = maxOf(value.events, quest.noiseCount))
            ?: SilentSearchState(
                runId = runId,
                settings = quest.noiseSettings,
                events = quest.noiseCount,
            )
    var code by rememberSaveable(runId) { mutableStateOf("") }
    val access = rememberMicrophoneAccess(active, state.running, viewModel::stop)
    LaunchedEffect(runId) { viewModel.bind(runId, quest) }
    val handleStart = { if (access.ready) viewModel.start() }
    val handleCode: (String) -> Unit = { code = it }
    val handleSubmit = { if (access.ready) viewModel.submit(code) }
    SilentSearchPanel(
        state,
        access.granted,
        code,
        handleCode,
        handleStart,
        viewModel::stop,
        handleSubmit,
        access.request,
        access.settings,
    )
}
