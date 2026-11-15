package ru.pogman.questplatform.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val QuestColors = lightColorScheme(
    primary = CaseGreen,
    onPrimary = Paper,
    primaryContainer = Amber,
    onPrimaryContainer = Ink,
    secondary = AmberInk,
    onSecondary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Sheet,
    onSurfaceVariant = MutedInk,
    outline = MutedInk,
    outlineVariant = PaperLine,
    error = ErrorInk,
)

@Composable
fun QuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuestColors,
        typography = QuestTypography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
