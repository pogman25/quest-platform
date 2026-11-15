package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.QuestPhoto
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.components.SectionCard
import ru.pogman.questplatform.domain.Game
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun GameEditorScreen(
    game: Game,
    photoUri: String,
    action: ActionState,
    onChange: (Game) -> Unit,
    onChoosePhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onSave: () -> Unit,
) {
    var answers by
        rememberSaveable(game.id) { mutableStateOf(game.photo.answers.joinToString(", ")) }
    val handleTitle: (String) -> Unit = { onChange(game.copy(title = it.take(80))) }
    val handleStory: (String) -> Unit = { onChange(game.copy(story = it.take(10_000))) }
    val handleRules: (String) -> Unit = { onChange(game.copy(rules = it.take(10_000))) }
    val handleInstruction: (String) -> Unit = {
        onChange(game.copy(photo = game.photo.copy(instruction = it.take(500))))
    }
    val handleAnswers: (String) -> Unit = {
        answers = it.take(2_000)
        onChange(
            game.copy(
                photo =
                    game.photo.copy(
                        answers = answers.split(',').map(String::trim).filter(String::isNotEmpty)
                    )
            )
        )
    }

    ScreenContent {
        SectionCard(stringResource(R.string.game_settings_title)) {
            OutlinedTextField(
                game.title,
                handleTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.game_title_label)) },
                singleLine = true,
                enabled = !action.busy,
            )
            OutlinedTextField(
                game.story,
                handleStory,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.story_label)) },
                minLines = 3,
                enabled = !action.busy,
            )
            OutlinedTextField(
                game.rules,
                handleRules,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rules_label)) },
                minLines = 3,
                enabled = !action.busy,
            )
        }
        SectionCard(stringResource(R.string.photo_title)) {
            QuestPhoto(photoUri)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onChoosePhoto, enabled = !action.busy) {
                    Text(stringResource(R.string.choose_photo_action))
                }
                Button(onClick = onTakePhoto, enabled = !action.busy) {
                    Text(stringResource(R.string.take_photo_action))
                }
            }
            Text(stringResource(R.string.photo_preparation_hint))
            OutlinedTextField(
                game.photo.instruction,
                handleInstruction,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.instruction_label)) },
                minLines = 2,
                enabled = !action.busy,
            )
            OutlinedTextField(
                answers,
                handleAnswers,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.answers_label)) },
                enabled = !action.busy,
            )
            Text(stringResource(R.string.answers_hint))
        }
        ActionMessage(action)
        Button(onClick = onSave, enabled = !action.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save_game_action))
        }
    }
}
