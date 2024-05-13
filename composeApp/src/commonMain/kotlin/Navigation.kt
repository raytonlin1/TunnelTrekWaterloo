import map.CampusMap
import map.MapPath
import java.time.Duration

data class NavigationRoute(val paths: List<MapPath>) : java.io.Serializable {
    val totalTime: Duration = paths.fold(Duration.ZERO) { acc, it ->
        acc + it.time
    }
    val totalDistanceMeters: Float = paths.fold(0f) { acc, it ->
        acc + it.distMeters
    }
}