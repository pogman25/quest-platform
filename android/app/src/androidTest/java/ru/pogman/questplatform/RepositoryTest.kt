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
import ru.pogman.questplatform.domain.QuestException
import ru.pogman.questplatform.domain.QuestFailure

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
    fun snapshotsSurviveSourceChangesAndTimingAndAttemptsAreIdempotent() = runBlocking {
        val game = repository.loadGame("detective-photo-demo")
        val id = repository.prepareRun(game.id, "Команда 1", "0")
        repository.saveGame(
            game.copy(title = "Изменённая игра", photo = game.photo.copy(answers = listOf("книга")))
        )
        repository.deleteGame(game.id)
        assertEquals(game.title, repository.activeRun.first()!!.gameTitle)
        assertNull(repository.activeRun.first()!!.quest.reward)
        assertNull(repository.activeRun.first()!!.finalCode)
        assertNull(repository.activeRun.first()!!.startedAt)
        expectFailure(QuestFailure.RUN_NOT_STARTED) { repository.submitAnswer("лампа") }
        clock.now = 10_000
        repository.startRun()
        clock.now = 12_000
        repository.startRun()
        repository.openPhoto()
        clock.now = 15_000
        repository.openPhoto()
        repeat(5) { assertFalse(repository.submitAnswer("стул")) }
        clock.now = 20_000
        assertTrue(repository.submitAnswer("  НАСТОЛЬНАЯ ЛАМПА  "))
        clock.now = 30_000
        assertTrue(repository.submitAnswer("лампа"))
        val result = database.quests().run(id)!!
        assertEquals(10_000L, result.run.startedAt)
        assertEquals(12_000L, result.quests.single().openedAt)
        assertEquals(20_000L, result.quests.single().solvedAt)
        assertEquals(20_000L, result.run.finishedAt)
        assertEquals(6, result.attempts.size)
        assertEquals("0", repository.activeRun.first()!!.finalCode)
        val history = repository.history.first().single()
        assertEquals(5, history.incorrectAttempts)
        assertEquals(6, history.log.size)
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
            repository.saveGame(game.copy(photo = game.photo.copy(photoRef = reference)))
            val id = repository.prepareRun(game.id, "Фото-команда", "7")
            repository.startRun()
            repository.openPhoto()
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
            assertNotNull(run.quest.openedAt)
            assertTrue(File(Uri.parse(run.quest.photoUri).path!!).isFile)
            assertFalse(restartedAccess.authorized.value)
            assertTrue(restarted.submitAnswer("лампа"))
            assertEquals("7", restarted.activeRun.first()!!.finalCode)
        } finally {
            bitmap.recycle()
            input.delete()
        }
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
