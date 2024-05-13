package ovh.plrapps.mapcompose.core

import java.io.InputStream

internal actual fun loadImageBitmap(stream: InputStream) =
    androidx.compose.ui.res.loadImageBitmap(stream)