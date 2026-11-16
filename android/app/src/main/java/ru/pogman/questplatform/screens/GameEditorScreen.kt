package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.*
import ru.pogman.questplatform.domain.*
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun GameEditorScreen(
    game: Game,
    photoUri: (String) -> String,
    action: ActionState,
    onChange: ((Game) -> Game) -> Unit,
    onToggleQuest: (QuestType) -> Unit,
    onChoosePhoto: (QuestType) -> Unit,
    onTakePhoto: (QuestType) -> Unit,
    onWriteNfc: (String) -> Unit,
    onSave: () -> Unit,
) {
    val handleTitle: (String) -> Unit = { value -> onChange { it.copy(title = value.take(80)) } }
    val handleStory: (String) -> Unit = { value ->
        onChange { it.copy(story = value.take(10_000)) }
    }
    val handleRules: (String) -> Unit = { value ->
        onChange { it.copy(rules = value.take(10_000)) }
    }
    ScreenContent {
        SectionCard(stringResource(R.string.game_settings_title)) {
            OutlinedTextField(
                game.title,
                handleTitle,
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.game_title_label)) },
                singleLine = true,
                enabled = !action.busy,
            )
            OutlinedTextField(
                game.story,
                handleStory,
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.story_label)) },
                minLines = 3,
                enabled = !action.busy,
            )
            OutlinedTextField(
                game.rules,
                handleRules,
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rules_label)) },
                minLines = 3,
                enabled = !action.busy,
            )
        }
        SectionCard(stringResource(R.string.quest_selection_title)) {
            Text(stringResource(R.string.quest_selection_hint))
            QuestType.entries.forEach { type ->
                val position = game.quests.indexOfFirst { it.type == type }
                val handleToggle: (Boolean) -> Unit = { onToggleQuest(type) }
                Row {
                    Checkbox(
                        checked = position >= 0,
                        onCheckedChange = handleToggle,
                        enabled = !action.busy && (position < 0 || game.quests.size > 1),
                    )
                    Text(if (position >= 0) "${position + 1}. ${type.title}" else type.title)
                }
            }
        }
        game.quests.forEachIndexed { index, quest ->
            key(quest.type.id) {
                val handleQuestChange: ((QuestConfig) -> QuestConfig) -> Unit = { transform ->
                    onChange { it.updateQuest(quest.type, transform) }
                }
                SectionCard("${index + 1}. ${quest.type.title}") {
                    QuestEditor(
                        game.id,
                        quest,
                        !action.busy,
                        photoUri,
                        handleQuestChange,
                        onChoosePhoto,
                        onTakePhoto,
                        onWriteNfc,
                    )
                }
            }
        }
        ActionMessage(action)
        Text(stringResource(R.string.game_save_hint))
        Button(onClick = onSave, enabled = !action.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save_game_action))
        }
    }
}
