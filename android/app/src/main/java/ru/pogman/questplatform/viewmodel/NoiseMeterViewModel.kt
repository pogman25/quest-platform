package ru.pogman.questplatform.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pogman.questplatform.data.*
import ru.pogman.questplatform.domain.*

enum class NoisePhase {
    IDLE,
    WARMING_UP,
    CALIBRATING,
    MONITORING,
    PAUSED,
}

enum class NoiseMeterError {
    PERMISSION,
    UNAVAILABLE,
    SILENCED,
    CALIBRATION_TOO_LOUD,
}

data class NoiseMeterState(
    val phase: NoisePhase = NoisePhase.IDLE,
    val level: NoiseLevel? = null,
    val baseline: NoiseBaseline? = null,
    val input: MicrophoneInput? = null,
    val sensitivity: Float = 0.5f,
    val events: Int = 0,
    val alert: Boolean = false,
    val calibrationProgress: Float = 0f,
    val error: NoiseMeterError? = null,
) {
    val running: Boolean
        get() =
            phase == NoisePhase.WARMING_UP ||
                phase == NoisePhase.CALIBRATING ||
                phase == NoisePhase.MONITORING
}

class NoiseMeterViewModel(private val microphone: MicrophoneMeter) : ViewModel() {
    private val mutableState = MutableStateFlow(NoiseMeterState())
    val state = mutableState.asStateFlow()
    private var capture: Job? = null
    private var detector: NoiseDetector? = null
    private var ignoreUntil = 0L
    private var configurationKey: String? = null

    fun configure(key: String, settings: NoiseSettings?, force: Boolean) {
        if (!force && configurationKey == key) return
        stop()
        configurationKey = key
        mutableState.value =
            NoiseMeterState(
                phase = if (settings == null) NoisePhase.IDLE else NoisePhase.PAUSED,
                baseline = settings?.baseline,
                sensitivity = settings?.sensitivity ?: 0.5f,
            )
    }

    fun calibrate() = start(calibrate = true)

    fun resume() {
        if (state.value.baseline != null) start(calibrate = false)
    }

    private fun start(calibrate: Boolean) {
        if (capture?.isActive == true) return
        val previous = capture
        mutableState.update {
            it.copy(
                phase = NoisePhase.WARMING_UP,
                baseline = if (calibrate) null else it.baseline,
                level = null,
                alert = false,
                error = null,
                calibrationProgress = 0f,
                events = if (calibrate) 0 else it.events,
            )
        }
        capture = viewModelScope.launch {
            // A fast stop/start must wait until the former AudioRecord releases the microphone.
            previous?.cancelAndJoin()
            var startedAt: Long? = null
            var calibrationAt: Long? = null
            val levels = mutableListOf<NoiseLevel>()
            detector = null
            try {
                microphone.samples().collect { sample ->
                    val start = startedAt ?: sample.timeMs.also { startedAt = it }
                    mutableState.update { it.copy(level = sample.level, input = sample.input) }
                    if (sample.timeMs - start < 1000) return@collect
                    if (calibrate && state.value.baseline == null) {
                        val calibrationStart =
                            calibrationAt ?: sample.timeMs.also { calibrationAt = it }
                        levels.add(sample.level)
                        val elapsed = sample.timeMs - calibrationStart
                        mutableState.update {
                            it.copy(
                                phase = NoisePhase.CALIBRATING,
                                calibrationProgress = (elapsed / 5000f).coerceAtMost(1f),
                            )
                        }
                        if (elapsed < 5000 || levels.size < 20) return@collect
                        val baseline = calibrateNoise(levels)
                        if (!baseline.hasHeadroom) throw CalibrationTooLoud()
                        mutableState.update {
                            it.copy(baseline = baseline, calibrationProgress = 1f)
                        }
                        ignoreUntil = sample.timeMs + 750
                    }
                    val baseline = state.value.baseline ?: return@collect
                    val currentDetector = detector ?: NoiseDetector(baseline).also { detector = it }
                    val event =
                        sample.timeMs >= ignoreUntil &&
                            currentDetector.accept(
                                sample.level,
                                sample.timeMs,
                                state.value.sensitivity,
                            )
                    mutableState.update {
                        it.copy(
                            phase = NoisePhase.MONITORING,
                            events = it.events + if (event) 1 else 0,
                            alert = currentDetector.active,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                val failure =
                    when (error) {
                        is CalibrationTooLoud -> NoiseMeterError.CALIBRATION_TOO_LOUD
                        is SecurityException -> NoiseMeterError.PERMISSION
                        is MicrophoneException ->
                            when (error.failure) {
                                MicrophoneFailure.PERMISSION -> NoiseMeterError.PERMISSION
                                MicrophoneFailure.SILENCED -> NoiseMeterError.SILENCED
                                MicrophoneFailure.UNAVAILABLE -> NoiseMeterError.UNAVAILABLE
                            }
                        else -> NoiseMeterError.UNAVAILABLE
                    }
                mutableState.update {
                    it.copy(
                        phase = if (it.baseline == null) NoisePhase.IDLE else NoisePhase.PAUSED,
                        level = null,
                        alert = false,
                        error = failure,
                        calibrationProgress = 0f,
                    )
                }
            }
        }
    }

    fun stop() {
        capture?.cancel()
        detector = null
        mutableState.update {
            it.copy(
                phase = if (it.baseline == null) NoisePhase.IDLE else NoisePhase.PAUSED,
                level = null,
                alert = false,
                calibrationProgress = 0f,
            )
        }
    }

    fun setSensitivity(value: Float) {
        mutableState.update { it.copy(sensitivity = value.coerceIn(0f, 1f)) }
        ignoreUntil = SystemClock.elapsedRealtime() + 750
    }

    fun resetCounter() {
        ignoreUntil = SystemClock.elapsedRealtime() + 750
        mutableState.update { it.copy(events = 0) }
    }

    private class CalibrationTooLoud : IllegalStateException()
}
