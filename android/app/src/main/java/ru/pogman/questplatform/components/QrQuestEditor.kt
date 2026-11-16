package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.UUID
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.*

@Composable
fun QrQuestEditor(
    config: QrQuestConfig,
    photoUri: String,
    enabled: Boolean,
    onChange: ((QrQuestConfig) -> QrQuestConfig) -> Unit,
    onGallery: (QuestType) -> Unit,
    onCamera: (QuestType) -> Unit,
) {
    val handleGallery = { onGallery(QuestType.QR) }
    val handleCamera = { onCamera(QuestType.QR) }
    val handleInstruction: (String) -> Unit = { value ->
        onChange { it.copy(instruction = value.take(500)) }
    }
    val handleToken: (String) -> Unit = { value -> onChange { it.copy(token = value.take(512)) } }
    val handleNewToken = {
        val token = UUID.randomUUID().toString()
        onChange { it.copy(token = token) }
    }
    QuestPhoto(photoUri)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = handleGallery, enabled = enabled) {
            Text(stringResource(R.string.choose_photo_action))
        }
        Button(onClick = handleCamera, enabled = enabled) {
            Text(stringResource(R.string.take_photo_action))
        }
    }
    OutlinedTextField(
        config.instruction,
        handleInstruction,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.instruction_label)) },
        minLines = 2,
        enabled = enabled,
    )
    OutlinedTextField(
        config.token,
        handleToken,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.qr_token_label)) },
        singleLine = true,
        enabled = enabled,
    )
    QrCodeImage(config.token)
    CopyTokenButton(config.token)
    TextButton(onClick = handleNewToken, enabled = enabled) {
        Text(stringResource(R.string.new_token_action))
    }
    Text(stringResource(R.string.qr_token_hint))
}
