package ru.pogman.questplatform.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
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
