package ovh.plrapps.mapcompose.ui.layout

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import ovh.plrapps.mapcompose.ui.gestures.detectTapGestures
import ovh.plrapps.mapcompose.ui.gestures.detectTransformGestures

@Composable
internal fun ZoomPanRotate(
    modifier: Modifier = Modifier,
    gestureListener: GestureListener,
    layoutSizeChangeListener: LayoutSizeChangeListener,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val flingSpec = rememberSplineBasedDecay<Offset>()
    val ctrl = remember { mutableStateOf(false) }

    Layout(
        content = content,
        modifier
            .registerScrollHandler(gestureListener, ctrl)
            .pointerInput(gestureListener.isListeningForGestures()) {
                if (!gestureListener.isListeningForGestures()) return@pointerInput
                detectTransformGestures(
                    onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                        gestureListener.onRotationDelta(gestureRotate)
                        gestureListener.onScaleRatio(gestureZoom, centroid)
                        gestureListener.onScrollDelta(pan)
                    },
                    onTouchDown = gestureListener::onTouchDown,
                    onTwoFingersTap = gestureListener::onTwoFingersTap,
                    onFling = { velocity -> gestureListener.onFling(flingSpec, velocity) },
                    onFlingZoom = { centroid, velocity ->
                        gestureListener.onFlingZoom(velocity, centroid)
                    }
                )
            }
            .pointerInput(gestureListener.isListeningForGestures()) {
                if (!gestureListener.isListeningForGestures()) return@pointerInput
                detectTapGestures(
                    onTap = { offset -> gestureListener.onTap(offset) },
                    onDoubleTap = { offset -> gestureListener.onDoubleTap(offset) },
                    onDoubleTapZoom = { centroid, zoom -> gestureListener.onScaleRatio(zoom, centroid)},
                    onDoubleTapZoomFling = { centroid, velocity ->
                        gestureListener.onFlingZoom(velocity, centroid)
                    },
                    onPress = { gestureListener.onPress() },
                    onLongPress = { offset -> gestureListener.onLongPress(offset) },
                    shouldConsumeTap = { offset -> gestureListener.shouldConsumeTapGesture(offset) },
                    shouldConsumeLongPress = { offset -> gestureListener.shouldConsumeLongPress(offset) }
                )
            }
            .onSizeChanged {
                layoutSizeChangeListener.onSizeChanged(scope, it)
            }
            .fillMaxSize(),
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            // Measure each children
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Place children in the parent layout
            placeables.forEach { placeable ->
                placeable.place(x = 0, y = 0)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)

internal expect fun Modifier.registerScrollHandler(
    gestureListener: GestureListener,
    ctrl: MutableState<Boolean>): Modifier

internal interface GestureListener {
    fun onScaleRatio(scaleRatio: Float, centroid: Offset)
    fun onRotationDelta(rotationDelta: Float)
    fun onScrollDelta(scrollDelta: Offset)
    fun onFling(flingSpec: DecayAnimationSpec<Offset>, velocity: Velocity)
    fun onFlingZoom(velocity: Float, centroid: Offset)
    fun onTouchDown()
    fun onPress()
    fun onTap(focalPt: Offset)
    fun onDoubleTap(focalPt: Offset)
    fun onTwoFingersTap(focalPt: Offset)
    fun onLongPress(focalPt: Offset)
    fun isListeningForGestures(): Boolean
    fun shouldConsumeTapGesture(focalPt: Offset): Boolean
    fun shouldConsumeLongPress(focalPt: Offset): Boolean
}

internal interface LayoutSizeChangeListener {
    fun onSizeChanged(composableScope: CoroutineScope, size: IntSize)
}
