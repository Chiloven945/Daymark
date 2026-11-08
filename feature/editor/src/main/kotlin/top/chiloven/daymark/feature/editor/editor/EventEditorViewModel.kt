package top.chiloven.daymark.feature.editor.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import top.chiloven.daymark.core.data.reminder.ReminderScheduler
import top.chiloven.daymark.core.data.repository.EventRepository
import top.chiloven.daymark.core.data.widget.WidgetRefreshScheduler
import top.chiloven.daymark.core.model.CountDirection
import top.chiloven.daymark.core.model.DateCalculator
import top.chiloven.daymark.core.model.Event
import top.chiloven.daymark.core.model.EventDraft
import top.chiloven.daymark.core.model.EventOccurrence
import top.chiloven.daymark.core.model.RecurrenceFrequency
import top.chiloven.daymark.core.model.RecurrenceRule
import top.chiloven.daymark.core.model.TimeProvider
import top.chiloven.daymark.core.model.toDraft
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditorField {
    TITLE,
}

data class EventEditorUiState(
    val draft: EventDraft = EventDraft(),
    val preview: EventOccurrence? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errors: Set<EditorField> = emptySet(),
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val saveError: Boolean = false,
)

@HiltViewModel
class EventEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
    private val widgetRefreshScheduler: WidgetRefreshScheduler,
    private val dateCalculator: DateCalculator,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val eventId: Long? = savedStateHandle.get<Long>("eventId")
    private val _uiState = MutableStateFlow(
        EventEditorUiState(
            draft = EventDraft(date = timeProvider.today()),
            isLoading = eventId != null,
        ),
    )
    val uiState: StateFlow<EventEditorUiState> = _uiState.asStateFlow()

    private var createdAt: Long? = null

    init {
        if (eventId == null) {
            refreshPreview()
        } else {
            viewModelScope.launch {
                val event = eventRepository.observeById(eventId).first()
                if (event != null) {
                    createdAt = event.createdAt
                    _uiState.update {
                        it.copy(
                            draft = event.toDraft(),
                            isEditing = true,
                            isLoading = false,
                        )
                    }
                    refreshPreview()
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun updateTitle(value: String) = updateDraft {
        it.copy(title = value)
    }

    fun updateDate(value: LocalDate) = updateDraft {
        it.copy(date = value)
    }

    fun updateDirection(value: CountDirection) = updateDraft {
        it.copy(direction = value)
    }

    fun updateRecurrence(value: RecurrenceFrequency) = updateDraft {
        it.copy(
            recurrence = RecurrenceRule(
                frequency = value,
                interval = 1,
                endDate = null,
            ),
        )
    }

    fun updateCategory(value: Long?) = updateDraft {
        it.copy(categoryId = value)
    }

    fun updateNote(value: String) = updateDraft {
        it.copy(note = value)
    }

    fun updateReminderEnabled(value: Boolean) = updateDraft {
        it.copy(reminderEnabled = value)
    }

    fun updateReminderOffset(value: Int) = updateDraft {
        it.copy(reminderOffsetDays = value.coerceAtLeast(0))
    }

    fun updateReminderTime(value: LocalTime) = updateDraft {
        it.copy(reminderTime = value)
    }

    fun save() {
        val current = _uiState.value
        if (current.draft.title.isBlank()) {
            _uiState.update { it.copy(errors = setOf(EditorField.TITLE)) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errors = emptySet(), saveError = false) }
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val event = current.draft.toEvent(
                    createdAt = createdAt ?: now,
                    updatedAt = now,
                )
                val id = eventRepository.upsert(event)
                val savedEvent = event.copy(id = id)
                reminderScheduler.schedule(savedEvent)
                widgetRefreshScheduler.refresh(savedEvent.id)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure {
                _uiState.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }

    fun delete() {
        val id = eventId ?: return
        viewModelScope.launch {
            eventRepository.delete(id)
            reminderScheduler.cancel(id)
            widgetRefreshScheduler.refresh(id)
            _uiState.update { it.copy(deleted = true) }
        }
    }

    private fun updateDraft(transform: (EventDraft) -> EventDraft) {
        _uiState.update { state ->
            state.copy(
                draft = transform(state.draft),
                errors = state.errors - EditorField.TITLE,
            )
        }
        refreshPreview()
    }

    private fun refreshPreview() {
        val event: Event = _uiState.value.draft.toEvent()
        _uiState.update {
            it.copy(preview = dateCalculator.occurrence(event, timeProvider.today()))
        }
    }
}
