package ru.pogman.questplatform.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun InstructionStep(number: Int, @StringRes title: Int, @StringRes description: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
            Text(number.toString(), modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(description), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
