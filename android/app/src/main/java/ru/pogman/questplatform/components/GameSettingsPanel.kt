package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.*
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun GameSettingsPanel(
    game: Game,
    action: ActionState,
    onChange: ((Game) -> Game) -> Unit,
    onToggleQuest: (QuestType) -> Unit,
    onOpenQuest: (QuestType) -> Unit,
    onMoveQuest: (QuestType, Int) -> Unit,
    onSave: () -> Unit,
) {
    var details by rememberSaveable(game.id) { mutableStateOf(false) }
    val handleDetails = { details = !details }
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
            TextButton(handleDetails) { Text(stringResource(R.string.briefing_action)) }
            if (details) {
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
        }
        Text(
            stringResource(R.string.quest_selection_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(stringResource(R.string.quest_setup_hint))
        game.quests.forEachIndexed { index, quest ->
            val handleOpen = { onOpenQuest(quest.type) }
            val handleRemove = { onToggleQuest(quest.type) }
            val handleMove: (Int) -> Unit = { onMoveQuest(quest.type, it) }
            SectionCard("${index + 1}. ${quest.type.title}") {
                Text(quest.config.instruction, maxLines = 2)
                OrderButtons(index, game.quests.size, !action.busy, handleMove)
                FlowRow {
                    Button(handleOpen, enabled = !action.busy) {
                        Text(stringResource(R.string.configure_quest_action))
                    }
                    TextButton(handleRemove, enabled = !action.busy && game.quests.size > 1) {
                        Text(stringResource(R.string.remove_quest_action))
                    }
                }
            }
        }
        val available = QuestType.entries.filter { type -> game.quests.none { it.type == type } }
        if (available.isNotEmpty())
            SectionCard(stringResource(R.string.add_quest_title)) {
                available.forEach { type ->
                    val handleAdd = { onToggleQuest(type) }
                    OutlinedButton(
                        handleAdd,
                        enabled = !action.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(type.title)
                    }
                }
            }
        ActionMessage(action)
        Text(stringResource(R.string.game_save_hint))
        Button(onSave, enabled = !action.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save_game_action))
        }
    }
}
