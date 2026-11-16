package ru.pogman.questplatform.screens

import android.nfc.Tag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.*
import ru.pogman.questplatform.domain.*
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun QuestScreen(
    runId: String,
    quest: PlayerQuest,
    finished: Boolean,
    active: Boolean,
    action: ActionState,
    feedback: AnswerVerdict?,
    onSubmit: (QuestAnswer) -> Unit,
    onNfc: (Tag) -> Unit,
    onScannerFailure: () -> Unit,
    onReturn: () -> Unit,
    onResult: () -> Unit,
) {
    var text by rememberSaveable(runId, quest.type.id) { mutableStateOf("") }
    var selected by rememberSaveable(runId, quest.type.id) { mutableStateOf(arrayListOf<String>()) }
    val handleText: (String) -> Unit = { text = it.take(500) }
    val handleTextSubmit = { onSubmit(QuestAnswer.Text(text)) }
    val handleToken: (String) -> Unit = { onSubmit(QuestAnswer.Token(it)) }
    val handleSelect: (String) -> Unit = { id ->
        selected =
            ArrayList(
                if (id in selected) selected - id
                else if (selected.size < quest.type.selectionSize) selected + id else selected
            )
    }
    val handleSelectionSubmit = { onSubmit(QuestAnswer.Selection(selected.toList())) }
    val handleClear = { selected = arrayListOf() }
    ScreenContent {
        Text(quest.title, style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.quest_position_label, quest.position))
        ElapsedTime(quest.openedAt, quest.solvedAt, stringResource(R.string.quest_time_label))
        quest.photoUri?.let { QuestPhoto(it) }
        Text(quest.instruction, style = MaterialTheme.typography.bodyLarge)
        if (quest.reward != null) {
            Text(
                stringResource(R.string.quest_reward, quest.position, quest.reward),
                style = MaterialTheme.typography.headlineMedium,
            )
            Button(onClick = if (finished) onResult else onReturn) {
                Text(
                    stringResource(
                        if (finished) R.string.results_action else R.string.quest_list_action
                    )
                )
            }
        } else {
            when (quest.type) {
                QuestType.PHOTO,
                QuestType.STENCIL -> {
                    OutlinedTextField(
                        text,
                        handleText,
                        Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                stringResource(
                                    if (quest.type == QuestType.PHOTO) R.string.player_answer_label
                                    else R.string.player_word_label
                                )
                            )
                        },
                        enabled = !action.busy,
                        singleLine = true,
                    )
                    Button(
                        onClick = handleTextSubmit,
                        enabled = !action.busy && text.isNotBlank() && quest.openedAt != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.submit_answer_action))
                    }
                }
                QuestType.OBJECTS,
                QuestType.TREE -> {
                    ChoiceGrid(
                        quest.cells,
                        selected,
                        quest.type == QuestType.TREE,
                        !action.busy,
                        handleSelect,
                    )
                    Text(
                        stringResource(
                            R.string.selection_count,
                            selected.size,
                            quest.type.selectionSize,
                        )
                    )
                    TextButton(onClick = handleClear, enabled = !action.busy) {
                        Text(stringResource(R.string.clear_selection_action))
                    }
                    Button(
                        onClick = handleSelectionSubmit,
                        enabled =
                            !action.busy &&
                                selected.size == quest.type.selectionSize &&
                                quest.openedAt != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.submit_answer_action))
                    }
                }
                QuestType.NFC -> NfcReader(active, onNfc)
                QuestType.QR -> QrScanner(active, action.busy, handleToken, onScannerFailure)
            }
            when (feedback) {
                AnswerVerdict.INCORRECT ->
                    Text(
                        stringResource(R.string.incorrect_answer),
                        color = MaterialTheme.colorScheme.error,
                    )
                AnswerVerdict.UNKNOWN_TAG ->
                    Text(
                        stringResource(R.string.nfc_unknown_tag),
                        color = MaterialTheme.colorScheme.error,
                    )
                else -> Unit
            }
            ActionMessage(action)
        }
    }
}
