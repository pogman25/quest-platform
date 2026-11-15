package ru.pogman.questplatform.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class CameraPhotoStore(private val context: Context) {
    private val authority = "${context.packageName}.photos"

    fun create(): Uri {
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File.createTempFile("capture-", ".jpg", directory)
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun discard(uri: Uri) {
        if (
            uri.scheme == "content" &&
                uri.authority == authority &&
                uri.pathSegments.firstOrNull() == "camera"
        ) {
            context.contentResolver.delete(uri, null, null)
        }
    }
}
