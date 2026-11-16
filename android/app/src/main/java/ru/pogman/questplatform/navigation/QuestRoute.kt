package ru.pogman.questplatform.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface QuestRoute : NavKey {
    @Serializable data object Player : QuestRoute

    // Redirect old saved navigation state after removing the generic instructions screen.
    @Serializable data object Instructions : QuestRoute

    @Serializable data object OrganizerEntry : QuestRoute

    @Serializable data object OrganizerGames : QuestRoute

    @Serializable data object GameEditor : QuestRoute

    @Serializable data class QuestSettings(val gameId: String, val questId: String) : QuestRoute

    // Old saved state is redirected to the unified game screen's team section.
    @Serializable data class PrepareRun(val gameId: String) : QuestRoute

    @Serializable data object RunHistory : QuestRoute

    @Serializable data object PasswordChange : QuestRoute

    // Restore old saved navigation without reopening the removed standalone prototype.
    @Serializable data object NoiseMeter : QuestRoute

    @Serializable data class SilentCalibration(val gameId: String) : QuestRoute

    @Serializable data class Briefing(val runId: String) : QuestRoute

    @Serializable data object PhotoQuest : QuestRoute

    @Serializable data class QuestDetails(val questId: String) : QuestRoute

    @Serializable data class NfcWriter(val gameId: String, val tagId: String) : QuestRoute

    @Serializable data object Result : QuestRoute
}

val NavKey.requiresOrganizer: Boolean
    get() =
        this == QuestRoute.OrganizerGames ||
            this == QuestRoute.GameEditor ||
            this is QuestRoute.QuestSettings ||
            this is QuestRoute.PrepareRun ||
            this == QuestRoute.RunHistory ||
            this == QuestRoute.PasswordChange ||
            this == QuestRoute.NoiseMeter ||
            this is QuestRoute.SilentCalibration ||
            this is QuestRoute.NfcWriter

// All these routes edit the same game draft, kept by OrganizerViewModel.
val NavKey.editsGame: Boolean
    get() =
        this == QuestRoute.GameEditor ||
            this is QuestRoute.QuestSettings ||
            this is QuestRoute.NfcWriter ||
            this is QuestRoute.SilentCalibration
