package top.chiloven.daymark.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import top.chiloven.daymark.R
import top.chiloven.daymark.core.designsystem.component.DaymarkNavItem
import top.chiloven.daymark.core.designsystem.component.FloatingNavigationBar
import top.chiloven.daymark.core.designsystem.component.FloatingNavigationRail
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
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {

    Home(HomeRoute, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    Events(EventsRoute, R.string.tab_events, Icons.Filled.Event, Icons.Outlined.Event),
    Calendar(
        CalendarRoute,
        R.string.tab_calendar,
        Icons.Filled.CalendarMonth,
        Icons.Outlined.CalendarMonth,
    ),
    Settings(
        SettingsRoute,
        R.string.tab_settings,
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
    ),
}

@Composable
fun DaymarkApp() {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = MainRoute,
        enterTransition = RootEnterTransition,
        exitTransition = RootExitTransition,
        popEnterTransition = RootPopEnterTransition,
        popExitTransition = RootPopExitTransition,
        predictivePopEnterTransition = PredictivePopEnterTransition,
        predictivePopExitTransition = PredictivePopExitTransition,
    ) {
        composable<MainRoute> {
            MainShell(
                onOpenEvent = { eventId ->
                    rootNavController.navigate(EventDetailRoute(eventId))
                },
                onEditEvent = { eventId ->
                    rootNavController.navigate(EventEditorRoute(eventId))
                },
                onNewEvent = {
                    rootNavController.navigate(EventEditorRoute(null))
                },
            )
        }
        composable<EventDetailRoute> {
            EventDetailScreen(
                onBack = { rootNavController.popBackStack() },
                onEdit = { eventId ->
                    rootNavController.navigate(EventEditorRoute(eventId))
                },
            )
        }
        composable<EventEditorRoute> {
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
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedRoute = TopLevelDestination.entries
            .firstOrNull { destination ->
                currentDestination
                        ?.hierarchy
                        ?.any { it.hasRoute(destination.route::class) } == true
            }
            ?.route

    val navItems = TopLevelDestination.entries.map { destination ->
        DaymarkNavItem(
            route = destination.route,
            label = stringResource(destination.labelRes),
            selectedIcon = destination.selectedIcon,
            unselectedIcon = destination.unselectedIcon,
        )
    }
    val navigateToTopLevel: (Any) -> Unit = { route ->
        tabNavController.navigate(route) {
            popUpTo(tabNavController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        if (useRail) {
            Row(Modifier.fillMaxSize()) {
                FloatingNavigationRail(
                    items = navItems,
                    selectedRoute = selectedRoute,
                    onNavigate = navigateToTopLevel,
                )
                TabNavHost(
                    navController = tabNavController,
                    modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .navigationBarsPadding(),
                    onOpenEvent = onOpenEvent,
                    onEditEvent = onEditEvent,
                    onNewEvent = onNewEvent,
                )
            }
        } else {
            androidx.compose.material3.Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    FloatingNavigationBar(
                        items = navItems,
                        selectedRoute = selectedRoute,
                        onNavigate = navigateToTopLevel,
                    )
                },
            ) { innerPadding ->
                TabNavHost(
                    navController = tabNavController,
                    modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding()),
                    onOpenEvent = onOpenEvent,
                    onEditEvent = onEditEvent,
                    onNewEvent = onNewEvent,
                )
            }
        }
    }
}

@Composable
private fun TabNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onOpenEvent: (Long) -> Unit,
    onEditEvent: (Long) -> Unit,
    onNewEvent: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = TabEnterTransition,
        exitTransition = TabExitTransition,
        popEnterTransition = TabPopEnterTransition,
        popExitTransition = TabPopExitTransition,
        predictivePopEnterTransition = PredictivePopEnterTransition,
        predictivePopExitTransition = PredictivePopExitTransition,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenEvent = onOpenEvent,
                onNewEvent = onNewEvent,
                onSeeAll = {
                    navController.navigate(EventsRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
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
