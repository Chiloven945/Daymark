package top.chiloven.daymark.feature.editor.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.designsystem.component.CountdownCard
import top.chiloven.daymark.core.designsystem.util.displayName
import top.chiloven.daymark.core.designsystem.util.formatMediumDate
import top.chiloven.daymark.core.model.DefaultCategories
import top.chiloven.daymark.core.model.EventOccurrence
import top.chiloven.daymark.core.model.EventPhase
import top.chiloven.daymark.core.model.RecurrenceFrequency
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ds_event_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    state.event?.let { event ->
                        IconButton(onClick = { viewModel.setPinned(!event.isPinned) }) {
                            Icon(
                                imageVector = if (event.isPinned) {
                                    Icons.Filled.Star
                                } else {
                                    Icons.Outlined.StarBorder
                                },
                                contentDescription = null,
                            )
                        }
                        IconButton(onClick = { onEdit(event.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val event = state.event ?: return@Scaffold
        val occurrence = state.occurrence
        LazyColumn(
            modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            occurrence?.let {
                item {
                    CountdownCard(
                        occurrence = it,
                        unitLabel = countdownUnit(it),
                    )
                }
            }
            item {
                ListItem(
                    supportingContent = { Text(event.date.formatMediumDate()) },
                ) {
                    Text(stringResource(R.string.ds_date))
                }
            }
            item {
                ListItem(
                    supportingContent = { Text(repeatLabel(event.recurrence.frequency)) },
                ) {
                    Text(stringResource(R.string.ds_repeat))
                }
            }
            item {
                ListItem(
                    supportingContent = {
                        Text(
                            DefaultCategories.byId(event.categoryId)?.displayName()
                                ?: stringResource(R.string.ds_category_other),
                        )
                    },
                ) {
                    Text(stringResource(R.string.ds_category))
                }
            }
            item {
                ListItem(
                    supportingContent = {
                        Text(
                            event.reminder?.let { reminder ->
                                "${reminder.offsetDays} ${stringResource(R.string.ds_days)} · " +
                                        reminder.time.format(
                                            DateTimeFormatter.ofPattern(
                                                "HH:mm",
                                                Locale.getDefault()
                                            )
                                        )
                            } ?: stringResource(R.string.ds_never),
                        )
                    },
                ) {
                    Text(stringResource(R.string.ds_reminder))
                }
            }
            item {
                ListItem(
                    supportingContent = {
                        Text(event.note.ifBlank { stringResource(R.string.ds_no_note) })
                    },
                ) {
                    Text(stringResource(R.string.ds_note))
                }
            }
            if (event.recurrence.frequency == RecurrenceFrequency.YEARLY && occurrence?.anniversaryYear != null) {
                item {
                    Text(
                        text = "${stringResource(R.string.ds_anniversary)} ${occurrence.anniversaryYear}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text(stringResource(R.string.ds_delete))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.ds_delete_event_title)) },
            text = { Text(stringResource(R.string.ds_delete_event_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                ) {
                    Text(stringResource(R.string.ds_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.ds_cancel))
                }
            },
        )
    }
}

@Composable
private fun countdownUnit(occurrence: EventOccurrence): String = when {
    occurrence.phase == EventPhase.TODAY -> stringResource(R.string.ds_today)
    occurrence.isCountUp -> stringResource(R.string.ds_days_passed)
    occurrence.days == 1L -> stringResource(R.string.ds_day_left)
    else -> stringResource(R.string.ds_days_left)
}

@Composable
private fun repeatLabel(frequency: RecurrenceFrequency): String = stringResource(
    when (frequency) {
        RecurrenceFrequency.NONE -> R.string.ds_never
        RecurrenceFrequency.WEEKLY -> R.string.ds_weekly
        RecurrenceFrequency.MONTHLY -> R.string.ds_monthly
        RecurrenceFrequency.YEARLY -> R.string.ds_yearly
    },
)
