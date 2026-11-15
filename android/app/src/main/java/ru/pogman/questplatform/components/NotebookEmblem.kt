package ru.pogman.questplatform.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun NotebookEmblem(modifier: Modifier = Modifier) {
    val cover = MaterialTheme.colorScheme.primary
    val detail = MaterialTheme.colorScheme.primaryContainer

    Canvas(modifier = modifier.size(36.dp)) {
        drawRoundRect(cover, cornerRadius = CornerRadius(size.width * 0.2f))
        drawLine(detail, Offset(size.width * 0.28f, size.height * 0.2f), Offset(size.width * 0.28f, size.height * 0.8f), size.width * 0.05f)
        repeat(3) { index ->
            drawRoundRect(
                color = detail,
                topLeft = Offset(size.width * 0.42f, size.height * (0.3f + index * 0.16f)),
                size = Size(size.width * (if (index == 2) 0.2f else 0.35f), size.height * 0.05f),
                cornerRadius = CornerRadius(size.width * 0.02f),
            )
        }
    }
}
