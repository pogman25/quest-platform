package ru.pogman.questplatform.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.QuestException
import ru.pogman.questplatform.domain.QuestFailure

class PhotoStore(
    private val context: Context,
    private val directory: File = File(context.filesDir, "photos"),
) {
    fun displayUri(reference: String): String =
        if (reference == DEMO_PHOTO) {
            "android.resource://${context.packageName}/${R.drawable.demo_room}"
        } else {
            Uri.fromFile(resolve(reference)).toString()
        }

    fun exists(reference: String): Boolean =
        reference == DEMO_PHOTO || runCatching { resolve(reference).isFile }.getOrDefault(false)

    suspend fun bitmap(reference: String, maxSize: Int = 2400): Bitmap =
        withContext(Dispatchers.IO) {
            if (reference == DEMO_PHOTO) {
                val drawable =
                    requireNotNull(ContextCompat.getDrawable(context, R.drawable.demo_room))
                val ratio = drawable.intrinsicWidth.toDouble() / drawable.intrinsicHeight
                val width = if (ratio >= 1) maxSize else (maxSize * ratio).toInt()
                val height = if (ratio >= 1) (maxSize / ratio).toInt() else maxSize
                return@withContext Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    .also {
                        drawable.setBounds(0, 0, width, height)
                        drawable.draw(Canvas(it))
                    }
            }
            val file = resolve(reference)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, options)
            if (options.outWidth <= 0 || options.outHeight <= 0)
                throw QuestException(QuestFailure.PHOTO_INVALID)
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / sample > maxSize) sample *= 2
            val original =
                BitmapFactory.decodeFile(
                    file.path,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                ) ?: throw QuestException(QuestFailure.PHOTO_INVALID)
            val matrix = Matrix()
            val orientation = runCatching {
                ExifInterface(file.path)
                    .getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
            }
                .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.setRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.setRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            }
            val scale = minOf(1f, maxSize.toFloat() / maxOf(original.width, original.height))
            matrix.postScale(scale, scale)
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                .also {
                    if (it !== original) original.recycle()
                }
        }

    fun discard(reference: String) {
        if (reference != DEMO_PHOTO) {
            resolve(reference).delete()
        }
    }

    suspend fun import(uri: Uri): String =
        withContext(Dispatchers.IO) {
            directory.mkdirs()
            val name = "${UUID.randomUUID()}.image"
            val target = File(directory, name)
            try {
                val input =
                    context.contentResolver.openInputStream(uri)
                        ?: throw QuestException(QuestFailure.PHOTO_INVALID)
                input.use { source ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(8_192)
                        var total = 0L
                        var count = source.read(buffer)
                        while (count != -1) {
                            coroutineContext.ensureActive()
                            total += count
                            if (total > 25 * 1024 * 1024) {
                                throw QuestException(QuestFailure.PHOTO_INVALID)
                            }
                            output.write(buffer, 0, count)
                            count = source.read(buffer)
                        }
                    }
                }
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(target.path, options)
                if (
                    options.outWidth <= 0 ||
                        options.outHeight <= 0 ||
                        options.outWidth.toLong() * options.outHeight > 100_000_000
                ) {
                    throw QuestException(QuestFailure.PHOTO_INVALID)
                }
                name
            } catch (error: Exception) {
                target.delete()
                throw error
            }
        }

    private fun resolve(reference: String): File {
        if (!Regex("[0-9a-f-]{36}\\.image").matches(reference)) {
            throw QuestException(QuestFailure.PHOTO_INVALID)
        }
        return File(directory, reference)
    }

    companion object {
        const val DEMO_PHOTO = "demo:room"
    }
}
