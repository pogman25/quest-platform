package ru.pogman.questplatform.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pogman.questplatform.R
import ru.pogman.questplatform.domain.SilentCompetition
import ru.pogman.questplatform.domain.formatDuration

@Composable
fun SilentStandingsCard(competition: SilentCompetition) {
    SectionCard(stringResource(R.string.silent_standings_title, competition.title)) {
        Text(stringResource(R.string.silent_standings_hint))
        competition.standings.forEach { row ->
            Text(
                stringResource(
                    R.string.silent_standing_row,
                    row.place,
                    row.team,
                    row.count,
                    formatDuration(row.durationSeconds),
                )
            )
        }
    }
}
