package top.chiloven.daymark.feature.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.designsystem.component.CategoryDot
import top.chiloven.daymark.core.designsystem.component.DaysBadge
import top.chiloven.daymark.core.designsystem.component.EmptyState
import top.chiloven.daymark.core.designsystem.component.SectionHeader
import top.chiloven.daymark.core.designsystem.util.displayName
import top.chiloven.daymark.core.designsystem.util.formatMediumDate
import top.chiloven.daymark.core.model.DefaultCategories
import top.chiloven.daymark.core.model.EventOccurrence
import top.chiloven.daymark.core.model.EventPhase
import kotlinx.coroutines.launch

@Composable
fun EventsScreen(
    onOpenEvent: (Long) -> Unit,
    onEditEvent: (Long) -> Unit,
    onNewEvent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.ds_event_deleted)
    val undoMessage = stringResource(R.string.ds_undo)

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.ds_tab_events)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewEvent,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.ds_new_event)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.ds_search_events)) },
                )
            }
            item(key = "filters") {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EventFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.label()) },
                        )
                    }
                }
            }

            state.groups.forEach { group ->
                item(key = "header-${group.type}") {
                    SectionHeader(group.type.label())
                }
                items(group.items, key = { "${group.type}-${it.event.id}" }) { occurrence ->
                    EventListRow(
                        occurrence = occurrence,
                        onClick = { onOpenEvent(occurrence.event.id) },
                        onEdit = { onEditEvent(occurrence.event.id) },
                        onTogglePin = {
                            viewModel.setPinned(occurrence.event, !occurrence.event.isPinned)
                        },
                        onDelete = {
                            viewModel.delete(occurrence.event)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = deletedMessage,
                                    actionLabel = undoMessage,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restore(occurrence.event)
                                }
                            }
                        },
                    )
                }
            }

            if (!state.isLoading && state.groups.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.ds_empty_events_title),
                        message = stringResource(R.string.ds_empty_events_message),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventListRow(
    occurrence: EventOccurrence,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val category = DefaultCategories.byId(occurrence.event.categoryId)
    ListItem(
        supportingContent = {
            val date = occurrence.targetDate.formatMediumDate()
            val categoryName = category?.displayName()
            Text(
                text = if (categoryName != null) "$date · $categoryName" else date,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = category?.let {
            { CategoryDot(color = Color(it.colorArgb.toInt())) }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DaysBadge(
                    text = when {
                        occurrence.phase == EventPhase.TODAY -> stringResource(R.string.ds_today)
                        occurrence.isCountUp -> "${-occurrence.days}"
                        else -> "${occurrence.days}"
                    },
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ds_edit)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (occurrence.event.isPinned) R.string.ds_unpin else R.string.ds_pin,
                                    ),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePin()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ds_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        },
        modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Text(occurrence.event.title)
    }
}

@Composable
private fun EventFilter.label(): String = stringResource(
    when (this) {
        EventFilter.ALL -> R.string.ds_filter_all
        EventFilter.COUNTDOWN -> R.string.ds_filter_countdown
        EventFilter.COUNT_UP -> R.string.ds_filter_count_up
        EventFilter.ANNIVERSARIES -> R.string.ds_filter_anniversaries
    },
)

@Composable
private fun EventGroupType.label(): String = stringResource(
    when (this) {
        EventGroupType.PINNED -> R.string.ds_pinned
        EventGroupType.THIS_MONTH -> R.string.ds_this_month
        EventGroupType.LATER -> R.string.ds_later
        EventGroupType.PAST -> R.string.ds_past
    },
)
