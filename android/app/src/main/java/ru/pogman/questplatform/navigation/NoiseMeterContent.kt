package ru.pogman.questplatform.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.pogman.questplatform.domain.NoiseSettings
import ru.pogman.questplatform.screens.NoiseMeterScreen
import ru.pogman.questplatform.viewmodel.NoiseMeterViewModel

@Composable
fun NoiseMeterContent(
    viewModel: NoiseMeterViewModel,
    active: Boolean,
    configurationKey: String,
    initialSettings: NoiseSettings?,
    onApply: (NoiseSettings) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val access = rememberMicrophoneAccess(active, state.running, viewModel::stop)
    LaunchedEffect(configurationKey) {
        viewModel.configure(initialSettings)
    }
    val handleCalibrate = { if (access.ready) viewModel.calibrate() }
    val handleResume = { if (access.ready) viewModel.resume() }
    val handleApply = {
        val baseline = state.baseline
        if (baseline != null) {
            viewModel.stop()
            onApply(NoiseSettings(baseline, state.sensitivity))
        }
        Unit
    }
    NoiseMeterScreen(
        state,
        access.granted,
        access.request,
        access.settings,
        handleCalibrate,
        handleResume,
        viewModel::stop,
        viewModel::setSensitivity,
        viewModel::resetCounter,
        handleApply,
    )
}
