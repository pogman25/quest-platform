package ru.pogman.questplatform.navigation

import androidx.compose.runtime.Composable
import ru.pogman.questplatform.components.LoadingContent

@Composable
fun OrganizerContent(authorized: Boolean, content: @Composable () -> Unit) {
    // Outgoing navigation scenes must also hide private content after explicit logout.
    if (authorized) content() else LoadingContent()
}
