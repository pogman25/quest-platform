package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.PlayerQuest
import ru.pogman.questplatform.domain.QuestType
import ru.pogman.questplatform.theme.OnSolvedContainer
import ru.pogman.questplatform.theme.QuestTheme
import ru.pogman.questplatform.theme.SolvedContainer

@Composable
fun PlayerQuestCard(quest: PlayerQuest, enabled: Boolean, onQuest: (QuestType) -> Unit) {
    val handleQuest = { onQuest(quest.type) }
    SectionCard(
        title = "${quest.position}. ${quest.title}",
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (quest.solvedAt != null) SolvedContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                contentColor =
                    if (quest.solvedAt != null) OnSolvedContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (quest.solvedAt != null)
                Icon(painterResource(R.drawable.ic_check_circle), null, Modifier.size(20.dp))
            Text(
                stringResource(
                    when {
                        quest.solvedAt != null -> R.string.quest_solved
                        quest.openedAt != null -> R.string.quest_in_progress
                        else -> R.string.quest_ready
                    }
                ),
                modifier = Modifier.weight(1f),
            )
        }
        quest.reward?.let { Text(stringResource(R.string.quest_reward, quest.position, it)) }
        if (quest.solvedAt != null) {
            OutlinedButton(handleQuest, enabled = enabled) {
                Text(stringResource(R.string.view_quest_action))
            }
        } else {
            Button(handleQuest, enabled = enabled) {
                Text(stringResource(R.string.open_quest_action))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PlayerQuestCardPreview() {
    QuestTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PlayerQuestCard(
                PlayerQuest(QuestType.OBJECTS, 1, "", null, emptyList(), null, null, null),
                true,
                {},
            )
            PlayerQuestCard(
                PlayerQuest(QuestType.PHOTO, 2, "", null, emptyList(), 1_000L, 2_000L, "0"),
                true,
                {},
            )
        }
    }
}
