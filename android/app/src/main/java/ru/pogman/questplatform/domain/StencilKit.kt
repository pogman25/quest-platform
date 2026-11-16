package ru.pogman.questplatform.domain

import java.text.Normalizer
import java.util.Locale
import kotlin.math.*

data class StencilPoint(val x: Double, val y: Double)

data class StencilStroke(val start: StencilPoint, val end: StencilPoint)

data class StencilGlyph(
    val reference: List<StencilStroke>,
    val base: List<StencilStroke>,
    val complement: List<StencilStroke>,
    val decoys: List<List<StencilStroke>>,
)

data class StencilKit(val letters: String, val glyphs: List<StencilGlyph>)

fun stencilLetters(word: String): String? {
    val letters =
        Normalizer.normalize(word.trim(), Normalizer.Form.NFC)
            .uppercase(Locale.forLanguageTag("ru"))
    return letters.takeIf {
        it.length in 4..12 && it.length % 2 == 0 && it.all(stencilGlyphs::containsKey)
    }
}

// Port of the web version-3 stroke generator. Keep its random calls and glyph order stable:
// a saved seed must reproduce a printed kit, including its seven false overlays.
fun makeStencilKit(word: String, seed: Long): StencilKit {
    val letters = requireNotNull(stencilLetters(word))
    val random = StencilRandom(seed)
    val originals = fragmentedGlyphs(letters.toList(), random)
    val usedShapes = letters.map { stencilGlyphs.getValue(it) }.toSet()
    val scripts = letters.map { it in 'A'..'Z' }.toSet()
    val absent =
        stencilGlyphs.keys.filter {
            (it in 'A'..'Z') in scripts && stencilGlyphs.getValue(it) !in usedShapes
        }
    val overlays =
        List(7) {
            val half = letters.length / 2
            val foreignPositions =
                listOf(0, half)
                    .flatMap { start ->
                        random
                            .shuffle((start until start + half).toMutableList())
                            .take(ceil(half / 2.0).toInt())
                    }
                    .toSet()
            val shuffled = random.shuffle(letters.toMutableList())
            val foreign = random.shuffle(absent.toMutableList())
            val decoy = letters.mapIndexed { index, letter ->
                val donor =
                    if (index !in foreignPositions)
                        shuffled.indexOfFirst { stencilGlyphs[it] != stencilGlyphs[letter] }
                    else -1
                if (donor >= 0) shuffled.removeAt(donor) else foreign.removeAt(foreign.lastIndex)
            }
            fragmentedGlyphs(decoy, random).map { it.complement }
        }
    return StencilKit(
        letters,
        originals.mapIndexed { index, glyph ->
            glyph.copy(decoys = overlays.map { it[index] })
        },
    )
}

private class StencilRandom(seed: Long) {
    private var state = seed.toInt()

    fun next(): Double {
        state += 0x6d2b79f5
        var value = (state xor (state ushr 15)) * (state or 1)
        value = value xor (value + (value xor (value ushr 7)) * (value or 61))
        return ((value xor (value ushr 14)).toLong() and 0xffffffffL) / 4294967296.0
    }

    fun <T> shuffle(items: MutableList<T>): MutableList<T> {
        for (index in items.lastIndex downTo 1) {
            val other = floor(next() * (index + 1)).toInt()
            val item = items[index]
            items[index] = items[other]
            items[other] = item
        }
        return items
    }
}

private fun pathEdges(paths: String): List<StencilStroke> =
    paths.split(" / ").flatMap { path ->
        path
            .split(' ')
            .map { StencilPoint(it[0].digitToInt().toDouble(), it[1].digitToInt().toDouble()) }
            .zipWithNext(::StencilStroke)
    }

private fun StencilStroke.along(fraction: Double) =
    StencilPoint(start.x + (end.x - start.x) * fraction, start.y + (end.y - start.y) * fraction)

private fun StencilStroke.length() = hypot(end.x - start.x, end.y - start.y)

private fun StencilStroke.distance(point: StencilPoint): Double {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val projection = ((point.x - start.x) * dx + (point.y - start.y) * dy) / (dx * dx + dy * dy)
    val near = along(projection.coerceIn(0.0, 1.0))
    return hypot(point.x - near.x, point.y - near.y)
}

private data class Tile(val stroke: StencilStroke, val index: Int, val projection: Double)

private data class FragmentLayout(
    val baseOnly: List<Tile>,
    val overlayOnly: List<Tile>,
    val middle: List<Tile>,
    val mask: Int,
)

private fun fragmentLayout(
    tiles: List<StencilStroke>,
    angle: Double,
    used: MutableSet<String>,
    random: StencilRandom,
): FragmentLayout {
    val sorted =
        tiles
            .mapIndexed { index, tile ->
                val point = tile.along(0.5)
                Tile(tile, index, point.x * cos(angle) + point.y * sin(angle))
            }
            .sortedBy { it.projection }
    val center = (sorted.size - 1) / 2.0
    val shared =
        sorted
            .mapIndexed { rank, tile -> tile to abs(rank - center) }
            .filter { it.first.stroke.length() >= 1.5 }
            .sortedBy { it.second }
            .take(2)
            .map { it.first }
    val sides = sorted.filter { it !in shared }
    val hidden = floor((tiles.size - 3) / 2.0).toInt()
    val first = sides.take(hidden)
    val second = sides.takeLast(hidden)
    val middle = (shared + sides.drop(hidden).dropLast(hidden)).sortedBy { it.index }
    val count = 1 shl middle.size
    val start = floor(random.next() * count).toInt()
    val directions = if (random.next() < 0.5) listOf(first, second) else listOf(second, first)
    for (pair in listOf(directions, directions.reversed())) {
        val baseOnly = pair[0]
        val overlayOnly = pair[1]
        val side = baseOnly.map { it.index }.sorted().joinToString(",")
        val other = overlayOnly.map { it.index }.sorted().joinToString(",")
        for (offset in 0 until count) {
            val mask = (start + offset) % count
            if (used.add("$side/$other/$mask"))
                return FragmentLayout(baseOnly, overlayOnly, middle, mask)
        }
    }
    error("Could not split repeated stencil letter")
}

private fun maskingStroke(
    ink: List<StencilStroke>,
    angle: Double,
    random: StencilRandom,
): StencilStroke? {
    repeat(64) {
        val x = random.next() * 4.5 - 0.25
        val y = random.next() * 6
        val radius = 0.34 + random.next() * 0.22
        val dx = cos(angle) * radius
        val dy = sin(angle) * radius
        val stroke = StencilStroke(StencilPoint(x - dx, y - dy), StencilPoint(x + dx, y + dy))
        val samples = listOf(stroke.start, StencilPoint(x, y), stroke.end)
        if (ink.all { line -> samples.all { line.distance(it) >= 0.68 } }) return stroke
    }
    return null
}

private fun fragmentedGlyphs(letters: List<Char>, random: StencilRandom): List<StencilGlyph> {
    val usedByLetter = mutableMapOf<Char, MutableSet<String>>()
    return letters.map { letter ->
        val isA = letter == 'A' || letter == 'А'
        val reference = pathEdges(if (isA) "06 20 46 / 13 33" else stencilGlyphs.getValue(letter))
        val tiles = reference.flatMap { edge ->
            val count = ceil(edge.length() / 2.45).toInt()
            List(count) {
                StencilStroke(edge.along(it.toDouble() / count), edge.along((it + 1.0) / count))
            }
        }
        val angle = (random.next() - 0.5) * (if (isA || letter == 'Д') PI / 4 else PI * 2)
        val layout =
            fragmentLayout(tiles, angle, usedByLetter.getOrPut(letter) { mutableSetOf() }, random)
        val fragments =
            layout.middle.mapIndexed { index, tile ->
                val share = 0.44 + random.next() * 0.05
                val matchingStarts = (layout.mask / (1 shl index)) % 2 == 1
                val cut = tile.stroke.along(if (matchingStarts) share else 1 - share)
                val base =
                    if (matchingStarts) StencilStroke(cut, tile.stroke.end)
                    else StencilStroke(tile.stroke.start, cut)
                val matching =
                    if (matchingStarts) StencilStroke(tile.stroke.start, cut)
                    else StencilStroke(cut, tile.stroke.end)
                base to matching
            }
        val base = (fragments.map { it.first } + layout.baseOnly.map { it.stroke }).toMutableList()
        val complement =
            (fragments.map { it.second } + layout.overlayOnly.map { it.stroke }).toMutableList()
        val ink = reference.toMutableList()
        for (layer in listOf(base, complement)) {
            val angles = listOf(0.0, (1 + floor(random.next() * 3)) * PI / 4)
            for (markAngle in angles) {
                maskingStroke(ink, markAngle, random)?.let {
                    layer.add(it)
                    ink.add(it)
                }
            }
        }
        // The web v3 generator consumes the shuffle from its v2 fragment layout too.
        val indices = fragments.indices.toList()
        val changes =
            indices
                .flatMap { index -> indices.drop(index + 1).map { listOf(index, it) } }
                .filter { pair -> pair.any { layout.middle[it].stroke.length() >= 1.5 } }
                .toMutableList()
        if (indices.size == 3) changes.add(indices)
        random.shuffle(changes)
        StencilGlyph(reference, base, complement, emptyList())
    }
}
