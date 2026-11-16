package ru.pogman.questplatform.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.pogman.questplatform.data.*
import ru.pogman.questplatform.domain.*

data class SilentSearchState(
    val runId: String? = null,
    val phase: NoisePhase = NoisePhase.IDLE,
    val settings: NoiseSettings? = null,
    val level: NoiseLevel? = null,
    val events: Int = 0,
    val alert: Boolean = false,
    val submitting: Boolean = false,
    val verdict: AnswerVerdict? = null,
    val microphoneError: NoiseMeterError? = null,
    val failure: QuestFailure? = null,
) {
    val running: Boolean
        get() = phase == NoisePhase.WARMING_UP || phase == NoisePhase.MONITORING
}

class SilentSearchViewModel(
    private val repository: QuestRepository,
    private val microphone: MicrophoneMeter,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SilentSearchState())
    val state = mutableState.asStateFlow()
    private var capture: Job? = null
    private var alertJob: Job? = null
    private var pendingCode: Pair<String, Long>? = null

    fun bind(runId: String, quest: PlayerQuest) {
        if (state.value.runId == runId) return
        stop()
        mutableState.value =
            SilentSearchState(
                runId = runId,
                settings = quest.noiseSettings,
                events = quest.noiseCount,
            )
        showAlert(runId, quest.lastNoiseAt)
    }

    fun start() {
        val runId = state.value.runId ?: return
        if (capture?.isActive == true) return
        val previous = capture
        pendingCode = null
        mutableState.update {
            it.copy(
                phase = NoisePhase.WARMING_UP,
                submitting = false,
                failure = null,
                microphoneError = null,
                verdict = null,
            )
        }
        capture = viewModelScope.launch {
            previous?.cancelAndJoin()
            val sessionId = UUID.randomUUID().toString()
            var setup: SilentSearchSetup? = null
            var detector: NoiseDetector? = null
            var active = false
            var startedAt: Long? = null
            try {
                microphone.samples().buffer(0).collect { sample ->
                    currentCoroutineContext().ensureActive()
                    mutableState.update { it.copy(level = sample.level) }
                    val started = startedAt ?: sample.timeMs.also { startedAt = it }
                    if (sample.timeMs - started < 1000) return@collect
                    if (setup == null) {
                        val saved = persist { repository.beginSilentSearch(runId, sessionId) }
                        setup = saved
                        active = saved.progress.active
                        detector = NoiseDetector(saved.settings.baseline, active)
                        currentCoroutineContext().ensureActive()
                        mutableState.update {
                            it.copy(
                                phase = NoisePhase.MONITORING,
                                settings = saved.settings,
                                events = saved.progress.count,
                            )
                        }
                        showAlert(runId, saved.progress.lastNoiseAt)
                    }
                    val currentSetup = checkNotNull(setup)
                    val currentDetector = detector ?: return@collect
                    currentDetector.accept(
                        sample.level,
                        sample.timeMs,
                        currentSetup.settings.sensitivity,
                    )
                    if (active != currentDetector.active) {
                        // Finish an already detected event even if navigation cancels the
                        // collector.
                        val progress = persist {
                            repository.recordSilentNoise(runId, sessionId, currentDetector.active)
                        }
                        active = progress.active
                        currentCoroutineContext().ensureActive()
                        mutableState.update { it.copy(events = progress.count) }
                        showAlert(runId, progress.lastNoiseAt)
                    }
                    val pending = pendingCode
                    if (pending != null && sample.timeMs >= pending.second) {
                        pendingCode = null
                        // A fresh microphone frame is processed before accepting the code.
                        val verdict = persist {
                            repository.submitSilentCode(runId, sessionId, pending.first)
                        }
                        currentCoroutineContext().ensureActive()
                        mutableState.update { it.copy(verdict = verdict, submitting = false) }
                        if (verdict == AnswerVerdict.CORRECT) throw SearchCompleted()
                    }
                }
            } catch (_: SearchCompleted) {
                mutableState.update { it.copy(phase = NoisePhase.PAUSED, level = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                val microphoneError =
                    when (error) {
                        is SecurityException -> NoiseMeterError.PERMISSION
                        is MicrophoneException ->
                            when (error.failure) {
                                MicrophoneFailure.PERMISSION -> NoiseMeterError.PERMISSION
                                MicrophoneFailure.SILENCED -> NoiseMeterError.SILENCED
                                MicrophoneFailure.UNAVAILABLE -> NoiseMeterError.UNAVAILABLE
                            }
                        is QuestException -> null
                        else -> NoiseMeterError.UNAVAILABLE
                    }
                mutableState.update {
                    it.copy(
                        phase = NoisePhase.PAUSED,
                        level = null,
                        submitting = false,
                        microphoneError = microphoneError,
                        failure = (error as? QuestException)?.failure,
                    )
                }
            } finally {
                repository.endSilentSearch(sessionId)
            }
        }
    }

    fun stop() {
        pendingCode = null
        capture?.cancel()
        mutableState.update { it.copy(phase = NoisePhase.PAUSED, level = null, submitting = false) }
    }

    fun submit(code: String) {
        if (
            state.value.phase != NoisePhase.MONITORING ||
                state.value.submitting ||
                code.length != 4 ||
                code.any { it !in '0'..'9' }
        )
            return
        pendingCode = code to SystemClock.elapsedRealtime()
        mutableState.update { it.copy(submitting = true, verdict = null) }
    }

    private fun showAlert(runId: String, time: Long?) {
        alertJob?.cancel()
        val remaining = ((time ?: 0L) + 3000 - clock.millis()).coerceAtLeast(0)
        mutableState.update { it.copy(alert = remaining > 0) }
        if (remaining > 0)
            alertJob = viewModelScope.launch {
                delay(remaining)
                if (state.value.runId == runId) mutableState.update { it.copy(alert = false) }
            }
    }

    private suspend fun <T> persist(block: suspend () -> T): T =
        withContext(NonCancellable) {
            try {
                block()
            } catch (error: QuestException) {
                throw error
            } catch (error: Exception) {
                throw QuestException(QuestFailure.STORAGE_ERROR)
            }
        }

    private class SearchCompleted : IllegalStateException()
}
