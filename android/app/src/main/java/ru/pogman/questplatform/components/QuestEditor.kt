package ru.pogman.questplatform.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import ru.pogman.questplatform.domain.*

@Composable
fun QuestEditor(
    gameId: String,
    quest: GameQuest,
    enabled: Boolean,
    photoUri: (String) -> String,
    onChange: ((QuestConfig) -> QuestConfig) -> Unit,
    onGallery: (PhotoTarget) -> Unit,
    onCamera: (PhotoTarget) -> Unit,
    onWriteNfc: (String) -> Unit,
    loadPhoto: suspend (String) -> Bitmap,
    onExport: () -> Unit,
    onCalibrateSilent: () -> Unit = {},
) {
    when (val config = quest.config) {
        is SilentQuestConfig ->
            SilentQuestEditor(
                config,
                enabled,
                typedChange<SilentQuestConfig>(onChange),
                onCalibrateSilent,
            )
        is PhotoQuestConfig ->
            PhotoQuestEditor(
                gameId,
                config,
                photoUri(config.photoRef),
                enabled,
                typedChange<PhotoQuestConfig>(onChange),
                onGallery,
                onCamera,
            )
        is QrQuestConfig ->
            QrQuestEditor(
                config,
                enabled,
                typedChange<QrQuestConfig>(onChange),
                onGallery,
                onCamera,
                loadPhoto,
                onExport,
            )
        is NfcQuestConfig ->
            NfcQuestEditor(config, enabled, typedChange<NfcQuestConfig>(onChange), onWriteNfc)
        is ChoiceQuestConfig ->
            ChoiceQuestEditor(
                quest.type,
                config,
                enabled,
                photoUri,
                typedChange<ChoiceQuestConfig>(onChange),
                onGallery,
                onCamera,
            )
        is WordQuestConfig ->
            WordQuestEditor(
                config,
                enabled,
                typedChange<WordQuestConfig>(onChange),
                loadPhoto,
                onExport,
            )
    }
}

// Adapts a concrete editor to the union without replacing a newer config with an old snapshot.
private inline fun <reified T : QuestConfig> typedChange(
    crossinline onChange: ((QuestConfig) -> QuestConfig) -> Unit
): ((T) -> T) -> Unit = { transform ->
    onChange { current -> if (current is T) transform(current) else current }
}
