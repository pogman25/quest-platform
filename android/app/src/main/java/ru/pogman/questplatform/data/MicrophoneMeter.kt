package ru.pogman.questplatform.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.pogman.questplatform.domain.NoiseLevel
import ru.pogman.questplatform.domain.measureNoise

enum class MicrophoneInput {
    UNPROCESSED,
    VOICE_RECOGNITION,
}

enum class MicrophoneFailure {
    PERMISSION,
    UNAVAILABLE,
    SILENCED,
}

class MicrophoneException(val failure: MicrophoneFailure) : IllegalStateException(failure.name)

data class MicrophoneSample(val level: NoiseLevel, val timeMs: Long, val input: MicrophoneInput)

class MicrophoneMeter(context: Context) {
    private val context = context.applicationContext
    private val captureMutex = Mutex()

    fun samples(): Flow<MicrophoneSample> = flow {
        captureMutex.withLock {
            if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED
            )
                throw MicrophoneException(MicrophoneFailure.PERMISSION)
            val manager =
                context.getSystemService(AudioManager::class.java)
                    ?: throw MicrophoneException(MicrophoneFailure.UNAVAILABLE)
            val input =
                if (
                    manager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) ==
                        "true"
                )
                    MicrophoneInput.UNPROCESSED
                else MicrophoneInput.VOICE_RECOGNITION
            val source =
                if (input == MicrophoneInput.UNPROCESSED) MediaRecorder.AudioSource.UNPROCESSED
                else MediaRecorder.AudioSource.VOICE_RECOGNITION
            val sampleRate = 44100
            val frameSize = sampleRate / 20
            val minBuffer =
                AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
            if (minBuffer <= 0) throw MicrophoneException(MicrophoneFailure.UNAVAILABLE)
            val recorder =
                AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuffer * 2, frameSize * 4))
                    .build()
            try {
                if (recorder.state != AudioRecord.STATE_INITIALIZED)
                    throw MicrophoneException(MicrophoneFailure.UNAVAILABLE)
                val builtIn =
                    manager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC
                    } ?: throw MicrophoneException(MicrophoneFailure.UNAVAILABLE)
                if (!recorder.setPreferredDevice(builtIn))
                    throw MicrophoneException(MicrophoneFailure.UNAVAILABLE)
                recorder.startRecording()
                if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING)
                    throw MicrophoneException(MicrophoneFailure.UNAVAILABLE)
                val buffer = ShortArray(frameSize)
                var zeroSince: Long? = null
                var checkedAt = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (count <= 0) throw MicrophoneException(MicrophoneFailure.UNAVAILABLE)
                    val now = SystemClock.elapsedRealtime()
                    if (now - checkedAt >= 250) {
                        if (
                            Build.VERSION.SDK_INT >= 29 &&
                                recorder.activeRecordingConfiguration?.isClientSilenced == true
                        )
                            throw MicrophoneException(MicrophoneFailure.SILENCED)
                        val routed = recorder.routedDevice
                        if (routed != null && routed.type != AudioDeviceInfo.TYPE_BUILTIN_MIC)
                            throw MicrophoneException(MicrophoneFailure.UNAVAILABLE)
                        checkedAt = now
                    }
                    if ((0 until count).all { buffer[it] == 0.toShort() }) {
                        val since = zeroSince ?: now.also { zeroSince = it }
                        if (now - since >= 1500)
                            throw MicrophoneException(MicrophoneFailure.SILENCED)
                    } else zeroSince = null
                    emit(MicrophoneSample(measureNoise(buffer, count), now, input))
                }
            } finally {
                try {
                    if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                        recorder.stop()
                } finally {
                    recorder.release()
                }
            }
        }
    }
        .flowOn(Dispatchers.IO)
}
