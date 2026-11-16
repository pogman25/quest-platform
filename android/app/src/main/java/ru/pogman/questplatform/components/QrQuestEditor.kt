package ru.pogman.questplatform.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.UUID
import ru.pogman.questplatform.R
import ru.pogman.questplatform.data.questQrModules
import ru.pogman.questplatform.domain.*

@Composable
fun QrQuestEditor(
    config: QrQuestConfig,
    enabled: Boolean,
    onChange: ((QrQuestConfig) -> QrQuestConfig) -> Unit,
    onGallery: (PhotoTarget) -> Unit,
    onCamera: (PhotoTarget) -> Unit,
    loadPhoto: suspend (String) -> Bitmap,
    onExport: () -> Unit,
) {
    val handleGallery = { onGallery(PhotoTarget(QuestType.QR)) }
    val handleCamera = { onCamera(PhotoTarget(QuestType.QR)) }
    val handleInstruction: (String) -> Unit = { value ->
        onChange { it.copy(instruction = value.take(500)) }
    }
    val handleToken: (String) -> Unit = { value -> onChange { it.copy(token = value.take(512)) } }
    val handleNewToken = {
        val token = UUID.randomUUID().toString()
        onChange { it.copy(token = token) }
    }
    var side by rememberSaveable { mutableIntStateOf(0) }
    val handleFront = { side = 0 }
    val handleBack = { side = 1 }
    val handleSize: (Int) -> Unit = { value -> onChange { it.copy(sizeMm = value) } }
    val handleCover = { onChange { it.copy(fit = PictureFit.COVER) } }
    val handleContain = { onChange { it.copy(fit = PictureFit.CONTAIN) } }
    val pages =
        remember(config) {
            runCatching { puzzlePages(config, questQrModules(config.token.trim())) }.getOrNull()
        }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = handleGallery, enabled = enabled) {
            Text(stringResource(R.string.choose_photo_action))
        }
        Button(onClick = handleCamera, enabled = enabled) {
            Text(stringResource(R.string.take_photo_action))
        }
    }
    NumberOptions(
        stringResource(R.string.puzzle_size_label),
        listOf(120, 150, 180),
        config.sizeMm,
        enabled,
        " мм",
        handleSize,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            config.fit == PictureFit.COVER,
            handleCover,
            label = { Text(stringResource(R.string.picture_cover)) },
            enabled = enabled,
        )
        FilterChip(
            config.fit == PictureFit.CONTAIN,
            handleContain,
            label = { Text(stringResource(R.string.picture_contain)) },
            enabled = enabled,
        )
    }
    OutlinedTextField(
        config.instruction,
        handleInstruction,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.instruction_label)) },
        minLines = 2,
        enabled = enabled,
    )
    OutlinedTextField(
        config.token,
        handleToken,
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.qr_token_label)) },
        singleLine = true,
        enabled = enabled,
    )
    CopyTokenButton(config.token)
    TextButton(onClick = handleNewToken, enabled = enabled) {
        Text(stringResource(R.string.new_token_action))
    }
    Text(stringResource(R.string.qr_token_hint))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(side == 0, handleFront, label = { Text(stringResource(R.string.puzzle_front)) })
        FilterChip(side == 1, handleBack, label = { Text(stringResource(R.string.puzzle_back)) })
    }
    if (pages != null) PrintPreview(pages.pages[side], loadPhoto)
    else Text(stringResource(R.string.qr_generation_error))
    Text(stringResource(R.string.puzzle_print_hint))
    Button(onExport, enabled = enabled && pages != null, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.download_pdf_action))
    }
}
