package top.chiloven.daymark.core.model

import java.time.DayOfWeek

enum class ThemeMode {

    SYSTEM,
    LIGHT,
    DARK,

}

enum class WidgetStyle {

    LUCENT,
    FROSTED,
    SOLID,
    MATERIAL,

}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val notificationsEnabled: Boolean = false,
    val defaultWidgetStyle: WidgetStyle = WidgetStyle.LUCENT,
)
