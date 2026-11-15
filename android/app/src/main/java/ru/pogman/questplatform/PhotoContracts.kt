package ru.pogman.questplatform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

class LocalPhotoContract : ActivityResultContracts.PickVisualMedia() {
    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
        val intent = super.createIntent(context, input)
        // On older Android versions AndroidX falls back to DocumentsUI (including Drive).
        // Ask a gallery app for a local MediaStore image instead of opening that fallback.
        return if (intent.action == Intent.ACTION_OPEN_DOCUMENT) {
                Intent(Intent.ACTION_PICK)
                    .setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            } else {
                intent
            }
            .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    }
}

class CameraPhotoContract : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            clipData = ClipData.newRawUri("quest-photo", input)
        }
}
