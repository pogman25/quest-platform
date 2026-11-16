package ru.pogman.questplatform.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.data.drawPrintPage
import ru.pogman.questplatform.domain.*

@Composable
fun PrintPreview(page: PrintPage, loadPhoto: suspend (String) -> Bitmap) {
    val references =
        remember(page) {
            page.marks.filterIsInstance<PrintMark.Picture>().map { it.reference }.distinct()
        }
    var pictures by remember(references) { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    var failed by remember(references) { mutableStateOf(false) }
    LaunchedEffect(references) {
        try {
            pictures = references.associateWith { loadPhoto(it) }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            failed = true
        }
    }
    if (failed) Text(stringResource(R.string.photo_load_error))
    else if (references.any { it !in pictures }) LoadingContent()
    else
        Canvas(Modifier.fillMaxWidth().aspectRatio((page.widthMm / page.heightMm).toFloat())) {
            drawIntoCanvas { target ->
                val canvas = target.nativeCanvas
                canvas.save()
                // Native drawColor fills the clip, so confine the white paper to this preview.
                canvas.clipRect(0f, 0f, size.width, size.height)
                canvas.scale(
                    size.width / page.widthMm.toFloat(),
                    size.height / page.heightMm.toFloat(),
                )
                drawPrintPage(canvas, page, pictures)
                canvas.restore()
            }
        }
}
