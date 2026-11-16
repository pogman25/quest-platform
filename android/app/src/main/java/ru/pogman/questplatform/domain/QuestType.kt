package ru.pogman.questplatform.domain

enum class QuestType(val id: String, val title: String, val selectionSize: Int = 0) {
    NFC("nfc", "Метка на месте преступления"),
    QR("qr", "Разорванная фотография"),
    OBJECTS("objects", "Три предмета в комнате", 3),
    PHOTO("photo", "Фото до метели"),
    TREE("tree", "След на ёлке", 4),
    STENCIL("stencil", "Два слоя снега");

    companion object {
        fun fromId(id: String): QuestType = entries.first { it.id == id }
    }
}
