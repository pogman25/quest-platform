package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.SilentQuestConfig

@Composable
fun SilentQuestEditor(
    config: SilentQuestConfig,
    enabled: Boolean,
    onChange: ((SilentQuestConfig) -> SilentQuestConfig) -> Unit,
    onCalibrate: () -> Unit,
) {
    val handleInstruction: (String) -> Unit = { value ->
        onChange { it.copy(instruction = value.take(500)) }
    }
    val handleCode: (String) -> Unit = { value ->
        onChange { it.copy(code = value.filter { it in '0'..'9' }.take(4)) }
    }
    SectionCard(stringResource(R.string.silent_target_title)) {
        OutlinedTextField(
            config.instruction,
            handleInstruction,
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.instruction_label)) },
            minLines = 3,
            enabled = enabled,
        )
        OutlinedTextField(
            config.code,
            handleCode,
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.silent_code_label)) },
            supportingText = { Text(stringResource(R.string.silent_code_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = enabled,
        )
    }
    SectionCard(stringResource(R.string.silent_alarm_settings)) {
        Text(
            stringResource(
                if (config.noise?.valid == true) R.string.silent_calibration_ready
                else R.string.silent_calibration_required
            )
        )
        config.noise?.let {
            Text(stringResource(R.string.silent_sensitivity_value, (it.sensitivity * 100).toInt()))
        }
        Button(onCalibrate, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.silent_calibrate_action))
        }
        Text(stringResource(R.string.silent_settings_hint))
    }
}
