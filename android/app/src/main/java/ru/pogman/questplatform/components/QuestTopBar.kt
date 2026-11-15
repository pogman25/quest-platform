package ru.pogman.questplatform.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pogman.questplatform.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestTopBar(
    @StringRes title: Int,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onOrganizer: () -> Unit,
) {
    val backDescription = stringResource(R.string.back_action)

    TopAppBar(
        title = { Text(stringResource(title), fontSize = 18.sp) },
        navigationIcon = {
            if (canGoBack) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = backDescription)
                }
            } else {
                NotebookEmblem(modifier = Modifier.size(48.dp))
            }
        },
        actions = {
            if (!canGoBack) {
                TextButton(onClick = onOrganizer) {
                    Text(stringResource(R.string.organizer_action))
                }
            }
        },
    )
}
