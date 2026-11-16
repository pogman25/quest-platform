package ru.pogman.questplatform

import org.junit.Assert.*
import org.junit.Test
import ru.pogman.questplatform.data.QuestConfigs
import ru.pogman.questplatform.domain.*

class SilentSearchRulesTest {
    @Test
    fun codeKeepsLeadingZerosAndCalibrationRoundTrips() {
        val config =
            SilentQuestConfig(
                "Найдите ключ",
                "0427",
                NoiseSettings(NoiseBaseline(-60.0, -48.0), 0.6f),
            )
        val codec = QuestConfigs()
        assertEquals(config, codec.decode(QuestType.SILENT, codec.encode(config)))
        assertEquals(
            AnswerVerdict.CORRECT,
            evaluateAnswer(QuestType.SILENT, config, QuestAnswer.Text("0427")),
        )
        assertEquals(
            AnswerVerdict.INCORRECT,
            evaluateAnswer(QuestType.SILENT, config, QuestAnswer.Text("427")),
        )
        assertFalse(NoiseSettings(NoiseBaseline(-60.0, -48.0), Float.NaN).valid)
        assertFalse(NoiseSettings(NoiseBaseline(Double.NaN, -48.0)).valid)
    }

    @Test
    fun rankingUsesNoiseThenVisibleSecondsAndSeparatesDifferentRules() {
        fun run(
            id: String,
            count: Int,
            duration: Long,
            rules: String = "same",
            done: Boolean = true,
        ): RunSummary =
            RunSummary(
                id,
                id,
                "Игра",
                1,
                1000,
                null,
                0,
                0,
                listOf(
                    QuestRunSummary(
                        "Тихий обыск",
                        1,
                        1000,
                        if (done) 1000 + duration else null,
                        count,
                        rules,
                    )
                ),
                emptyList(),
                "game",
            )
        val groups =
            silentCompetitions(
                listOf(
                    run("three", 3, 1000),
                    run("slow", 1, 20000),
                    run("fast", 1, 10500),
                    run("tie", 1, 10900),
                    run("not-finished", 0, 1, done = false),
                    run("other-calibration", 0, 1, "other"),
                )
            )
        assertEquals(2, groups.size)
        val rows = groups.first { it.standings.size == 4 }.standings
        assertEquals(listOf("fast", "tie", "slow", "three"), rows.map { it.team })
        assertEquals(listOf(1, 1, 3, 4), rows.map { it.place })
        assertEquals(10L, rows.first().durationSeconds)
    }

    @Test
    fun restoredDetectorDoesNotCountTheSameLongNoiseAgain() {
        val detector = NoiseDetector(NoiseBaseline(-60.0, -48.0), initiallyActive = true)
        val loud = NoiseLevel(-30.0, -10.0)
        assertFalse(detector.accept(loud, 5000, 0.5f))
        for (time in 5100L..5800L step 100) assertFalse(
            detector.accept(NoiseLevel(-60.0, -48.0), time, 0.5f)
        )
        assertTrue(detector.accept(loud, 5900, 0.5f))
    }
}
