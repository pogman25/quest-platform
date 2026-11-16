package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.PlayerQuest

@Composable
fun QuestSuccessContent(quest: PlayerQuest, finished: Boolean, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_check_circle),
            null,
            Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.quest_success_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
        )
        Text(
            quest.title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.earned_digit_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(quest.reward.orEmpty(), style = MaterialTheme.typography.displayLarge)
                Text(
                    stringResource(R.string.quest_position_label, quest.position),
                    textAlign = TextAlign.Center,
                )
            }
        }
        ElapsedTime(quest.openedAt, quest.solvedAt, stringResource(R.string.quest_time_label))
        Text(
            stringResource(
                if (finished) R.string.quest_success_final_hint else R.string.quest_success_hint
            ),
            textAlign = TextAlign.Center,
        )
        Button(onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (finished) R.string.view_result_action else R.string.quest_list_action
                )
            )
        }
    }
}
