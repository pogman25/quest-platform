package ru.pogman.questplatform.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import ru.pogman.questplatform.domain.elapsedSeconds
import ru.pogman.questplatform.domain.formatDuration

@Composable
fun ElapsedTime(startedAt: Long?, finishedAt: Long?, label: String) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt, finishedAt) {
        while (startedAt != null && finishedAt == null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val seconds = elapsedSeconds(startedAt, finishedAt, now) ?: 0
    Text("$label ${formatDuration(seconds)}", style = MaterialTheme.typography.titleMedium)
}
