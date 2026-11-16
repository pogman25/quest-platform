package ru.pogman.questplatform.domain

// The same item set as the web catalog; labels and pictures remain editable per cell.
data class CatalogItem(val label: String, val symbol: String)

val choiceCatalog =
    listOf(
        CatalogItem("Часы", "🕰️"),
        CatalogItem("Кружка", "☕"),
        CatalogItem("Свеча", "🕯️"),
        CatalogItem("Книга", "📚"),
        CatalogItem("Ключ", "🔑"),
        CatalogItem("Облако", "☁️"),
        CatalogItem("Лампа", "💡"),
        CatalogItem("Зонт", "☂️"),
        CatalogItem("Мяч", "⚽"),
        CatalogItem("Кресло", "🪑"),
        CatalogItem("Звезда", "⭐"),
        CatalogItem("Колокольчик", "🔔"),
        CatalogItem("Подарок", "🎁"),
        CatalogItem("Леденец", "🍬"),
        CatalogItem("Снежинка", "❄️"),
        CatalogItem("Варежка", "🧤"),
        CatalogItem("Снеговик", "⛄"),
        CatalogItem("Бант", "🎀"),
        CatalogItem("Заяц", "🐇"),
        CatalogItem("Ёлка", "🎄"),
        CatalogItem("Камера", "📷"),
    )
