package ru.pogman.questplatform.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import ru.pogman.questplatform.AppContainer
import ru.pogman.questplatform.domain.Game
import ru.pogman.questplatform.domain.normalizeAnswer

data class OrganizerState(
    val loading: Boolean = true,
    val passwordConfigured: Boolean = false,
    val authorized: Boolean = false,
)

sealed interface OrganizerEvent {
    data object Authenticated : OrganizerEvent

    data object EditGame : OrganizerEvent

    data object GameSaved : OrganizerEvent

    data object PasswordChanged : OrganizerEvent

    data class HandedOver(val runId: String) : OrganizerEvent
}

class OrganizerViewModel(private val container: AppContainer) : ActionViewModel() {
    private val access = container.access
    private val repository = container.repository
    private val channel = Channel<OrganizerEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private val mutableEditor = MutableStateFlow<Game?>(null)
    val editor = mutableEditor.asStateFlow()

    val state =
        combine(access.passwordConfigured, access.authorized) { configured, authorized ->
                OrganizerState(
                    loading = false,
                    passwordConfigured = configured,
                    authorized = authorized,
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, OrganizerState())

    val games =
        repository.games.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history =
        repository.history.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    fun authenticate(password: String, confirmation: String) = perform {
        if (state.value.passwordConfigured) access.login(password)
        else access.setup(password, confirmation)
        channel.send(OrganizerEvent.Authenticated)
    }

    fun changePassword(current: String, password: String, confirmation: String) = perform {
        access.changePassword(current, password, confirmation)
        channel.send(OrganizerEvent.PasswordChanged)
    }

    fun editGame(id: String?) = perform {
        access.requireOrganizer()
        mutableEditor.value = id?.let { repository.loadGame(it) } ?: repository.newGame()
        channel.send(OrganizerEvent.EditGame)
    }

    fun updateGame(game: Game) {
        if (!access.authorized.value) {
            return
        }
        mutableEditor.value = game
        clearError()
    }

    fun importPhoto(uri: Uri) = perform {
        access.requireOrganizer()
        val reference = container.photos.import(uri)
        access.requireOrganizer()
        mutableEditor.value =
            mutableEditor.value?.let { game ->
                game.copy(photo = game.photo.copy(photoRef = reference))
            }
    }

    fun photoUri(game: Game): String = container.photos.displayUri(game.photo.photoRef)

    fun saveGame() = perform {
        val game = mutableEditor.value ?: return@perform
        val answers =
            game.photo.answers
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinctBy(::normalizeAnswer)
        repository.saveGame(game.copy(photo = game.photo.copy(answers = answers)))
        mutableEditor.value = null
        channel.send(OrganizerEvent.GameSaved)
    }

    fun deleteGame(id: String) = perform { repository.deleteGame(id) }

    fun prepareRun(gameId: String, teamName: String, code: String) = perform {
        val id = repository.prepareRun(gameId, teamName, code)
        repository.activeRun.first { it?.id == id }
        closeSession()
        channel.send(OrganizerEvent.HandedOver(id))
    }

    fun selectRun(id: String) = perform {
        repository.selectRun(id)
        repository.activeRun.first { it?.id == id }
        closeSession()
        channel.send(OrganizerEvent.HandedOver(id))
    }

    fun logout() {
        cancelPending()
        closeSession()
    }

    private fun closeSession() {
        access.logout()
        mutableEditor.value = null
        clearError()
    }
}
