package ru.pogman.questplatform.viewmodel

import android.nfc.Tag
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import ru.pogman.questplatform.data.NfcTagStore
import ru.pogman.questplatform.data.QuestRepository
import ru.pogman.questplatform.domain.*

sealed interface PlayerEvent {
    data object Started : PlayerEvent

    data class OpenedQuest(val type: QuestType) : PlayerEvent
}

class PlayerViewModel(private val repository: QuestRepository, private val nfc: NfcTagStore) :
    ActionViewModel() {
    private val mutableLoading = MutableStateFlow(true)
    val loading = mutableLoading.asStateFlow()
    private val mutableFeedback = MutableStateFlow<AnswerVerdict?>(null)
    val feedback = mutableFeedback.asStateFlow()
    val run =
        repository.activeRun
            .onEach { mutableLoading.value = false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val channel = Channel<PlayerEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()

    fun resetInteraction() {
        cancelPending()
        mutableFeedback.value = null
    }

    fun start() {
        val id = run.value?.id ?: return
        perform {
            repository.startRun(id)
            channel.send(PlayerEvent.Started)
        }
    }

    fun openQuest(type: QuestType) {
        val id = run.value?.id ?: return
        perform {
            mutableFeedback.value = null
            repository.openQuest(type, id)
            channel.send(PlayerEvent.OpenedQuest(type))
        }
    }

    fun submitAnswer(type: QuestType, answer: QuestAnswer) {
        val id = run.value?.id ?: return
        perform { resolve(type, answer, id) }
    }

    fun readNfc(tag: Tag) {
        val id = run.value?.id ?: return
        perform { resolve(QuestType.NFC, QuestAnswer.Token(nfc.read(tag)), id) }
    }

    fun reportScannerFailure() = reportFailure(QuestFailure.CAMERA_UNAVAILABLE)

    private suspend fun resolve(type: QuestType, answer: QuestAnswer, runId: String) {
        mutableFeedback.value = null
        mutableFeedback.value = repository.submitAnswer(type, answer, runId)
    }
}
