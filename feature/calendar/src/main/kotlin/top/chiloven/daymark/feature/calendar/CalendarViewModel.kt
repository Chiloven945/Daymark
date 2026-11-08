package top.chiloven.daymark.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import top.chiloven.daymark.core.data.repository.EventRepository
import top.chiloven.daymark.core.data.repository.SettingsRepository
import top.chiloven.daymark.core.model.DateCalculator
import top.chiloven.daymark.core.model.EventOccurrence
import top.chiloven.daymark.core.model.TimeProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CalendarUiState(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val firstDayOfWeek: DayOfWeek,
    val eventsByDay: Map<LocalDate, List<EventOccurrence>>,
    val selectedDayEvents: List<EventOccurrence>,
    val isLoading: Boolean = true,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    eventRepository: EventRepository,
    settingsRepository: SettingsRepository,
    private val dateCalculator: DateCalculator,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val today = timeProvider.today()
    private val month = MutableStateFlow(YearMonth.from(today))
    private val selectedDate = MutableStateFlow(today)

    val uiState: StateFlow<CalendarUiState> = combine(
        eventRepository.observeAll(),
        settingsRepository.settings,
        month,
        selectedDate,
    ) { events, settings, currentMonth, currentSelectedDate ->
        val gridStart = currentMonth.atDay(1).startOfWeek(settings.firstDayOfWeek)
        val gridEnd = gridStart.plusDays(41)
        val occurrencesByDay = mutableMapOf<LocalDate, List<EventOccurrence>>()

        events.forEach { event ->
            dateCalculator.occurrencesInRange(event, gridStart, gridEnd).forEach { date ->
                val occurrence = dateCalculator.occurrenceOn(event, date, today)
                occurrencesByDay[date] = occurrencesByDay[date].orEmpty() + occurrence
            }
        }

        CalendarUiState(
            month = currentMonth,
            selectedDate = currentSelectedDate,
            today = today,
            firstDayOfWeek = settings.firstDayOfWeek,
            eventsByDay = occurrencesByDay,
            selectedDayEvents = occurrencesByDay[currentSelectedDate].orEmpty(),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(
            month = YearMonth.from(today),
            selectedDate = today,
            today = today,
            firstDayOfWeek = DayOfWeek.MONDAY,
            eventsByDay = emptyMap(),
            selectedDayEvents = emptyList(),
        ),
    )

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
        ensureSelectionInMonth()
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
        ensureSelectionInMonth()
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        month.value = YearMonth.from(date)
    }

    fun goToToday() {
        month.value = YearMonth.from(today)
        selectedDate.value = today
    }

    private fun ensureSelectionInMonth() {
        val current = selectedDate.value
        if (YearMonth.from(current) != month.value) {
            selectedDate.value = month.value.atDay(1)
        }
    }

}

private fun LocalDate.startOfWeek(firstDayOfWeek: DayOfWeek): LocalDate {
    val offset = (dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    return minusDays(offset.toLong())
}
