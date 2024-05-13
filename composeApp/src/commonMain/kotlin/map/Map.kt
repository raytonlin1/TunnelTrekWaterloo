package map

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.jgrapht.graph.DefaultWeightedEdge
import java.io.InputStream
import java.nio.file.Path
import java.time.Duration
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.io.path.inputStream
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

data class CampusMap(val meta: MapMetadata,
                     val size: MapSize,
                     val topLeftAbsCoords: MapCoordinateAbs,
                     val points: List<MapPoint>) : java.io.Serializable {

    fun rawPaths(): List<RawMapPath> {
        return points.flatMap { point ->
            point.paths
                .filter { it.type != MapPathType.Virtual }
                .map { it.toRaw() }
        }
    }

    companion object {

        fun loadLocal(base: Path, meta: MapMetadata): CampusMap {
            val stream = meta.getMapPath(base).inputStream()
            val result = loadStream(stream, meta)
            stream.close()
            return result
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun loadStream(stream: InputStream, meta: MapMetadata): CampusMap {
            val raw = Json.decodeFromStream<RawCampusMap>(stream)
            return CampusMap(raw, meta)
        }
    }

    private constructor(raw: RawCampusMap, meta: MapMetadata) : this(
        meta,
        raw.size,
        raw.topLeftAbsCoords,
        raw.points.mapIndexed { i, it ->
            MapPoint(it, i)
        }
    ) {
        // list of outgoing edges by src vertex id
        val pathsBySrc = Stream.concat(
            raw.paths.parallelStream(),
            raw.paths.parallelStream().filter { true }.map { it.reversed() }
        ).collect(Collectors.groupingBy { it.srcId })

        // set paths for each equal to list of outgoing paths from point
        points.parallelStream().forEach {
            it.paths = pathsBySrc
                .getOrDefault(it.id, listOf())
                .map { MapPath(it, points, size) }
        }
    }
}

@Serializable
data class MapSize(@SerialName("width_meters")
                   val widthMeters: Float,
                   @SerialName("height_meters")
                   val heightMeters: Float) : java.io.Serializable

@Serializable
data class MapTileInfo(@SerialName("tile_size")
                       val tileSize: Int,
                       val levels: Int,
                       val cols: Int,
                       val rows: Int,
                       val type: String,
                       @SerialName("initial_scale")
                       val initialScale: Float? = null) : java.io.Serializable {
    val width = cols * tileSize
    val height = rows * tileSize
}

enum class MapPathType {
    Bidirectional, // represents the real path for an undirected-path
    Virtual, // represents the virtual mirror-image path for an undirected path
    Unidirectional // represents a unidirectional path
}

data class MapPath(val src: MapPoint,
                   val dst: MapPoint,
                   val points: List<MapCoordinateRel>,
                   private val mapSize: MapSize,
                   val floorConnections: Map<Int, Int>? = null,
                   val directions: List<String>? = null,
                   val type: MapPathType = MapPathType.Bidirectional,
                   private val outdoors: Float = 0.0f) : DefaultWeightedEdge(), java.io.Serializable {

    val distMeters: Float = points.windowed(2).fold(0.0) { acc, it ->
        acc + it[0].distTo(it[1], mapSize)
    }.toFloat()

    // TODO: don't just use the 15min/km estimate
    val time: Duration = Duration.ofSeconds((15f * 60f * distMeters / 1e3f).roundToLong())
    val distOutdoors = distMeters * outdoors

    override fun getWeight(): Double {
        return distMeters.toDouble()
    }

    fun toRaw() =
        RawMapPath(
            src.id,
            dst.id,
            floorConnections?.map { (src, dst) -> RawFloorConnection(src, dst) },
            points.subList(1, points.size - 1).let { it.ifEmpty { null } },
            directions,
            listOf(MapPathType.Bidirectional, MapPathType.Virtual).contains(type),
            outdoors
        )

    internal constructor(raw: RawMapPath,
                         mapPoints: List<MapPoint>,
                         mapSize: MapSize) : this(
        mapPoints[raw.srcId],
        mapPoints[raw.dstId],
        listOf(mapPoints[raw.srcId].coordinate) +
        (raw.middlePoints ?: listOf()) +
        listOf(mapPoints[raw.dstId].coordinate),
        mapSize,
        raw.floorConnections?.associate { it.src to it.dst },
        raw.directions,
        raw.pathType(),
        raw.outdoors
    )
}

@Serializable
data class MapCoordinateRel(val x: Double, val y: Double) : java.io.Serializable {
    fun toPair() = Pair(x, y)
    fun distTo(other: MapCoordinateRel, size: MapSize) =
        sqrt(
            ((other.y - y) * size.heightMeters).pow(2) +
                    ((other.x - x) * size.widthMeters).pow(2)
        )
}
@Serializable
data class MapCoordinateAbs(val long: Double, val lat: Double) : java.io.Serializable

@Serializable
data class MapMetadata(val name: String,
                       val desc: String? = null,
                       val id: String,
                       @SerialName("tile_info")
                       val tileInfo: MapTileInfo) : java.io.Serializable {
    fun getMapPath(base: Path) =
        base.resolve("maps")
            .resolve(id)
            .resolve("map.json")

    fun getTilePath(base: Path, zoom: Int, row: Int, col: Int) =
        base.resolve("maps")
            .resolve(id)
            .resolve("$zoom")
            .resolve("$row")
            .resolve("$col.${tileInfo.type}")

    companion object {
        fun loadLocal(path: Path) = loadStream(path.inputStream())

        @OptIn(ExperimentalSerializationApi::class)
        fun loadStream(s: InputStream) =
            Json.decodeFromStream<MapMetadata>(s)
    }

    val width = tileInfo.width
    val height = tileInfo.height
    val tileSize = tileInfo.tileSize
    val levels = tileInfo.levels
    val initialScale = tileInfo.initialScale
}


data class MapPoint(val id: Int,
                    val coordinate: MapCoordinateRel,
                    val name: String,
                    val fullName: String? = null,
                    val desc: String? = null,
                    private var initialPaths: List<MapPath>? = null,
                    val isPlace: Boolean = false) : java.io.Serializable {
    fun toRaw() =
        RawMapPoint(
            coordinate,
            name,
            fullName,
            desc,
            isPlace
        )

    var paths = initialPaths ?: listOf()
        internal set

    internal constructor(raw: RawMapPoint, id: Int): this(
        id,
        raw.coords,
        raw.name,
        raw.fullName,
        raw.desc,
        null,
        raw.isPlace
    )
}
