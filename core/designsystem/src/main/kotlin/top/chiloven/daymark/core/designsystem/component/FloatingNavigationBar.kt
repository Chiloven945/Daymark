package top.chiloven.daymark.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.chiloven.daymark.core.designsystem.theme.DaymarkSizes

data class DaymarkNavItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun FloatingNavigationBar(
    items: List<DaymarkNavItem>,
    selectedRoute: Any?,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedColors = navItemColors(colorScheme)

    Surface(
        modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = DaymarkSizes.bottomBarHorizontal,
                    vertical = DaymarkSizes.bottomBarVertical,
                ),
        shape = RoundedCornerShape(32.dp),
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        contentColor = colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.30f)),
    ) {
        ShortNavigationBar(
            containerColor = Color.Transparent,
            contentColor = colorScheme.onSurface,
            windowInsets = WindowInsets(0),
            arrangement = ShortNavigationBarArrangement.EqualWeight,
        ) {
            items.forEach { item ->
                val selected = selectedRoute?.let { it::class == item.route::class } == true
                ShortNavigationBarItem(
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
                    colors = selectedColors,
                )
            }
        }
    }
}

@Composable
private fun navItemColors(colorScheme: ColorScheme) = ShortNavigationBarItemDefaults.colors(
    selectedIconColor = colorScheme.onSurface,
    selectedTextColorTopIconPosition = colorScheme.onSurface,
    selectedIndicatorColor = colorScheme.surfaceContainerHighest,
    unselectedIconColor = colorScheme.onSurfaceVariant,
    unselectedTextColor = colorScheme.onSurfaceVariant,
)
