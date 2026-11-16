package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.*

@Composable
fun ChoiceCellEditor(
    cell: ChoiceCell,
    index: Int,
    count: Int,
    enabled: Boolean,
    onChange: ((ChoiceCell) -> ChoiceCell) -> Unit,
    onMove: (Int) -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
) {
    var catalog by rememberSaveable(cell.id) { mutableStateOf(false) }
    val handleCatalog = { catalog = true }
    val handleDismiss = { catalog = false }
    val handleLabel: (String) -> Unit = { value -> onChange { it.copy(label = value.take(40)) } }
    val handleSymbol: (String) -> Unit = { value ->
        onChange { it.copy(symbol = value.take(16), photoRef = null) }
    }
    val handleReset = { onChange { it.copy(photoRef = null) } }
    SectionCard(stringResource(R.string.cell_number, index + 1)) {
        OrderButtons(index, count, enabled, onMove)
        OutlinedTextField(
            cell.label,
            handleLabel,
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.cell_label)) },
            enabled = enabled,
            singleLine = true,
        )
        Button(handleCatalog, enabled = enabled) {
            Text(stringResource(R.string.choose_catalog_item))
        }
        OutlinedTextField(
            cell.symbol,
            handleSymbol,
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.custom_symbol_label)) },
            enabled = enabled,
            singleLine = true,
        )
        FlowRow {
            TextButton(onGallery, enabled = enabled) {
                Text(stringResource(R.string.choose_photo_action))
            }
            TextButton(onCamera, enabled = enabled) {
                Text(stringResource(R.string.take_photo_action))
            }
            if (cell.photoRef != null)
                TextButton(handleReset, enabled = enabled) {
                    Text(stringResource(R.string.restore_symbol_action))
                }
        }
        Text(stringResource(R.string.custom_item_hint))
    }
    if (catalog)
        AlertDialog(
            onDismissRequest = handleDismiss,
            title = { Text(stringResource(R.string.choose_catalog_item)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(choiceCatalog, key = { it.label }) { item ->
                        val handleChoose = {
                            onChange {
                                it.copy(label = item.label, symbol = item.symbol, photoRef = null)
                            }
                            catalog = false
                        }
                        TextButton(handleChoose, Modifier.fillMaxWidth()) {
                            Text("${item.symbol}  ${item.label}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(handleDismiss) { Text(stringResource(R.string.cancel_action)) }
            },
        )
}
