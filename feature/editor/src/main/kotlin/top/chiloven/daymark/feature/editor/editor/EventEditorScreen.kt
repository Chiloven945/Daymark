package top.chiloven.daymark.feature.editor.editor

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.designsystem.component.CountdownCard
import top.chiloven.daymark.core.designsystem.component.DaymarkBackground
import top.chiloven.daymark.core.designsystem.component.DaymarkSurfaceCard
import top.chiloven.daymark.core.designsystem.theme.DaymarkSpacing
import top.chiloven.daymark.core.designsystem.util.displayName
import top.chiloven.daymark.core.designsystem.util.formatMediumDate
import top.chiloven.daymark.core.model.CountDirection
import top.chiloven.daymark.core.model.DefaultCategories
import top.chiloven.daymark.core.model.EventOccurrence
import top.chiloven.daymark.core.model.EventPhase
import top.chiloven.daymark.core.model.RecurrenceFrequency
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun EventEditorScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorMessage = stringResource(R.string.ds_unable_to_save)
    var showDatePicker by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onClose()
    }
    LaunchedEffect(state.saveError) {
        if (state.saveError) {
            snackbarHostState.showSnackbar(saveErrorMessage)
        }
    }

    DaymarkBackground(modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                if (state.isEditing) R.string.ds_edit else R.string.ds_new_event,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = viewModel::save,
                            enabled = !state.isSaving,
                        ) {
                            Text(stringResource(R.string.ds_save))
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

            LazyColumn(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = DaymarkSpacing.xl),
                contentPadding = PaddingValues(top = DaymarkSpacing.sm, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(DaymarkSpacing.xl),
            ) {
                state.preview?.let { preview ->
                    item(key = "preview") {
                        CountdownCard(
                            occurrence = preview,
                            unitLabel = previewUnit(preview),
                        )
                    }
                }

                item(key = "event-section") {
                    FormSection(title = stringResource(R.string.ds_section_event)) {
                        OutlinedTextField(
                            value = state.draft.title,
                            onValueChange = viewModel::updateTitle,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.ds_title)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            isError = EditorField.TITLE in state.errors,
                            supportingText = if (EditorField.TITLE in state.errors) {
                                { Text(stringResource(R.string.ds_title_required)) }
                            } else {
                                null
                            },
                        )
                    }
                }

                item(key = "schedule-section") {
                    FormSection(title = stringResource(R.string.ds_section_schedule)) {
                        SettingRow(
                            label = stringResource(R.string.ds_date),
                            value = state.draft.date.formatMediumDate(),
                            onClick = { showDatePicker = true },
                        )
                        Text(
                            text = stringResource(R.string.ds_direction),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                        ) {
                            CountDirection.entries.forEachIndexed { index, direction ->
                                SegmentedButton(
                                    selected = state.draft.direction == direction,
                                    onClick = { viewModel.updateDirection(direction) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = CountDirection.entries.size,
                                    ),
                                    label = { Text(direction.label()) },
                                )
                            }
                        }
                        SettingRow(
                            label = stringResource(R.string.ds_repeat),
                            value = state.draft.recurrence.frequency.label(),
                            onClick = { showRepeatDialog = true },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                item(key = "category-section") {
                    FormSection(title = stringResource(R.string.ds_category)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(DaymarkSpacing.sm)) {
                            items(DefaultCategories.all, key = { it.id }) { category ->
                                FilterChip(
                                    selected = state.draft.categoryId == category.id,
                                    onClick = { viewModel.updateCategory(category.id) },
                                    label = { Text(category.displayName()) },
                                    shape = RoundedCornerShape(999.dp),
                                )
                            }
                        }
                    }
                }

                item(key = "reminder-section") {
                    DaymarkSurfaceCard(
                        modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.ds_reminder_enabled),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = state.draft.reminderEnabled,
                                onCheckedChange = viewModel::updateReminderEnabled,
                            )
                        }
                        if (state.draft.reminderEnabled) {
                            Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = DaymarkSpacing.md),
                                horizontalArrangement = Arrangement.spacedBy(DaymarkSpacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = state.draft.reminderOffsetDays.toString(),
                                    onValueChange = { value ->
                                        value.toIntOrNull()?.let(viewModel::updateReminderOffset)
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text(stringResource(R.string.ds_reminder_offset)) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true,
                                )
                                TextButton(onClick = { showTimePicker = true }) {
                                    Text(
                                        text = state.draft.reminderTime.formatTime(),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "note-section") {
                    FormSection(title = stringResource(R.string.ds_note)) {
                        OutlinedTextField(
                            value = state.draft.note,
                            onValueChange = viewModel::updateNote,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.ds_note_hint)) },
                            shape = RoundedCornerShape(16.dp),
                            minLines = 3,
                        )
                    }
                }

                if (state.isEditing) {
                    item(key = "delete") {
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text(
                                text = stringResource(R.string.ds_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.draft.date
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.updateDate(
                                Instant.ofEpochMilli(millis)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate(),
                            )
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ds_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.ds_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRepeatDialog) {
        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            title = { Text(stringResource(R.string.ds_repeat)) },
            text = {
                Column {
                    RecurrenceFrequency.entries.forEach { frequency ->
                        Row(
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateRecurrence(frequency)
                                        showRepeatDialog = false
                                    }
                                    .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.draft.recurrence.frequency == frequency,
                                onClick = {
                                    viewModel.updateRecurrence(frequency)
                                    showRepeatDialog = false
                                },
                            )
                            Text(frequency.label())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepeatDialog = false }) {
                    Text(stringResource(R.string.ds_close))
                }
            },
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.draft.reminderTime.hour,
            initialMinute = state.draft.reminderTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.ds_reminder_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateReminderTime(
                            LocalTime.of(timePickerState.hour, timePickerState.minute),
                        )
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ds_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.ds_cancel))
                }
            },
        )
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
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DaymarkSpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
        DaymarkSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun previewUnit(occurrence: EventOccurrence): String =
    when {
        occurrence.phase == EventPhase.TODAY -> stringResource(R.string.ds_today)
        occurrence.isCountUp -> stringResource(R.string.ds_days_passed)
        occurrence.days == 1L -> stringResource(R.string.ds_day_left)
        else -> stringResource(R.string.ds_days_left)
    }

@Composable
private fun CountDirection.label(): String =
    stringResource(
        when (this) {
            CountDirection.COUNT_DOWN -> R.string.ds_countdown
            CountDirection.COUNT_UP -> R.string.ds_count_up
            CountDirection.AUTO -> R.string.ds_auto
        },
    )

@Composable
private fun RecurrenceFrequency.label(): String =
    stringResource(
        when (this) {
            RecurrenceFrequency.NONE -> R.string.ds_never
            RecurrenceFrequency.WEEKLY -> R.string.ds_weekly
            RecurrenceFrequency.MONTHLY -> R.string.ds_monthly
            RecurrenceFrequency.YEARLY -> R.string.ds_yearly
        },
    )

private fun LocalTime.formatTime(): String =
    "%02d:%02d".format(hour, minute)
