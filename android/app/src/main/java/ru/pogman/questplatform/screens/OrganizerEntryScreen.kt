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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ScreenContent

@Composable
fun OrganizerEntryScreen(onReturn: () -> Unit) {
    ScreenContent {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.organizer_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(stringResource(R.string.organizer_heading), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.organizer_description), style = MaterialTheme.typography.bodyLarge)
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.organizer_setup_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.organizer_setup_description), style = MaterialTheme.typography.bodyLarge)
                // Password setup is implemented in stage 2; do not provide a mock admin bypass.
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.organizer_setup_action))
                }
            }
        }

        Text(
            stringResource(R.string.organizer_privacy_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TextButton(onClick = onReturn, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.organizer_return_action))
        }
    }
}
