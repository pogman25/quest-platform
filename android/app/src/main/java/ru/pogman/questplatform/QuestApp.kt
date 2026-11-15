package ru.pogman.questplatform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ru.pogman.questplatform.components.QuestTopBar
import ru.pogman.questplatform.navigation.QuestRoute
import ru.pogman.questplatform.screens.InstructionsScreen
import ru.pogman.questplatform.screens.OrganizerEntryScreen
import ru.pogman.questplatform.screens.PlayerHomeScreen
import ru.pogman.questplatform.theme.QuestTheme

@Composable
fun QuestApp() {
    val backStack = rememberNavBackStack(QuestRoute.Player)
    val handleBack = {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
        Unit
    }
    val handleOrganizer = {
        backStack.add(QuestRoute.OrganizerEntry)
        Unit
    }
    val handleInstructions = {
        backStack.add(QuestRoute.Instructions)
        Unit
    }
    val title = when (backStack.last()) {
        QuestRoute.Instructions -> R.string.instructions_title
        QuestRoute.OrganizerEntry -> R.string.organizer_title
        else -> R.string.app_name
    }

    QuestTheme {
        Scaffold(
            topBar = {
                QuestTopBar(
                    title = title,
                    canGoBack = backStack.size > 1,
                    onBack = handleBack,
                    onOrganizer = handleOrganizer,
                )
            },
        ) { contentPadding ->
            NavDisplay(
                backStack = backStack,
                onBack = handleBack,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                entryProvider = entryProvider {
                    entry<QuestRoute.Player> {
                        PlayerHomeScreen(onInstructions = handleInstructions)
                    }
                    entry<QuestRoute.Instructions> {
                        InstructionsScreen(onReturn = handleBack)
                    }
                    entry<QuestRoute.OrganizerEntry> {
                        OrganizerEntryScreen(onReturn = handleBack)
                    }
                },
            )
        }
    }
}
