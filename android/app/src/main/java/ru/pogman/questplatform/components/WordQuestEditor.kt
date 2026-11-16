package ru.pogman.questplatform.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlin.random.Random
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.*

@Composable
fun WordQuestEditor(
    config: WordQuestConfig,
    enabled: Boolean,
    onChange: ((WordQuestConfig) -> WordQuestConfig) -> Unit,
    loadPhoto: suspend (String) -> Bitmap,
    onExport: () -> Unit,
) {
    var selected by
        rememberSaveable(config.correctOverlay) { mutableIntStateOf(config.correctOverlay) }
    var combined by rememberSaveable { mutableStateOf(true) }
    val kit =
        remember(config.word, config.strokeSeed) {
            stencilLetters(config.word)?.let { makeStencilKit(it, config.strokeSeed) }
        }
    val handleInstruction: (String) -> Unit = { value ->
        onChange { it.copy(instruction = value.take(500)) }
    }
    val handleWord: (String) -> Unit = { value -> onChange { it.copy(word = value.take(12)) } }
    val handleWidth: (Int) -> Unit = { value -> onChange { it.copy(widthMm = value) } }
    val handleCorrect: (Int) -> Unit = { value -> onChange { it.copy(correctOverlay = value) } }
    val handleSelect: (Int) -> Unit = { selected = it }
    val handleCombined: (Boolean) -> Unit = { combined = it }
    val handleNewKit = {
        val seed = Random.nextLong(0, 0x100000000L)
        onChange { current ->
            current.copy(
                strokeSeed = if (seed == current.strokeSeed) (seed + 1) and 0xffffffffL else seed,
                strokeVersion = 3,
                correctOverlay = (1..8).filter { it != current.correctOverlay }.random(),
            )
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
        config.word,
        handleWord,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.stencil_word_label)) },
        singleLine = true,
        enabled = enabled,
    )
    Text(stringResource(R.string.stencil_word_hint))
    NumberOptions(
        stringResource(R.string.stencil_width_label),
        listOf(144, 168, 184),
        config.widthMm,
        enabled,
        " мм",
        handleWidth,
    )
    NumberOptions(
        stringResource(R.string.stencil_correct_label),
        (1..8).toList(),
        config.correctOverlay,
        enabled,
        onSelect = handleCorrect,
    )
    TextButton(handleNewKit, enabled = enabled && kit != null) {
        Text(stringResource(R.string.stencil_new_kit))
    }
    if (kit != null) {
        Text(stringResource(R.string.stencil_card_a))
        PrintPreview(stencilPreview(config, kit, selected, false, 0), loadPhoto)
        Text(stringResource(R.string.stencil_card_b))
        PrintPreview(stencilPreview(config, kit, selected, false, 1), loadPhoto)
        NumberOptions(
            stringResource(R.string.stencil_preview_label),
            (1..8).toList(),
            selected,
            true,
            onSelect = handleSelect,
        )
        Row {
            Checkbox(combined, handleCombined)
            Text(stringResource(R.string.stencil_combine))
        }
        PrintPreview(stencilPreview(config, kit, selected, combined), loadPhoto)
        Text(
            stringResource(
                if (combined && selected == config.correctOverlay) R.string.stencil_matching_hint
                else R.string.stencil_compare_hint
            )
        )
    }
    Text(stringResource(R.string.stencil_print_hint))
    Button(onExport, enabled = enabled && kit != null, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.download_pdf_action))
    }
}
