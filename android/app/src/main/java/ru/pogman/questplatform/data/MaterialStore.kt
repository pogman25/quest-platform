package ru.pogman.questplatform.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ru.pogman.questplatform.domain.*

data class PreparedMaterial(val id: String, val filename: String)

class MaterialStore(private val context: Context, private val photos: PhotoStore) {
    private val directory = File(context.cacheDir, "materials")

    suspend fun prepare(game: Game, type: QuestType): PreparedMaterial {
        val id = "${UUID.randomUUID()}.pdf"
        val file = resolve(id)
        try {
            return withContext(Dispatchers.IO) {
                directory.mkdirs()
                directory
                    .listFiles()
                    ?.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000 }
                    ?.forEach { it.delete() }
                val config = game.quests.single { it.type == type }.config
                val pages =
                    when (config) {
                        is QrQuestConfig -> puzzlePages(config, questQrModules(config.token))
                        is WordQuestConfig ->
                            stencilPages(config, makeStencilKit(config.word, config.strokeSeed))
                        else -> throw QuestException(QuestFailure.MATERIAL_EXPORT_FAILED)
                    }
                val pictures = mutableMapOf<String, Bitmap>()
                val document = PdfDocument()
                try {
                    pages.pages
                        .flatMap { page ->
                            page.marks.filterIsInstance<PrintMark.Picture>().map { it.reference }
                        }
                        .distinct()
                        .forEach {
                            pictures[it] = photos.bitmap(it)
                        }
                    pages.pages.forEachIndexed { index, layout ->
                        coroutineContext.ensureActive()
                        val info = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
                        val page = document.startPage(info)
                        val points = 72f / 25.4f
                        // Centre the exact-mm layout on the integer-point A4 media box.
                        page.canvas.translate((595 - 210 * points) / 2, (842 - 297 * points) / 2)
                        page.canvas.scale(points, points)
                        try {
                            drawPrintPage(page.canvas, layout, pictures)
                        } finally {
                            document.finishPage(page)
                        }
                    }
                    file.outputStream().use(document::writeTo)
                } finally {
                    document.close()
                    pictures.values.forEach(Bitmap::recycle)
                }
                coroutineContext.ensureActive()
                PreparedMaterial(
                    id,
                    if (type == QuestType.QR) "qr-puzzle-duplex-a4.pdf" else "snow-stencils-a4.pdf",
                )
            }
        } catch (error: CancellationException) {
            file.delete()
            throw error
        } catch (error: Exception) {
            file.delete()
            if (error is QuestException) throw error
            throw QuestException(QuestFailure.MATERIAL_EXPORT_FAILED)
        }
    }

    suspend fun save(id: String, destination: Uri) =
        withContext(Dispatchers.IO) {
            val source = resolve(id)
            try {
                val output =
                    context.contentResolver.openOutputStream(destination, "wt")
                        ?: throw QuestException(QuestFailure.MATERIAL_EXPORT_FAILED)
                output.use { target -> source.inputStream().use { it.copyTo(target) } }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                throw QuestException(QuestFailure.MATERIAL_EXPORT_FAILED)
            } finally {
                source.delete()
            }
        }

    fun discard(id: String) {
        resolve(id).delete()
    }

    private fun resolve(id: String): File {
        require(Regex("[0-9a-f-]{36}\\.pdf").matches(id))
        return File(directory, id)
    }
}
