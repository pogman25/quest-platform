package ru.pogman.questplatform

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.IOException
import ru.pogman.questplatform.data.CameraPhotoStore
import ru.pogman.questplatform.domain.PhotoTarget
import ru.pogman.questplatform.domain.QuestFailure
import ru.pogman.questplatform.domain.QuestType
import ru.pogman.questplatform.viewmodel.OrganizerViewModel

data class PhotoActions(val gallery: (PhotoTarget) -> Unit, val camera: (PhotoTarget) -> Unit)

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
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var galleryType by rememberSaveable { mutableStateOf(QuestType.PHOTO.id) }
    var cameraType by rememberSaveable { mutableStateOf(QuestType.PHOTO.id) }
    var galleryCellId by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraCellId by rememberSaveable { mutableStateOf<String?>(null) }

    val gallery =
        rememberLauncherForActivityResult(LocalPhotoContract()) { uri ->
            val owner = galleryGameId
            galleryGameId = null
            if (uri != null && owner != null)
                organizer.importPhoto(
                    uri,
                    owner,
                    PhotoTarget(QuestType.fromId(galleryType), galleryCellId),
                )
        }
    val camera =
        rememberLauncherForActivityResult(CameraPhotoContract()) { success ->
            val uri = cameraUri?.toUri()
            val owner = cameraGameId
            cameraUri = null
            cameraGameId = null
            if (uri != null) {
                if (success && owner != null)
                    organizer.importPhoto(
                        uri,
                        owner,
                        PhotoTarget(QuestType.fromId(cameraType), cameraCellId),
                        fromCamera = true,
                    )
                else captures.discard(uri)
            }
        }
    val handleCameraFailure: (QuestFailure) -> Unit = { failure ->
        cameraUri?.let { captures.discard(it.toUri()) }
        cameraUri = null
        cameraGameId = null
        organizer.reportExternalFailure(failure)
    }
    val launchCapture = {
        if (enabled && gameId != null && cameraGameId == gameId) {
            try {
                val uri = captures.create()
                cameraUri = uri.toString()
                camera.launch(uri)
            } catch (_: ActivityNotFoundException) {
                handleCameraFailure(QuestFailure.PHOTO_SOURCE_UNAVAILABLE)
            } catch (_: SecurityException) {
                handleCameraFailure(QuestFailure.CAMERA_PERMISSION_REQUIRED)
            } catch (_: IOException) {
                handleCameraFailure(QuestFailure.STORAGE_ERROR)
            }
        } else cameraGameId = null
    }
    // CAMERA is declared for QR scanning; Android then also checks it for an external capture
    // intent.
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCapture()
            else handleCameraFailure(QuestFailure.CAMERA_PERMISSION_REQUIRED)
        }
    val handleGallery: (PhotoTarget) -> Unit = { target ->
        if (enabled && gameId != null && galleryGameId == null && cameraGameId == null) {
            galleryGameId = gameId
            galleryType = target.type.id
            galleryCellId = target.cellId
            try {
                gallery.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (_: ActivityNotFoundException) {
                galleryGameId = null
                organizer.reportExternalFailure(QuestFailure.PHOTO_SOURCE_UNAVAILABLE)
            } catch (_: SecurityException) {
                galleryGameId = null
                organizer.reportExternalFailure(QuestFailure.PHOTO_SOURCE_UNAVAILABLE)
            }
        }
    }
    val handleCamera: (PhotoTarget) -> Unit = { target ->
        if (enabled && gameId != null && galleryGameId == null && cameraGameId == null) {
            cameraGameId = gameId
            cameraType = target.type.id
            cameraCellId = target.cellId
            if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
            )
                launchCapture()
            else permission.launch(Manifest.permission.CAMERA)
        }
    }
    return PhotoActions(handleGallery, handleCamera)
}
