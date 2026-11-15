package ru.pogman.questplatform

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import java.io.IOException
import ru.pogman.questplatform.data.CameraPhotoStore
import ru.pogman.questplatform.domain.QuestFailure
import ru.pogman.questplatform.viewmodel.OrganizerViewModel

data class PhotoActions(val gallery: () -> Unit, val camera: () -> Unit)

@Composable
fun rememberPhotoActions(
    gameId: String?,
    enabled: Boolean,
    organizer: OrganizerViewModel,
): PhotoActions {
    val context = LocalContext.current
    val captures = remember(context) { CameraPhotoStore(context.applicationContext) }
    var galleryGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraGameId by rememberSaveable { mutableStateOf<String?>(null) }
    // Keep the output URI across Activity recreation while the camera is open.
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val gallery =
        rememberLauncherForActivityResult(LocalPhotoContract()) { uri ->
            val owner = galleryGameId
            galleryGameId = null
            if (uri != null && owner != null) {
                organizer.importPhoto(uri, owner)
            }
        }
    val camera =
        rememberLauncherForActivityResult(CameraPhotoContract()) { success ->
            val uri = cameraUri?.toUri()
            val owner = cameraGameId
            cameraUri = null
            cameraGameId = null
            if (uri != null) {
                if (success && owner != null) {
                    organizer.importPhoto(uri, owner, fromCamera = true)
                } else {
                    captures.discard(uri)
                }
            }
        }
    val handleCameraFailure: (QuestFailure) -> Unit = { failure ->
        cameraUri?.let { captures.discard(it.toUri()) }
        cameraUri = null
        cameraGameId = null
        organizer.reportPhotoFailure(failure)
    }
    val handleGallery = {
        if (enabled && gameId != null && galleryGameId == null && cameraUri == null) {
            galleryGameId = gameId
            try {
                gallery.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (_: ActivityNotFoundException) {
                galleryGameId = null
                organizer.reportPhotoFailure(QuestFailure.PHOTO_SOURCE_UNAVAILABLE)
            } catch (_: SecurityException) {
                galleryGameId = null
                organizer.reportPhotoFailure(QuestFailure.PHOTO_SOURCE_UNAVAILABLE)
            }
        }
    }
    val handleCamera = {
        if (enabled && gameId != null && galleryGameId == null && cameraUri == null) {
            try {
                val uri = captures.create()
                cameraUri = uri.toString()
                cameraGameId = gameId
                camera.launch(uri)
            } catch (_: ActivityNotFoundException) {
                handleCameraFailure(QuestFailure.PHOTO_SOURCE_UNAVAILABLE)
            } catch (_: SecurityException) {
                handleCameraFailure(QuestFailure.PHOTO_SOURCE_UNAVAILABLE)
            } catch (_: IOException) {
                handleCameraFailure(QuestFailure.STORAGE_ERROR)
            }
        }
    }
    return PhotoActions(handleGallery, handleCamera)
}
