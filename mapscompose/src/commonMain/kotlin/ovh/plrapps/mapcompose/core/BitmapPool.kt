package ovh.plrapps.mapcompose.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.util.concurrent.ConcurrentHashMap

/**
 * A pool of bitmaps, internally split by allocation byte count.
 * This class is thread-safe.
 */
internal class BitmapPool {
    private val pool: ConcurrentHashMap<Int, Channel<ImageBitmap>> = ConcurrentHashMap<Int, Channel<ImageBitmap>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun get(allocationByteCount: Int): ImageBitmap? {
        if (pool[allocationByteCount]?.isEmpty == true) {
            return null
        }
        return pool[allocationByteCount]?.tryReceive()?.getOrNull()
    }

    fun put(b: ImageBitmap) {
        val allocationByteCount = b.width * b.height * bpp(b.config)
        /* Since we can't use pool.computeIfAbsent() on api below 24, we're using manual
         * synchronization */
        if (!pool.containsKey(allocationByteCount)) {
            synchronized(pool) {
                if (!pool.containsKey(allocationByteCount)) {
                    pool[allocationByteCount] = Channel(UNLIMITED)
                }
            }
        }

        pool[allocationByteCount]?.trySend(b)
    }

    private fun bpp(config: ImageBitmapConfig): Int =
        when (config) {
            ImageBitmapConfig.Rgb565 -> 2
            ImageBitmapConfig.Argb8888 -> 4
            ImageBitmapConfig.F16 -> 8
            else -> -1
        }
}