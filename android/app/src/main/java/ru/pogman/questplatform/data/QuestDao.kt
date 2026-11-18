package ru.pogman.questplatform.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM organizer_credentials WHERE id = 1")
    fun observeCredentials(): Flow<CredentialRow?>

    @Query("SELECT * FROM organizer_credentials WHERE id = 1")
    suspend fun credentials(): CredentialRow?

    @Insert suspend fun insertCredentials(row: CredentialRow)

    @Update suspend fun updateCredentials(row: CredentialRow)

    @Query("SELECT * FROM app_settings WHERE id = 1") suspend fun settings(): SettingsRow?

    @Query("SELECT * FROM app_settings WHERE id = 1") fun observeSettings(): Flow<SettingsRow?>

    @Upsert suspend fun saveSettings(row: SettingsRow)

    @Query(
        """UPDATE app_settings SET activeRunId = NULL
        WHERE id = 1 AND activeRunId IN (SELECT id FROM runs WHERE gameId = :gameId)"""
    )
    suspend fun clearActiveRunForGame(gameId: String)

    @Query(
        """UPDATE app_settings SET activeRunId = NULL
        WHERE id = 1 AND activeRunId NOT IN (
            SELECT runs.id FROM runs INNER JOIN games ON games.id = runs.gameId
        )"""
    )
    suspend fun clearActiveRunWithoutGame()

    @Query("SELECT * FROM games ORDER BY createdAt DESC, id")
    @Transaction
    fun observeGames(): Flow<List<GameWithQuests>>

    @Query("DELETE FROM game_quests WHERE gameId = :gameId")
    suspend fun deleteGameQuests(gameId: String)

    @Transaction
    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun game(id: String): GameWithQuests?

    @Upsert suspend fun saveGame(row: GameRow)

    @Upsert suspend fun saveGameQuest(row: GameQuestRow)

    @Query("DELETE FROM games WHERE id = :id") suspend fun deleteGame(id: String)

    @Insert suspend fun insertRun(row: RunRow)

    @Update suspend fun updateRun(row: RunRow)

    @Insert suspend fun insertRunQuest(row: RunQuestRow)

    @Update suspend fun updateRunQuest(row: RunQuestRow)

    @Transaction
    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun run(id: String): RunWithQuests?

    @Transaction
    @Query("SELECT * FROM runs WHERE id = :id")
    fun observeRun(id: String): Flow<RunWithQuests?>

    @Transaction
    @Query("SELECT * FROM runs ORDER BY createdAt DESC, id")
    fun observeRuns(): Flow<List<RunWithQuests>>

    @Insert suspend fun insertAttempt(row: AttemptRow)

    @Query("DELETE FROM attempts WHERE runId = :runId AND questId = :questId")
    suspend fun deleteQuestAttempts(runId: String, questId: String)

    @Query("DELETE FROM runs WHERE id = :id") suspend fun deleteRun(id: String)
}
