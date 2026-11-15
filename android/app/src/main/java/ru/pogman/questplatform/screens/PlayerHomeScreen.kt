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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.CaseIntroduction
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.theme.QuestTheme

@Composable
fun PlayerHomeScreen(onInstructions: () -> Unit) {
    ScreenContent {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.player_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.player_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        CaseIntroduction()

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.game_waiting_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.game_waiting_description), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Button(onClick = onInstructions, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.instructions_action), modifier = Modifier.padding(vertical = 6.dp))
        }

        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun PlayerHomePreview() {
    QuestTheme {
        PlayerHomeScreen(onInstructions = {})
    }
}
