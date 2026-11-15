package ru.pogman.questplatform.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface QuestRoute : NavKey {
    @Serializable
    data object Player : QuestRoute

    @Serializable
    data object Instructions : QuestRoute

    @Serializable
    data object OrganizerEntry : QuestRoute
}
