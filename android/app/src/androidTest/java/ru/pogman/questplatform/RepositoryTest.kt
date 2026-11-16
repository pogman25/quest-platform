package ru.pogman.questplatform

import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.pogman.questplatform.data.*
import ru.pogman.questplatform.domain.*

class RepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: QuestDatabase
    private lateinit var access: OrganizerAccess
    private lateinit var repository: QuestRepository
    private lateinit var photos: PhotoStore
    private lateinit var directory: File
    private val databaseName = "quest-test-${System.nanoTime()}.db"
    private val clock = MutableClock()

    @Before
    fun createStore() = runBlocking {
        database = Room.databaseBuilder(context, QuestDatabase::class.java, databaseName).build()
        directory = File(context.cacheDir, "photo-test-${System.nanoTime()}")
        photos = PhotoStore(context, directory)
        access = OrganizerAccess(database, PasswordHasher(1_000))
        repository = QuestRepository(database, access, photos, clock)
        repository.initialize()
        access.setup("password1", "password1")
    }

    @After
    fun closeStore() {
        database.close()
        context.deleteDatabase(databaseName)
        directory.deleteRecursively()
    }

    @Test
    fun inactiveRunKeepsSnapshotAndTimingAndAttemptsAreIdempotent() = runBlocking {
        val game = repository.loadGame("detective-photo-demo")
        val id = repository.prepareRun(game.id, "Команда 1", "0")
        repository.prepareRun(game.id, "Другая команда", "8")
        repository.saveGame(
            game
                .updateQuest(QuestType.PHOTO) {
                    (it as PhotoQuestConfig).copy(answers = listOf("книга"))
                }
                .copy(title = "Изменённая игра")
        )
        repository.deleteGame(game.id)
        assertNull(repository.activeRun.first())
        assertNull(database.quests().settings()!!.activeRunId)
        repository.selectRun(id)
        assertEquals(game.title, repository.activeRun.first()!!.gameTitle)
        assertNull(repository.activeRun.first()!!.quests.single().reward)
        assertNull(repository.activeRun.first()!!.finalCode)
        assertNull(repository.activeRun.first()!!.startedAt)
        expectFailure(QuestFailure.RUN_NOT_STARTED) { repository.submitPhoto("лампа") }
        clock.now = 10_000
        repository.startRun()
        clock.now = 12_000
        repository.startRun()
        repository.openQuest(QuestType.PHOTO)
        clock.now = 15_000
        repository.openQuest(QuestType.PHOTO)
        repeat(5) { assertFalse(repository.submitPhoto("стул")) }
        clock.now = 20_000
        assertTrue(repository.submitPhoto("  НАСТОЛЬНАЯ ЛАМПА  "))
        clock.now = 30_000
        assertTrue(repository.submitPhoto("лампа"))
        val result = database.quests().run(id)!!
        assertEquals(10_000L, result.run.startedAt)
        assertEquals(12_000L, result.quests.single().openedAt)
        assertEquals(20_000L, result.quests.single().solvedAt)
        assertEquals(20_000L, result.run.finishedAt)
        assertEquals(6, result.attempts.size)
        assertEquals("0", repository.activeRun.first()!!.finalCode)
        val history = repository.history.first().single { it.id == id }
        assertEquals(5, history.incorrectAttempts)
        assertEquals(6, history.log.size)
    }

    @Test
    fun legacyDeletedGameActivationIsRepairedWithoutLosingItsArchive() = runBlocking {
        val id = repository.prepareRun("detective-photo-demo", "Старая команда", "4")
        repository.startRun()
        repository.openQuest(QuestType.PHOTO)
        repository.submitPhoto("книга")
        val snapshot = database.quests().run(id)
        // Simulate deletion by the previous app version, which kept the activation.
        database.quests().deleteGame("detective-photo-demo")
        repository.initialize(repairDeletedGameActivation = true)
        assertNull(repository.activeRun.first())
        assertEquals(snapshot, database.quests().run(id))
        repository.selectRun(id)
        repository.initialize()
        assertEquals(id, repository.activeRun.first()!!.id)
    }

    @Test
    fun passwordPersistsWhileSessionDoesNotAndChangesNeedOldPassword() = runBlocking {
        access.logout()
        expectFailure(QuestFailure.ORGANIZER_REQUIRED) {
            repository.loadGame("detective-photo-demo")
        }
        expectFailure(QuestFailure.WRONG_PASSWORD) { access.login("wrong-password") }
        assertFalse(access.authorized.value)
        access.login("password1")
        expectFailure(QuestFailure.WRONG_PASSWORD) {
            access.changePassword("wrong", "password2", "password2")
        }
        access.changePassword("password1", "password2", "password2")
        val restarted = OrganizerAccess(database, PasswordHasher(2_000))
        assertTrue(restarted.passwordConfigured.first())
        assertFalse(restarted.authorized.value)
        expectFailure(QuestFailure.WRONG_PASSWORD) { restarted.login("password1") }
        restarted.login("password2")
        assertTrue(restarted.authorized.value)
        assertTrue(database.quests().credentials()!!.iterations > 0)
    }

    @Test
    fun runAndPhotoCanBeLoadedByNewRepositoryWithoutPickerPermission() = runBlocking {
        val input = File(context.cacheDir, "quest-photo-input.png")
        val bitmap = Bitmap.createBitmap(24, 16, Bitmap.Config.ARGB_8888)
        try {
            input.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val reference = photos.import(Uri.fromFile(input))
            val game = repository.loadGame("detective-photo-demo")
            repository.saveGame(
                game.updateQuest(QuestType.PHOTO) {
                    (it as PhotoQuestConfig).copy(photoRef = reference)
                }
            )
            val id = repository.prepareRun(game.id, "Фото-команда", "7")
            repository.startRun()
            repository.openQuest(QuestType.PHOTO)
            input.delete()
            database.close()
            database =
                Room.databaseBuilder(context, QuestDatabase::class.java, databaseName).build()
            val restartedAccess = OrganizerAccess(database, PasswordHasher(1_000))
            val restarted =
                QuestRepository(database, restartedAccess, PhotoStore(context, directory), clock)
            restarted.initialize()
            val run = restarted.activeRun.first()!!
            assertEquals(id, run.id)
            assertNotNull(run.startedAt)
            assertNotNull(run.quests.single().openedAt)
            assertTrue(File(Uri.parse(run.quests.single().photoUri).path!!).isFile)
            assertFalse(restartedAccess.authorized.value)
            assertTrue(restarted.submitPhoto("лампа"))
            assertEquals("7", restarted.activeRun.first()!!.finalCode)
        } finally {
            bitmap.recycle()
            input.delete()
        }
    }

    @Test
    fun restartClearsOnlySelectedTeamAndKeepsItsSnapshotAfterGameDeletion() = runBlocking {
        val game = repository.loadGame("detective-photo-demo")
        val selectedId = repository.prepareRun(game.id, "Повторная команда", "3")
        repository.startRun()
        repository.openQuest(QuestType.PHOTO)
        repository.submitPhoto("книга")
        repository.submitPhoto("лампа")
        val snapshot = database.quests().run(selectedId)!!
        val otherId = repository.prepareRun(game.id, "Другая команда", "7")
        repository.startRun()
        repository.openQuest(QuestType.PHOTO)
        repository.submitPhoto("стул")
        val other = database.quests().run(otherId)!!
        repository.deleteGame(game.id)
        access.logout()
        expectFailure(QuestFailure.ORGANIZER_REQUIRED) { repository.restartRun(selectedId) }
        assertEquals(2, database.quests().run(selectedId)!!.attempts.size)
        access.login("password1")
        clock.now = 45_000
        val restartedId = repository.restartRun(selectedId)
        assertNull(database.quests().run(selectedId))
        val restarted = database.quests().run(restartedId)!!
        assertEquals(snapshot.run.teamName, restarted.run.teamName)
        assertEquals(snapshot.run.gameTitle, restarted.run.gameTitle)
        assertEquals(snapshot.run.story, restarted.run.story)
        assertEquals(snapshot.run.rules, restarted.run.rules)
        assertEquals(snapshot.quests.single().config, restarted.quests.single().config)
        assertEquals(snapshot.quests.single().digit, restarted.quests.single().digit)
        assertNull(restarted.run.startedAt)
        assertNull(restarted.run.finishedAt)
        assertNull(restarted.quests.single().openedAt)
        assertNull(restarted.quests.single().solvedAt)
        assertTrue(restarted.attempts.isEmpty())
        assertEquals(other, database.quests().run(otherId))
        assertEquals(restartedId, repository.activeRun.first()!!.id)
        assertNull(repository.activeRun.first()!!.quests.single().reward)
        assertNull(repository.activeRun.first()!!.finalCode)
        assertEquals(2, repository.history.first().size)
        expectFailure(QuestFailure.RUN_NOT_STARTED) { repository.submitPhoto("лампа") }
        repository.startRun()
        repository.openQuest(QuestType.PHOTO)
        assertTrue(repository.submitPhoto("лампа"))
        assertEquals("3", repository.activeRun.first()!!.finalCode)
        assertEquals(1, database.quests().run(restartedId)!!.attempts.size)
    }

    @Test
    fun sixCluesCanBeSolvedOutOfOrderWithoutFinishingEarly() = runBlocking {
        val game = repository.newGame()
        repository.saveGame(game)
        val id = repository.prepareRun(game.id, "Шесть улик", "012345")
        repository.startRun()
        val nfc = game.quests.single { it.type == QuestType.NFC }.config as NfcQuestConfig
        assertEquals(
            AnswerVerdict.UNKNOWN_TAG,
            repository.submitAnswer(QuestType.NFC, QuestAnswer.Token("unknown")),
        )
        assertTrue(database.quests().run(id)!!.attempts.isEmpty())
        val falseTag = nfc.tags.first { it.id != nfc.correctId }
        assertEquals(
            AnswerVerdict.INCORRECT,
            repository.submitAnswer(QuestType.NFC, QuestAnswer.Token(falseTag.token)),
        )
        game.quests
            .filter { it.type != QuestType.PHOTO }
            .reversed()
            .forEach { quest ->
                clock.now += 1_000
                repository.openQuest(quest.type)
                val answer =
                    when (val config = quest.config) {
                        is NfcQuestConfig ->
                            QuestAnswer.Token(
                                config.tags.single { it.id == config.correctId }.token
                            )
                        is QrQuestConfig -> QuestAnswer.Token(config.token)
                        is ChoiceQuestConfig ->
                            QuestAnswer.Selection(
                                if (quest.type == QuestType.OBJECTS) config.answers.reversed()
                                else config.answers
                            )
                        is WordQuestConfig -> QuestAnswer.Text(config.word.uppercase())
                        is PhotoQuestConfig -> QuestAnswer.Text(config.answers.first())
                        is SilentQuestConfig ->
                            error("Silent search is covered by its microphone session tests")
                    }
                assertEquals(AnswerVerdict.CORRECT, repository.submitAnswer(quest.type, answer))
                assertNull(repository.activeRun.first()!!.finalCode)
            }
        repository.openQuest(QuestType.PHOTO)
        clock.now += 5_000
        assertTrue(repository.submitPhoto("лампа"))
        val finishedAt = clock.now
        assertEquals("012345", repository.activeRun.first()!!.finalCode)
        assertEquals(7, database.quests().run(id)!!.attempts.size)
        clock.now += 9_000
        repository.startRun()
        repository.submitPhoto("лампа")
        assertEquals(finishedAt, database.quests().run(id)!!.run.finishedAt)
        assertEquals(7, database.quests().run(id)!!.attempts.size)
        database.close()
        database = Room.databaseBuilder(context, QuestDatabase::class.java, databaseName).build()
        val reopened =
            QuestRepository(
                database,
                OrganizerAccess(database, PasswordHasher(1_000)),
                photos,
                clock,
            )
        assertEquals("012345", reopened.activeRun.first()!!.finalCode)
        assertEquals(6, reopened.activeRun.first()!!.quests.size)
    }

    @Test
    fun deletingAllGamesDoesNotReseedAndInvalidInputCannotCreateRuns() = runBlocking {
        expectFailure(QuestFailure.CODE_INVALID) {
            repository.prepareRun("detective-photo-demo", "Команда", "12")
        }
        expectFailure(QuestFailure.TEAM_NAME_REQUIRED) {
            repository.prepareRun("detective-photo-demo", "  ", "1")
        }
        repository.deleteGame("detective-photo-demo")
        repository.initialize()
        assertTrue(repository.games.first().isEmpty())
        assertTrue(repository.history.first().isEmpty())
    }

    private suspend fun expectFailure(expected: QuestFailure, block: suspend () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (error: QuestException) {
            assertEquals(expected, error.failure)
        }
    }
}

private class MutableClock(var now: Long = 1_000) : Clock() {
    override fun instant(): Instant = Instant.ofEpochMilli(now)

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}

private suspend fun QuestRepository.submitPhoto(value: String): Boolean =
    submitAnswer(QuestType.PHOTO, QuestAnswer.Text(value)) == AnswerVerdict.CORRECT
