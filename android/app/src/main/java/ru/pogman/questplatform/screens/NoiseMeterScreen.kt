package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.NoiseLevelCard
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.components.SectionCard
import ru.pogman.questplatform.viewmodel.NoiseMeterError
import ru.pogman.questplatform.viewmodel.NoiseMeterState
import ru.pogman.questplatform.viewmodel.NoisePhase

@Composable
fun NoiseMeterScreen(
    state: NoiseMeterState,
    permissionGranted: Boolean,
    onPermission: () -> Unit,
    onSettings: () -> Unit,
    onCalibrate: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSensitivity: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    var instructions by rememberSaveable { mutableStateOf(false) }
    val handleInstructions = { instructions = !instructions }
    ScreenContent {
        Text(stringResource(R.string.noise_setup_description))
        if (!permissionGranted) {
            SectionCard(stringResource(R.string.noise_permission_title)) {
                Text(stringResource(R.string.noise_permission_hint))
                Button(onPermission) { Text(stringResource(R.string.noise_permission_action)) }
                TextButton(onSettings) { Text(stringResource(R.string.app_settings_action)) }
            }
        }
        NoiseLevelCard(state, onReset)
        state.error?.let { error ->
            val message =
                when (error) {
                    NoiseMeterError.PERMISSION -> R.string.noise_error_permission
                    NoiseMeterError.UNAVAILABLE -> R.string.noise_error_unavailable
                    NoiseMeterError.SILENCED -> R.string.noise_error_silenced
                    NoiseMeterError.CALIBRATION_TOO_LOUD -> R.string.noise_error_calibration
                }
            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.running) {
                Button(onStop, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.noise_stop_action))
                }
            } else {
                if (state.baseline == null) Text(stringResource(R.string.noise_start_hint))
                Button(
                    onCalibrate,
                    enabled = permissionGranted,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (state.baseline == null) R.string.noise_calibrate_action
                            else R.string.noise_recalibrate_action
                        )
                    )
                }
                if (state.baseline != null) {
                    OutlinedButton(
                        onResume,
                        enabled = permissionGranted,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.noise_resume_action))
                    }
                }
            }
        }
        SectionCard(stringResource(R.string.noise_sensitivity_title)) {
            Text(stringResource(R.string.noise_sensitivity_hint))
            Slider(
                value = state.sensitivity,
                onValueChange = onSensitivity,
                steps = 8,
                enabled =
                    state.phase != NoisePhase.CALIBRATING && state.phase != NoisePhase.WARMING_UP,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.noise_sensitivity_low))
                Text(stringResource(R.string.noise_sensitivity_high))
            }
        }
        Button(
            onApply,
            enabled =
                state.baseline != null &&
                    state.phase != NoisePhase.CALIBRATING &&
                    state.phase != NoisePhase.WARMING_UP,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.silent_apply_calibration))
        }
        TextButton(handleInstructions) { Text(stringResource(R.string.noise_instructions_action)) }
        if (instructions) {
            Text(stringResource(R.string.noise_calibration_hint))
            Text(stringResource(R.string.noise_events_hint))
            Text(stringResource(R.string.noise_lifecycle_hint))
        }
    }
}
