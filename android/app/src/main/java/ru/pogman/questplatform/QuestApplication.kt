package ru.pogman.questplatform

import android.app.Application

class QuestApplication : Application() {
    // Tests replace the container before launching an Activity and use an isolated database.
    var container: AppContainer? = null

    fun appContainer(): AppContainer = container ?: AppContainer(this).also { container = it }
}
