package ru.pogman.questplatform.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "organizer_credentials")
data class CredentialRow(
    @PrimaryKey val id: Int = 1,
    val salt: ByteArray,
    val hash: ByteArray,
    val iterations: Int,
)

@Entity(tableName = "app_settings")
data class SettingsRow(@PrimaryKey val id: Int = 1, val activeRunId: String? = null)

@Entity(tableName = "games")
data class GameRow(
    @PrimaryKey val id: String,
    val title: String,
    val story: String,
    val rules: String,
    val createdAt: Long,
)

@Entity(
    tableName = "game_quests",
    primaryKeys = ["gameId", "questId"],
    foreignKeys =
        [
            ForeignKey(
                entity = GameRow::class,
                parentColumns = ["id"],
                childColumns = ["gameId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
)
data class GameQuestRow(
    val gameId: String,
    val questId: String = "photo",
    val position: Int = 1,
    val config: String,
)

@Entity(tableName = "runs")
data class RunRow(
    @PrimaryKey val id: String,
    val gameId: String,
    val gameTitle: String,
    val teamName: String,
    val story: String,
    val rules: String,
    val createdAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
)

@Entity(
    tableName = "run_quests",
    primaryKeys = ["runId", "questId"],
    foreignKeys =
        [
            ForeignKey(
                entity = RunRow::class,
                parentColumns = ["id"],
                childColumns = ["runId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
)
data class RunQuestRow(
    val runId: String,
    val questId: String = "photo",
    val position: Int = 1,
    val config: String,
    val digit: String,
    val openedAt: Long? = null,
    val solvedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val noiseCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val noiseActive: Boolean = false,
    val lastNoiseAt: Long? = null,
)

@Entity(
    tableName = "attempts",
    foreignKeys =
        [
            ForeignKey(
                entity = RunRow::class,
                parentColumns = ["id"],
                childColumns = ["runId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("runId")],
)
data class AttemptRow(
    @PrimaryKey val id: String,
    val runId: String,
    val questId: String = "photo",
    val answer: String,
    val correct: Boolean,
    val submittedAt: Long,
)

data class GameWithQuests(
    @Embedded val game: GameRow,
    @Relation(parentColumn = "id", entityColumn = "gameId") val quests: List<GameQuestRow>,
)

data class RunWithQuests(
    @Embedded val run: RunRow,
    @Relation(parentColumn = "id", entityColumn = "runId") val quests: List<RunQuestRow>,
    @Relation(parentColumn = "id", entityColumn = "runId") val attempts: List<AttemptRow>,
)
