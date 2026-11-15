package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.pogman.questplatform.R

@Composable
fun CaseIntroduction() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                stringResource(R.string.case_eyebrow),
                color = MaterialTheme.colorScheme.primaryContainer,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(stringResource(R.string.case_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.case_description), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
