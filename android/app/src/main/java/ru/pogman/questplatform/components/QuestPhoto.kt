package ru.pogman.questplatform.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import ru.pogman.questplatform.R

@Composable
fun QuestPhoto(uri: String) {
    SubcomposeAsyncImage(
        model = uri,
        contentDescription = stringResource(R.string.photo_description),
        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(MaterialTheme.shapes.medium),
        contentScale = ContentScale.Fit,
        success = { SubcomposeAsyncImageContent(Modifier.testTag("questPhoto")) },
        error = {
            Text(stringResource(R.string.photo_load_error), color = MaterialTheme.colorScheme.error)
        },
    )
}
