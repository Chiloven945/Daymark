package top.chiloven.daymark.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Shared page background: a restrained tonal gradient with two very soft
 * accent glows. It keeps the dark theme from feeling flat without using
 * expensive backdrop blur.
 */
@Composable
fun DaymarkBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val background = Brush.verticalGradient(
        colors = listOf(
            colorScheme.surface,
            colorScheme.surfaceContainerLowest,
        ),
    )

    Box(
        modifier = modifier
                .fillMaxSize()
                .background(background),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawAccentGlow(
                color = colorScheme.primary.copy(alpha = 0.10f),
                center = Offset(size.width * 0.82f, size.height * 0.02f),
                radius = size.width * 0.72f,
            )
            drawAccentGlow(
                color = colorScheme.tertiary.copy(alpha = 0.07f),
                center = Offset(size.width * 0.08f, size.height * 0.58f),
                radius = size.width * 0.55f,
            )
        }
        content()
    }
}

private fun DrawScope.drawAccentGlow(
    color: Color,
    center: Offset,
    radius: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
