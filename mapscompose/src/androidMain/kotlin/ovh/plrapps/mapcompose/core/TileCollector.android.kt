package ovh.plrapps.mapcompose.core

import java.io.InputStream
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun loadImageBitmap(stream: InputStream) =
    BitmapFactory.decodeStream(stream).asImageBitmap()