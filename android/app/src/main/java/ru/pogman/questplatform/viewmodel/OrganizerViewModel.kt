package ru.pogman.questplatform.viewmodel

import android.net.Uri
import android.nfc.Tag
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pogman.questplatform.AppContainer
import ru.pogman.questplatform.data.PreparedMaterial
import ru.pogman.questplatform.data.defaultQuest
import ru.pogman.questplatform.domain.*

data class OrganizerState(
    val loading: Boolean = true,
    val passwordConfigured: Boolean = false,
    val authorized: Boolean = false,
)

sealed interface OrganizerEvent {
    data object Authenticated : OrganizerEvent

    data object EditGame : OrganizerEvent

    data object GameSaved : OrganizerEvent

    data object QuestSaved : OrganizerEvent

    data object MaterialSaved : OrganizerEvent

    data class MaterialReady(val material: PreparedMaterial) : OrganizerEvent

    data object PasswordChanged : OrganizerEvent

    data object PlayerMode : OrganizerEvent

    data class PrepareGame(val gameId: String) : OrganizerEvent

    data class NfcWriter(val gameId: String, val tagId: String) : OrganizerEvent

    data class HandedOver(val runId: String) : OrganizerEvent
}

class OrganizerViewModel(private val container: AppContainer) : ActionViewModel() {
    private val access = container.access
    private val repository = container.repository
    private val channel = Channel<OrganizerEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private val mutableEditor = MutableStateFlow<Game?>(null)
    val editor = mutableEditor.asStateFlow()
    private val mutableNfcResult = MutableStateFlow<Pair<String, Boolean>?>(null)
    val nfcResult = mutableNfcResult.asStateFlow()

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

    fun updateGame(transform: (Game) -> Game) {
        if (!access.authorized.value) {
            return
        }
        // Apply field edits to the latest draft, including a photo imported asynchronously.
        mutableEditor.update { game -> game?.let(transform) }
        clearError()
    }

    fun reportExternalFailure(failure: QuestFailure) = reportFailure(failure)

    fun toggleQuest(type: QuestType) = updateGame { game ->
        if (game.quests.any { it.type == type }) {
            if (game.quests.size > 1) game.copy(quests = game.quests.filter { it.type != type })
            else game
        } else game.copy(quests = game.quests + defaultQuest(type))
    }

    fun moveQuest(type: QuestType, target: Int) = updateGame { game ->
        val quests = game.quests.toMutableList()
        val index = quests.indexOfFirst { it.type == type }
        if (index >= 0 && target in quests.indices) {
            val item = quests.removeAt(index)
            quests.add(target, item)
        }
        game.copy(quests = quests)
    }

    fun clearNfcResult() {
        mutableNfcResult.value = null
        clearError()
    }

    fun openNfcWriter(tagId: String) = perform {
        val game = saveDraft() ?: return@perform
        val config = game.quests.find { it.type == QuestType.NFC }?.config as? NfcQuestConfig
        if (config?.tags?.none { it.id == tagId } != false)
            throw QuestException(QuestFailure.GAME_INVALID)
        clearNfcResult()
        channel.send(OrganizerEvent.NfcWriter(game.id, tagId))
    }

    fun stopNfcInteraction() {
        cancelPending()
        clearNfcResult()
    }

    fun writeNfcTag(tag: Tag, tagId: String, verify: Boolean) = perform {
        mutableNfcResult.value = null
        access.requireOrganizer()
        val config =
            mutableEditor.value?.quests?.find { it.type == QuestType.NFC }?.config
                as? NfcQuestConfig
        val expected =
            config?.tags?.find { it.id == tagId } ?: throw QuestException(QuestFailure.GAME_INVALID)
        if (verify) {
            if (container.nfc.read(tag).trim() != expected.token.trim())
                throw QuestException(QuestFailure.NFC_WRONG_TAG)
        } else container.nfc.write(tag, expected.token.trim())
        access.requireOrganizer()
        mutableNfcResult.value = tagId to verify
    }

    fun importPhoto(
        uri: Uri,
        gameId: String,
        target: PhotoTarget = PhotoTarget(QuestType.PHOTO),
        fromCamera: Boolean = false,
    ) {
        if (action.value.busy) {
            if (fromCamera) container.cameraPhotos.discard(uri)
            return
        }
        perform {
            var imported: String? = null
            try {
                access.requireOrganizer()
                if (mutableEditor.value?.id != gameId) return@perform
                val reference = container.photos.import(uri)
                imported = reference
                access.requireOrganizer()
                val game = mutableEditor.value
                if (
                    game?.id == gameId &&
                        game.quests.any { quest ->
                            quest.type == target.type &&
                                when (val config = quest.config) {
                                    is PhotoQuestConfig,
                                    is QrQuestConfig -> target.cellId == null
                                    is ChoiceQuestConfig ->
                                        config.cells.any { it.id == target.cellId }
                                    else -> false
                                }
                        }
                ) {
                    mutableEditor.value =
                        game.updateQuest(target.type) { config ->
                            when (config) {
                                is PhotoQuestConfig -> config.copy(photoRef = reference)
                                is QrQuestConfig -> config.copy(photoRef = reference)
                                is ChoiceQuestConfig ->
                                    config.copy(
                                        cells =
                                            config.cells.map {
                                                if (it.id == target.cellId)
                                                    it.copy(photoRef = reference)
                                                else it
                                            }
                                    )
                                else -> config
                            }
                        }
                    imported = null
                }
            } finally {
                imported?.let(container.photos::discard)
                if (fromCamera) container.cameraPhotos.discard(uri)
            }
        }
    }

    fun photoUri(reference: String): String = container.photos.displayUri(reference)

    suspend fun previewPhoto(reference: String) = container.photos.bitmap(reference, 1000)

    fun saveQuest() = perform {
        saveDraft()
        channel.send(OrganizerEvent.QuestSaved)
    }

    fun prepareMaterial(type: QuestType) = perform {
        val game = saveDraft() ?: return@perform
        val material = container.materials.prepare(game, type)
        if (!access.authorized.value || mutableEditor.value?.id != game.id) {
            container.materials.discard(material.id)
            return@perform
        }
        channel.send(OrganizerEvent.MaterialReady(material))
    }

    fun saveMaterial(id: String, uri: Uri) = perform {
        try {
            access.requireOrganizer()
            container.materials.save(id, uri)
            channel.send(OrganizerEvent.MaterialSaved)
        } finally {
            container.materials.discard(id)
        }
    }

    fun discardMaterial(id: String) = container.materials.discard(id)

    fun saveGame() = perform {
        saveDraft()
        channel.send(OrganizerEvent.GameSaved)
    }

    fun saveAndSwitchToPlayer() = perform {
        val game = saveDraft() ?: return@perform
        val active = repository.activeRun.first()
        if (
            active != null &&
                active.gameId == game.id &&
                active.quests.map { it.type } == game.quests.map { it.type }
        ) {
            mutableEditor.value = null
            channel.send(OrganizerEvent.PlayerMode)
        } else {
            channel.send(OrganizerEvent.PrepareGame(game.id))
        }
    }

    private suspend fun saveDraft(): Game? {
        val game = mutableEditor.value ?: return null
        val normalized =
            game.copy(
                title = game.title.trim(),
                quests =
                    game.quests.map { quest ->
                        quest.copy(
                            config =
                                when (val config = quest.config) {
                                    is PhotoQuestConfig ->
                                        config.copy(
                                            answers =
                                                config.answers
                                                    .map(String::trim)
                                                    .filter(String::isNotEmpty)
                                                    .distinctBy(::normalizeAnswer)
                                        )
                                    is NfcQuestConfig ->
                                        config.copy(
                                            tags =
                                                config.tags.map {
                                                    it.copy(
                                                        label = it.label.trim(),
                                                        token = it.token.trim(),
                                                    )
                                                }
                                        )
                                    is QrQuestConfig -> config.copy(token = config.token.trim())
                                    is WordQuestConfig -> config.copy(word = config.word.trim())
                                    is ChoiceQuestConfig ->
                                        config.copy(
                                            cells =
                                                config.cells.map {
                                                    it.copy(
                                                        label = it.label.trim(),
                                                        symbol = it.symbol.trim(),
                                                    )
                                                }
                                        )
                                }
                        )
                    },
            )
        repository.saveGame(normalized)
        mutableEditor.value = normalized
        return normalized
    }

    fun leaveOrganizer() {
        cancelPending()
        mutableEditor.value = null
        clearError()
    }

    fun deleteGame(id: String) = perform { repository.deleteGame(id) }

    fun prepareRun(teamName: String, code: String) = perform {
        val game = saveDraft() ?: return@perform
        val id = repository.prepareRun(game.id, teamName, code)
        repository.activeRun.first { it?.id == id }
        mutableEditor.value = null
        channel.send(OrganizerEvent.HandedOver(id))
    }

    fun selectRun(id: String) = perform {
        repository.selectRun(id)
        repository.activeRun.first { it?.id == id }
        channel.send(OrganizerEvent.HandedOver(id))
    }

    fun restartRun(id: String) = perform {
        val restartedId = repository.restartRun(id)
        repository.activeRun.first { it?.id == restartedId }
        channel.send(OrganizerEvent.HandedOver(restartedId))
    }

    fun logout() {
        leaveOrganizer()
        access.logout()
    }
}
