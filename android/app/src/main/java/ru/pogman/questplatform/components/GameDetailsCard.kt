package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.Game

@Composable
fun GameDetailsCard(game: Game, enabled: Boolean, onChange: ((Game) -> Game) -> Unit) {
    var details by rememberSaveable(game.id) { mutableStateOf(false) }
    val handleDetails = { details = !details }
    val handleTitle: (String) -> Unit = { value -> onChange { it.copy(title = value.take(80)) } }
    val handleStory: (String) -> Unit = { value ->
        onChange { it.copy(story = value.take(10_000)) }
    }
    val handleRules: (String) -> Unit = { value ->
        onChange { it.copy(rules = value.take(10_000)) }
    }

    SectionCard(stringResource(R.string.game_settings_title)) {
        OutlinedTextField(
            game.title,
            handleTitle,
            Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.game_title_label)) },
            singleLine = true,
            enabled = enabled,
        )
        TextButton(handleDetails) { Text(stringResource(R.string.briefing_action)) }
        if (details) {
            OutlinedTextField(
                game.story,
                handleStory,
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.story_label)) },
                minLines = 3,
                enabled = enabled,
            )
            OutlinedTextField(
                game.rules,
                handleRules,
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rules_label)) },
                minLines = 3,
                enabled = enabled,
            )
        }
    }
}
