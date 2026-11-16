package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.GameSummary

@Composable
fun OrganizerGameCard(
    game: GameSummary,
    enabled: Boolean,
    onEdit: (String) -> Unit,
    onPrepare: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val handleEdit = { onEdit(game.id) }
    val handlePrepare = { onPrepare(game.id) }
    val handleDelete = { onDelete(game.id) }

    SectionCard(game.title) {
        Text(pluralStringResource(R.plurals.quest_count, game.questCount, game.questCount))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = handlePrepare, enabled = enabled) {
                Text(stringResource(R.string.prepare_action))
            }
            TextButton(onClick = handleEdit, enabled = enabled) {
                Text(stringResource(R.string.edit_action))
            }
            TextButton(onClick = handleDelete, enabled = enabled) {
                Text(stringResource(R.string.delete_action))
            }
        }
    }
}
