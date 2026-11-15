package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.ElapsedTime
import ru.pogman.questplatform.components.QuestPhoto
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.domain.PlayerRun
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun PhotoQuestScreen(
    run: PlayerRun,
    action: ActionState,
    incorrect: Boolean,
    onSubmit: (String) -> Unit,
    onResult: () -> Unit,
) {
    var answer by rememberSaveable(run.id) { mutableStateOf("") }
    val handleAnswer: (String) -> Unit = { answer = it.take(500) }
    val handleSubmit = { onSubmit(answer) }

    ScreenContent {
        Text(run.quest.title, style = MaterialTheme.typography.headlineLarge)
        ElapsedTime(
            run.quest.openedAt,
            run.quest.solvedAt,
            stringResource(R.string.quest_time_label),
        )
        QuestPhoto(run.quest.photoUri)
        Text(run.quest.instruction, style = MaterialTheme.typography.bodyLarge)
        if (run.quest.reward == null) {
            OutlinedTextField(
                answer,
                handleAnswer,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.player_answer_label)) },
                enabled = !action.busy,
                singleLine = true,
            )
            if (incorrect) {
                Text(
                    stringResource(R.string.incorrect_answer),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            ActionMessage(action)
            Button(
                onClick = handleSubmit,
                enabled = !action.busy && answer.isNotBlank() && run.quest.openedAt != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.submit_answer_action))
            }
        } else {
            Text(run.quest.reward, style = MaterialTheme.typography.headlineLarge)
            Button(onClick = onResult) { Text(stringResource(R.string.results_action)) }
        }
    }
}
