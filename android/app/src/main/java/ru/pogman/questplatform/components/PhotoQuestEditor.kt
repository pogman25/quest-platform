package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.*

@Composable
fun PhotoQuestEditor(
    gameId: String,
    config: PhotoQuestConfig,
    photoUri: String,
    enabled: Boolean,
    onChange: ((PhotoQuestConfig) -> PhotoQuestConfig) -> Unit,
    onGallery: (QuestType) -> Unit,
    onCamera: (QuestType) -> Unit,
) {
    var answers by rememberSaveable(gameId) { mutableStateOf(config.answers.joinToString(", ")) }
    val handleGallery = { onGallery(QuestType.PHOTO) }
    val handleCamera = { onCamera(QuestType.PHOTO) }
    val handleInstruction: (String) -> Unit = { value ->
        onChange { it.copy(instruction = value.take(500)) }
    }
    val handleAnswers: (String) -> Unit = { value ->
        answers = value.take(2_000)
        val accepted = answers.split(',').map(String::trim).filter(String::isNotEmpty)
        onChange { it.copy(answers = accepted) }
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
    Text(stringResource(R.string.photo_preparation_hint))
    OutlinedTextField(
        config.instruction,
        handleInstruction,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.instruction_label)) },
        minLines = 2,
        enabled = enabled,
    )
    OutlinedTextField(
        answers,
        handleAnswers,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.answers_label)) },
        enabled = enabled,
    )
    Text(stringResource(R.string.answers_hint))
}
