package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.*

@Composable
fun ChoiceQuestEditor(
    type: QuestType,
    config: ChoiceQuestConfig,
    enabled: Boolean,
    photoUri: (String) -> String,
    onChange: ((ChoiceQuestConfig) -> ChoiceQuestConfig) -> Unit,
    onGallery: (PhotoTarget) -> Unit,
    onCamera: (PhotoTarget) -> Unit,
) {
    var cellId by rememberSaveable(type.id) { mutableStateOf(config.cells.first().id) }
    val index = config.cells.indexOfFirst { it.id == cellId }.coerceAtLeast(0)
    val cell = config.cells[index]
    val items = config.cells.map { it.forDisplay(photoUri) }
    val handleInstruction: (String) -> Unit = { value ->
        onChange { it.copy(instruction = value.take(500)) }
    }
    val handleCell: (String) -> Unit = { cellId = it }
    val handleCellChange: ((ChoiceCell) -> ChoiceCell) -> Unit = { transform ->
        onChange { current ->
            current.copy(cells = current.cells.map { if (it.id == cell.id) transform(it) else it })
        }
    }
    val handleMove: (Int) -> Unit = { direction ->
        onChange { current ->
            val cells = current.cells.toMutableList()
            val from = cells.indexOfFirst { it.id == cell.id }
            val to = from + direction
            if (from >= 0 && to in cells.indices) {
                val moved = cells.removeAt(from)
                cells.add(to, moved)
            }
            current.copy(cells = cells)
        }
    }
    val handleGallery = { onGallery(PhotoTarget(type, cell.id)) }
    val handleCamera = { onCamera(PhotoTarget(type, cell.id)) }
    val handleAnswer: (String) -> Unit = { id ->
        onChange { current ->
            current.copy(
                answers =
                    if (id in current.answers) current.answers - id
                    else if (current.answers.size < type.selectionSize) current.answers + id
                    else current.answers
            )
        }
    }
    val handleClear = { onChange { it.copy(answers = emptyList()) } }
    OutlinedTextField(
        config.instruction,
        handleInstruction,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.instruction_label)) },
        minLines = 2,
        enabled = enabled,
    )
    Text(stringResource(R.string.edit_keyboard_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.edit_keyboard_hint))
    ChoiceGrid(items, listOf(cell.id), false, enabled, handleCell)
    ChoiceCellEditor(
        cell,
        index,
        config.cells.size,
        enabled,
        handleCellChange,
        handleMove,
        handleGallery,
        handleCamera,
    )
    Text(
        stringResource(R.string.correct_answers_title),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        stringResource(
            if (type == QuestType.TREE) R.string.tree_answers_hint
            else R.string.objects_answers_hint
        )
    )
    ChoiceGrid(items, config.answers, type == QuestType.TREE, enabled, handleAnswer)
    Text(stringResource(R.string.selection_count, config.answers.size, type.selectionSize))
    TextButton(handleClear, enabled = enabled) {
        Text(stringResource(R.string.clear_selection_action))
    }
}
