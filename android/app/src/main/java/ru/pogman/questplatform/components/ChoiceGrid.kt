package ru.pogman.questplatform.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pogman.questplatform.domain.ChoiceCell

@Composable
fun ChoiceGrid(
    cells: List<ChoiceCell>,
    selected: List<String>,
    ordered: Boolean,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cell ->
                    val index = selected.indexOf(cell.id)
                    val handleSelect = { onSelect(cell.id) }
                    Card(
                        onClick = handleSelect,
                        enabled = enabled,
                        modifier =
                            Modifier.weight(1f).heightIn(min = 104.dp).semantics {
                                this.selected = index >= 0
                            },
                        border =
                            BorderStroke(
                                if (index >= 0) 2.dp else 1.dp,
                                if (index >= 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (index >= 0) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface
                            ),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                if (index >= 0 && ordered) "${index + 1} · ${cell.symbol}"
                                else cell.symbol,
                                fontSize = 28.sp,
                            )
                            Text(
                                cell.label,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
