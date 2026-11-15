package ru.pogman.questplatform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import ru.pogman.questplatform.viewmodel.OrganizerViewModel
import ru.pogman.questplatform.viewmodel.PlayerViewModel
import ru.pogman.questplatform.viewmodel.QuestViewModelFactory

class MainActivity : ComponentActivity() {
    private val organizer: OrganizerViewModel by viewModels {
        QuestViewModelFactory((application as QuestApplication).appContainer())
    }
    private val player: PlayerViewModel by viewModels {
        QuestViewModelFactory((application as QuestApplication).appContainer())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        setContent {
            QuestApp(organizer = organizer, player = player)
        }
    }
}
