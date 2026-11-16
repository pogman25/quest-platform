package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ru.pogman.questplatform.R

@Composable
fun OrderButtons(index: Int, count: Int, enabled: Boolean, onMove: (Int) -> Unit) {
    val earlier = stringResource(R.string.move_earlier)
    val later = stringResource(R.string.move_later)
    val handleEarlier = { onMove(-1) }
    val handleLater = { onMove(1) }
    Row {
        IconButton(
            handleEarlier,
            Modifier.semantics { contentDescription = earlier },
            enabled && index > 0,
        ) {
            Text("↑")
        }
        IconButton(
            handleLater,
            Modifier.semantics { contentDescription = later },
            enabled && index < count - 1,
        ) {
            Text("↓")
        }
    }
}
