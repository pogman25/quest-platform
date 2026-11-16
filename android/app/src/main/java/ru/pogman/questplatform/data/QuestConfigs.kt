package ru.pogman.questplatform.data

import java.util.UUID
import kotlinx.serialization.json.Json
import ru.pogman.questplatform.domain.*

// The type remains in questId. Explicit serializers also read the original photo-only JSON.
class QuestConfigs {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(type: QuestType, value: String): QuestConfig =
        when (type) {
            QuestType.PHOTO -> json.decodeFromString<PhotoQuestConfig>(value)
            QuestType.NFC -> json.decodeFromString<NfcQuestConfig>(value)
            QuestType.QR -> json.decodeFromString<QrQuestConfig>(value)
            QuestType.OBJECTS,
            QuestType.TREE -> json.decodeFromString<ChoiceQuestConfig>(value)
            QuestType.STENCIL -> json.decodeFromString<WordQuestConfig>(value)
        }

    fun encode(config: QuestConfig): String =
        when (config) {
            is PhotoQuestConfig -> json.encodeToString(config)
            is NfcQuestConfig -> json.encodeToString(config)
            is QrQuestConfig -> json.encodeToString(config)
            is ChoiceQuestConfig -> json.encodeToString(config)
            is WordQuestConfig -> json.encodeToString(config)
        }
}

fun defaultQuest(type: QuestType): GameQuest =
    GameQuest(
        type,
        when (type) {
            QuestType.PHOTO ->
                PhotoQuestConfig(
                    "Сравните снимок с комнатой. Какой предмет исчез?",
                    PhotoStore.DEMO_PHOTO,
                    listOf("лампа", "настольная лампа"),
                )
            QuestType.NFC -> {
                val tags =
                    listOf("Подлинник", "Ложная метка 1", "Ложная метка 2").map {
                        NfcTagConfig(UUID.randomUUID().toString(), it, UUID.randomUUID().toString())
                    }
                NfcQuestConfig(
                    "Найдите улику с подлинной NFC-меткой и приложите телефон к ней.",
                    tags,
                    tags.first().id,
                )
            }
            QuestType.QR ->
                QrQuestConfig(
                    "Найдите девять частей фотографии, соберите пазл и отсканируйте QR на обороте.",
                    PhotoStore.DEMO_PHOTO,
                    UUID.randomUUID().toString(),
                )
            QuestType.OBJECTS -> {
                val cells = defaultCells(false)
                ChoiceQuestConfig(
                    "Осмотрите комнату. Выберите три предмета, которые вы нашли.",
                    cells,
                    cells.take(3).map { it.id },
                )
            }
            QuestType.TREE -> {
                val cells = defaultCells(true)
                ChoiceQuestConfig(
                    "Выберите по порядку: звезда, колокольчик, подарок, снежинка.",
                    cells,
                    cells.take(4).map { it.id },
                )
            }
            QuestType.STENCIL ->
                WordQuestConfig(
                    "Совместите найденные карточки и накладки. Какое слово получилось?",
                    "метель",
                )
        },
    )

private fun defaultCells(tree: Boolean): List<ChoiceCell> {
    val values =
        if (tree)
            listOf(
                "Звезда" to "⭐",
                "Колокольчик" to "🔔",
                "Подарок" to "🎁",
                "Снежинка" to "❄️",
                "Шар" to "🔴",
                "Леденец" to "🍬",
                "Снеговик" to "⛄",
                "Бант" to "🎀",
                "Ёлка" to "🎄",
            )
        else
            listOf(
                "Часы" to "🕰️",
                "Книга" to "📚",
                "Ключ" to "🔑",
                "Чашка" to "☕",
                "Свеча" to "🕯️",
                "Лампа" to "💡",
                "Зонт" to "☂️",
                "Мяч" to "⚽",
                "Стул" to "🪑",
            )
    return values.mapIndexed { index, pair ->
        ChoiceCell("cell-${index + 1}", pair.first, pair.second)
    }
}
