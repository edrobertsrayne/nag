package dev.nag.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nag.R
import dev.nag.data.NagRepository
import kotlinx.coroutines.launch

internal const val DECK_CARD_TAG = "deck_card"

@Composable
fun DeckScreen(repository: NagRepository, onOpenQueue: () -> Unit, modifier: Modifier = Modifier) {
    val deck by repository.deck.collectAsState(initial = emptyList())
    val streak by repository.streak.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = onOpenQueue, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = stringResource(R.string.open_queue),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
        ) {
            val top = deck.firstOrNull()
            if (top == null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = streak.toString(),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Text(
                        text = stringResource(R.string.streak_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.empty_deck_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                SwipeCard(
                    cardId = top.id,
                    onSwipeRight = { scope.launch { repository.completeChore(top.id) } },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(DECK_CARD_TAG),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = top.name,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
        }
    }
}
