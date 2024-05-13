import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import map.CampusMap
import map.RawCampusMap
import java.io.FileInputStream
import java.time.Duration
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
val rawMap = Json.decodeFromStream<RawCampusMap>(FileInputStream(mapPath.pathString))
val campusMap = CampusMap.loadLocal(resourcesDir, mapMeta)
class NavigationRouteTest {
    val navRoute: NavigationRoute = NavigationRoute(campusMap.points[0].paths)
    @Test
    fun totalTimeTest() {
        var expected = Duration.ZERO
        for (i in 0..navRoute.paths.size-1) {
            expected += navRoute.paths[i].time
        }
        val actual = navRoute.totalTime
        assertEquals(expected, actual)
    }

    @Test
    fun totalDistanceMetersTest() {
        var expected = 0F
        for (i in 0..navRoute.paths.size-1) {
            expected += navRoute.paths[i].distMeters
        }
        val actual = navRoute.totalDistanceMeters
        assertEquals(expected, actual)
    }
}

