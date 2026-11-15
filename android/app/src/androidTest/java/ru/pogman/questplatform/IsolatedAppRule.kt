package ru.pogman.questplatform

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import ru.pogman.questplatform.data.QuestDatabase

// Replaces dependencies before Activity is launched; never opens the user's database.
class IsolatedAppRule : ExternalResource() {
    lateinit var container: AppContainer
        private set

    private lateinit var application: QuestApplication

    override fun before() {
        application =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                as QuestApplication
        val database = Room.inMemoryDatabaseBuilder(application, QuestDatabase::class.java).build()
        container = AppContainer(application, database)
        application.container = container
        runBlocking { container.initialization.join() }
    }

    override fun after() {
        application.container = null
        container.database.close()
    }
}
