package top.chiloven.daymark.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import top.chiloven.daymark.core.data.export.EventExportService
import top.chiloven.daymark.core.data.reminder.ReminderScheduler
import top.chiloven.daymark.core.data.repository.EventRepository
import top.chiloven.daymark.core.data.repository.SettingsRepository
import top.chiloven.daymark.core.model.ThemeMode
import top.chiloven.daymark.core.model.UserSettings
import top.chiloven.daymark.core.model.WidgetStyle
import java.time.DayOfWeek
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SettingsEvent {
    data object Exported : SettingsEvent
    data object Imported : SettingsEvent
    data object Cleared : SettingsEvent
    data class Error(val reason: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val eventRepository: EventRepository,
    private val exportService: EventExportService,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(),
    )

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setFirstDayOfWeek(day: DayOfWeek) {
        viewModelScope.launch { settingsRepository.setFirstDayOfWeek(day) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            reminderScheduler.rescheduleAll()
        }
    }

    fun setDefaultWidgetStyle(style: WidgetStyle) {
        viewModelScope.launch { settingsRepository.setDefaultWidgetStyle(style) }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val events = eventRepository.observeAll().first()
                    val payload = exportService.export(events, Instant.now().toString())
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open output stream")
                }
                _events.emit(SettingsEvent.Exported)
            }.onFailure { error ->
                _events.emit(SettingsEvent.Error(error.message ?: "Export failed"))
            }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val content = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                        ?: error("Unable to open input stream")
                    val events = exportService.import(content)
                    eventRepository.upsertAll(events)
                    reminderScheduler.rescheduleAll()
                }
                _events.emit(SettingsEvent.Imported)
            }.onFailure { error ->
                _events.emit(SettingsEvent.Error(error.message ?: "Import failed"))
            }
        }
    }

    fun clearData() {
        viewModelScope.launch {
            runCatching {
                eventRepository.observeAll().first().forEach { event ->
                    reminderScheduler.cancel(event.id)
                }
                eventRepository.deleteAll()
                settingsRepository.clear()
                _events.emit(SettingsEvent.Cleared)
            }.onFailure { error ->
                _events.emit(SettingsEvent.Error(error.message ?: "Clear failed"))
            }
        }
    }

}
