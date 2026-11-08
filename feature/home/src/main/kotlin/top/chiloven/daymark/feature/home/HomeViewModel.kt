package top.chiloven.daymark.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import top.chiloven.daymark.core.data.repository.EventRepository
import top.chiloven.daymark.core.model.CountDirection
import top.chiloven.daymark.core.model.DateCalculator
import top.chiloven.daymark.core.model.EventOccurrence
import top.chiloven.daymark.core.model.EventPhase
import top.chiloven.daymark.core.model.TimeProvider
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val nextEvent: EventOccurrence? = null,
    val todayEvents: List<EventOccurrence> = emptyList(),
    val upcoming: List<EventOccurrence> = emptyList(),
    val hasAnyEvents: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    eventRepository: EventRepository,
    private val dateCalculator: DateCalculator,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val todayFlow: Flow<LocalDate> = flow {
        while (true) {
            emit(timeProvider.today())
            delay(60_000L.milliseconds)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<HomeUiState> = combine(
        eventRepository.observeAll(),
        todayFlow,
    ) { events, today ->
        val occurrences = events.map { dateCalculator.occurrence(it, today) }
        val upcoming = occurrences
                .filter { it.days >= 0 }
                .sortedBy { it.days }
        val fallbackCountUp = occurrences
                .filter { it.event.direction != CountDirection.COUNT_DOWN }
                .maxByOrNull { it.targetDate }
        val next = upcoming.firstOrNull() ?: fallbackCountUp
        val upcomingAfterNext = upcoming
                .filter { occurrence -> occurrence.event.id != next?.event?.id }
                .take(5)

        HomeUiState(
            today = today,
            nextEvent = next,
            todayEvents = occurrences.filter { it.phase == EventPhase.TODAY },
            upcoming = upcomingAfterNext,
            hasAnyEvents = events.isNotEmpty(),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}
