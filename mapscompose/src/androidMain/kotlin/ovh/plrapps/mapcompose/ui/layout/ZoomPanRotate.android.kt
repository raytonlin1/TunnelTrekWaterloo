package ovh.plrapps.mapcompose.ui.layout

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier

internal actual fun Modifier.registerScrollHandler(
    gestureListener: GestureListener,
    ctrl: MutableState<Boolean>
): Modifier = this