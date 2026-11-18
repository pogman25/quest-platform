package ru.pogman.questplatform

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.pogman.questplatform.data.QuestDatabase

class QuestMigrationTest {
    @Test fun versionOneDataSurvivesAndNoiseCountersStartAtZero() = checkMigration(1)

    @Test fun versionTwoDataSurvivesIncludingNoiseCounters() = checkMigration(2)

    private fun checkMigration(version: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "migration-test-${System.nanoTime()}.db"
        val schema =
            JSONObject(
                    instrumentation.context.assets
                        .open("ru.pogman.questplatform.data.QuestDatabase/$version.json")
                        .bufferedReader()
                        .use { it.readText() }
                )
                .getJSONObject("database")
        context.openOrCreateDatabase(name, 0, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                entity.optJSONArray("indices")?.let { indices ->
                    for (j in 0 until indices.length()) db.execSQL(
                        indices
                            .getJSONObject(j)
                            .getString("createSql")
                            .replace("\${TABLE_NAME}", table)
                    )
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO games VALUES ('game','Старое дело','История','Правила',1)")
            db.execSQL(
                "INSERT INTO runs VALUES ('run','game','Старое дело','Команда','История','Правила',1,2,NULL)"
            )
            db.execSQL(
                "INSERT INTO run_quests (runId,questId,position,config,digit,openedAt,solvedAt) VALUES ('run','photo',1,'old-config-with-photo','0',3,NULL)"
            )
            if (version == 2)
                db.execSQL("UPDATE run_quests SET noiseCount=4, noiseActive=1, lastNoiseAt=5")
            db.execSQL("INSERT INTO attempts VALUES ('attempt','run','photo','Ответ',0,4)")
            db.execSQL("INSERT INTO app_settings VALUES (1,'run')")
            db.version = version
        }
        val database =
            Room.databaseBuilder(context, QuestDatabase::class.java, name)
                .addMigrations(QuestDatabase.MIGRATION_1_2, QuestDatabase.MIGRATION_2_3)
                .build()
        try {
            val db =
                database.openHelper
                    .writableDatabase // Room also validates the complete migrated schema.
            db.query(
                    "SELECT config, digit, noiseCount, noiseActive, lastNoiseAt, revision, openedAt FROM run_quests"
                )
                .use {
                    assertTrue(it.moveToFirst())
                    assertEquals("old-config-with-photo", it.getString(0))
                    assertEquals("0", it.getString(1))
                    assertEquals(if (version == 2) 4 else 0, it.getInt(2))
                    assertEquals(if (version == 2) 1 else 0, it.getInt(3))
                    if (version == 2) assertEquals(5L, it.getLong(4)) else assertTrue(it.isNull(4))
                    assertEquals(0L, it.getLong(5))
                    assertEquals(3L, it.getLong(6))
                }
            db.query("SELECT activeRunId FROM app_settings").use {
                assertTrue(it.moveToFirst())
                assertEquals("run", it.getString(0))
            }
            db.query("SELECT answer, submittedAt FROM attempts").use {
                assertTrue(it.moveToFirst())
                assertEquals("Ответ", it.getString(0))
                assertEquals(4L, it.getLong(1))
            }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
