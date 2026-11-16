package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R

@Composable
fun SilentCodePad(
    code: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
    alert: Boolean = false,
) {
    val clear = stringResource(R.string.silent_clear_code)
    val erase = stringResource(R.string.silent_erase_digit)
    SectionCard(
        stringResource(R.string.silent_code_label),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (alert) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
            ),
    ) {
        Text(code.padEnd(4, '·'), style = MaterialTheme.typography.headlineLarge)
        listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("×", "0", "⌫"),
            )
            .forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        val handleKey = {
                            onChange(
                                when (key) {
                                    "×" -> ""
                                    "⌫" -> code.dropLast(1)
                                    else -> (code + key).take(4)
                                }
                            )
                        }
                        val label =
                            when (key) {
                                "×" -> clear
                                "⌫" -> erase
                                else -> key
                            }
                        OutlinedButton(
                            handleKey,
                            enabled = enabled,
                            modifier = Modifier.weight(1f).semantics { contentDescription = label },
                        ) {
                            Text(key)
                        }
                    }
                }
            }
        Button(
            onSubmit,
            enabled = enabled && code.length == 4,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.submit_answer_action))
        }
    }
}
