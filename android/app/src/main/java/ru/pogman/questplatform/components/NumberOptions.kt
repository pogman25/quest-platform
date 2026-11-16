package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun NumberOptions(
    label: String,
    values: List<Int>,
    selected: Int,
    enabled: Boolean,
    suffix: String = "",
    onSelect: (Int) -> Unit,
) {
    Text(label)
    FlowRow {
        values.forEach { value ->
            val handleSelect = { onSelect(value) }
            FilterChip(
                selected == value,
                handleSelect,
                label = { Text("$value$suffix") },
                enabled = enabled,
            )
        }
    }
}
