package top.chiloven.daymark.core.data.repository

import top.chiloven.daymark.core.model.ThemeMode
import top.chiloven.daymark.core.model.UserSettings
import top.chiloven.daymark.core.model.WidgetStyle
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val settings: Flow<UserSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setFirstDayOfWeek(day: DayOfWeek)

    suspend fun setNotificationsEnabled(enabled: Boolean)

    suspend fun setDefaultWidgetStyle(style: WidgetStyle)

    suspend fun clear()

}
