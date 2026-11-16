package ru.pogman.questplatform.domain

// Millimetres throughout. Preview and PDF consume this same drawing, with no layout in the
// renderer.
data class PrintPage(val widthMm: Double, val heightMm: Double, val marks: List<PrintMark>)

data class PrintDocument(val pages: List<PrintPage>)

sealed interface PrintMark {
    data class Text(
        val x: Double,
        val y: Double,
        val value: String,
        val size: Double = 3.2,
        val color: Int = INK,
    ) : PrintMark

    data class Line(
        val x: Double,
        val y: Double,
        val endX: Double,
        val endY: Double,
        val width: Double = 0.2,
        val color: Int = INK,
        val dashed: Boolean = false,
        val roundCaps: Boolean = false,
    ) : PrintMark

    data class Rect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val fill: Int? = null,
        val border: Int? = null,
    ) : PrintMark

    data class Picture(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val reference: String,
        val fit: PictureFit,
    ) : PrintMark
}

const val INK: Int = -16777216
private const val GRAY: Int = -10458264
private const val WHITE: Int = -1

fun puzzlePages(config: QrQuestConfig, modules: List<List<Boolean>>): PrintDocument {
    require(
        config.sizeMm in listOf(120, 150, 180) &&
            modules.isNotEmpty() &&
            modules.all { it.size == modules.size }
    )
    val size = config.sizeMm.toDouble()
    val x = (210 - size) / 2
    val y = (297 - size) / 2
    fun label(front: Boolean): List<PrintMark> =
        listOf(
            PrintMark.Text(
                x,
                y - 8,
                if (front) "БЮРО КВЕСТОВ / 01 ЛИЦЕВАЯ СТОРОНА" else "БЮРО КВЕСТОВ / 02 ОБОРОТ",
            ),
            PrintMark.Rect(x, y, size, size, border = GRAY),
            PrintMark.Text(
                15.0,
                y + size + 10,
                "${config.sizeMm} × ${config.sizeMm} мм / A4 / Масштаб 100%",
                2.8,
                GRAY,
            ),
            PrintMark.Text(
                15.0,
                y + size + 16,
                "Двусторонняя печать: переворот по длинному краю.",
                2.8,
                GRAY,
            ),
            PrintMark.Text(
                15.0,
                y + size + 22,
                "Проверьте совмещение, затем разрежьте лицевую сторону по сетке.",
                2.8,
                GRAY,
            ),
            PrintMark.Line(15.0, 282.0, 65.0, 282.0, color = GRAY),
            PrintMark.Text(15.0, 287.0, "Контроль: 50 мм", 2.8, GRAY),
        )
    val front =
        mutableListOf<PrintMark>(PrintMark.Picture(x, y, size, size, config.photoRef, config.fit))
    for (fraction in listOf(1.0 / 3, 2.0 / 3)) {
        val offset = size * fraction
        front.add(PrintMark.Line(x + offset, y, x + offset, y + size, 0.55))
        front.add(PrintMark.Line(x, y + offset, x + size, y + offset, 0.55))
        front.add(PrintMark.Line(x + offset, y, x + offset, y + size, 0.24, WHITE, true))
        front.add(PrintMark.Line(x, y + offset, x + size, y + offset, 0.24, WHITE, true))
    }
    front.addAll(label(true))
    val back = mutableListOf<PrintMark>()
    val unit = (size - 16) / modules.size
    modules.forEachIndexed { row, cells ->
        var start = -1
        for (col in 0..cells.size) {
            val filled = col < cells.size && cells[col]
            if (filled && start < 0) start = col
            if (!filled && start >= 0) {
                back.add(
                    PrintMark.Rect(
                        x + 8 + start * unit,
                        y + 8 + row * unit,
                        (col - start) * unit,
                        unit,
                        INK,
                    )
                )
                start = -1
            }
        }
    }
    back.addAll(label(false))
    return PrintDocument(listOf(PrintPage(210.0, 297.0, front), PrintPage(210.0, 297.0, back)))
}

fun stencilPreview(
    config: WordQuestConfig,
    kit: StencilKit,
    selected: Int,
    combined: Boolean,
    half: Int? = null,
): PrintPage {
    val glyphs = if (half == null) kit.glyphs else kit.glyphs.chunked(kit.glyphs.size / 2)[half]
    val width = if (half == null) config.widthMm.toDouble() else config.widthMm / 2.0
    val marks = mutableListOf<PrintMark>(PrintMark.Rect(0.0, 0.0, width, 34.0, WHITE, GRAY))
    if (half != null || combined) marks.addAll(stencilLines(glyphs, width, 0.0, 0.0) { it.base })
    if (half == null)
        marks.addAll(
            stencilLines(glyphs, width, 0.0, 0.0) {
                if (selected == config.correctOverlay) it.complement
                else it.decoys[if (selected < config.correctOverlay) selected - 1 else selected - 2]
            }
        )
    return PrintPage(width, 34.0, marks)
}

fun stencilPages(config: WordQuestConfig, kit: StencilKit): PrintDocument {
    require(config.widthMm in listOf(144, 168, 184) && config.correctOverlay in 1..8)
    val width = config.widthMm.toDouble()
    val x = (210 - width) / 2
    fun header(page: Int): MutableList<PrintMark> =
        mutableListOf(
            PrintMark.Text(
                15.0,
                20.0,
                if (page == 1) "ДВА СЛОЯ СНЕГА / ВЕДУЩЕМУ" else "ДВА СЛОЯ СНЕГА / НАКЛАДКИ",
                4.6,
            ),
            PrintMark.Text(
                15.0,
                27.0,
                if (page == 1) "A4 / Масштаб 100% / Подлинная накладка: ${config.correctOverlay}"
                else "A4 / Масштаб 100% / Лист $page из 3",
                3.0,
                GRAY,
            ),
            PrintMark.Line(15.0, 282.0, 65.0, 282.0, color = GRAY),
            PrintMark.Text(15.0, 287.0, "Контроль: 50 мм", 2.8, GRAY),
        )
    fun card(
        marks: MutableList<PrintMark>,
        label: String,
        left: Double,
        top: Double,
        span: Double,
        glyphs: List<StencilGlyph>,
        strokes: (StencilGlyph) -> List<StencilStroke>,
    ) {
        marks.add(PrintMark.Text(left, top - 3, label, 2.8, GRAY))
        marks.addAll(stencilLines(glyphs, span, left, top, strokes))
        marks.add(PrintMark.Rect(left, top, span, 34.0, border = GRAY))
    }
    val first = header(1)
    card(first, "Контрольное слово (не выдавать игрокам)", x, 46.0, width, kit.glyphs) {
        it.reference
    }
    val halfX = (210 - width - 6) / 2
    val halves = kit.glyphs.chunked(kit.glyphs.size / 2)
    card(first, "Карточка A", halfX, 104.0, width / 2, halves[0]) { it.base }
    card(first, "Карточка B", halfX + width / 2 + 6, 104.0, width / 2, halves[1]) { it.base }
    first.add(
        PrintMark.Text(15.0, 152.0, "Перенесите штрихи и рамки на прозрачный пластик.", 3.0, GRAY)
    )
    first.add(
        PrintMark.Text(
            15.0,
            158.0,
            "Подписи карточек оставьте за рамкой: игрокам не сообщается их порядок.",
            2.8,
            GRAY,
        )
    )
    val pages = mutableListOf(PrintPage(210.0, 297.0, first))
    for (page in 0..1) {
        val marks = header(page + 2)
        for (row in 0..3) {
            val number = page * 4 + row + 1
            card(marks, "Накладка $number", x, 44.0 + row * 55, width, kit.glyphs) {
                if (number == config.correctOverlay) it.complement
                else it.decoys[if (number < config.correctOverlay) number - 1 else number - 2]
            }
        }
        pages.add(PrintPage(210.0, 297.0, marks))
    }
    return PrintDocument(pages)
}

private fun stencilLines(
    glyphs: List<StencilGlyph>,
    width: Double,
    x: Double,
    y: Double,
    select: (StencilGlyph) -> List<StencilStroke>,
): List<PrintMark.Line> {
    val cell = width / glyphs.size
    val scale = minOf(cell / 6.3, 34.0 / 8.3)
    val xOffset = (cell - 4 * scale) / 2
    val yOffset = (34 - 6 * scale) / 2
    return glyphs.flatMapIndexed { index, glyph ->
        select(glyph).map { stroke ->
            PrintMark.Line(
                x + index * cell + xOffset + stroke.start.x * scale,
                y + yOffset + stroke.start.y * scale,
                x + index * cell + xOffset + stroke.end.x * scale,
                y + yOffset + stroke.end.y * scale,
                scale * 0.3,
                roundCaps = true,
            )
        }
    }
}
