package top.chiloven.daymark.feature.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.designsystem.component.CategoryDot
import top.chiloven.daymark.core.designsystem.component.DaymarkBackground
import top.chiloven.daymark.core.designsystem.component.DaymarkSectionHeader
import top.chiloven.daymark.core.designsystem.component.DaymarkSurfaceCard
import top.chiloven.daymark.core.designsystem.theme.DaymarkSpacing
import top.chiloven.daymark.core.designsystem.util.displayName
import top.chiloven.daymark.core.designsystem.util.formatMediumDate
import top.chiloven.daymark.core.designsystem.util.formatMonthYear
import top.chiloven.daymark.core.model.DefaultCategories
import top.chiloven.daymark.core.model.EventOccurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onOpenEvent: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DaymarkBackground(modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text(state.month.atDay(1).formatMonthYear()) },
                    navigationIcon = {
                        IconButton(onClick = viewModel::previousMonth) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = viewModel::goToToday) {
                            Text(stringResource(R.string.ds_calendar_today))
                        }
                        IconButton(onClick = viewModel::nextMonth) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = DaymarkSpacing.xl),
                contentPadding = PaddingValues(top = DaymarkSpacing.sm, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(DaymarkSpacing.md),
            ) {
                item(key = "month") {
                    AnimatedContent(
                        targetState = state.month,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (
                                        slideInHorizontally(tween(300)) { it / 3 } +
                                                fadeIn(tween(200))
                                        ).togetherWith(
                                            slideOutHorizontally(tween(260)) { -it / 3 } +
                                                    fadeOut(tween(140)),
                                        )
                            } else {
                                (
                                        slideInHorizontally(tween(300)) { -it / 3 } +
                                                fadeIn(tween(200))
                                        ).togetherWith(
                                            slideOutHorizontally(tween(260)) { it / 3 } +
                                                    fadeOut(tween(140)),
                                        )
                            }
                        },
                        label = "calendar-month",
                    ) { month ->
                        MonthCard(
                            month = month,
                            state = state,
                            onSelectDate = viewModel::selectDate,
                        )
                    }
                }
                item(key = "selected-header") {
                    DaymarkSectionHeader(
                        title = state.selectedDate.formatMediumDate(),
                        count = state.selectedDayEvents.size,
                    )
                }
                items(
                    state.selectedDayEvents,
                    key = { "${state.selectedDate}-${it.event.id}" },
                ) { occurrence ->
                    DayEventCard(
                        occurrence = occurrence,
                        modifier = Modifier.animateItem(),
                        onClick = { onOpenEvent(occurrence.event.id) },
                    )
                }
                if (state.selectedDayEvents.isEmpty()) {
                    item(key = "empty-day") {
                        Text(
                            text = stringResource(R.string.ds_no_events_day),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCard(
    month: YearMonth,
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
) {
    DaymarkSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    ) {
        WeekdayHeader(firstDayOfWeek = state.firstDayOfWeek)
        MonthGrid(
            month = month,
            today = state.today,
            selectedDate = state.selectedDate,
            eventsByDay = state.eventsByDay,
            firstDayOfWeek = state.firstDayOfWeek,
            onSelectDate = onSelectDate,
        )
    }
}

@Composable
private fun WeekdayHeader(firstDayOfWeek: DayOfWeek) {
    val days = (0..6).map { offset -> firstDayOfWeek.plus(offset.toLong()) }
    Row(
        modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
    ) {
        days.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    eventsByDay: Map<LocalDate, List<EventOccurrence>>,
    firstDayOfWeek: DayOfWeek,
    onSelectDate: (LocalDate) -> Unit,
) {
    val gridStart = month.atDay(1).startOfWeek(firstDayOfWeek)
    val weeks = (0 until 6).map { week ->
        (0 until 7).map { day -> gridStart.plusDays((week * 7 + day).toLong()) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        month = month,
                        today = today,
                        selected = date == selectedDate,
                        events = eventsByDay[date].orEmpty(),
                        onClick = { onSelectDate(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    month: YearMonth,
    today: LocalDate,
    selected: Boolean,
    events: List<EventOccurrence>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inMonth = YearMonth.from(date) == month
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        inMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    }
    val visibleEvents = events.take(3)

    Box(
        modifier = modifier
                .padding(2.dp)
                .size(46.dp)
                .background(background, CircleShape)
                .then(
                    if (date == today) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    },
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                visibleEvents.forEach { occurrence ->
                    val category = DefaultCategories.byId(occurrence.event.categoryId)
                    Box(
                        modifier = Modifier
                                .size(4.dp)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        category?.let { Color(it.colorArgb.toInt()) }
                                            ?: MaterialTheme.colorScheme.primary
                                    },
                                    CircleShape,
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayEventCard(
    occurrence: EventOccurrence,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val category = DefaultCategories.byId(occurrence.event.categoryId)
    val categoryName = category?.displayName()
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
                if (categoryName != null) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun LocalDate.startOfWeek(firstDayOfWeek: DayOfWeek): LocalDate {
    val offset = (dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    return minusDays(offset.toLong())
}
