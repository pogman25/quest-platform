package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.InstructionStep
import ru.pogman.questplatform.components.ScreenContent

@Composable
fun InstructionsScreen(onReturn: () -> Unit) {
    ScreenContent {
        Text(stringResource(R.string.instructions_intro), style = MaterialTheme.typography.headlineMedium)

        InstructionStep(1, R.string.instructions_search_title, R.string.instructions_search_description)
        InstructionStep(2, R.string.instructions_solve_title, R.string.instructions_solve_description)
        InstructionStep(3, R.string.instructions_finish_title, R.string.instructions_finish_description)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.instructions_timing_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.instructions_timing_description), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Button(onClick = onReturn, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.instructions_return_action), modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}
