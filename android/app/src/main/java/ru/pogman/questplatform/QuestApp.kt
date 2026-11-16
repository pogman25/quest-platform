package ru.pogman.questplatform

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ru.pogman.questplatform.components.LoadingContent
import ru.pogman.questplatform.components.QuestTopBar
import ru.pogman.questplatform.components.UnavailableContent
import ru.pogman.questplatform.domain.*
import ru.pogman.questplatform.navigation.OrganizerContent
import ru.pogman.questplatform.navigation.QuestRoute
import ru.pogman.questplatform.navigation.editsGame
import ru.pogman.questplatform.navigation.requiresOrganizer
import ru.pogman.questplatform.screens.BriefingScreen
import ru.pogman.questplatform.screens.GameEditorScreen
import ru.pogman.questplatform.screens.NfcWriterScreen
import ru.pogman.questplatform.screens.OrganizerEntryScreen
import ru.pogman.questplatform.screens.OrganizerGamesScreen
import ru.pogman.questplatform.screens.PasswordChangeScreen
import ru.pogman.questplatform.screens.PlayerHomeScreen
import ru.pogman.questplatform.screens.PrepareRunScreen
import ru.pogman.questplatform.screens.QuestScreen
import ru.pogman.questplatform.screens.QuestSettingsScreen
import ru.pogman.questplatform.screens.ResultScreen
import ru.pogman.questplatform.screens.RunHistoryScreen
import ru.pogman.questplatform.theme.QuestTheme
import ru.pogman.questplatform.viewmodel.OrganizerEvent
import ru.pogman.questplatform.viewmodel.OrganizerViewModel
import ru.pogman.questplatform.viewmodel.PlayerEvent
import ru.pogman.questplatform.viewmodel.PlayerViewModel

@Composable
fun QuestApp(organizer: OrganizerViewModel, player: PlayerViewModel) {
    val context = LocalContext.current
    val access by organizer.state.collectAsStateWithLifecycle()
    val organizerAction by organizer.action.collectAsStateWithLifecycle()
    val playerAction by player.action.collectAsStateWithLifecycle()
    val games by organizer.games.collectAsStateWithLifecycle()
    val history by organizer.history.collectAsStateWithLifecycle()
    val editor by organizer.editor.collectAsStateWithLifecycle()
    val run by player.run.collectAsStateWithLifecycle()
    val playerLoading by player.loading.collectAsStateWithLifecycle()
    val feedback by player.feedback.collectAsStateWithLifecycle()
    val nfcResult by organizer.nfcResult.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(QuestRoute.Player)
    val currentRoute = backStack.last()

    val showNotebook = {
        backStack.clear()
        backStack.add(QuestRoute.Player)
        Unit
    }
    val returnToPlayer = {
        while (
            backStack.size > 1 &&
                (backStack.last().requiresOrganizer ||
                    backStack.last() == QuestRoute.OrganizerEntry)
        ) {
            backStack.removeLastOrNull()
        }
    }
    val handlePlayer = {
        if (currentRoute.editsGame) {
            organizer.saveAndSwitchToPlayer()
        } else {
            organizer.leaveOrganizer()
            returnToPlayer()
        }
    }
    val handleLogout = {
        organizer.logout()
        returnToPlayer()
    }
    val handleBack = {
        if (
            currentRoute == QuestRoute.OrganizerGames ||
                currentRoute == QuestRoute.OrganizerEntry ||
                currentRoute.requiresOrganizer && !access.authorized
        ) {
            handlePlayer()
        } else if (backStack.size > 1) {
            if (currentRoute == QuestRoute.GameEditor) organizer.leaveOrganizer()
            if (currentRoute is QuestRoute.NfcWriter) organizer.stopNfcInteraction()
            organizer.clearError()
            player.clearError()
            backStack.removeLastOrNull()
        }
        Unit
    }
    val handleOrganizer = {
        player.resetInteraction()
        organizer.clearError()
        backStack.add(
            if (access.authorized) QuestRoute.OrganizerGames else QuestRoute.OrganizerEntry
        )
        Unit
    }
    val handleBriefing = {
        run?.let { backStack.add(QuestRoute.Briefing(it.id)) }
        Unit
    }
    val handleResult = {
        backStack.clear()
        backStack.add(QuestRoute.Player)
        backStack.add(QuestRoute.Result)
        Unit
    }
    val handleCreate = { organizer.editGame(null) }
    val handleQuestSettings: (QuestType) -> Unit = { type ->
        editor?.let { backStack.add(QuestRoute.QuestSettings(it.id, type.id)) }
        Unit
    }
    val saveMaterial = rememberMaterialActions(organizer)
    val handlePrepare: (String) -> Unit = {
        organizer.clearError()
        backStack.add(QuestRoute.PrepareRun(it))
    }
    val handleHistory = {
        backStack.add(QuestRoute.RunHistory)
        Unit
    }
    val handlePassword = {
        organizer.clearError()
        backStack.add(QuestRoute.PasswordChange)
        Unit
    }
    val photoActions =
        rememberPhotoActions(
            editor?.id,
            access.authorized && currentRoute is QuestRoute.QuestSettings && !organizerAction.busy,
            organizer,
        )

    LaunchedEffect(organizer) {
        organizer.events.collect { event ->
            when (event) {
                OrganizerEvent.Authenticated -> {
                    backStack.clear()
                    backStack.add(QuestRoute.Player)
                    backStack.add(QuestRoute.OrganizerGames)
                }
                OrganizerEvent.EditGame -> backStack.add(QuestRoute.GameEditor)
                OrganizerEvent.GameSaved,
                OrganizerEvent.PasswordChanged -> backStack.removeLastOrNull()
                OrganizerEvent.QuestSaved -> {
                    if (backStack.lastOrNull() is QuestRoute.QuestSettings)
                        backStack.removeLastOrNull()
                }
                is OrganizerEvent.MaterialReady -> saveMaterial(event.material)
                OrganizerEvent.MaterialSaved ->
                    Toast.makeText(context, R.string.material_saved, Toast.LENGTH_SHORT).show()
                OrganizerEvent.PlayerMode -> returnToPlayer()
                is OrganizerEvent.PrepareGame -> {
                    while (backStack.lastOrNull()?.editsGame == true) {
                        backStack.removeLastOrNull()
                    }
                    backStack.add(QuestRoute.PrepareRun(event.gameId))
                }
                is OrganizerEvent.NfcWriter ->
                    backStack.add(QuestRoute.NfcWriter(event.gameId, event.tagId))
                is OrganizerEvent.HandedOver -> {
                    player.resetInteraction()
                    backStack.clear()
                    backStack.add(QuestRoute.Player)
                    backStack.add(QuestRoute.Briefing(event.runId))
                }
            }
        }
    }
    LaunchedEffect(currentRoute) {
        if (currentRoute == QuestRoute.Instructions) showNotebook()
        if (currentRoute == QuestRoute.PhotoQuest) {
            backStack.removeLastOrNull()
            backStack.add(QuestRoute.QuestDetails(QuestType.PHOTO.id))
        }
    }
    LaunchedEffect(player) {
        player.events.collect { event ->
            when (event) {
                PlayerEvent.Started -> {
                    backStack.clear()
                    backStack.add(QuestRoute.Player)
                }
                is PlayerEvent.OpenedQuest -> backStack.add(QuestRoute.QuestDetails(event.type.id))
            }
        }
    }
    LaunchedEffect(access.loading, access.authorized, currentRoute) {
        if (!access.loading && !access.authorized && currentRoute.requiresOrganizer) {
            backStack.clear()
            backStack.add(QuestRoute.Player)
            backStack.add(QuestRoute.OrganizerEntry)
        }
    }

    val titleResource =
        when (currentRoute) {
            QuestRoute.OrganizerEntry -> R.string.organizer_title
            QuestRoute.OrganizerGames -> R.string.organizer_games_title
            QuestRoute.GameEditor -> R.string.game_settings_title
            QuestRoute.RunHistory -> R.string.history_title
            QuestRoute.PasswordChange -> R.string.password_change_title
            QuestRoute.PhotoQuest -> R.string.photo_title
            QuestRoute.Result -> R.string.results_action
            is QuestRoute.PrepareRun -> R.string.prepare_action
            is QuestRoute.Briefing -> R.string.briefing_action
            is QuestRoute.NfcWriter -> R.string.nfc_writer_title
            else -> R.string.app_name
        }
    val title =
        if (currentRoute is QuestRoute.QuestSettings)
            editor?.quests?.find { it.type.id == currentRoute.questId }?.type?.title
                ?: stringResource(R.string.game_settings_title)
        else if (currentRoute is QuestRoute.QuestDetails)
            run?.quests?.find { it.type.id == currentRoute.questId }?.title
                ?: stringResource(titleResource)
        else stringResource(titleResource)

    QuestTheme {
        Scaffold(
            topBar = {
                QuestTopBar(
                    title =
                        if (currentRoute.requiresOrganizer && !access.authorized)
                            stringResource(R.string.organizer_title)
                        else title,
                    canGoBack = backStack.size > 1,
                    onBack = handleBack,
                    onOrganizer =
                        handleOrganizer.takeIf {
                            !currentRoute.requiresOrganizer &&
                                currentRoute != QuestRoute.OrganizerEntry
                        },
                    onPlayer =
                        handlePlayer.takeIf { currentRoute.requiresOrganizer && access.authorized },
                    onLogout = handleLogout.takeIf { access.authorized },
                )
            }
        ) { contentPadding ->
            Box(
                Modifier.fillMaxSize()
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding)
                    .imePadding()
            ) {
                if (access.loading || playerLoading) {
                    LoadingContent()
                } else if (currentRoute.requiresOrganizer && !access.authorized) {
                    // Gate private content immediately, before the navigation-reset effect runs.
                    OrganizerEntryScreen(
                        access.passwordConfigured,
                        organizerAction,
                        organizer::authenticate,
                        handlePlayer,
                    )
                } else {
                    NavDisplay(
                        backStack = backStack,
                        onBack = handleBack,
                        modifier = Modifier.fillMaxSize(),
                        entryProvider =
                            entryProvider {
                                entry<QuestRoute.Player> {
                                    PlayerHomeScreen(
                                        run,
                                        playerAction,
                                        handleBriefing,
                                        player::openQuest,
                                        handleResult,
                                    )
                                }
                                entry<QuestRoute.Instructions> {
                                    LoadingContent()
                                }
                                entry<QuestRoute.OrganizerEntry> {
                                    OrganizerEntryScreen(
                                        access.passwordConfigured,
                                        organizerAction,
                                        organizer::authenticate,
                                        handlePlayer,
                                    )
                                }
                                entry<QuestRoute.OrganizerGames> {
                                    OrganizerContent(access.authorized) {
                                        OrganizerGamesScreen(
                                            games,
                                            organizerAction,
                                            handleCreate,
                                            organizer::editGame,
                                            handlePrepare,
                                            organizer::deleteGame,
                                            handleHistory,
                                            handlePassword,
                                        )
                                    }
                                }
                                entry<QuestRoute.GameEditor> {
                                    OrganizerContent(access.authorized) {
                                        editor?.let { game ->
                                            GameEditorScreen(
                                                game,
                                                organizerAction,
                                                organizer::updateGame,
                                                organizer::toggleQuest,
                                                handleQuestSettings,
                                                organizer::moveQuest,
                                                organizer::saveGame,
                                            )
                                        } ?: LoadingContent()
                                    }
                                }
                                entry<QuestRoute.QuestSettings> { route ->
                                    OrganizerContent(access.authorized) {
                                        val game = editor?.takeIf { it.id == route.gameId }
                                        val quest =
                                            game?.quests?.find { it.type.id == route.questId }
                                        if (game != null && quest != null) {
                                            val handleChange:
                                                ((QuestConfig) -> QuestConfig) -> Unit =
                                                { transform ->
                                                    organizer.updateGame {
                                                        it.updateQuest(quest.type, transform)
                                                    }
                                                }
                                            val handleExport = {
                                                organizer.prepareMaterial(quest.type)
                                            }
                                            QuestSettingsScreen(
                                                game.id,
                                                quest,
                                                organizerAction,
                                                organizer::photoUri,
                                                organizer::previewPhoto,
                                                handleChange,
                                                photoActions.gallery,
                                                photoActions.camera,
                                                organizer::openNfcWriter,
                                                handleExport,
                                                organizer::saveQuest,
                                            )
                                        } else
                                            UnavailableContent(
                                                R.string.error_quest_not_found,
                                                handleBack,
                                            )
                                    }
                                }
                                entry<QuestRoute.PrepareRun> { route ->
                                    OrganizerContent(access.authorized) {
                                        val game = games.find { it.id == route.gameId }
                                        val handleHandover: (String, String) -> Unit =
                                            { team, code ->
                                                organizer.prepareRun(route.gameId, team, code)
                                            }
                                        if (game != null)
                                            PrepareRunScreen(game, organizerAction, handleHandover)
                                        else LoadingContent()
                                    }
                                }
                                entry<QuestRoute.RunHistory> {
                                    OrganizerContent(access.authorized) {
                                        RunHistoryScreen(
                                            history,
                                            organizerAction,
                                            organizer::selectRun,
                                            organizer::restartRun,
                                        )
                                    }
                                }
                                entry<QuestRoute.PasswordChange> {
                                    OrganizerContent(access.authorized) {
                                        PasswordChangeScreen(
                                            organizerAction,
                                            organizer::changePassword,
                                        )
                                    }
                                }
                                entry<QuestRoute.Briefing> { route ->
                                    val selected = run?.takeIf { it.id == route.runId }
                                    if (selected?.finishedAt != null)
                                        ResultScreen(selected, showNotebook)
                                    else if (selected != null)
                                        BriefingScreen(selected, playerAction, player::start)
                                    else LoadingContent()
                                }
                                entry<QuestRoute.PhotoQuest> {
                                    LoadingContent()
                                }
                                entry<QuestRoute.QuestDetails> { route ->
                                    val selected = run
                                    val quest =
                                        selected?.quests?.find { it.type.id == route.questId }
                                    if (selected != null && selected.startedAt == null)
                                        BriefingScreen(selected, playerAction, player::start)
                                    else if (selected != null && quest != null) {
                                        val handleSubmit: (QuestAnswer) -> Unit = {
                                            player.submitAnswer(quest.type, it)
                                        }
                                        QuestScreen(
                                            selected.id,
                                            quest,
                                            selected.finishedAt != null,
                                            currentRoute == route,
                                            playerAction,
                                            feedback,
                                            handleSubmit,
                                            player::readNfc,
                                            player::reportScannerFailure,
                                            showNotebook,
                                            handleResult,
                                        )
                                    } else
                                        UnavailableContent(
                                            R.string.error_quest_not_found,
                                            showNotebook,
                                        )
                                }
                                entry<QuestRoute.NfcWriter> { route ->
                                    OrganizerContent(access.authorized) {
                                        val config =
                                            editor
                                                ?.takeIf { it.id == route.gameId }
                                                ?.quests
                                                ?.find { it.type == QuestType.NFC }
                                                ?.config as? NfcQuestConfig
                                        val tag = config?.tags?.find { it.id == route.tagId }
                                        val handleTag: (android.nfc.Tag, Boolean) -> Unit =
                                            { value, verify ->
                                                organizer.writeNfcTag(value, route.tagId, verify)
                                            }
                                        if (tag != null)
                                            NfcWriterScreen(
                                                tag,
                                                currentRoute == route,
                                                organizerAction,
                                                nfcResult,
                                                handleTag,
                                            )
                                        else
                                            UnavailableContent(
                                                R.string.error_game_invalid,
                                                handleBack,
                                            )
                                    }
                                }
                                entry<QuestRoute.Result> {
                                    run?.takeIf { it.finishedAt != null }
                                        ?.let { ResultScreen(it, showNotebook) } ?: LoadingContent()
                                }
                            },
                    )
                }
            }
        }
    }
}
