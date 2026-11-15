package ru.pogman.questplatform.data

import androidx.room.withTransaction
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import ru.pogman.questplatform.domain.AnswerAttempt
import ru.pogman.questplatform.domain.Game
import ru.pogman.questplatform.domain.GameSummary
import ru.pogman.questplatform.domain.PhotoQuestConfig
import ru.pogman.questplatform.domain.PlayerQuest
import ru.pogman.questplatform.domain.PlayerRun
import ru.pogman.questplatform.domain.QuestException
import ru.pogman.questplatform.domain.QuestFailure
import ru.pogman.questplatform.domain.RunSummary
import ru.pogman.questplatform.domain.matchesPhotoAnswer

@OptIn(ExperimentalCoroutinesApi::class)
class QuestRepository(
    private val database: QuestDatabase,
    private val access: OrganizerAccess,
    private val photos: PhotoStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val dao = database.quests()
    private val json = Json { ignoreUnknownKeys = true }

    val games: Flow<List<GameSummary>> =
        access.authorized.flatMapLatest { authorized ->
            if (authorized)
                dao.observeGames().map { rows -> rows.map { GameSummary(it.id, it.title) } }
            else flowOf(emptyList())
        }

    val history: Flow<List<RunSummary>> =
        access.authorized.flatMapLatest { authorized ->
            if (authorized) dao.observeRuns().map { rows -> rows.map { it.summary() } }
            else flowOf(emptyList())
        }

    val activeRun: Flow<PlayerRun?> =
        dao.observeSettings().flatMapLatest { settings ->
            settings?.activeRunId?.let { id -> dao.observeRun(id).map { it?.playerView() } }
                ?: flowOf(null)
        }

    suspend fun initialize() {
        database.withTransaction {
            if (dao.settings() != null) {
                return@withTransaction
            }
            dao.saveSettings(SettingsRow())
            val game =
                newGame()
                    .copy(
                        id = "detective-photo-demo",
                        title = "Дело о пропавшем предмете",
                        photo =
                            PhotoQuestConfig(
                                instruction = "Сравните снимок с комнатой. Какой предмет исчез?",
                                photoRef = PhotoStore.DEMO_PHOTO,
                                answers = listOf("лампа", "настольная лампа"),
                            ),
                    )
            writeGame(game)
        }
    }

    fun newGame(): Game =
        Game(
            id = UUID.randomUUID().toString(),
            title = "Новое дело",
            story =
                "В комнате исчез важный предмет. Внимательно изучите старый снимок и найдите пропажу.",
            rules =
                "Сравните снимок с комнатой и введите название пропавшего предмета. Правильный ответ откроет цифру замка. Время начнёт отсчитываться после кнопки «Начать расследование».",
            photo = PhotoQuestConfig("Какой предмет исчез?", PhotoStore.DEMO_PHOTO, emptyList()),
        )

    suspend fun loadGame(id: String): Game {
        access.requireOrganizer()
        val stored = dao.game(id) ?: throw QuestException(QuestFailure.GAME_NOT_FOUND)
        access.requireOrganizer()
        val quest = stored.quests.single { it.questId == "photo" }
        return Game(
            stored.game.id,
            stored.game.title,
            stored.game.story,
            stored.game.rules,
            json.decodeFromString(quest.config),
        )
    }

    suspend fun saveGame(game: Game) {
        access.requireOrganizer()
        validateGame(game)
        database.withTransaction {
            access.requireOrganizer()
            writeGame(game)
        }
    }

    suspend fun deleteGame(id: String) {
        access.requireOrganizer()
        database.withTransaction {
            access.requireOrganizer()
            dao.deleteGame(id)
        }
    }

    suspend fun prepareRun(gameId: String, teamName: String, code: String): String {
        access.requireOrganizer()
        if (teamName.trim().isEmpty() || teamName.length > 80) {
            throw QuestException(QuestFailure.TEAM_NAME_REQUIRED)
        }
        if (!Regex("[0-9]").matches(code)) {
            throw QuestException(QuestFailure.CODE_INVALID)
        }
        return database.withTransaction {
            access.requireOrganizer()
            val game = loadGame(gameId)
            validateGame(game)
            val id = UUID.randomUUID().toString()
            dao.insertRun(
                RunRow(
                    id,
                    game.id,
                    game.title,
                    teamName.trim(),
                    game.story,
                    game.rules,
                    clock.millis(),
                )
            )
            dao.insertRunQuest(
                RunQuestRow(runId = id, config = json.encodeToString(game.photo), digit = code)
            )
            dao.saveSettings(SettingsRow(activeRunId = id))
            id
        }
    }

    suspend fun selectRun(id: String) {
        access.requireOrganizer()
        database.withTransaction {
            access.requireOrganizer()
            if (dao.run(id) == null) {
                throw QuestException(QuestFailure.RUN_NOT_FOUND)
            }
            dao.saveSettings(SettingsRow(activeRunId = id))
        }
    }

    suspend fun startRun() {
        database.withTransaction {
            val stored = currentRun()
            if (stored.run.startedAt == null) {
                dao.updateRun(stored.run.copy(startedAt = clock.millis()))
            }
        }
    }

    suspend fun openPhoto() {
        database.withTransaction {
            val stored = currentRun()
            requireStarted(stored)
            val quest = stored.quests.single()
            if (quest.openedAt == null) {
                dao.updateRunQuest(quest.copy(openedAt = clock.millis()))
            }
        }
    }

    suspend fun submitAnswer(answer: String): Boolean = database.withTransaction {
        val stored = currentRun()
        requireStarted(stored)
        val quest = stored.quests.single()
        if (quest.solvedAt != null) {
            return@withTransaction true
        }
        val now = clock.millis()
        val config = json.decodeFromString<PhotoQuestConfig>(quest.config)
        val correct = matchesPhotoAnswer(answer, config.answers)
        dao.insertAttempt(
            AttemptRow(
                UUID.randomUUID().toString(),
                stored.run.id,
                answer = answer.take(500),
                correct = correct,
                submittedAt = now,
            )
        )
        dao.updateRunQuest(
            quest.copy(openedAt = quest.openedAt ?: now, solvedAt = now.takeIf { correct })
        )
        if (correct) {
            dao.updateRun(stored.run.copy(finishedAt = now))
        }
        correct
    }

    private suspend fun writeGame(game: Game) {
        val existing = dao.game(game.id)
        dao.saveGame(
            GameRow(
                game.id,
                game.title.trim(),
                game.story,
                game.rules,
                existing?.game?.createdAt ?: clock.millis(),
            )
        )
        dao.saveGameQuest(GameQuestRow(gameId = game.id, config = json.encodeToString(game.photo)))
    }

    private fun validateGame(game: Game) {
        if (
            game.title.trim().isEmpty() ||
                game.title.length > 80 ||
                game.story.length > 10_000 ||
                game.rules.length > 10_000 ||
                game.photo.instruction.isBlank() ||
                game.photo.instruction.length > 500 ||
                game.photo.answers.isEmpty() ||
                game.photo.answers.any { it.isBlank() || it.length > 100 } ||
                !photos.exists(game.photo.photoRef)
        ) {
            throw QuestException(QuestFailure.GAME_INVALID)
        }
    }

    private suspend fun currentRun(): RunWithQuests {
        val id = dao.settings()?.activeRunId ?: throw QuestException(QuestFailure.RUN_NOT_FOUND)
        return dao.run(id) ?: throw QuestException(QuestFailure.RUN_NOT_FOUND)
    }

    private fun requireStarted(stored: RunWithQuests) {
        if (stored.run.startedAt == null) {
            throw QuestException(QuestFailure.RUN_NOT_STARTED)
        }
    }

    private fun RunWithQuests.playerView(): PlayerRun {
        val quest = quests.single()
        val config = json.decodeFromString<PhotoQuestConfig>(quest.config)
        return PlayerRun(
            run.id,
            run.gameTitle,
            run.teamName,
            run.story,
            run.rules,
            run.startedAt,
            run.finishedAt,
            PlayerQuest(
                "Фото до метели",
                config.instruction,
                photos.displayUri(config.photoRef),
                quest.openedAt,
                quest.solvedAt,
                quest.digit.takeIf { quest.solvedAt != null },
            ),
        )
    }

    private fun RunWithQuests.summary(): RunSummary =
        RunSummary(
            run.id,
            run.teamName,
            run.gameTitle,
            run.createdAt,
            run.startedAt,
            run.finishedAt,
            attempts.size,
            attempts.count { !it.correct },
            quests.single().openedAt,
            quests.single().solvedAt,
            attempts
                .sortedBy { it.submittedAt }
                .map { AnswerAttempt(it.answer, it.correct, it.submittedAt) },
        )
}
