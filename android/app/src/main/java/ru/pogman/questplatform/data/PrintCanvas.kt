package ru.pogman.questplatform.data

import android.graphics.*
import ru.pogman.questplatform.domain.*

// Renders in the layout's millimetres; the caller chooses screen pixels or PDF points.
fun drawPrintPage(canvas: Canvas, page: PrintPage, pictures: Map<String, Bitmap> = emptyMap()) {
    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    for (mark in page.marks) {
        paint.reset()
        paint.isAntiAlias = true
        when (mark) {
            is PrintMark.Text -> {
                paint.color = mark.color
                paint.textSize = mark.size.toFloat()
                paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                canvas.drawText(mark.value, mark.x.toFloat(), mark.y.toFloat(), paint)
            }
            is PrintMark.Line -> {
                paint.color = mark.color
                paint.strokeWidth = mark.width.toFloat()
                paint.strokeCap = if (mark.roundCaps) Paint.Cap.ROUND else Paint.Cap.BUTT
                if (mark.dashed) paint.pathEffect = DashPathEffect(floatArrayOf(2f, 1.2f), 0f)
                canvas.drawLine(
                    mark.x.toFloat(),
                    mark.y.toFloat(),
                    mark.endX.toFloat(),
                    mark.endY.toFloat(),
                    paint,
                )
            }
            is PrintMark.Rect -> {
                val rect =
                    RectF(
                        mark.x.toFloat(),
                        mark.y.toFloat(),
                        (mark.x + mark.width).toFloat(),
                        (mark.y + mark.height).toFloat(),
                    )
                mark.fill?.let {
                    paint.color = it
                    canvas.drawRect(rect, paint)
                }
                mark.border?.let {
                    paint.color = it
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.2f
                    canvas.drawRect(rect, paint)
                }
            }
            is PrintMark.Picture ->
                pictures[mark.reference]?.let { bitmap ->
                    val scale =
                        if (mark.fit == PictureFit.COVER)
                            maxOf(mark.width / bitmap.width, mark.height / bitmap.height)
                        else minOf(mark.width / bitmap.width, mark.height / bitmap.height)
                    val width = bitmap.width * scale
                    val height = bitmap.height * scale
                    val x = mark.x + (mark.width - width) / 2
                    val y = mark.y + (mark.height - height) / 2
                    canvas.save()
                    canvas.clipRect(
                        mark.x.toFloat(),
                        mark.y.toFloat(),
                        (mark.x + mark.width).toFloat(),
                        (mark.y + mark.height).toFloat(),
                    )
                    paint.isFilterBitmap = true
                    canvas.drawBitmap(
                        bitmap,
                        null,
                        RectF(
                            x.toFloat(),
                            y.toFloat(),
                            (x + width).toFloat(),
                            (y + height).toFloat(),
                        ),
                        paint,
                    )
                    canvas.restore()
                }
        }
    }
}
