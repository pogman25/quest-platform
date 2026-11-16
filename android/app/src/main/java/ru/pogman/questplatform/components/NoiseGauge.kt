package ru.pogman.questplatform.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun NoiseGauge(progress: Float, alert: Boolean, threshold: Boolean = true) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = if (alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val marker = MaterialTheme.colorScheme.onSurface
    val value = progress.coerceIn(0f, 1f)
    Canvas(Modifier.fillMaxWidth().height(24.dp).progressSemantics(value)) {
        val radius = CornerRadius(6.dp.toPx())
        drawRoundRect(track, cornerRadius = radius)
        if (value > 0f)
            drawRoundRect(fill, size = Size(size.width * value, size.height), cornerRadius = radius)
        if (threshold)
            drawLine(
                marker,
                Offset(size.width * 0.8f, 0f),
                Offset(size.width * 0.8f, size.height),
                strokeWidth = 2.dp.toPx(),
            )
    }
}
