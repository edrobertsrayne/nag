package dev.nag.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import dev.nag.Constants
import kotlinx.coroutines.launch

@Composable
fun SwipeCard(
    cardId: Long,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember(cardId) { Animatable(0f) }
    val velocityTracker = remember(cardId) { VelocityTracker() }
    val committed = remember(cardId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX.value
                rotationZ = if (size.width > 0f) {
                    offsetX.value / size.width * Constants.SWIPE_MAX_TILT_DEGREES
                } else {
                    0f
                }
            }
            .pointerInput(cardId) {
                val flingVelocityPx = Constants.SWIPE_FLING_VELOCITY_DP_PER_S.dp.toPx()
                detectDragGestures(
                    onDragStart = { velocityTracker.resetTracking() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!committed.value) {
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                        }
                    },
                    onDragEnd = {
                        if (!committed.value) {
                            val draggedRight =
                                offsetX.value > size.width * Constants.SWIPE_COMMIT_FRACTION
                            val flungRight =
                                velocityTracker.calculateVelocity().x > flingVelocityPx
                            if (draggedRight || flungRight) {
                                committed.value = true
                                scope.launch {
                                    offsetX.animateTo(size.width * 1.5f, spring())
                                    onSwipeRight()
                                }
                            } else {
                                scope.launch { springBack(offsetX) }
                            }
                        }
                    },
                    onDragCancel = { scope.launch { springBack(offsetX) } },
                )
            },
    ) {
        content()
    }
}

private suspend fun springBack(offsetX: Animatable<Float, *>) {
    offsetX.animateTo(
        0f,
        spring(
            dampingRatio = Constants.SWIPE_SPRING_DAMPING_RATIO,
            stiffness = Constants.SWIPE_SPRING_STIFFNESS,
        ),
    )
}
