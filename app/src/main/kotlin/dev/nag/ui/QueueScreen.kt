package dev.nag.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.nag.Constants
import dev.nag.R
import dev.nag.data.NagRepository
import dev.nag.domain.Chore
import kotlinx.coroutines.launch

@Composable
fun QueueScreen(repository: NagRepository, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val chores by repository.activeChores.collectAsState(initial = emptyList())
    var name by rememberSaveable { mutableStateOf("") }
    var cadenceText by rememberSaveable { mutableStateOf("1") }
    var editingChoreId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val cadence = cadenceText.toIntOrNull()
    val canSubmit = name.isNotBlank() && cadence != null && cadence >= Constants.CADENCE_MIN_DAYS

    fun resetForm() {
        name = ""
        cadenceText = "1"
        editingChoreId = null
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_deck),
                )
            }
            Text(
                text = stringResource(R.string.queue_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        if (chores.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_queue_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(chores, key = { it.id }) { chore ->
                    ChoreRow(
                        chore = chore,
                        onEdit = {
                            editingChoreId = chore.id
                            name = chore.name
                            cadenceText = chore.cadenceDays.toString()
                        },
                        onArchive = {
                            if (editingChoreId == chore.id) resetForm()
                            scope.launch { repository.archiveChore(chore.id) }
                        },
                    )
                }
            }
        }
        if (chores.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.chore_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = cadenceText,
            onValueChange = { cadenceText = it },
            label = { Text(stringResource(R.string.cadence_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                focusManager.clearFocus()
                val submittedName = name.trim()
                val submittedCadence = cadence ?: Constants.CADENCE_MIN_DAYS
                val editedChoreId = editingChoreId
                scope.launch {
                    if (editedChoreId == null) {
                        repository.addChore(submittedName, submittedCadence)
                    } else {
                        repository.editChore(editedChoreId, submittedName, submittedCadence)
                    }
                }
                resetForm()
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (editingChoreId == null) R.string.add_chore else R.string.save_chore,
                ),
            )
        }
    }
}

@Composable
private fun ChoreRow(
    chore: Chore,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = chore.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = pluralStringResource(
                    R.plurals.cadence_description,
                    chore.cadenceDays,
                    chore.cadenceDays,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onEdit) {
            Text(stringResource(R.string.edit_chore))
        }
        TextButton(onClick = onArchive) {
            Text(stringResource(R.string.archive_chore))
        }
    }
}
