package ru.pogman.questplatform

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.pogman.questplatform.data.*
import ru.pogman.questplatform.domain.*

class SilentSearchRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "silent-test-${System.nanoTime()}.db"
    private lateinit var database: QuestDatabase
    private lateinit var access: OrganizerAccess
    private lateinit var repository: QuestRepository
    private val settings = NoiseSettings(NoiseBaseline(-60.0, -48.0))
    private lateinit var game: Game
    private lateinit var runId: String

    private fun open() {
        database =
            Room.databaseBuilder(context, QuestDatabase::class.java, name)
                .addMigrations(QuestDatabase.MIGRATION_1_2)
                .build()
        access = OrganizerAccess(database, PasswordHasher(1000))
        repository = QuestRepository(database, access, PhotoStore(context))
    }

    @Before
    fun prepare() = runBlocking {
        open()
        repository.initialize()
        access.setup("password1", "password1")
        game =
            repository
                .newGame()
                .copy(
                    quests =
                        listOf(
                            GameQuest(
                                QuestType.SILENT,
                                SilentQuestConfig("Найдите ключ", "0427", settings),
                            )
                        )
                )
        repository.saveGame(game)
        runId = repository.prepareRun(game.id, "Команда", "7")
        repository.startRun(runId)
        repository.openQuest(QuestType.SILENT, runId)
    }

    @After
    fun close() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun counterAndLatchSurviveReopenAndArchivedRestartClearsThem() = runBlocking {
        repository.beginSilentSearch(runId, "one")
        assertEquals(1, repository.recordSilentNoise(runId, "one", true).count)
        assertEquals(1, repository.recordSilentNoise(runId, "one", true).count)
        repository.endSilentSearch("one")
        database.close()
        open()
        access.login("password1")
        val resumed = repository.beginSilentSearch(runId, "two")
        assertEquals(1, resumed.progress.count)
        assertTrue(resumed.progress.active)
        assertEquals(1, repository.recordSilentNoise(runId, "two", true).count)
        repository.recordSilentNoise(runId, "two", false)
        assertEquals(2, repository.recordSilentNoise(runId, "two", true).count)
        assertEquals(AnswerVerdict.CORRECT, repository.submitSilentCode(runId, "two", "0427"))
        assertEquals(2, repository.activeRun.first()!!.quests.single().noiseCount)
        assertEquals(2, repository.history.first().single().quests.single().noiseCount)
        assertEquals("7", repository.activeRun.first()!!.finalCode)
        repository.deleteGame(game.id)
        repository.restartRun(runId)
        val restarted = repository.activeRun.first()!!.quests.single()
        assertEquals(0, restarted.noiseCount)
        assertFalse(restarted.noiseActive)
        assertNull(restarted.lastNoiseAt)
        assertNull(restarted.reward)
    }

    @Test
    fun microphoneSessionIsRequiredAndStartedSearchKeepsItsCalibrationAndCode() = runBlocking {
        assertNull(repository.activeRun.first()!!.quests.single().openedAt)
        try {
            repository.submitAnswer(QuestType.SILENT, QuestAnswer.Text("0427"), runId)
            fail("Expected microphone guard")
        } catch (error: QuestException) {
            assertEquals(QuestFailure.SILENT_MIC_REQUIRED, error.failure)
        }
        repository.beginSilentSearch(runId, "session")
        assertNotNull(repository.activeRun.first()!!.quests.single().openedAt)
        repository.saveGame(
            game.updateQuest(QuestType.SILENT) {
                (it as SilentQuestConfig).copy(
                    code = "1234",
                    noise = settings.copy(sensitivity = 1f),
                )
            }
        )
        repository.endSilentSearch("session")
        val resumed = repository.beginSilentSearch(runId, "resumed")
        assertEquals(settings, resumed.settings)
        try {
            repository.submitSilentCode(runId, "session", "0427")
            fail("Expected old session guard")
        } catch (error: QuestException) {
            assertEquals(QuestFailure.SILENT_MIC_REQUIRED, error.failure)
        }
        assertEquals(AnswerVerdict.INCORRECT, repository.submitSilentCode(runId, "resumed", "1234"))
        assertEquals(AnswerVerdict.CORRECT, repository.submitSilentCode(runId, "resumed", "0427"))
    }

    @Test
    fun uncalibratedQuestCannotBeSaved() = runBlocking {
        try {
            repository.saveGame(
                game.updateQuest(QuestType.SILENT) { (it as SilentQuestConfig).copy(noise = null) }
            )
            fail("Expected calibration guard")
        } catch (error: QuestException) {
            assertEquals(QuestFailure.SILENT_CALIBRATION_REQUIRED, error.failure)
        }
    }
}
