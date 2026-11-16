package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.*
import ru.pogman.questplatform.viewmodel.ActionState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun GameSettingsPanel(
    game: Game,
    action: ActionState,
    onChange: ((Game) -> Game) -> Unit,
    onToggleQuest: (QuestType) -> Unit,
    onOpenQuest: (QuestType) -> Unit,
    onMoveQuest: (QuestType, Int) -> Unit,
    onSave: () -> Unit,
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val quests by rememberUpdatedState(game.quests)
    val handleReorder: suspend CoroutineScope.(LazyListItemInfo, LazyListItemInfo) -> Unit =
        { from, to ->
            val order = quests.map { it.type }
            val type = order.find { it.id == from.key }
            val target = order.indexOfFirst { it.id == to.key }
            if (!action.busy && type != null && target >= 0 && order.indexOf(type) != target) {
                onMoveQuest(type, target)
                // StateFlow reaches Compose asynchronously. Wait for this frame's new order before
                // the library calculates the next move, so the dragged card stays under the finger.
                snapshotFlow { quests.map { it.type } }.first { it != order }
                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }
        }
    val reorderState = rememberReorderableLazyListState(listState, onMove = handleReorder)
    val handleDragStart: (Offset) -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }
    val handleDragStop = { haptic.performHapticFeedback(HapticFeedbackType.GestureEnd) }
    val available = QuestType.entries.filter { type -> game.quests.none { it.type == type } }

    LazyScreenContent(state = listState) {
        item(key = "gameDetails") {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                GameDetailsCard(game, !action.busy, onChange)
                Text(
                    stringResource(R.string.quest_selection_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.quest_setup_hint))
            }
        }
        itemsIndexed(game.quests, key = { _, quest -> quest.type.id }) { index, quest ->
            val handleOpen = { onOpenQuest(quest.type) }
            val handleRemove = { onToggleQuest(quest.type) }
            val handleMove: (Int) -> Unit = { onMoveQuest(quest.type, it) }
            ReorderableItem(reorderState, key = quest.type.id, enabled = !action.busy) { dragging ->
                QuestSetupCard(
                    quest,
                    index,
                    game.quests.size,
                    !action.busy,
                    dragging,
                    Modifier.draggableHandle(
                        enabled = !action.busy && game.quests.size > 1,
                        onDragStarted = handleDragStart,
                        onDragStopped = handleDragStop,
                    ),
                    handleOpen,
                    handleRemove,
                    handleMove,
                )
            }
        }
        if (available.isNotEmpty()) {
            item(key = "availableQuests") {
                SectionCard(stringResource(R.string.add_quest_title)) {
                    available.forEach { type ->
                        val handleAdd = { onToggleQuest(type) }
                        OutlinedButton(
                            handleAdd,
                            enabled = !action.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(type.title)
                        }
                    }
                }
            }
        }
        item(key = "saveGame") {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ActionMessage(action)
                Text(stringResource(R.string.game_save_hint))
                Button(onSave, enabled = !action.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.save_game_action))
                }
            }
        }
    }
}
