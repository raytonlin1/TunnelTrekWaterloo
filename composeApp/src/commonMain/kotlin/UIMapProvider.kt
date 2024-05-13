import map.MapMetadata
import map.MapsProvider
import ovh.plrapps.mapcompose.core.TileStreamProvider
import java.io.FileInputStream
import java.nio.file.Paths

fun MapsProvider.makeTileStreamProvider(meta: MapMetadata): TileStreamProvider {
    val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
        tileStream(meta, zoomLvl, row, col)
    }

    return tileStreamProvider
}