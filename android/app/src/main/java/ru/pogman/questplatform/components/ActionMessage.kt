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
        QuestFailure.MATERIAL_EXPORT_FAILED -> R.string.error_material_export
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
        QuestFailure.RUN_LAYOUT_CHANGED -> R.string.error_run_layout
        QuestFailure.QUEST_NOT_FOUND -> R.string.error_quest_not_found
        QuestFailure.NFC_UNAVAILABLE -> R.string.nfc_unavailable
        QuestFailure.NFC_DISABLED -> R.string.nfc_disabled
        QuestFailure.NFC_NO_TEXT -> R.string.error_nfc_no_text
        QuestFailure.NFC_READ_ONLY -> R.string.error_nfc_read_only
        QuestFailure.NFC_TOO_SMALL -> R.string.error_nfc_too_small
        QuestFailure.NFC_IO -> R.string.error_nfc_io
        QuestFailure.NFC_WRONG_TAG -> R.string.error_nfc_wrong_tag
        QuestFailure.CAMERA_UNAVAILABLE -> R.string.error_camera
        QuestFailure.CAMERA_PERMISSION_REQUIRED -> R.string.error_camera_permission
        QuestFailure.PHOTO_INVALID -> R.string.error_photo
        QuestFailure.PHOTO_SOURCE_UNAVAILABLE -> R.string.error_photo_source
        QuestFailure.STORAGE_ERROR -> R.string.error_storage
        QuestFailure.SILENT_CALIBRATION_REQUIRED -> R.string.silent_calibration_required
        QuestFailure.SILENT_MIC_REQUIRED -> R.string.silent_microphone_required
    }
