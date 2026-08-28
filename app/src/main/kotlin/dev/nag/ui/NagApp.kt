package dev.nag.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nag.data.NagRepository

@Composable
fun NagApp(repository: NagRepository, modifier: Modifier = Modifier) {
    var showQueue by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showQueue) { showQueue = false }
    if (showQueue) {
        QueueScreen(repository = repository, onBack = { showQueue = false }, modifier = modifier)
    } else {
        DeckScreen(repository = repository, onOpenQueue = { showQueue = true }, modifier = modifier)
    }
}
