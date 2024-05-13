package map

import firebase.getBasePath
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.toPath

actual object LocalMapsProvider : DirectoryMapsProvider(Path(getBasePath())) {
    init {
        listOf(
            base,
            base.resolve("maps")
        ).map { it.toFile().mkdir() }
    }
}
actual class DemoMapsProvider : DirectoryMapsProvider(resolveBase()) {
    companion object {
        private fun resolveBase(): Path {
            val home = System.getProperty("user.home")
            return Paths.get(home, "demo")
        }
    }
}

actual object DemoResourceMapsProvider: MapsProvider {
    private const val BASE = "/demo"

    // load the jar as a virtual file system
    private val zipfs =
        FileSystems.newFileSystem(
            // what in the world is this?
            javaClass
                .protectionDomain
                .codeSource
                .location
                .toURI()
                .toPath()
        )
    private val provider = DirectoryMapsProvider(zipfs.getPath(BASE))

    override fun listMaps(): List<MapMetadata> = provider.listMaps()
    override fun loadMap(meta: MapMetadata) = provider.loadMap(meta)

    override fun tileStream(meta: MapMetadata, zoom: Int, row: Int, col: Int) =
        provider.tileStream(meta, zoom, row, col)

    override fun saveMap(meta: MapMetadata, map: RawCampusMap): Any {
        throw NotImplementedError("Not implemented")
    }
}