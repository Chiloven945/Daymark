package top.chiloven.daymark.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.designsystem.component.CategoryDot
import top.chiloven.daymark.core.designsystem.component.CountdownCard
import top.chiloven.daymark.core.designsystem.component.DaymarkBackground
import top.chiloven.daymark.core.designsystem.component.DaymarkSectionHeader
import top.chiloven.daymark.core.designsystem.component.DaymarkSurfaceCard
import top.chiloven.daymark.core.designsystem.component.DaysBadge
import top.chiloven.daymark.core.designsystem.component.EmptyState
import top.chiloven.daymark.core.designsystem.theme.DaymarkSpacing
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
    DaymarkBackground(modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onNewEvent,
                    modifier = Modifier.padding(20.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.ds_new_event)) },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding)
                        .padding(horizontal = DaymarkSpacing.xl),
                contentPadding = PaddingValues(top = DaymarkSpacing.lg, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(DaymarkSpacing.lg),
            ) {
                item(key = "header") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.ds_your_days),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = state.today.formatMediumDate(),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                }

                state.nextEvent?.let { next ->
                    item(key = "hero") {
                        CountdownCard(
                            occurrence = next,
                            unitLabel = countdownLabel(next),
                            onClick = { onOpenEvent(next.event.id) },
                        )
                    }
                }

                if (state.todayEvents.isNotEmpty()) {
                    item(key = "today-header") {
                        DaymarkSectionHeader(
                            title = stringResource(R.string.ds_today),
                            count = state.todayEvents.size,
                        )
                    }
                    items(state.todayEvents, key = { "today-${it.event.id}" }) { occurrence ->
                        TodayEventCard(
                            occurrence = occurrence,
                            onClick = { onOpenEvent(occurrence.event.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (state.upcoming.isNotEmpty()) {
                    item(key = "upcoming-header") {
                        DaymarkSectionHeader(
                            title = stringResource(R.string.ds_upcoming),
                            count = state.upcoming.size,
                            action = {
                                TextButton(onClick = onSeeAll) {
                                    Text(stringResource(R.string.ds_see_all))
                                }
                            },
                        )
                    }
                    items(state.upcoming, key = { "upcoming-${it.event.id}" }) { occurrence ->
                        UpcomingEventCard(
                            occurrence = occurrence,
                            onClick = { onOpenEvent(occurrence.event.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (!state.isLoading && !state.hasAnyEvents) {
                    item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.ds_empty_home_title),
                            message = stringResource(R.string.ds_empty_home_message),
                            actionLabel = stringResource(R.string.ds_new_event),
                            onAction = onNewEvent,
                        )
                    }
                }

                if (!state.isLoading && state.hasAnyEvents && state.nextEvent == null &&
                    state.todayEvents.isEmpty()
                ) {
                    item(key = "nothing-today") {
                        DaymarkSurfaceCard {
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
    }
}

@Composable
private fun TodayEventCard(
    occurrence: EventOccurrence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = DefaultCategories.byId(occurrence.event.categoryId)
    DaymarkSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DaymarkSpacing.md),
        ) {
            CategoryDot(
                color = category?.let { Color(it.colorArgb.toInt()) }
                    ?: MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = occurrence.event.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                val categoryName = category?.displayName()
                Text(
                    text = if (categoryName != null) {
                        "$categoryName · ${occurrence.targetDate.formatMediumDate()}"
                    } else {
                        occurrence.targetDate.formatMediumDate()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DaysBadge(text = stringResource(R.string.ds_today))
        }
    }
}

@Composable
private fun UpcomingEventCard(
    occurrence: EventOccurrence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = DefaultCategories.byId(occurrence.event.categoryId)
    DaymarkSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DaymarkSpacing.md),
        ) {
            CategoryDot(
                color = category?.let { Color(it.colorArgb.toInt()) }
                    ?: MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = occurrence.event.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = occurrence.targetDate.formatMediumDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DaysBadge(
                text = if (occurrence.days == 1L) {
                    "1"
                } else {
                    occurrence.days.toString()
                },
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun countdownLabel(occurrence: EventOccurrence): String =
    when {
        occurrence.phase == EventPhase.TODAY -> stringResource(R.string.ds_today)
        occurrence.isCountUp -> stringResource(R.string.ds_days_passed)
        occurrence.days == 1L -> stringResource(R.string.ds_day_left)
        else -> stringResource(R.string.ds_days_left)
    }
