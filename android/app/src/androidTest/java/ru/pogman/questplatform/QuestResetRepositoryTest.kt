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

class QuestResetRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "quest-reset-test-${System.nanoTime()}.db"
    private lateinit var database: QuestDatabase
    private lateinit var access: OrganizerAccess
    private lateinit var repository: QuestRepository
    private lateinit var game: Game

    @Before
    fun prepare() = runBlocking {
        database = Room.databaseBuilder(context, QuestDatabase::class.java, name).build()
        access = OrganizerAccess(database, PasswordHasher(1000))
        repository = QuestRepository(database, access, PhotoStore(context))
        repository.initialize()
        access.setup("password1", "password1")
        game =
            repository
                .newGame()
                .copy(
                    quests =
                        listOf(
                            GameQuest(
                                QuestType.PHOTO,
                                PhotoQuestConfig("Фото", "demo:room", listOf("лампа")),
                            ),
                            GameQuest(QuestType.STENCIL, WordQuestConfig("Слово", "снег")),
                        )
                )
        repository.saveGame(game)
    }

    @After
    fun close() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun finishedRunReopensWithOnlyTheSelectedQuestClearedAndItsOriginalSettings() = runBlocking {
        val id = repository.prepareRun(game.id, "Команда", "07")
        repository.startRun(id)
        repository.openQuest(QuestType.PHOTO, id)
        repository.submitAnswer(QuestType.PHOTO, QuestAnswer.Text("стул"), id)
        repository.submitAnswer(QuestType.PHOTO, QuestAnswer.Text("лампа"), id)
        repository.openQuest(QuestType.STENCIL, id)
        repository.submitAnswer(QuestType.STENCIL, QuestAnswer.Text("снег"), id)
        val before = database.quests().run(id)!!
        assertNotNull(before.run.finishedAt)
        assertEquals("07", repository.activeRun.first()!!.finalCode)
        repository.saveGame(
            game.updateQuest(QuestType.PHOTO) {
                (it as PhotoQuestConfig).copy(instruction = "Новое фото", answers = listOf("книга"))
            }
        )

        repository.resetQuest(id, QuestType.PHOTO)

        val after = database.quests().run(id)!!
        val oldQuest = before.quests.single { it.questId == QuestType.PHOTO.id }
        val reset = after.quests.single { it.questId == QuestType.PHOTO.id }
        assertEquals(before.run.copy(finishedAt = null), after.run)
        assertEquals(oldQuest.copy(openedAt = null, solvedAt = null, revision = 1), reset)
        assertEquals(
            before.quests.filter { it.questId != QuestType.PHOTO.id },
            after.quests.filter { it.questId != QuestType.PHOTO.id },
        )
        assertEquals(before.attempts.filter { it.questId != QuestType.PHOTO.id }, after.attempts)
        val player = repository.activeRun.first()!!
        assertNull(player.finalCode)
        assertNull(player.quests.single { it.type == QuestType.PHOTO }.reward)
        assertEquals("7", player.quests.single { it.type == QuestType.STENCIL }.reward)
        expectFailure(QuestFailure.QUEST_PROGRESS_CHANGED) {
            repository.submitAnswer(QuestType.PHOTO, QuestAnswer.Text("лампа"), id, 0)
        }
        repository.openQuest(QuestType.PHOTO, id, 1)
        assertEquals(
            AnswerVerdict.INCORRECT,
            repository.submitAnswer(QuestType.PHOTO, QuestAnswer.Text("книга"), id, 1),
        )
        assertEquals(
            AnswerVerdict.CORRECT,
            repository.submitAnswer(QuestType.PHOTO, QuestAnswer.Text("лампа"), id, 1),
        )
        assertEquals("07", repository.activeRun.first()!!.finalCode)
    }

    @Test
    fun resettingAnInactiveArchivedRunKeepsTheCurrentTeamAndAllSettings() = runBlocking {
        val old = repository.prepareRun(game.id, "Первая команда", "07")
        repository.startRun(old)
        repository.openQuest(QuestType.PHOTO, old)
        repository.submitAnswer(QuestType.PHOTO, QuestAnswer.Text("стул"), old)
        val before = database.quests().run(old)!!
        val current = repository.prepareRun(game.id, "Вторая команда", "25")
        val other = database.quests().run(current)
        repository.resetQuest(old, QuestType.PHOTO)
        assertEquals(current, repository.activeRun.first()!!.id)
        assertEquals(other, database.quests().run(current))

        repository.deleteGame(game.id)
        repository.resetQuest(old, QuestType.PHOTO)
        assertNull(repository.activeRun.first())
        val reset = database.quests().run(old)!!
        assertEquals(before.run, reset.run)
        assertEquals(
            before.quests.single { it.questId == QuestType.PHOTO.id }.config,
            reset.quests.single { it.questId == QuestType.PHOTO.id }.config,
        )
        assertEquals("0", reset.quests.single { it.questId == QuestType.PHOTO.id }.digit)
        assertEquals(2L, reset.quests.single { it.questId == QuestType.PHOTO.id }.revision)
        assertTrue(reset.attempts.isEmpty())
    }

    @Test
    fun resetRequiresOrganizerAndAStartedRun() = runBlocking {
        val id = repository.prepareRun(game.id, "Команда", "07")
        val before = database.quests().run(id)
        expectFailure(QuestFailure.RUN_NOT_STARTED) { repository.resetQuest(id, QuestType.PHOTO) }
        assertEquals(before, database.quests().run(id))
        repository.startRun(id)
        repository.openQuest(QuestType.PHOTO, id)
        val started = database.quests().run(id)
        access.logout()
        expectFailure(QuestFailure.ORGANIZER_REQUIRED) {
            repository.resetQuest(id, QuestType.PHOTO)
        }
        assertEquals(started, database.quests().run(id))
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
