package top.chiloven.daymark.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.designsystem.component.CategoryDot
import top.chiloven.daymark.core.designsystem.component.CountdownCard
import top.chiloven.daymark.core.designsystem.component.DaysBadge
import top.chiloven.daymark.core.designsystem.component.EmptyState
import top.chiloven.daymark.core.designsystem.component.SectionHeader
import top.chiloven.daymark.core.designsystem.util.displayName
import top.chiloven.daymark.core.designsystem.util.formatMediumDate
import top.chiloven.daymark.core.model.DefaultCategories
import top.chiloven.daymark.core.model.EventOccurrence
import top.chiloven.daymark.core.model.EventPhase

@Composable
fun HomeScreen(
    onOpenEvent: (Long) -> Unit,
    onNewEvent: () -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onOpenEvent = onOpenEvent,
        onNewEvent = onNewEvent,
        onSeeAll = onSeeAll,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onOpenEvent: (Long) -> Unit,
    onNewEvent: () -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.today.formatMediumDate(),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
            )
        },
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val nextEvent = state.nextEvent
            if (nextEvent != null) {
                item(key = "next") {
                    CountdownCard(
                        occurrence = nextEvent,
                        unitLabel = countdownLabel(nextEvent),
                        onClick = { onOpenEvent(nextEvent.event.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.todayEvents.isNotEmpty()) {
                item(key = "today-header") {
                    SectionHeader(stringResource(R.string.ds_today))
                }
                items(state.todayEvents, key = { "today-${it.event.id}" }) { occurrence ->
                    EventRow(
                        occurrence = occurrence,
                        onClick = { onOpenEvent(occurrence.event.id) },
                    )
                }
            }

            if (state.upcoming.isNotEmpty()) {
                item(key = "upcoming-header") {
                    SectionHeader(
                        title = stringResource(R.string.ds_upcoming),
                        action = {
                            TextButton(onClick = onSeeAll) {
                                Text(stringResource(R.string.ds_see_all))
                            }
                        },
                    )
                }
                items(state.upcoming, key = { "upcoming-${it.event.id}" }) { occurrence ->
                    EventRow(
                        occurrence = occurrence,
                        onClick = { onOpenEvent(occurrence.event.id) },
                    )
                }
            }

            if (!state.isLoading && !state.hasAnyEvents) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.ds_empty_home_title),
                        message = stringResource(R.string.ds_empty_home_message),
                    )
                    Button(
                        onClick = onNewEvent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.ds_new_event))
                    }
                }
            }

            if (!state.isLoading && state.hasAnyEvents && state.nextEvent == null && state.todayEvents.isEmpty()) {
                item(key = "nothing-today") {
                    Text(
                        text = stringResource(R.string.ds_no_events_today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun countdownLabel(occurrence: EventOccurrence): String {
    return when {
        occurrence.phase == EventPhase.TODAY -> stringResource(R.string.ds_today)
        occurrence.isCountUp -> stringResource(R.string.ds_days_passed)
        occurrence.days == 1L -> stringResource(R.string.ds_day_left)
        else -> stringResource(R.string.ds_days_left)
    }
}

@Composable
private fun EventRow(
    occurrence: EventOccurrence,
    onClick: () -> Unit,
) {
    val category = DefaultCategories.byId(occurrence.event.categoryId)
    ListItem(
        supportingContent = {
            val categoryName = category?.displayName()
            val date = occurrence.targetDate.formatMediumDate()
            Text(if (categoryName != null) "$date · $categoryName" else date)
        },
        leadingContent = category?.let {
            { CategoryDot(color = Color(it.colorArgb.toInt())) }
        },
        trailingContent = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                DaysBadge(
                    text = when {
                        occurrence.phase == EventPhase.TODAY -> stringResource(R.string.ds_today)
                        occurrence.isCountUp -> "${-occurrence.days}"
                        else -> "${occurrence.days}"
                    },
                )
            }
        },
        modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Text(occurrence.event.title)
    }
}
