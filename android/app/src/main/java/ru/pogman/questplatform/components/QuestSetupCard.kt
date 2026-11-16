package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.GameQuest

@Composable
fun QuestSetupCard(
    quest: GameQuest,
    index: Int,
    count: Int,
    enabled: Boolean,
    dragging: Boolean,
    dragHandle: Modifier,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
) {
    var orderMenu by remember { mutableStateOf(false) }
    val handleOrderMenu = { orderMenu = true }
    val handleDismiss = { orderMenu = false }
    val handleEarlier = {
        orderMenu = false
        onMove(index - 1)
    }
    val handleLater = {
        orderMenu = false
        onMove(index + 1)
    }

    SectionCard(
        "${index + 1}. ${quest.type.title}",
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (dragging) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
            ),
        modifier = Modifier.shadow(if (dragging) 8.dp else 0.dp, CardDefaults.shape),
        headerAction = {
            Box {
                IconButton(
                    onClick = handleOrderMenu,
                    modifier = dragHandle,
                    enabled = enabled && count > 1,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_drag_handle),
                        stringResource(R.string.move_quest_action, quest.type.title),
                    )
                }
                DropdownMenu(orderMenu, handleDismiss) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_earlier)) },
                        onClick = handleEarlier,
                        enabled = enabled && index > 0,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_later)) },
                        onClick = handleLater,
                        enabled = enabled && index < count - 1,
                    )
                }
            }
        },
    ) {
        Text(quest.config.instruction, maxLines = 2)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onOpen, modifier = Modifier.weight(1f), enabled = enabled) {
                Text(
                    stringResource(R.string.configure_quest_action),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onRemove, modifier = Modifier.weight(1f), enabled = enabled && count > 1) {
                Text(
                    stringResource(R.string.remove_quest_action),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
