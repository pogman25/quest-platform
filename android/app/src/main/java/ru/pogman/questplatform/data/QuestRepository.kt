package ru.pogman.questplatform.data

import androidx.room.withTransaction
import java.security.MessageDigest
import java.time.Clock
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ru.pogman.questplatform.domain.*

data class SilentProgress(val count: Int, val active: Boolean, val lastNoiseAt: Long?)

data class SilentSearchSetup(val settings: NoiseSettings, val progress: SilentProgress)

private data class SilentSession(val runId: String, val id: String)

@OptIn(ExperimentalCoroutinesApi::class)
class QuestRepository(
    private val database: QuestDatabase,
    private val access: OrganizerAccess,
    private val photos: PhotoStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val dao = database.quests()
    private val configs = QuestConfigs()
    private val silentSession = AtomicReference<SilentSession?>(null)

    val games: Flow<List<GameSummary>> =
        access.authorized.flatMapLatest { authorized ->
            if (authorized)
                dao.observeGames().map { rows ->
                    rows.map { GameSummary(it.game.id, it.game.title, it.quests.size) }
                }
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

    suspend fun initialize(repairDeletedGameActivation: Boolean = false) {
        database.withTransaction {
            if (repairDeletedGameActivation) dao.clearActiveRunWithoutGame()
            if (dao.settings() != null) return@withTransaction
            dao.saveSettings(SettingsRow())
            writeGame(
                newGame()
                    .copy(
                        id = "detective-photo-demo",
                        title = "Дело о пропавшем предмете",
                        quests = listOf(defaultQuest(QuestType.PHOTO)),
                    )
            )
        }
    }

    fun newGame(): Game =
        Game(
            UUID.randomUUID().toString(),
            "Новое дело",
            "В комнате спрятаны улики. Исследуйте её вместе и соберите код настоящего замка.",
            "Проходите улики в любом порядке. Каждая даст цифру и её место в коде. Ошибки не ограничены. Начните расследование, когда команда будет готова.",
            QuestType.entries.filter { it != QuestType.SILENT }.map(::defaultQuest),
        )

    suspend fun loadGame(id: String): Game {
        access.requireOrganizer()
        val stored = dao.game(id) ?: fail(QuestFailure.GAME_NOT_FOUND)
        access.requireOrganizer()
        return stored.toGame()
    }

    suspend fun saveGame(game: Game) {
        access.requireOrganizer()
        validateGame(game)
        database.withTransaction {
            access.requireOrganizer()
            writeGame(game)
            val active = dao.settings()?.activeRunId?.let { dao.run(it) }
            if (
                active != null &&
                    active.run.gameId == game.id &&
                    active.run.finishedAt == null &&
                    active.quests.sortedBy { it.position }.map { it.questId } ==
                        game.quests.map { it.type.id }
            ) {
                // Same layout: update unsolved clues for the active team. A new layout needs a new
                // code.
                dao.updateRun(
                    active.run.copy(
                        gameTitle = game.title.trim(),
                        story = game.story,
                        rules = game.rules,
                    )
                )
                game.quests.forEach { quest ->
                    val progress = active.quests.single { it.questId == quest.type.id }
                    // Once a silent search starts, keep the same calibration and target for its
                    // score.
                    if (
                        progress.solvedAt == null &&
                            (quest.type != QuestType.SILENT || progress.openedAt == null)
                    )
                        dao.updateRunQuest(progress.copy(config = configs.encode(quest.config)))
                }
            }
        }
    }

    suspend fun deleteGame(id: String) {
        access.requireOrganizer()
        database.withTransaction {
            access.requireOrganizer()
            dao.clearActiveRunForGame(id)
            dao.deleteGame(id)
        }
    }

    suspend fun prepareRun(gameId: String, teamName: String, code: String): String {
        access.requireOrganizer()
        if (teamName.trim().isEmpty() || teamName.length > 80) fail(QuestFailure.TEAM_NAME_REQUIRED)
        return database.withTransaction {
            access.requireOrganizer()
            val game = loadGame(gameId)
            validateGame(game)
            validateCode(code, game.quests.size)
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
            game.quests.forEachIndexed { index, quest ->
                dao.insertRunQuest(
                    RunQuestRow(
                        id,
                        quest.type.id,
                        index + 1,
                        configs.encode(quest.config),
                        code[index].toString(),
                    )
                )
            }
            dao.saveSettings(SettingsRow(activeRunId = id))
            id
        }
    }

    suspend fun selectRun(id: String) {
        access.requireOrganizer()
        database.withTransaction {
            access.requireOrganizer()
            if (dao.run(id) == null) fail(QuestFailure.RUN_NOT_FOUND)
            dao.saveSettings(SettingsRow(activeRunId = id))
        }
    }

    suspend fun restartRun(id: String): String {
        access.requireOrganizer()
        return database.withTransaction {
            access.requireOrganizer()
            val stored = dao.run(id) ?: fail(QuestFailure.RUN_NOT_FOUND)
            val game = dao.game(stored.run.gameId)?.toGame()
            game?.let(::validateGame)
            val code = stored.quests.sortedBy { it.position }.joinToString("") { it.digit }
            if (game != null && game.quests.size != code.length)
                fail(QuestFailure.RUN_LAYOUT_CHANGED)
            val restartedId = UUID.randomUUID().toString()
            dao.insertRun(
                stored.run.copy(
                    id = restartedId,
                    createdAt = clock.millis(),
                    startedAt = null,
                    finishedAt = null,
                    gameTitle = game?.title ?: stored.run.gameTitle,
                    story = game?.story ?: stored.run.story,
                    rules = game?.rules ?: stored.run.rules,
                )
            )
            if (game != null) {
                game.quests.forEachIndexed { index, quest ->
                    dao.insertRunQuest(
                        RunQuestRow(
                            restartedId,
                            quest.type.id,
                            index + 1,
                            configs.encode(quest.config),
                            code[index].toString(),
                        )
                    )
                }
            } else {
                stored.quests.forEach {
                    dao.insertRunQuest(
                        it.copy(
                            runId = restartedId,
                            openedAt = null,
                            solvedAt = null,
                            noiseCount = 0,
                            noiseActive = false,
                            lastNoiseAt = null,
                        )
                    )
                }
            }
            dao.deleteRun(id)
            dao.saveSettings(SettingsRow(activeRunId = restartedId))
            restartedId
        }
    }

    suspend fun startRun(runId: String? = null) {
        database.withTransaction {
            val stored = currentRun(runId)
            if (stored.run.startedAt == null)
                dao.updateRun(stored.run.copy(startedAt = clock.millis()))
        }
    }

    suspend fun openQuest(type: QuestType, runId: String? = null) {
        database.withTransaction {
            val stored = currentRun(runId)
            requireStarted(stored)
            val quest =
                stored.quests.find { it.questId == type.id } ?: fail(QuestFailure.QUEST_NOT_FOUND)
            if (quest.openedAt == null && type != QuestType.SILENT)
                dao.updateRunQuest(quest.copy(openedAt = clock.millis()))
        }
    }

    suspend fun submitAnswer(
        type: QuestType,
        answer: QuestAnswer,
        runId: String? = null,
    ): AnswerVerdict = resolveAnswer(type, answer, runId)

    suspend fun beginSilentSearch(runId: String, sessionId: String): SilentSearchSetup =
        database.withTransaction {
            val stored = currentRun(runId)
            requireStarted(stored)
            val quest =
                stored.quests.find { it.questId == QuestType.SILENT.id }
                    ?: fail(QuestFailure.QUEST_NOT_FOUND)
            if (quest.solvedAt != null) fail(QuestFailure.SILENT_MIC_REQUIRED)
            val config = configs.decode(QuestType.SILENT, quest.config) as SilentQuestConfig
            val settings =
                config.noise?.takeIf { it.valid } ?: fail(QuestFailure.SILENT_CALIBRATION_REQUIRED)
            if (quest.openedAt == null) dao.updateRunQuest(quest.copy(openedAt = clock.millis()))
            silentSession.set(SilentSession(runId, sessionId))
            SilentSearchSetup(settings, quest.silentProgress())
        }

    fun endSilentSearch(sessionId: String) {
        val current = silentSession.get()
        if (current?.id == sessionId) silentSession.compareAndSet(current, null)
    }

    suspend fun recordSilentNoise(
        runId: String,
        sessionId: String,
        active: Boolean,
    ): SilentProgress = database.withTransaction {
        val stored = currentRun(runId)
        requireStarted(stored)
        val quest =
            stored.quests.find { it.questId == QuestType.SILENT.id }
                ?: fail(QuestFailure.QUEST_NOT_FOUND)
        requireSilentSession(runId, sessionId)
        if (quest.solvedAt != null || quest.noiseActive == active)
            return@withTransaction quest.silentProgress()
        val updated =
            quest.copy(
                noiseCount = quest.noiseCount + if (active) 1 else 0,
                noiseActive = active,
                lastNoiseAt = if (active) clock.millis() else quest.lastNoiseAt,
            )
        dao.updateRunQuest(updated)
        updated.silentProgress()
    }

    suspend fun submitSilentCode(runId: String, sessionId: String, code: String): AnswerVerdict =
        resolveAnswer(QuestType.SILENT, QuestAnswer.Text(code), runId, sessionId)

    private fun requireSilentSession(runId: String, sessionId: String?) {
        if (sessionId == null || silentSession.get() != SilentSession(runId, sessionId))
            fail(QuestFailure.SILENT_MIC_REQUIRED)
    }

    private fun RunQuestRow.silentProgress() = SilentProgress(noiseCount, noiseActive, lastNoiseAt)

    private suspend fun resolveAnswer(
        type: QuestType,
        answer: QuestAnswer,
        runId: String?,
        sessionId: String? = null,
    ): AnswerVerdict = database.withTransaction {
        val stored = currentRun(runId)
        requireStarted(stored)
        val quest =
            stored.quests.find { it.questId == type.id } ?: fail(QuestFailure.QUEST_NOT_FOUND)
        if (quest.solvedAt != null) return@withTransaction AnswerVerdict.CORRECT
        if (type == QuestType.SILENT) requireSilentSession(stored.run.id, sessionId)
        val config = configs.decode(type, quest.config)
        val verdict = evaluateAnswer(type, config, answer)
        if (verdict == AnswerVerdict.UNKNOWN_TAG) return@withTransaction verdict
        val now = clock.millis()
        val correct = verdict == AnswerVerdict.CORRECT
        dao.insertAttempt(
            AttemptRow(
                UUID.randomUUID().toString(),
                stored.run.id,
                type.id,
                config.answerDescription(answer),
                correct,
                now,
            )
        )
        dao.updateRunQuest(
            quest.copy(
                openedAt = quest.openedAt ?: now,
                solvedAt = now.takeIf { correct },
                noiseActive = if (correct) false else quest.noiseActive,
            )
        )
        if (correct && stored.quests.filter { it.questId != type.id }.all { it.solvedAt != null })
            dao.updateRun(stored.run.copy(finishedAt = now))
        if (correct && type == QuestType.SILENT && sessionId != null) endSilentSearch(sessionId)
        verdict
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
        dao.deleteGameQuests(game.id)
        game.quests.forEachIndexed { index, quest ->
            dao.saveGameQuest(
                GameQuestRow(game.id, quest.type.id, index + 1, configs.encode(quest.config))
            )
        }
    }

    private fun validateGame(game: Game) {
        if (
            game.title.isBlank() ||
                game.title.length > 80 ||
                game.story.length > 10_000 ||
                game.rules.length > 10_000 ||
                game.quests.size !in 1..QuestType.entries.size ||
                game.quests.map { it.type }.distinct().size != game.quests.size
        )
            fail(QuestFailure.GAME_INVALID)
        game.quests.forEach { quest ->
            val config = quest.config
            if (config.instruction.isBlank() || config.instruction.length > 500)
                fail(QuestFailure.GAME_INVALID)
            val valid =
                when (quest.type) {
                    QuestType.PHOTO ->
                        config is PhotoQuestConfig &&
                            photos.exists(config.photoRef) &&
                            config.answers.isNotEmpty() &&
                            config.answers.all { it.isNotBlank() && it.length <= 100 }
                    QuestType.QR ->
                        config is QrQuestConfig &&
                            photos.exists(config.photoRef) &&
                            config.token.isNotBlank() &&
                            config.token.length <= 512 &&
                            config.sizeMm in listOf(120, 150, 180)
                    QuestType.NFC ->
                        config is NfcQuestConfig &&
                            config.tags.size in 1..12 &&
                            config.tags.map { it.id }.distinct().size == config.tags.size &&
                            config.tags.map { it.token }.distinct().size == config.tags.size &&
                            config.tags.any { it.id == config.correctId } &&
                            config.tags.all {
                                it.label.isNotBlank() &&
                                    it.label.length <= 80 &&
                                    it.token.isNotBlank() &&
                                    it.token.length <= 512
                            }
                    QuestType.OBJECTS,
                    QuestType.TREE ->
                        config is ChoiceQuestConfig &&
                            config.cells.size == 9 &&
                            config.cells.map { it.id }.distinct().size == 9 &&
                            config.cells.all {
                                it.label.isNotBlank() &&
                                    it.label.length <= 40 &&
                                    it.symbol.isNotBlank() &&
                                    it.symbol.length <= 16 &&
                                    (it.photoRef == null || photos.exists(it.photoRef))
                            } &&
                            config.answers.size ==
                                (if (quest.type == QuestType.OBJECTS) 3 else 4) &&
                            config.answers.distinct().size == config.answers.size &&
                            config.answers.all { id -> config.cells.any { it.id == id } }
                    QuestType.STENCIL ->
                        config is WordQuestConfig &&
                            normalizeAnswer(config.word).matches(Regex("[а-яa-z]{4,12}")) &&
                            normalizeAnswer(config.word).length % 2 == 0 &&
                            config.widthMm in listOf(144, 168, 184) &&
                            config.correctOverlay in 1..8 &&
                            config.strokeSeed in 0..0xffffffffL &&
                            config.strokeVersion == 3
                    QuestType.SILENT -> {
                        if (config is SilentQuestConfig && config.noise?.valid != true)
                            fail(QuestFailure.SILENT_CALIBRATION_REQUIRED)
                        config is SilentQuestConfig &&
                            config.code.length == 4 &&
                            config.code.all { it in '0'..'9' }
                    }
                }
            if (!valid) fail(QuestFailure.GAME_INVALID)
        }
    }

    private fun validateCode(code: String, count: Int) {
        if (code.length != count || code.any { it !in '0'..'9' }) fail(QuestFailure.CODE_INVALID)
    }

    private suspend fun currentRun(expectedId: String?): RunWithQuests {
        val id = dao.settings()?.activeRunId ?: fail(QuestFailure.RUN_NOT_FOUND)
        if (expectedId != null && expectedId != id) fail(QuestFailure.RUN_NOT_FOUND)
        return dao.run(id) ?: fail(QuestFailure.RUN_NOT_FOUND)
    }

    private fun requireStarted(stored: RunWithQuests) {
        if (stored.run.startedAt == null) fail(QuestFailure.RUN_NOT_STARTED)
    }

    private fun GameWithQuests.toGame(): Game =
        Game(
            game.id,
            game.title,
            game.story,
            game.rules,
            quests
                .sortedBy { it.position }
                .map {
                    GameQuest(
                        QuestType.fromId(it.questId),
                        configs.decode(QuestType.fromId(it.questId), it.config),
                    )
                },
        )

    private fun RunWithQuests.playerView(): PlayerRun =
        PlayerRun(
            run.id,
            run.gameTitle,
            run.teamName,
            run.story,
            run.rules,
            run.startedAt,
            run.finishedAt,
            quests
                .sortedBy { it.position }
                .map { quest ->
                    val type = QuestType.fromId(quest.questId)
                    val config = configs.decode(type, quest.config)
                    val photo =
                        when (config) {
                            is PhotoQuestConfig -> config.photoRef
                            is QrQuestConfig -> null
                            else -> null
                        }
                    PlayerQuest(
                        type,
                        quest.position,
                        config.instruction,
                        photo?.let(photos::displayUri),
                        (config as? ChoiceQuestConfig)?.cells.orEmpty().map {
                            it.forDisplay(photos::displayUri)
                        },
                        quest.openedAt,
                        quest.solvedAt,
                        quest.digit.takeIf { quest.solvedAt != null },
                        (config as? SilentQuestConfig)?.noise,
                        quest.noiseCount,
                        quest.noiseActive,
                        quest.lastNoiseAt,
                    )
                },
            run.gameId,
        )

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
            quests
                .sortedBy { it.position }
                .map {
                    QuestRunSummary(
                        QuestType.fromId(it.questId).title,
                        it.position,
                        it.openedAt,
                        it.solvedAt,
                        if (it.questId == QuestType.SILENT.id) it.noiseCount else null,
                        if (it.questId == QuestType.SILENT.id)
                            MessageDigest.getInstance("SHA-256")
                                .digest(it.config.toByteArray())
                                .joinToString("") { byte -> "%02x".format(byte) }
                        else null,
                    )
                },
            attempts
                .sortedBy { it.submittedAt }
                .map {
                    AnswerAttempt(
                        QuestType.fromId(it.questId).title,
                        it.answer,
                        it.correct,
                        it.submittedAt,
                    )
                },
            run.gameId,
        )

    private fun fail(failure: QuestFailure): Nothing = throw QuestException(failure)
}
