package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.*

@Composable
fun ChoiceQuestEditor(
    type: QuestType,
    config: ChoiceQuestConfig,
    enabled: Boolean,
    onChange: ((ChoiceQuestConfig) -> ChoiceQuestConfig) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val handleExpand = { expanded = !expanded }
    val handleInstruction: (String) -> Unit = { value ->
        onChange { it.copy(instruction = value.take(500)) }
    }
    val handleSelect: (String) -> Unit = { id ->
        onChange { current ->
            val selected =
                if (id in current.answers) current.answers - id
                else if (current.answers.size < type.selectionSize) current.answers + id
                else current.answers
            current.copy(answers = selected)
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
    Text(
        stringResource(
            if (type == QuestType.TREE) R.string.tree_answers_hint
            else R.string.objects_answers_hint
        )
    )
    ChoiceGrid(config.cells, config.answers, type == QuestType.TREE, enabled, handleSelect)
    Text(stringResource(R.string.selection_count, config.answers.size, type.selectionSize))
    TextButton(onClick = handleClear, enabled = enabled) {
        Text(stringResource(R.string.clear_selection_action))
    }
    TextButton(onClick = handleExpand) { Text(stringResource(R.string.edit_cells_action)) }
    if (expanded)
        config.cells.forEach { cell ->
            val handleLabel: (String) -> Unit = { value ->
                onChange { current ->
                    current.copy(
                        cells =
                            current.cells.map {
                                if (it.id == cell.id) it.copy(label = value.take(40)) else it
                            }
                    )
                }
            }
            val handleSymbol: (String) -> Unit = { value ->
                onChange { current ->
                    current.copy(
                        cells =
                            current.cells.map {
                                if (it.id == cell.id) it.copy(symbol = value.take(16)) else it
                            }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    cell.symbol,
                    handleSymbol,
                    Modifier.weight(1f),
                    label = { Text(stringResource(R.string.cell_symbol_label)) },
                    singleLine = true,
                    enabled = enabled,
                )
                OutlinedTextField(
                    cell.label,
                    handleLabel,
                    Modifier.weight(2f),
                    label = { Text(stringResource(R.string.cell_label)) },
                    singleLine = true,
                    enabled = enabled,
                )
            }
        }
}
