package top.chiloven.daymark.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import top.chiloven.daymark.R
import top.chiloven.daymark.feature.calendar.CalendarScreen
import top.chiloven.daymark.feature.editor.detail.EventDetailScreen
import top.chiloven.daymark.feature.editor.editor.EventEditorScreen
import top.chiloven.daymark.feature.events.EventsScreen
import top.chiloven.daymark.feature.home.HomeScreen
import top.chiloven.daymark.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute

@Serializable
data object HomeRoute

@Serializable
data object EventsRoute

@Serializable
data object CalendarRoute

@Serializable
data object SettingsRoute

@Serializable
data class EventDetailRoute(val eventId: Long)

@Serializable
data class EventEditorRoute(val eventId: Long? = null)

private enum class TopLevelDestination(
    val route: Any,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
) {

    Home(HomeRoute, Icons.Filled.Home, R.string.tab_home),
    Events(EventsRoute, Icons.AutoMirrored.Filled.List, R.string.tab_events),
    Calendar(CalendarRoute, Icons.Filled.CalendarMonth, R.string.tab_calendar),
    Settings(SettingsRoute, Icons.Filled.Settings, R.string.tab_settings),

}

@Composable
fun DaymarkApp() {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = MainRoute,
    ) {
        composable<MainRoute> {
            MainShell(
                onOpenEvent = {
                    rootNavController.navigate(EventDetailRoute(it))
                },
                onEditEvent = {
                    rootNavController.navigate(EventEditorRoute(it))
                },
                onNewEvent = {
                    rootNavController.navigate(EventEditorRoute(null))
                },
            )
        }
        composable<EventDetailRoute> {
            EventDetailScreen(
                onBack = { rootNavController.popBackStack() },
                onEdit = {
                    rootNavController.navigate(EventEditorRoute(it))
                },
            )
        }
        composable<EventEditorRoute> { entry ->
            entry.toRoute<EventEditorRoute>()
            EventEditorScreen(
                onClose = { rootNavController.popBackStack() },
                onSaved = { rootNavController.popBackStack() },
            )
        }
    }
}

@Composable
private fun MainShell(
    onOpenEvent: (Long) -> Unit,
    onEditEvent: (Long) -> Unit,
    onNewEvent: () -> Unit,
) {
    val tabNavController = rememberNavController()
    val currentBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = currentDestination
                            ?.hierarchy
                            ?.any { it.hasRoute(destination.route::class) } == true,
                    onClick = {
                        tabNavController.navigate(destination.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) {
        NavHost(
            navController = tabNavController,
            startDestination = HomeRoute,
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenEvent = onOpenEvent,
                    onNewEvent = onNewEvent,
                    onSeeAll = {
                        tabNavController.navigate(EventsRoute) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable<EventsRoute> {
                EventsScreen(
                    onOpenEvent = onOpenEvent,
                    onEditEvent = onEditEvent,
                    onNewEvent = onNewEvent,
                )
            }
            composable<CalendarRoute> {
                CalendarScreen(onOpenEvent = onOpenEvent)
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
    }
}
