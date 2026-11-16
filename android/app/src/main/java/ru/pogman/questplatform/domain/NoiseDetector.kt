package ru.pogman.questplatform.domain

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

data class NoiseLevel(val rmsDbfs: Double, val peakDbfs: Double)

data class NoiseThresholds(val rmsDbfs: Double, val peakDbfs: Double)

data class NoiseBaseline(val rmsDbfs: Double, val peakDbfs: Double) {
    fun thresholds(sensitivity: Float) =
        NoiseThresholds(
            minOf(-3.0, rmsDbfs + noiseMargin(sensitivity)),
            minOf(-1.0, peakDbfs + noiseMargin(sensitivity) + 6.0),
        )

    fun relativeLevel(level: NoiseLevel, sensitivity: Float): Float {
        val threshold = thresholds(sensitivity)
        return maxOf(
                0.0,
                (level.rmsDbfs - rmsDbfs) / (threshold.rmsDbfs - rmsDbfs),
                (level.peakDbfs - peakDbfs) / (threshold.peakDbfs - peakDbfs),
            )
            .toFloat()
    }

    val hasHeadroom: Boolean
        get() = rmsDbfs < -24.0 && peakDbfs < -12.0
}

fun noiseMargin(sensitivity: Float): Double = 24.0 - sensitivity.coerceIn(0f, 1f) * 18.0

fun measureNoise(samples: ShortArray, count: Int): NoiseLevel {
    require(count in 1..samples.size)
    var energy = 0.0
    var peak = 0.0
    for (index in 0 until count) {
        val value = samples[index].toDouble() / 32768.0
        energy += value * value
        peak = maxOf(peak, abs(value))
    }
    fun dbfs(amplitude: Double) =
        (20.0 * log10(amplitude.coerceAtLeast(0.00001585))).coerceAtLeast(-96.0)
    return NoiseLevel(dbfs(sqrt(energy / count)), dbfs(peak))
}

fun calibrateNoise(levels: List<NoiseLevel>): NoiseBaseline {
    require(levels.size >= 20)
    // The upper quartile captures ordinary room variation without letting one clap set the floor.
    val index = ((levels.size - 1) * 0.75).toInt()
    return NoiseBaseline(
        levels.map { it.rmsDbfs }.sorted()[index],
        levels.map { it.peakDbfs }.sorted()[index],
    )
}

class NoiseDetector(private val baseline: NoiseBaseline) {
    var active: Boolean = false
        private set

    private var loudSince: Long? = null
    private var quietSince: Long? = null
    private var lastEvent: Long? = null

    fun accept(level: NoiseLevel, timeMs: Long, sensitivity: Float): Boolean {
        val threshold = baseline.thresholds(sensitivity)
        val rmsLoud = level.rmsDbfs >= threshold.rmsDbfs
        val peakLoud = level.peakDbfs >= threshold.peakDbfs
        if (active) {
            // Hysteresis and a quiet interval keep echoes and long sounds within one event.
            if (
                level.rmsDbfs < threshold.rmsDbfs - 3.0 && level.peakDbfs < threshold.peakDbfs - 3.0
            ) {
                val since = quietSince ?: timeMs.also { quietSince = it }
                if (timeMs - since >= 600 && timeMs - (lastEvent ?: timeMs) >= 1000) {
                    active = false
                    loudSince = null
                }
            } else quietSince = null
            return false
        }
        if (rmsLoud) {
            if (loudSince == null) loudSince = timeMs
        } else loudSince = null
        if (peakLoud || loudSince?.let { timeMs - it >= 100 } == true) {
            active = true
            lastEvent = timeMs
            quietSince = null
            return true
        }
        return false
    }
}
