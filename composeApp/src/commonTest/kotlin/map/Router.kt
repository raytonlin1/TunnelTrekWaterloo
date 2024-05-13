import map.DirectoryMapsProvider
import map.MapPoint
import map.Router
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouterTest {
    val mapProvider = DirectoryMapsProvider(resourcesDir)
    val map = mapProvider.loadMap(mapMeta)
    val router = Router(map)
    val mapPoints = rawMap.points.mapIndexed { i, it ->
        MapPoint(it, i)
    }
    @Test
    fun findAndResetRoutesTest() {
        assertTrue { mapPoints.size >= 2 }
        val mySrc = mapPoints[0]
        val myDst = mapPoints[mapPoints.size-1]
        val activeRoutes = router.findRoutes(mySrc, myDst)
        assertTrue { activeRoutes.size > 0 }
        for (activeRoute in activeRoutes) {
            for (path in activeRoute.paths) {
                assertEquals(path.src.id, mySrc.id)
                assertEquals(path.dst.id, myDst.id)
            }
        }
    }
}