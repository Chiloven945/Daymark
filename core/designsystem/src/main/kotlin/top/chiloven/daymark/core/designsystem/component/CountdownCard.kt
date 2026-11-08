package top.chiloven.daymark.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.designsystem.theme.DaymarkSizes
import top.chiloven.daymark.core.designsystem.util.displayName
import top.chiloven.daymark.core.designsystem.util.formatMediumDate
import top.chiloven.daymark.core.model.DefaultCategories
import top.chiloven.daymark.core.model.EventOccurrence

@Composable
fun CountdownCard(
    occurrence: EventOccurrence,
    unitLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val value = if (occurrence.isCountUp) -occurrence.days else occurrence.days
    val category = DefaultCategories.byId(occurrence.event.categoryId)
    val clickModifier = if (onClick != null) {
        Modifier
                .clip(RoundedCornerShape(DaymarkSizes.heroRadius))
                .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
                .fillMaxWidth()
                .then(clickModifier),
        shape = RoundedCornerShape(DaymarkSizes.heroRadius),
        color = colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            colorScheme.outlineVariant.copy(alpha = 0.25f),
        ),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    colorScheme.primary.copy(alpha = 0.16f),
                                    colorScheme.surfaceContainerLow,
                                    colorScheme.tertiary.copy(alpha = 0.08f),
                                ),
                            ),
                        ),
            )
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.ds_next_event),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(
                        targetState = value,
                        transitionSpec = {
                            (
                                    slideInVertically(
                                        animationSpec = tween(280),
                                        initialOffsetY = { it / 2 },
                                    ) + fadeIn(tween(180))
                                    ).togetherWith(
                                        slideOutVertically(
                                            animationSpec = tween(220),
                                            targetOffsetY = { -it / 2 },
                                        ) + fadeOut(tween(140)),
                                    )
                        },
                        label = "countdown",
                    ) { animatedValue ->
                        Text(
                            text = animatedValue.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = unitLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Text(
                    text = occurrence.event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (category != null) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.surfaceContainerHighest,
                            contentColor = colorScheme.onSurfaceVariant,
                        ) {
                            Text(
                                text = category.displayName(),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Text(
                        text = occurrence.targetDate.formatMediumDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        colorScheme.primary,
                                        colorScheme.tertiary.copy(alpha = 0.65f),
                                        colorScheme.primary.copy(alpha = 0.12f),
                                    ),
                                ),
                            ),
                )
            }
        }
    }
}
