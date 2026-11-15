package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.PasswordField
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun PasswordChangeScreen(action: ActionState, onChangePassword: (String, String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val handleCurrent: (String) -> Unit = { current = it }
    val handlePassword: (String) -> Unit = { password = it }
    val handleConfirmation: (String) -> Unit = { confirmation = it }
    val handleSave = { onChangePassword(current, password, confirmation) }

    ScreenContent {
        PasswordField(
            current,
            stringResource(R.string.current_password_label),
            !action.busy,
            handleCurrent,
        )
        PasswordField(
            password,
            stringResource(R.string.new_password_label),
            !action.busy,
            handlePassword,
        )
        PasswordField(
            confirmation,
            stringResource(R.string.password_confirm_label),
            !action.busy,
            handleConfirmation,
        )
        Text(stringResource(R.string.password_hint))
        ActionMessage(action)
        Button(
            onClick = handleSave,
            enabled =
                !action.busy &&
                    current.isNotEmpty() &&
                    password.length >= 8 &&
                    password == confirmation,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.password_save_action))
        }
    }
}
