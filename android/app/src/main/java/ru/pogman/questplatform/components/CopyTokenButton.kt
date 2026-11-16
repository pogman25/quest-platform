package ru.pogman.questplatform.components

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R

@Composable
fun CopyTokenButton(token: String) {
    val context = LocalContext.current
    val label = stringResource(R.string.copy_token_action)
    val handleCopy: () -> Unit = {
        context
            .getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText(label, token))
    }
    TextButton(onClick = handleCopy, enabled = token.isNotBlank()) { Text(label) }
}
