package ru.pogman.questplatform

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ru.pogman.questplatform.components.LoadingContent
import ru.pogman.questplatform.components.QuestTopBar
import ru.pogman.questplatform.navigation.OrganizerContent
import ru.pogman.questplatform.navigation.QuestRoute
import ru.pogman.questplatform.navigation.requiresOrganizer
import ru.pogman.questplatform.screens.BriefingScreen
import ru.pogman.questplatform.screens.GameEditorScreen
import ru.pogman.questplatform.screens.InstructionsScreen
import ru.pogman.questplatform.screens.OrganizerEntryScreen
import ru.pogman.questplatform.screens.OrganizerGamesScreen
import ru.pogman.questplatform.screens.PasswordChangeScreen
import ru.pogman.questplatform.screens.PhotoQuestScreen
import ru.pogman.questplatform.screens.PlayerHomeScreen
import ru.pogman.questplatform.screens.PrepareRunScreen
import ru.pogman.questplatform.screens.ResultScreen
import ru.pogman.questplatform.screens.RunHistoryScreen
import ru.pogman.questplatform.theme.QuestTheme
import ru.pogman.questplatform.viewmodel.OrganizerEvent
import ru.pogman.questplatform.viewmodel.OrganizerViewModel
import ru.pogman.questplatform.viewmodel.PlayerEvent
import ru.pogman.questplatform.viewmodel.PlayerViewModel

@Composable
fun QuestApp(organizer: OrganizerViewModel, player: PlayerViewModel) {
    val access by organizer.state.collectAsStateWithLifecycle()
    val organizerAction by organizer.action.collectAsStateWithLifecycle()
    val playerAction by player.action.collectAsStateWithLifecycle()
    val games by organizer.games.collectAsStateWithLifecycle()
    val history by organizer.history.collectAsStateWithLifecycle()
    val editor by organizer.editor.collectAsStateWithLifecycle()
    val run by player.run.collectAsStateWithLifecycle()
    val playerLoading by player.loading.collectAsStateWithLifecycle()
    val incorrect by player.incorrectAnswer.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(QuestRoute.Player)
    val currentRoute = backStack.last()

    val handlePlayer = {
        organizer.logout()
        backStack.clear()
        backStack.add(QuestRoute.Player)
        Unit
    }
    val handleBack = {
        if (
            currentRoute == QuestRoute.OrganizerGames ||
                currentRoute == QuestRoute.OrganizerEntry ||
                currentRoute.requiresOrganizer && !access.authorized
        ) {
            handlePlayer()
        } else if (backStack.size > 1) {
            organizer.clearError()
            player.clearError()
            backStack.removeLastOrNull()
        }
        Unit
    }
    val handleOrganizer = {
        organizer.clearError()
        backStack.add(
            if (access.authorized) QuestRoute.OrganizerGames else QuestRoute.OrganizerEntry
        )
        Unit
    }
    val handleInstructions = {
        backStack.add(QuestRoute.Instructions)
        Unit
    }
    val handleBriefing = {
        run?.let { backStack.add(QuestRoute.Briefing(it.id)) }
        Unit
    }
    val handleResult = {
        backStack.add(QuestRoute.Result)
        Unit
    }
    val handleCreate = { organizer.editGame(null) }
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
            access.authorized && currentRoute == QuestRoute.GameEditor && !organizerAction.busy,
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
                is OrganizerEvent.HandedOver -> {
                    player.resetInteraction()
                    backStack.clear()
                    backStack.add(QuestRoute.Player)
                    backStack.add(QuestRoute.Briefing(event.runId))
                }
            }
        }
    }
    LaunchedEffect(player) {
        player.events.collect { event ->
            when (event) {
                PlayerEvent.Started -> {
                    backStack.clear()
                    backStack.add(QuestRoute.Player)
                }
                PlayerEvent.OpenedPhoto -> backStack.add(QuestRoute.PhotoQuest)
                PlayerEvent.Solved -> {
                    backStack.clear()
                    backStack.add(QuestRoute.Player)
                    backStack.add(QuestRoute.Result)
                }
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

    val title =
        when (currentRoute) {
            QuestRoute.Instructions -> R.string.instructions_title
            QuestRoute.OrganizerEntry -> R.string.organizer_title
            QuestRoute.OrganizerGames -> R.string.organizer_games_title
            QuestRoute.GameEditor -> R.string.game_settings_title
            QuestRoute.RunHistory -> R.string.history_title
            QuestRoute.PasswordChange -> R.string.password_change_title
            QuestRoute.PhotoQuest -> R.string.photo_title
            QuestRoute.Result -> R.string.results_action
            is QuestRoute.PrepareRun -> R.string.prepare_action
            is QuestRoute.Briefing -> R.string.briefing_action
            else -> R.string.app_name
        }

    QuestTheme {
        Scaffold(
            topBar = {
                QuestTopBar(
                    title =
                        if (currentRoute.requiresOrganizer && !access.authorized)
                            R.string.organizer_title
                        else title,
                    canGoBack = backStack.size > 1,
                    onBack = handleBack,
                    onOrganizer = handleOrganizer,
                    onPlayer =
                        handlePlayer.takeIf { currentRoute.requiresOrganizer && access.authorized },
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
                                        handleInstructions,
                                        handleBriefing,
                                        player::openPhoto,
                                        handleResult,
                                    )
                                }
                                entry<QuestRoute.Instructions> {
                                    InstructionsScreen(onReturn = handleBack)
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
                                                organizer.photoUri(game),
                                                organizerAction,
                                                organizer::updateGame,
                                                photoActions.gallery,
                                                photoActions.camera,
                                                organizer::saveGame,
                                            )
                                        } ?: LoadingContent()
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
                                        ResultScreen(selected, handlePlayer)
                                    else if (selected != null)
                                        BriefingScreen(selected, playerAction, player::start)
                                    else LoadingContent()
                                }
                                entry<QuestRoute.PhotoQuest> {
                                    run?.let {
                                        PhotoQuestScreen(
                                            it,
                                            playerAction,
                                            incorrect,
                                            player::submitAnswer,
                                            handleResult,
                                        )
                                    } ?: LoadingContent()
                                }
                                entry<QuestRoute.Result> {
                                    run?.takeIf { it.finishedAt != null }
                                        ?.let { ResultScreen(it, handlePlayer) } ?: LoadingContent()
                                }
                            },
                    )
                }
            }
        }
    }
}
