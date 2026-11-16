package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.data.MicrophoneInput
import ru.pogman.questplatform.viewmodel.NoiseMeterState
import ru.pogman.questplatform.viewmodel.NoisePhase

@Composable
fun NoiseLevelCard(state: NoiseMeterState, onReset: () -> Unit) {
    var details by remember { mutableStateOf(false) }
    val handleDetails = { details = !details }
    val baseline = state.baseline
    val level = state.level
    val threshold = baseline?.thresholds(state.sensitivity)
    val progress =
        if (level == null) 0f
        else if (baseline == null) ((level.rmsDbfs + 80.0) / 80.0).toFloat().coerceIn(0f, 1f)
        else (baseline.relativeLevel(level, state.sensitivity) * 0.8f).coerceIn(0f, 1f)
    val status =
        when (state.phase) {
            NoisePhase.IDLE -> R.string.noise_status_idle
            NoisePhase.PAUSED -> R.string.noise_status_paused
            NoisePhase.WARMING_UP -> R.string.noise_status_warming
            NoisePhase.CALIBRATING -> R.string.noise_status_calibrating
            NoisePhase.MONITORING ->
                if (state.alert) R.string.noise_status_alert else R.string.noise_status_quiet
        }

    SectionCard(
        stringResource(R.string.noise_level_title),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (state.alert) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
            ),
    ) {
        Text(stringResource(status), style = MaterialTheme.typography.headlineSmall)
        NoiseGauge(progress, state.alert, threshold = baseline != null)
        if (state.phase == NoisePhase.CALIBRATING) {
            LinearProgressIndicator(
                progress = { state.calibrationProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(
                    R.string.noise_calibration_progress,
                    (state.calibrationProgress * 100).toInt(),
                )
            )
        } else if (baseline != null) Text(stringResource(R.string.noise_threshold_hint))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.noise_events_title))
                Text(state.events.toString(), style = MaterialTheme.typography.displaySmall)
            }
            TextButton(onReset, enabled = state.events > 0) {
                Text(stringResource(R.string.noise_reset_action))
            }
        }
        TextButton(handleDetails) { Text(stringResource(R.string.noise_readings_action)) }
        if (details) {
            Text(stringResource(R.string.noise_units_hint))
            if (level != null)
                Text(stringResource(R.string.noise_signal_reading, level.rmsDbfs, level.peakDbfs))
            if (baseline != null && threshold != null) {
                Text(
                    stringResource(
                        R.string.noise_baseline_reading,
                        baseline.rmsDbfs,
                        baseline.peakDbfs,
                    )
                )
                Text(
                    stringResource(
                        R.string.noise_threshold_reading,
                        threshold.rmsDbfs,
                        threshold.peakDbfs,
                    )
                )
            }
            state.input?.let { input ->
                Text(
                    stringResource(
                        if (input == MicrophoneInput.UNPROCESSED) R.string.noise_input_unprocessed
                        else R.string.noise_input_voice
                    )
                )
            }
        }
    }
}
