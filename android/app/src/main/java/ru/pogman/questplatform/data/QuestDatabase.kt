package ru.pogman.questplatform.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities =
        [
            CredentialRow::class,
            SettingsRow::class,
            GameRow::class,
            GameQuestRow::class,
            RunRow::class,
            RunQuestRow::class,
            AttemptRow::class,
        ],
    version = 1,
    exportSchema = true,
)
abstract class QuestDatabase : RoomDatabase() {
    abstract fun quests(): QuestDao
}
