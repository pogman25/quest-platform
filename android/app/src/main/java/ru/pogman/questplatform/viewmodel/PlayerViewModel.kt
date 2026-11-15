package ru.pogman.questplatform.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import ru.pogman.questplatform.data.QuestRepository

sealed interface PlayerEvent {
    data object Started : PlayerEvent

    data object OpenedPhoto : PlayerEvent

    data object Solved : PlayerEvent
}

class PlayerViewModel(private val repository: QuestRepository) : ActionViewModel() {
    private val mutableLoading = MutableStateFlow(true)
    val loading = mutableLoading.asStateFlow()
    val run =
        repository.activeRun
            .onEach { mutableLoading.value = false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val channel = Channel<PlayerEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private val incorrect = MutableStateFlow(false)
    val incorrectAnswer = incorrect.asStateFlow()

    fun resetInteraction() {
        cancelPending()
        incorrect.value = false
    }

    fun start() = perform {
        repository.startRun()
        channel.send(PlayerEvent.Started)
    }

    fun openPhoto() = perform {
        incorrect.value = false
        repository.openPhoto()
        channel.send(PlayerEvent.OpenedPhoto)
    }

    fun submitAnswer(answer: String) = perform {
        incorrect.value = false
        if (repository.submitAnswer(answer)) {
            channel.send(PlayerEvent.Solved)
        } else {
            incorrect.value = true
        }
    }
}
