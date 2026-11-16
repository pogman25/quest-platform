package ru.pogman.questplatform

import org.junit.Assert.*
import org.junit.Test
import ru.pogman.questplatform.domain.*

class NoiseDetectorTest {
    private val baseline = NoiseBaseline(-60.0, -48.0)
    private val quiet = NoiseLevel(-60.0, -48.0)
    private val impulse = NoiseLevel(-50.0, -15.0)

    @Test
    fun pcmMeasurementHandlesSilenceAndTheNegativeFullScaleSample() {
        assertEquals(-96.0, measureNoise(shortArrayOf(0, 0), 2).rmsDbfs, 0.01)
        val level = measureNoise(shortArrayOf(Short.MIN_VALUE, 0), 2)
        assertEquals(0.0, level.peakDbfs, 0.001)
        assertEquals(-3.0103, level.rmsDbfs, 0.001)
        // The unread tail of a partially filled AudioRecord buffer must not affect the result.
        assertEquals(-96.0, measureNoise(shortArrayOf(0, Short.MAX_VALUE), 1).peakDbfs, 0.01)
    }

    @Test
    fun calibrationIgnoresIsolatedLoudFramesButRejectsAnOverloadedRoom() {
        assertEquals(baseline, calibrateNoise(List(95) { quiet } + List(5) { impulse }))
        assertFalse(calibrateNoise(List(100) { NoiseLevel(-15.0, -3.0) }).hasHeadroom)
    }

    @Test
    fun anImpulseOrLongSoundCountsOnceUntilThereIsAQuietInterval() {
        val detector = NoiseDetector(baseline)
        assertTrue(detector.accept(impulse, 0, 0.5f))
        for (time in 50L..2000L step 50) assertFalse(detector.accept(impulse, time, 0.5f))
        assertFalse(detector.accept(impulse, 2010, 1f))
        for (time in 2050L..2650L step 50) assertFalse(detector.accept(quiet, time, 0.5f))
        assertFalse(detector.active)
        assertTrue(detector.accept(impulse, 2700, 0.5f))
    }

    @Test
    fun sustainedSoundNeedsDurationAndNearThresholdNoiseDoesNotRearmTheDetector() {
        val detector = NoiseDetector(baseline)
        val speech = NoiseLevel(-40.0, -35.0)
        assertFalse(detector.accept(speech, 0, 0.5f))
        assertFalse(detector.accept(speech, 50, 0.5f))
        assertTrue(detector.accept(speech, 100, 0.5f))
        val almostQuiet = NoiseLevel(-46.0, -35.0)
        for (time in 150L..2000L step 50) assertFalse(detector.accept(almostQuiet, time, 0.5f))
        assertTrue(detector.active)
        assertFalse(detector.accept(impulse, 2100, 0.5f))
    }

    @Test
    fun sensitivityAndHeadroomKeepTheTriggerReachable() {
        assertTrue(noiseMargin(1f) < noiseMargin(0f))
        assertFalse(NoiseDetector(baseline).accept(NoiseLevel(-56.0, -30.0), 0, 0f))
        assertTrue(NoiseDetector(baseline).accept(NoiseLevel(-56.0, -30.0), 0, 1f))
        val loudBaseline = NoiseBaseline(-25.0, -13.0)
        val threshold = loudBaseline.thresholds(0f)
        assertTrue(threshold.rmsDbfs < 0 && threshold.peakDbfs < 0)
        assertEquals(
            1f,
            loudBaseline.relativeLevel(NoiseLevel(-40.0, threshold.peakDbfs), 0f),
            0.001f,
        )
        assertTrue(NoiseDetector(loudBaseline).accept(NoiseLevel(-40.0, -0.5), 0, 0f))
    }
}
