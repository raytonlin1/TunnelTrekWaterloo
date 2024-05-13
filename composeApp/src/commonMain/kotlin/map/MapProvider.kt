package map

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

interface MapsProvider : java.io.Serializable {
    fun listMaps(): List<MapMetadata>
    fun loadMap(meta: MapMetadata): CampusMap
    fun tileStream(meta: MapMetadata, zoom: Int, row: Int, col: Int): InputStream
    fun saveMap(meta: MapMetadata, map: RawCampusMap): Any
}

open class DirectoryMapsProvider(val base: Path) : MapsProvider {
    override fun listMaps(): List<MapMetadata>  {
        val mapsPath = base.resolve("maps")
        return mapsPath.listDirectoryEntries()
            .filter { file -> !file.name.startsWith(".") && file.isDirectory() }
            .map { mapDir ->
                mapDir.resolve("meta.json")
                    // map directory's name is the id
                    .let { MapMetadata.loadLocal(it) }
            }
    }

    override fun loadMap(meta: MapMetadata): CampusMap =
        CampusMap.loadLocal(base, meta)

    override fun tileStream(meta: MapMetadata, zoom: Int, row: Int, col: Int) =
        meta.getTilePath(base, zoom, row, col).inputStream()

    override fun saveMap(meta: MapMetadata, map: RawCampusMap) =
        map.saveLocal(base, meta)
}

expect object LocalMapsProvider : DirectoryMapsProvider {}

expect class DemoMapsProvider() : DirectoryMapsProvider {}

expect object DemoResourceMapsProvider : MapsProvider {}