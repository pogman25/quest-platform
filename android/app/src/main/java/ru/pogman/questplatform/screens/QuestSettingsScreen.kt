package ru.pogman.questplatform.screens

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.*
import ru.pogman.questplatform.domain.*
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun QuestSettingsScreen(
    gameId: String,
    quest: GameQuest,
    action: ActionState,
    photoUri: (String) -> String,
    loadPhoto: suspend (String) -> Bitmap,
    onChange: ((QuestConfig) -> QuestConfig) -> Unit,
    onGallery: (PhotoTarget) -> Unit,
    onCamera: (PhotoTarget) -> Unit,
    onWriteNfc: (String) -> Unit,
    onExport: () -> Unit,
    onSave: () -> Unit,
    onCalibrateSilent: () -> Unit = {},
) {
    ScreenContent {
        Text(quest.type.title, style = MaterialTheme.typography.headlineLarge)
        if (quest.type == QuestType.QR || quest.type == QuestType.STENCIL)
            Text(stringResource(R.string.pdf_save_hint))
        QuestEditor(
            gameId,
            quest,
            !action.busy,
            photoUri,
            onChange,
            onGallery,
            onCamera,
            onWriteNfc,
            loadPhoto,
            onExport,
            onCalibrateSilent,
        )
        ActionMessage(action)
        Text(stringResource(R.string.quest_draft_hint))
        Button(onSave, enabled = !action.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save_changes_action))
        }
    }
}
