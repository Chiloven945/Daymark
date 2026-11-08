package top.chiloven.daymark.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FloatingNavigationRail(
    items: List<DaymarkNavItem>,
    selectedRoute: Any?,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Surface(
            modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
            shape = RoundedCornerShape(40.dp),
            color = colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            contentColor = colorScheme.onSurface,
            tonalElevation = 3.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.30f)),
        ) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = Color.Transparent,
                contentColor = colorScheme.onSurface,
                windowInsets = WindowInsets(0),
            ) {
                items.forEach { item ->
                    val selected = selectedRoute?.let { it::class == item.route::class } == true
                    NavigationRailItem(
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) {
                                    item.selectedIcon
                                } else {
                                    item.unselectedIcon
                                },
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    }
}
