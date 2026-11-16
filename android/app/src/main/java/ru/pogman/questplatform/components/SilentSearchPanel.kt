package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.AnswerVerdict
import ru.pogman.questplatform.viewmodel.*

@Composable
fun SilentSearchPanel(
    state: SilentSearchState,
    granted: Boolean,
    code: String,
    onCode: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSubmit: () -> Unit,
    onPermission: () -> Unit,
    onSettings: () -> Unit,
) {
    val progress =
        state.level?.let { level ->
            state.settings?.let { it.baseline.relativeLevel(level, it.sensitivity) * 0.8f }
        } ?: 0f
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (!granted) {
            Text(stringResource(R.string.noise_permission_hint))
            Button(onPermission) { Text(stringResource(R.string.noise_permission_action)) }
            TextButton(onSettings) { Text(stringResource(R.string.app_settings_action)) }
        }
        SectionCard(
            stringResource(R.string.silent_alarm_title),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (state.alert) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                ),
        ) {
            Text(
                stringResource(
                    when {
                        state.alert -> R.string.noise_status_alert
                        state.phase == NoisePhase.WARMING_UP -> R.string.noise_status_warming
                        state.phase == NoisePhase.MONITORING -> R.string.noise_status_quiet
                        else -> R.string.noise_status_paused
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            NoiseGauge(progress, state.alert)
            Text(
                stringResource(R.string.silent_noise_count, state.events),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(stringResource(R.string.silent_player_hint))
        }
        state.microphoneError?.let { error ->
            Text(
                stringResource(
                    when (error) {
                        NoiseMeterError.PERMISSION -> R.string.noise_error_permission
                        NoiseMeterError.SILENCED -> R.string.noise_error_silenced
                        NoiseMeterError.UNAVAILABLE -> R.string.noise_error_unavailable
                        NoiseMeterError.CALIBRATION_TOO_LOUD -> R.string.silent_calibration_required
                    }
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }
        ActionMessage(ActionState(busy = state.submitting, error = state.failure))
        if (state.running)
            OutlinedButton(
                onStop,
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.noise_stop_action))
            }
        else
            Button(onStart, enabled = granted, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.silent_start_action))
            }
        SilentCodePad(
            code,
            state.phase == NoisePhase.MONITORING && !state.submitting,
            onCode,
            onSubmit,
            alert = state.alert,
        )
        if (state.verdict == AnswerVerdict.INCORRECT)
            Text(
                stringResource(R.string.silent_wrong_code),
                color = MaterialTheme.colorScheme.error,
            )
    }
}
