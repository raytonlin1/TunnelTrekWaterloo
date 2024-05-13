import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import map.CampusMap
import map.MapCoordinateRel
import map.MapMetadata
import map.MapPath
import map.MapPoint
import map.MapSize
import map.RawCampusMap
import org.junit.Before
import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

val resourcesDir =
    Paths.get("src", "commonTest", "resources")
val metaPath =
    resourcesDir
        .resolve("maps")
        .resolve("mont_blanc")
        .resolve("meta.json")
val mapPath =
    resourcesDir
        .resolve("maps")
        .resolve("mont_blanc")
        .resolve("map.json")
val tilePath =
    resourcesDir
        .resolve("maps")
        .resolve("mont_blanc")
        .resolve("2")
        .resolve("1")
        .resolve("7.jpg")
@OptIn(ExperimentalSerializationApi::class)
val mapMeta = Json.decodeFromStream<MapMetadata>(FileInputStream(metaPath.pathString))

class MapMetadataTest {
    @Test
    fun getPath_Returns_ResourcePath_Given_BasePath() {
        assertEquals(mapPath, mapMeta.getMapPath(resourcesDir))
    }

    @Test
    fun getTileLocalPath_ReturnsValid_Tile_Path_GivenZoom_RowAndColumn() {
        assertEquals(tilePath, mapMeta.getTilePath(resourcesDir, 2, 1, 7))
        assertTrue(File(resourcesDir.pathString).exists())
    }

    @Test
    fun getPathTest() {
        val mapPath = mapMeta.getMapPath(resourcesDir)
        assertEquals(mapPath.pathString, mapPath.pathString)

    }

    @Test
    fun testProperties() {
        assertEquals("mont_blanc", mapMeta.id)
        assertEquals("one of the demo maps included with MapCompose", mapMeta.desc)
        assertEquals("Demo Map (v2)", mapMeta.name)
        assertEquals(4, mapMeta.levels)
        assertEquals(256, mapMeta.tileSize)
        assertEquals(mapMeta.tileInfo.rows * mapMeta.tileSize, mapMeta.height)
        assertEquals(mapMeta.tileInfo.cols * mapMeta.tileSize, mapMeta.width)
    }
}

class CampusMapTest {
    @OptIn(ExperimentalSerializationApi::class)
    val desiredRawMap = Json.decodeFromStream<RawCampusMap>(FileInputStream(mapPath.pathString))
    @Test
    fun loadLocal_Returns_Corresponding_CampusMap() {
        val campusMap = CampusMap.loadLocal(resourcesDir, mapMeta)
        assertEquals(campusMap.meta, mapMeta)
        assertEquals(campusMap.size, desiredRawMap.size)
        assertEquals(campusMap.topLeftAbsCoords, desiredRawMap.topLeftAbsCoords)
        assertEquals(campusMap.points.size, desiredRawMap.points.size)
    }

//    @Test
//    fun loadStream_Returns_Corresponding_CampusMap() {
//        CampusMap()
//    }
}

class MapPointTest {
    val desiredRawMap = Json.decodeFromStream<RawCampusMap>(FileInputStream(mapPath.pathString))
    @Test
    fun rawCampusMap_Has_Expected_MapPoints() {
        val mapPoints = desiredRawMap.points.mapIndexed { i, it ->
            MapPoint(it, i)
        }
        assertEquals(10, mapPoints.size)
        val point1 = mapPoints.find {
            it.name == "point 1" && it.isPlace &&
                    it.coordinate.x == 0.1 && it.coordinate.y == 0.1
        }
        val point7 = mapPoints.find {
            it.name == "point 7" && !it.isPlace &&
                    it.coordinate.x == 0.8 && it.coordinate.y == 0.2
        }
        val point10 = mapPoints.find {
            it.name == "point 10" && it.isPlace &&
                    it.coordinate.x == 0.2 && it.coordinate.y == 0.1 && it.desc == "no clue"
        }
        assertNotEquals(null, point1)
        assertNotEquals(null, point7)
        assertNotEquals(null, point10)
    }
}

class MapCoordinateRelTest {
    val rawMap = Json.decodeFromStream<RawCampusMap>(FileInputStream(mapPath.pathString))

    @Test
    fun distToTest() {
        val width: Float = 1.5F
        val height: Float = 2.0F
        val mapSize = MapSize(widthMeters = width, heightMeters = height)
        val mapCoordRel = MapCoordinateRel(x=0.4, y=0.8)
        val mapCoordRel2 = MapCoordinateRel(x=0.1, y=0.5)
        val actualDist = mapCoordRel.distTo(mapCoordRel2, mapSize)
        val expectedDist = sqrt(
            ((mapCoordRel.x - mapCoordRel2.x) * width).pow(2) +
                    ((mapCoordRel.y - mapCoordRel2.y) * height).pow(2)
        )
        assertEquals(expectedDist, actualDist)
    }
}

class MapPathTest {
    val rawMap = Json.decodeFromStream<RawCampusMap>(FileInputStream(mapPath.pathString))
    val mapPoints = rawMap.points.mapIndexed { i, it ->
        MapPoint(it, i)
    }
    lateinit var testMapPath: MapPath
    lateinit var mapSize: MapSize

    @Before
    fun setup(){
        assertTrue { mapPoints.size >= 10 }
        val width: Float = 1.5F
        val height: Float = 2.0F
        mapSize = MapSize(widthMeters = width, heightMeters = height)
        val mapCoordinateRel = mapPoints.map {
            MapCoordinateRel(it.coordinate.x, it.coordinate.y)
        }
        testMapPath = MapPath(mapPoints[0], mapPoints[9], mapCoordinateRel, mapSize = mapSize)
    }
    @Test
    fun initPropertiesTest() {
        assertTrue { testMapPath.points.size == 10 }
        assertTrue { testMapPath.directions == null }
        assertTrue { testMapPath.src.name == "point 1" }
        assertTrue { testMapPath.dst.name == "point 10" }
    }

    @Test
    fun distMetersTest() {
        var expectedDist = 0.0
        for (i in 1..testMapPath.points.size-1) {
            val edgeDist = testMapPath.points[i].distTo(testMapPath.points[i-1], mapSize)
            expectedDist += edgeDist
        }
        val actualDist = testMapPath.distMeters
        assertEquals(expectedDist.toFloat(), actualDist)
    }
}
