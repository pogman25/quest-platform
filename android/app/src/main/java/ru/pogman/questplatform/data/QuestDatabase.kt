package ru.pogman.questplatform.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 3,
    exportSchema = true,
)
abstract class QuestDatabase : RoomDatabase() {
    abstract fun quests(): QuestDao

    companion object {
        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE run_quests ADD COLUMN revision INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE run_quests ADD COLUMN noiseCount INTEGER NOT NULL DEFAULT 0"
                    )
                    db.execSQL(
                        "ALTER TABLE run_quests ADD COLUMN noiseActive INTEGER NOT NULL DEFAULT 0"
                    )
                    db.execSQL("ALTER TABLE run_quests ADD COLUMN lastNoiseAt INTEGER")
                }
            }
    }
}
