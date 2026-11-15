package ru.pogman.questplatform.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.pogman.questplatform.domain.QuestException
import ru.pogman.questplatform.domain.QuestFailure

data class ActionState(val busy: Boolean = false, val error: QuestFailure? = null)

abstract class ActionViewModel : ViewModel() {
    private val mutableAction = MutableStateFlow(ActionState())
    val action = mutableAction.asStateFlow()
    private var pending: Job? = null

    fun clearError() {
        mutableAction.value = mutableAction.value.copy(error = null)
    }

    protected fun reportFailure(failure: QuestFailure) {
        mutableAction.value = mutableAction.value.copy(error = failure)
    }

    protected fun cancelPending() {
        pending?.cancel()
        pending = null
        mutableAction.value = ActionState()
    }

    protected fun perform(block: suspend () -> Unit) {
        if (mutableAction.value.busy) {
            return
        }
        mutableAction.value = ActionState(busy = true)
        pending = viewModelScope.launch {
            try {
                block()
                mutableAction.value = ActionState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: QuestException) {
                mutableAction.value = ActionState(error = error.failure)
            } catch (_: Exception) {
                mutableAction.value = ActionState(error = QuestFailure.STORAGE_ERROR)
            }
        }
    }
}
