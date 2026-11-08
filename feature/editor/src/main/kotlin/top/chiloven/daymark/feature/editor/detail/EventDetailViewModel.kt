package top.chiloven.daymark.feature.editor.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import top.chiloven.daymark.core.data.reminder.ReminderScheduler
import top.chiloven.daymark.core.data.repository.EventRepository
import top.chiloven.daymark.core.data.widget.WidgetRefreshScheduler
import top.chiloven.daymark.core.model.DateCalculator
import top.chiloven.daymark.core.model.Event
import top.chiloven.daymark.core.model.EventOccurrence
import top.chiloven.daymark.core.model.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val event: Event? = null,
    val occurrence: EventOccurrence? = null,
    val isLoading: Boolean = true,
    val deleted: Boolean = false,
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
    private val widgetRefreshScheduler: WidgetRefreshScheduler,
    private val dateCalculator: DateCalculator,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val eventId: Long = checkNotNull(savedStateHandle.get<Long>("eventId"))
    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            eventRepository.observeById(eventId).collect { event ->
                _uiState.update {
                    it.copy(
                        event = event,
                        occurrence = event?.let { value ->
                            dateCalculator.occurrence(value, timeProvider.today())
                        },
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun setPinned(pinned: Boolean) {
        viewModelScope.launch {
            eventRepository.setPinned(eventId, pinned)
        }
    }

    fun delete() {
        viewModelScope.launch {
            eventRepository.delete(eventId)
            reminderScheduler.cancel(eventId)
            widgetRefreshScheduler.refresh(eventId)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
