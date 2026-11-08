package top.chiloven.daymark.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import top.chiloven.daymark.core.model.ThemeMode
import top.chiloven.daymark.core.model.UserSettings
import top.chiloven.daymark.core.model.WidgetStyle
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings",
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences ->
                UserSettings(
                    themeMode = preferences[Keys.ThemeMode]
                            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                        ?: ThemeMode.SYSTEM,
                    dynamicColor = preferences[Keys.DynamicColor] ?: true,
                    firstDayOfWeek = preferences[Keys.FirstDayOfWeek]
                            ?.let { value -> DayOfWeek.entries.firstOrNull { it.value == value } }
                        ?: DayOfWeek.MONDAY,
                    notificationsEnabled = preferences[Keys.NotificationsEnabled] ?: false,
                    defaultWidgetStyle = preferences[Keys.DefaultWidgetStyle]
                            ?.let { name -> WidgetStyle.entries.firstOrNull { it.name == name } }
                        ?: WidgetStyle.LUCENT,
                )
            }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.ThemeMode] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DynamicColor] = enabled }
    }

    override suspend fun setFirstDayOfWeek(day: DayOfWeek) {
        dataStore.edit { it[Keys.FirstDayOfWeek] = day.value }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NotificationsEnabled] = enabled }
    }

    override suspend fun setDefaultWidgetStyle(style: WidgetStyle) {
        dataStore.edit { it[Keys.DefaultWidgetStyle] = style.name }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {

        val ThemeMode = stringPreferencesKey("theme_mode")
        val DynamicColor = booleanPreferencesKey("dynamic_color")
        val FirstDayOfWeek = intPreferencesKey("first_day_of_week")
        val NotificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val DefaultWidgetStyle = stringPreferencesKey("default_widget_style")

    }

}
