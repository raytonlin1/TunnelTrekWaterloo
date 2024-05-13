package map

import android.content.res.AssetManager
import android.os.Environment
import firebase.FirebaseResourceManager
import java.io.InputStream
import java.nio.file.Paths

actual object LocalMapsProvider : DirectoryMapsProvider(Paths.get(FirebaseResourceManager.filesDir?.path)) {
    init {
        listOf(
            base,
            base.resolve("maps")
        ).map { it.toFile().mkdir() }
    }
}

actual class DemoMapsProvider : DirectoryMapsProvider(Paths.get(Environment.getExternalStorageDirectory().getPath(), "demo")) {}

actual object DemoResourceMapsProvider : MapsProvider {
    var assets: AssetManager? = null
    private const val BASE = "demo"
    private const val MAPS_BASE = "$BASE/maps"
    override fun listMaps(): List<MapMetadata> =
        assets!!.list(MAPS_BASE)!!.map {
            val stream = assets!!.open("$MAPS_BASE/$it/meta.json")
            MapMetadata.loadStream(stream)
        }

    override fun loadMap(meta: MapMetadata): CampusMap =
        meta.getMapPath(Paths.get(BASE))
            .toString()
            .let {
                val stream = assets!!.open(it)
                val result = CampusMap.loadStream(stream, meta)
                stream.close()
                result
            }

    override fun tileStream(meta: MapMetadata, zoom: Int, row: Int, col: Int): InputStream =
        meta.getTilePath(Paths.get(BASE), zoom, row, col)
            .toString()
            .let { assets!!.open(it) }

    override fun saveMap(meta: MapMetadata, map: RawCampusMap): Any {
        throw NotImplementedError("Not implemented")
    }
}