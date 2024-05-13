package map

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream


@Serializable
data class RawCampusMap(val size: MapSize,
                        val points: List<RawMapPoint>,
                        @SerialName("top_left_abs_coords")
                        val topLeftAbsCoords: MapCoordinateAbs,
                        val paths: List<RawMapPath>) {
    fun saveLocal(base: Path, meta: MapMetadata) {
        val stream = meta.getMapPath(base).outputStream()
        saveStream(stream)
        stream.close()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun saveStream(stream: OutputStream) {
        Json.encodeToStream(this, stream)
    }
}

@Serializable
data class RawFloorConnection(val src: Int, val dst: Int) {
    fun reversed() = RawFloorConnection(dst, src)
}

@Serializable
data class RawMapPath(@SerialName("src_id")
                      val srcId: Int,
                      @SerialName("dst_id")
                      val dstId: Int,
                      @SerialName("floor_connections")
                      val floorConnections: List<RawFloorConnection>? = null,
                      @SerialName("middle_points")
                      val middlePoints: List<MapCoordinateRel>? = null,
                      val directions: List<String>? = null,
                      val bidirectional: Boolean = true,
                      val outdoors: Float = 0.0f) {
    var virtual = false
        private set

    fun reversed() =
        RawMapPath(
            dstId, srcId,
            floorConnections?.map { it.reversed() },
            middlePoints?.reversed(),
            directions?.reversed(),
            bidirectional
        ).apply { virtual = true }

    fun pathType() =
        if (bidirectional) {
            if (virtual) MapPathType.Virtual
            else MapPathType.Bidirectional
        }
        else MapPathType.Unidirectional
}

@Serializable
data class RawMapPoint(val coords: MapCoordinateRel,
                       val name: String,
                       val fullName: String? = null,
                       val desc: String? = null,
                       @SerialName("is_place")
                       val isPlace: Boolean = false)
