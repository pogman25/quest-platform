package ru.pogman.questplatform

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ru.pogman.questplatform.data.PreparedMaterial
import ru.pogman.questplatform.domain.QuestFailure
import ru.pogman.questplatform.viewmodel.OrganizerViewModel

@Composable
fun rememberMaterialActions(organizer: OrganizerViewModel): (PreparedMaterial) -> Unit {
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    val handleResult: (Uri?) -> Unit = { uri ->
        val id = pendingId
        pendingId = null
        if (id != null) {
            if (uri == null) organizer.discardMaterial(id) else organizer.saveMaterial(id, uri)
        }
    }
    val pdf =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf"),
            handleResult,
        )
    return { material ->
        if (!organizer.state.value.authorized || pendingId != null)
            organizer.discardMaterial(material.id)
        else {
            pendingId = material.id
            try {
                pdf.launch(material.filename)
            } catch (_: ActivityNotFoundException) {
                pendingId = null
                organizer.discardMaterial(material.id)
                organizer.reportExternalFailure(QuestFailure.MATERIAL_EXPORT_FAILED)
            }
        }
    }
}
