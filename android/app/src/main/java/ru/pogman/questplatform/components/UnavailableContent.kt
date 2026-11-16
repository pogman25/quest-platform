package ru.pogman.questplatform.components

import androidx.annotation.StringRes
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R

@Composable
fun UnavailableContent(@StringRes message: Int, onReturn: () -> Unit) {
    ScreenContent {
        Text(stringResource(message))
        Button(onClick = onReturn) { Text(stringResource(R.string.back_action)) }
    }
}
