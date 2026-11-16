package ru.pogman.questplatform.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.components.GameSettingsPanel
import ru.pogman.questplatform.components.TeamSetupPanel
import ru.pogman.questplatform.domain.Game
import ru.pogman.questplatform.domain.QuestType
import ru.pogman.questplatform.viewmodel.ActionState

enum class GameSection {
    SETTINGS,
    TEAM,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    game: Game,
    section: GameSection,
    action: ActionState,
    onSectionChange: (GameSection) -> Unit,
    onChange: ((Game) -> Game) -> Unit,
    onToggleQuest: (QuestType) -> Unit,
    onOpenQuest: (QuestType) -> Unit,
    onMoveQuest: (QuestType, Int) -> Unit,
    onSave: () -> Unit,
    onPrepare: (String, String) -> Unit,
) {
    // Keep the team form above the section branches, like state lifted above React tabs.
    var team by rememberSaveable(game.id) { mutableStateOf("") }
    var code by rememberSaveable(game.id) { mutableStateOf("") }
    val handleTeam: (String) -> Unit = { team = it.take(80) }
    val handleCode: (String) -> Unit = {
        code = it.filter { char -> char in '0'..'9' }.take(game.quests.size)
    }
    val handleSettings = { onSectionChange(GameSection.SETTINGS) }
    val handleTeamSection = { onSectionChange(GameSection.TEAM) }
    LaunchedEffect(game.quests.size) { code = code.take(game.quests.size) }

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = section.ordinal) {
            Tab(
                section == GameSection.SETTINGS,
                handleSettings,
                enabled = !action.busy,
                text = { Text(stringResource(R.string.game_settings_section)) },
            )
            Tab(
                section == GameSection.TEAM,
                handleTeamSection,
                enabled = !action.busy,
                text = { Text(stringResource(R.string.game_team_section)) },
            )
        }
        Box(Modifier.weight(1f)) {
            when (section) {
                GameSection.SETTINGS ->
                    GameSettingsPanel(
                        game,
                        action,
                        onChange,
                        onToggleQuest,
                        onOpenQuest,
                        onMoveQuest,
                        onSave,
                    )
                GameSection.TEAM ->
                    TeamSetupPanel(game, team, code, action, handleTeam, handleCode, onPrepare)
            }
        }
    }
}
