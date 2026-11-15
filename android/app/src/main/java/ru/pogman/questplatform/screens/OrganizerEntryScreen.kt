package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.ActionMessage
import ru.pogman.questplatform.components.PasswordField
import ru.pogman.questplatform.components.ScreenContent
import ru.pogman.questplatform.viewmodel.ActionState

@Composable
fun OrganizerEntryScreen(
    passwordConfigured: Boolean,
    action: ActionState,
    onAuthenticate: (String, String) -> Unit,
    onReturn: () -> Unit,
) {
    // Passwords stay in memory and are not written into Android saved UI state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val handlePassword: (String) -> Unit = { password = it }
    val handleConfirmation: (String) -> Unit = { confirmation = it }
    val handleAuthenticate = { onAuthenticate(password, confirmation) }

    ScreenContent {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.organizer_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                stringResource(R.string.organizer_heading),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                stringResource(R.string.organizer_description),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Card(
            colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(
                        if (passwordConfigured) R.string.organizer_login_title
                        else R.string.organizer_setup_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(
                        if (passwordConfigured) R.string.organizer_login_description
                        else R.string.organizer_setup_description
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                PasswordField(
                    password,
                    stringResource(R.string.password_label),
                    !action.busy,
                    handlePassword,
                )
                if (!passwordConfigured) {
                    PasswordField(
                        confirmation,
                        stringResource(R.string.password_confirm_label),
                        !action.busy,
                        handleConfirmation,
                    )
                    Text(
                        stringResource(R.string.password_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                ActionMessage(action)
                Button(
                    onClick = handleAuthenticate,
                    enabled =
                        !action.busy &&
                            password.isNotBlank() &&
                            (passwordConfigured ||
                                password.length >= 8 && password == confirmation),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (passwordConfigured) R.string.login_action
                            else R.string.organizer_setup_action
                        )
                    )
                }
            }
        }

        Text(
            stringResource(R.string.organizer_privacy_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TextButton(onClick = onReturn, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.organizer_return_action))
        }
    }
}
