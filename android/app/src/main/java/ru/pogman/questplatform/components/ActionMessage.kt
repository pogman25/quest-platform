package ru.pogman.questplatform.components

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.QuestFailure
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun ActionMessage(state: ActionState) {
    if (state.busy) {
        LinearProgressIndicator()
    }
    state.error?.let { error ->
        Text(stringResource(errorMessage(error)), color = MaterialTheme.colorScheme.error)
    }
}

private fun errorMessage(error: QuestFailure): Int =
    when (error) {
        QuestFailure.PASSWORD_TOO_SHORT -> R.string.error_password_short
        QuestFailure.PASSWORD_MISMATCH -> R.string.error_password_mismatch
        QuestFailure.WRONG_PASSWORD -> R.string.error_wrong_password
        QuestFailure.PASSWORD_ALREADY_SET -> R.string.error_password_exists
        QuestFailure.ORGANIZER_REQUIRED -> R.string.error_organizer_required
        QuestFailure.GAME_INVALID -> R.string.error_game_invalid
        QuestFailure.GAME_NOT_FOUND -> R.string.error_game_not_found
        QuestFailure.TEAM_NAME_REQUIRED -> R.string.error_team_name
        QuestFailure.CODE_INVALID -> R.string.error_code
        QuestFailure.RUN_NOT_FOUND -> R.string.error_run_not_found
        QuestFailure.RUN_NOT_STARTED -> R.string.error_run_not_started
        QuestFailure.PHOTO_INVALID -> R.string.error_photo
        QuestFailure.STORAGE_ERROR -> R.string.error_storage
    }
