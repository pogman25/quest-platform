package ru.pogman.questplatform.components

import androidx.compose.runtime.Composable
import ru.pogman.questplatform.domain.*

@Composable
fun QuestEditor(
    gameId: String,
    quest: GameQuest,
    enabled: Boolean,
    photoUri: (String) -> String,
    onChange: ((QuestConfig) -> QuestConfig) -> Unit,
    onGallery: (QuestType) -> Unit,
    onCamera: (QuestType) -> Unit,
    onWriteNfc: (String) -> Unit,
) {
    when (val config = quest.config) {
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
                photoUri(config.photoRef),
                enabled,
                typedChange<QrQuestConfig>(onChange),
                onGallery,
                onCamera,
            )
        is NfcQuestConfig ->
            NfcQuestEditor(config, enabled, typedChange<NfcQuestConfig>(onChange), onWriteNfc)
        is ChoiceQuestConfig ->
            ChoiceQuestEditor(quest.type, config, enabled, typedChange<ChoiceQuestConfig>(onChange))
        is WordQuestConfig ->
            WordQuestEditor(config, enabled, typedChange<WordQuestConfig>(onChange))
    }
}

// Adapts a concrete editor to the union without replacing a newer config with an old snapshot.
private inline fun <reified T : QuestConfig> typedChange(
    crossinline onChange: ((QuestConfig) -> QuestConfig) -> Unit
): ((T) -> T) -> Unit = { transform ->
    onChange { current -> if (current is T) transform(current) else current }
}
