package ru.pogman.questplatform.screens

import android.nfc.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.*
import ru.pogman.questplatform.domain.NfcTagConfig
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun NfcWriterScreen(
    tag: NfcTagConfig,
    active: Boolean,
    action: ActionState,
    result: Pair<String, Boolean>?,
    onTag: (Tag, Boolean) -> Unit,
) {
    var verify by rememberSaveable(tag.id) { mutableStateOf(false) }
    val handleTag: (Tag) -> Unit = { onTag(it, verify) }
    val handleWrite = { verify = false }
    val handleVerify = { verify = true }
    LaunchedEffect(result) { if (result?.first == tag.id && !result.second) verify = true }
    ScreenContent {
        Text(tag.label, style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(if (verify) R.string.nfc_verify_hint else R.string.nfc_write_hint))
        NfcReader(active, handleTag)
        ActionMessage(action)
        if (result?.first == tag.id)
            Text(stringResource(if (result.second) R.string.nfc_verified else R.string.nfc_written))
        Button(onClick = handleWrite, enabled = verify && !action.busy) {
            Text(stringResource(R.string.nfc_write_action))
        }
        OutlinedButton(onClick = handleVerify, enabled = !verify && !action.busy) {
            Text(stringResource(R.string.nfc_verify_action))
        }
    }
}
