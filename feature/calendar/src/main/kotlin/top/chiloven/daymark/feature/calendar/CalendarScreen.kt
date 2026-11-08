package top.chiloven.daymark.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.designsystem.component.CategoryDot
import top.chiloven.daymark.core.designsystem.util.formatMediumDate
import top.chiloven.daymark.core.designsystem.util.formatMonthYear
import top.chiloven.daymark.core.model.DefaultCategories
import top.chiloven.daymark.core.model.EventOccurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun CalendarScreen(
    onOpenEvent: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
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
        Column(
            modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp),
        ) {
            WeekdayHeader(firstDayOfWeek = state.firstDayOfWeek)
            MonthGrid(
                state = state,
                onSelectDate = viewModel::selectDate,
            )
            Text(
                text = state.selectedDate.formatMediumDate(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    state.selectedDayEvents,
                    key = { "${state.selectedDate}-${it.event.id}" }) { occurrence ->
                    DayEventRow(
                        occurrence = occurrence,
                        onClick = { onOpenEvent(occurrence.event.id) },
                    )
                }
                if (state.selectedDayEvents.isEmpty()) {
                    item {
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
private fun WeekdayHeader(firstDayOfWeek: DayOfWeek) {
    val days = (0..6).map { offset -> firstDayOfWeek.plus(offset.toLong()) }
    Row(
        modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
    ) {
        days.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale),
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
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
) {
    val gridStart = state.month.atDay(1).startOfWeek(state.firstDayOfWeek)
    val weeks = (0 until 6).map { week ->
        (0 until 7).map { day -> gridStart.plusDays((week * 7 + day).toLong()) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        month = state.month,
                        today = state.today,
                        selected = date == state.selectedDate,
                        hasEvents = !state.eventsByDay[date].isNullOrEmpty(),
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
    hasEvents: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inMonth = YearMonth.from(date) == month
    val background = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        inMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
                .padding(2.dp)
                .size(44.dp)
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
            if (hasEvents) {
                Box(
                    modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary,
                                CircleShape,
                            ),
                )
            }
        }
    }
}

@Composable
private fun DayEventRow(
    occurrence: EventOccurrence,
    onClick: () -> Unit,
) {
    val category = DefaultCategories.byId(occurrence.event.categoryId)
    ListItem(
        leadingContent = category?.let {
            { CategoryDot(color = Color(it.colorArgb.toInt())) }
        },
        modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Text(occurrence.event.title)
    }
}

private fun LocalDate.startOfWeek(firstDayOfWeek: DayOfWeek): LocalDate {
    val offset = (dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    return minusDays(offset.toLong())
}
