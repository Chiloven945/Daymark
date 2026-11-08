package top.chiloven.daymark.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import top.chiloven.daymark.core.data.reminder.ReminderScheduler
import top.chiloven.daymark.core.data.repository.EventRepository
import top.chiloven.daymark.core.data.widget.WidgetRefreshScheduler
import top.chiloven.daymark.core.model.CountDirection
import top.chiloven.daymark.core.model.DateCalculator
import top.chiloven.daymark.core.model.Event
import top.chiloven.daymark.core.model.EventOccurrence
import top.chiloven.daymark.core.model.EventPhase
import top.chiloven.daymark.core.model.RecurrenceFrequency
import top.chiloven.daymark.core.model.TimeProvider
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EventFilter {

    ALL,
    COUNTDOWN,
    COUNT_UP,
    ANNIVERSARIES,

}

enum class EventGroupType {

    PINNED,
    THIS_MONTH,
    LATER,
    PAST,

}

data class EventGroup(
    val type: EventGroupType,
    val items: List<EventOccurrence>,
)

data class EventsUiState(
    val query: String = "",
    val filter: EventFilter = EventFilter.ALL,
    val groups: List<EventGroup> = emptyList(),
    val hasAnyEvents: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
    private val widgetRefreshScheduler: WidgetRefreshScheduler,
    private val dateCalculator: DateCalculator,
    timeProvider: TimeProvider,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(EventFilter.ALL)
    private val today = MutableStateFlow<LocalDate>(timeProvider.today())

    val uiState: StateFlow<EventsUiState> = combine(
        eventRepository.observeAll(),
        query,
        filter,
        today,
    ) { events, currentQuery, currentFilter, currentToday ->
        val normalizedQuery = currentQuery.trim()
        val filtered = events
                .asSequence()
                .filter { event ->
                    normalizedQuery.isEmpty() ||
                            event.title.contains(normalizedQuery, ignoreCase = true) ||
                            event.note.contains(normalizedQuery, ignoreCase = true)
                }
                .filter { event -> currentFilter.matches(event) }
                .map { event -> dateCalculator.occurrence(event, currentToday) }
                .toList()

        val grouped = filtered
                .sortedWith(compareBy<EventOccurrence> { it.phase == EventPhase.PASSED }.thenBy { it.days })
                .groupBy { occurrence -> occurrence.groupType(currentToday) }
                .toSortedMap(compareBy { it.ordinal })
                .map { (type, occurrences) -> EventGroup(type, occurrences) }

        EventsUiState(
            query = currentQuery,
            filter = currentFilter,
            groups = grouped,
            hasAnyEvents = events.isNotEmpty(),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUiState(),
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: EventFilter) {
        filter.value = value
    }

    fun setPinned(event: Event, pinned: Boolean) {
        viewModelScope.launch {
            eventRepository.setPinned(event.id, pinned)
        }
    }

    fun delete(event: Event) {
        viewModelScope.launch {
            eventRepository.delete(event.id)
            reminderScheduler.cancel(event.id)
            widgetRefreshScheduler.refresh(event.id)
        }
    }

    fun restore(event: Event) {
        viewModelScope.launch {
            val id = eventRepository.upsert(event)
            val restored = event.copy(id = id)
            reminderScheduler.schedule(restored)
            widgetRefreshScheduler.refresh(id)
        }
    }

}

private fun EventFilter.matches(event: Event): Boolean = when (this) {
    EventFilter.ALL -> true
    EventFilter.COUNTDOWN -> event.direction == CountDirection.COUNT_DOWN
    EventFilter.COUNT_UP -> event.direction == CountDirection.COUNT_UP
    EventFilter.ANNIVERSARIES -> event.recurrence.frequency == RecurrenceFrequency.YEARLY
}

private fun EventOccurrence.groupType(today: LocalDate): EventGroupType = when {
    event.isPinned && phase != EventPhase.PASSED ->
        EventGroupType.PINNED

    phase == EventPhase.PASSED ->
        EventGroupType.PAST

    targetDate.year == today.year && targetDate.month == today.month ->
        EventGroupType.THIS_MONTH

    else -> EventGroupType.LATER
}
