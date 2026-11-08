package top.chiloven.daymark.ui

import androidx.activity.BackEventCompat
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

private const val SlideDuration = 320
private const val FadeDuration = 220

private val SlideSpec = tween<IntOffset>(
    durationMillis = SlideDuration,
    easing = FastOutSlowInEasing,
)

internal val RootEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = SlideDirection.Start,
        animationSpec = SlideSpec,
    ) + fadeIn(tween(FadeDuration))
}

internal val RootExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = SlideDirection.Start,
        animationSpec = SlideSpec,
    ) + fadeOut(tween(FadeDuration))
}

internal val RootPopEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = SlideDirection.End,
        animationSpec = SlideSpec,
    ) + fadeIn(tween(FadeDuration))
}

internal val RootPopExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = SlideDirection.End,
        animationSpec = SlideSpec,
    ) + fadeOut(tween(FadeDuration))
}

/**
 * Navigation Compose's default predictive-pop exit is scaleOut(0.7f). Override
 * it so the page follows the back gesture horizontally instead of shrinking.
 */
internal val PredictivePopEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = { swipeEdge ->
    val direction = if (swipeEdge == BackEventCompat.EDGE_LEFT) {
        SlideDirection.End
    } else {
        SlideDirection.Start
    }
    slideIntoContainer(direction, animationSpec = SlideSpec) + fadeIn(tween(FadeDuration))
}

internal val PredictivePopExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = { swipeEdge ->
    val direction = if (swipeEdge == BackEventCompat.EDGE_LEFT) {
        SlideDirection.End
    } else {
        SlideDirection.Start
    }
    slideOutOfContainer(direction, animationSpec = SlideSpec) + fadeOut(tween(FadeDuration))
}

internal val TabEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(FadeDuration)) + slideInHorizontally(tween(SlideDuration)) { it / 16 }
}

internal val TabExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(140)) + slideOutHorizontally(tween(SlideDuration)) { -it / 16 }
}

internal val TabPopEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(FadeDuration)) + slideInHorizontally(tween(SlideDuration)) { -it / 16 }
}

internal val TabPopExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(140)) + slideOutHorizontally(tween(SlideDuration)) { it / 16 }
}
