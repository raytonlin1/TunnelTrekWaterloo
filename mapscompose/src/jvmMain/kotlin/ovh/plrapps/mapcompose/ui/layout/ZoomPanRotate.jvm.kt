package ovh.plrapps.mapcompose.ui.layout

import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.registerScrollHandler(
    gestureListener: GestureListener,
    ctrl: MutableState<Boolean>
): Modifier =
    onPointerEvent(PointerEventType.Scroll) {
        // poor man's scroll zoom & rotate impl
        val centroid = it.changes.first().position
        val deltaY = it.changes.first().scrollDelta.y
        if (deltaY == 0f) {
            return@onPointerEvent
        }

        if (ctrl.value) {
            gestureListener.onRotationDelta(-deltaY * 0.2f)
            return@onPointerEvent
        }

        gestureListener.onScaleRatio(1f - deltaY * 0.2f, centroid)
    }
