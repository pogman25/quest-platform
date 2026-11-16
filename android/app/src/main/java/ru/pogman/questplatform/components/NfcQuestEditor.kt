package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.util.UUID
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.*

@Composable
fun NfcQuestEditor(
    config: NfcQuestConfig,
    enabled: Boolean,
    onChange: ((NfcQuestConfig) -> NfcQuestConfig) -> Unit,
    onWrite: (String) -> Unit,
) {
    val handleInstruction: (String) -> Unit = { value ->
        onChange { it.copy(instruction = value.take(500)) }
    }
    val handleAdd = {
        val tag =
            NfcTagConfig(UUID.randomUUID().toString(), "Новая метка", UUID.randomUUID().toString())
        onChange { it.copy(tags = it.tags + tag) }
    }
    OutlinedTextField(
        config.instruction,
        handleInstruction,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.instruction_label)) },
        minLines = 2,
        enabled = enabled,
    )
    Text(stringResource(R.string.nfc_setup_hint))
    config.tags.forEach { tag ->
        val handleCorrect = { onChange { it.copy(correctId = tag.id) } }
        val handleLabel: (String) -> Unit = { value ->
            onChange { current ->
                current.copy(
                    tags =
                        current.tags.map {
                            if (it.id == tag.id) it.copy(label = value.take(80)) else it
                        }
                )
            }
        }
        val handleToken: (String) -> Unit = { value ->
            onChange { current ->
                current.copy(
                    tags =
                        current.tags.map {
                            if (it.id == tag.id) it.copy(token = value.take(512)) else it
                        }
                )
            }
        }
        val handleWrite = { onWrite(tag.id) }
        val handleDelete = {
            onChange { current ->
                val remaining = current.tags.filter { it.id != tag.id }
                if (remaining.isEmpty()) current
                else
                    current.copy(
                        tags = remaining,
                        correctId =
                            if (current.correctId == tag.id) remaining.first().id
                            else current.correctId,
                    )
            }
        }
        Row {
            RadioButton(
                selected = config.correctId == tag.id,
                onClick = handleCorrect,
                enabled = enabled,
            )
            OutlinedTextField(
                tag.label,
                handleLabel,
                Modifier.weight(1f),
                label = { Text(stringResource(R.string.nfc_tag_label)) },
                singleLine = true,
                enabled = enabled,
            )
        }
        OutlinedTextField(
            tag.token,
            handleToken,
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.nfc_token_label)) },
            singleLine = true,
            enabled = enabled,
        )
        FlowRow {
            TextButton(onClick = handleWrite, enabled = enabled) {
                Text(stringResource(R.string.nfc_write_action))
            }
            CopyTokenButton(tag.token)
            TextButton(onClick = handleDelete, enabled = enabled && config.tags.size > 1) {
                Text(stringResource(R.string.delete_action))
            }
        }
    }
    TextButton(onClick = handleAdd, enabled = enabled && config.tags.size < 12) {
        Text(stringResource(R.string.nfc_add_tag_action))
    }
}
