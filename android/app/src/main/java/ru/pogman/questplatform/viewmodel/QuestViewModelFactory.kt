package ru.pogman.questplatform.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.pogman.questplatform.AppContainer

class QuestViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when (modelClass) {
            OrganizerViewModel::class.java -> OrganizerViewModel(container) as T
            PlayerViewModel::class.java -> PlayerViewModel(container.repository, container.nfc) as T
            NoiseMeterViewModel::class.java -> NoiseMeterViewModel(container.microphone) as T
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
}
