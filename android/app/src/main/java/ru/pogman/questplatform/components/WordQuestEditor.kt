package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.WordQuestConfig

@Composable
fun WordQuestEditor(
    config: WordQuestConfig,
    enabled: Boolean,
    onChange: ((WordQuestConfig) -> WordQuestConfig) -> Unit,
) {
    val handleInstruction: (String) -> Unit = { value ->
        onChange { it.copy(instruction = value.take(500)) }
    }
    val handleWord: (String) -> Unit = { value -> onChange { it.copy(word = value.take(12)) } }
    OutlinedTextField(
        config.instruction,
        handleInstruction,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.instruction_label)) },
        minLines = 2,
        enabled = enabled,
    )
    OutlinedTextField(
        config.word,
        handleWord,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.stencil_word_label)) },
        singleLine = true,
        enabled = enabled,
    )
    Text(stringResource(R.string.stencil_word_hint))
}
