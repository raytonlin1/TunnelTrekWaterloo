import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import map.CampusMap
import map.MapMetadata
import map.MapPath
import map.MapPoint
import map.MapsProvider
import map.MutableCampusMap
import map.RawMapPath
import map.RawMapPoint
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.removeAllPaths
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.InitialValues
import ovh.plrapps.mapcompose.ui.state.MapState
import widgets.Callout
import widgets.EditCallout
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.math.max


data class UIMapState(@Transient val map: MapState) : java.io.Serializable {
    val selectedMarkers = mutableStateListOf<Int>()
    val activeDirections = mutableStateListOf<String>()
    var activeRoutes by mutableStateOf<List<NavigationRoute>>(emptyList())
    var selectedRoute by mutableStateOf(0)
    var routeSelectionCollapsed by mutableStateOf(true)
    var directionsListCollapsed by mutableStateOf(true)

    @Suppress("UNCHECKED_CAST")
    private fun readObject(stream: ObjectInputStream) {
        val selectedMarkersArray = stream.readObject() as IntArray
        val activeDirectionsArray = stream.readObject() as Array<String>
        selectedMarkers.addAll(selectedMarkersArray.asList())
        activeDirections.addAll(activeDirectionsArray)
    }

    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeObject(selectedMarkers.toIntArray())
        stream.writeObject(activeDirections.toTypedArray())
    }

    suspend fun selectRoute(index: Int, collapse: Boolean = true) {
        routeSelectionCollapsed = collapse
        if (index >= activeRoutes.size)
            return

        selectedRoute = index
        activeRoutes[index].render(this)
        directionsListCollapsed = false
        activeDirections.forEachIndexed { i, it ->
            println("[DEBUG] directions[$i] = $it")
        }
    }
}

@Composable
fun Marker(id: Int, uiState: UIMapState, editMode: Boolean, isPlace: Boolean) {
    Icon(
        Icons.Filled.Place,
        null,
        Modifier.size(if (uiState.selectedMarkers.contains(id)) 50f.dp else 25f.dp),
        if (uiState.selectedMarkers.contains(id)) theme.marker_selected_color
        else if (editMode && isPlace) theme.edit_visible_marker_color
        else theme.marker_color
    )
}

fun CampusMap.renderMarkers(uiState: UIMapState) {
    //println("DBG: renderMarkers called on ${uiState}")
    uiState.map.removeAllMarkers()
    points.filter { it.isPlace }
        .forEach {
            uiState.map.addMarker(
                id = it.id.toString(),
                x = it.coordinate.x,
                y = it.coordinate.y
            ) {
                Marker(it.id, uiState, false, it.isPlace)
            }
        }
}

fun MapPath.render(uiState: UIMapState, id: Int) {
    if (dst.isPlace) {
        uiState.activeDirections.addAll(
            directions ?: listOf("Head to ${dst.name}")
        )
    }

    uiState.map.addPath("$id", color = theme.path_color) {
        addPoints(points.map { it.toPair() })
    }
}

fun MapPoint.renderCallout(uiState: UIMapState) {
    uiState.map.addCallout(
        id = id.toString(),
        x = coordinate.x,
        y = coordinate.y,
        autoDismiss = true,
    ) {
        // FIXME: make these abs coords
        Callout(x = coordinate.x,y = coordinate.y, title=name)
    }
}

suspend fun NavigationRoute.render(uiState: UIMapState) {
    uiState.map.removeAllPaths()
    uiState.activeDirections.clear()

    if (paths.isEmpty()) return

    // TODO: we should optimize starting floor choices?
    paths.foldIndexed(null as Int?) { i, floor, path ->
        var nextFloor = path.floorConnections?.get(floor)
        if (nextFloor == null) {
            val entry = path.floorConnections?.entries?.first()
            nextFloor = entry?.value
            entry?.key?.let { uiState.activeDirections.add("Go to ${path.src.name} floor $it") }
        }

        path.render(uiState, i)
        nextFloor
    }

    val src = paths.first().src
    val dst = paths.last().dst
    uiState.map.scrollTo(
        (src.coordinate.x + dst.coordinate.x) / 2,
        (src.coordinate.y + dst.coordinate.y) / 2
    )
    listOf(dst, src).forEach { it.renderCallout(uiState) }
}

fun MapMetadata.makeMapState(provider: MapsProvider, initialValuesBuilder: InitialValues.() -> Unit = {}): MapState {
    val minScale = 128f / max(height, width)
    val self = this
    return MapState(
        levels,
        fullWidth = width,
        fullHeight = height,
        tileSize = tileSize
    ) {
        minimumScaleMode(Forced(minScale))
        scale(initialScale ?: minScale)
        initialValuesBuilder()
    }.apply {
        addLayer(provider.makeTileStreamProvider(self))
        enableRotation()
    }
}

fun MutableCampusMap.renderPaths(uiState: UIMapState) {
    paths.forEach { (id, path) ->
        renderRawPath(uiState, id, points, path)
    }
}

fun renderRawPath(uiState: UIMapState, id: Int, points: Map<Int, RawMapPoint>, path: RawMapPath) {
    val src = points[path.srcId]!!.coords
    val dst = points[path.dstId]!!.coords
    uiState.map.addPath("$id", color = theme.path_color, clickable = true) {
        addPoint(src.x, src.y)
        path.middlePoints?.let { addPoints(it.map { it.x to it.y }) }
        addPoint(dst.x, dst.y)
    }
}

fun MutableCampusMap.renderMarkers(uiState: UIMapState) {
    uiState.map.removeAllMarkers()
    points.forEach { (id, it) -> renderRawPoint(uiState, id, it) }
}

private fun renderRawPoint(uiState: UIMapState, id: Int, point: RawMapPoint) {
    uiState.map.addMarker(
        id = "$id",
        x = point.coords.x,
        y = point.coords.y
    ) {
        Marker(id, uiState, true, point.isPlace)
    }
}


fun MutableCampusMap.deleteMarker(uiState: UIMapState, id: String) {
    val idNum = id.toInt()
    uiState.map.removeMarker(id)
    forEachPathWithPoint(idNum) { uiState.map.removePath("$it") }
    deletePoint(idNum)
}

fun MutableCampusMap.deletePath(uiState: UIMapState, id: String) {
    val idNum = id.toInt()
    uiState.map.removePath(id)
    deletePath(idNum)
}

fun MutableCampusMap.addPoint(uiState: UIMapState, point: RawMapPoint) {
    val id = addPoint(point)
    renderRawPoint(uiState, id, point)
}

fun MutableCampusMap.addPath(uiState: UIMapState, path: RawMapPath) {
    val id = addPath(path)
    renderRawPath(uiState, id, points, path)
}

fun MutableCampusMap.renderPointCallout(uiState: UIMapState, id: String) {
    val idNum = id.toInt()
    val point = points[idNum]!!
    uiState.map.addCallout(
        id = id.toString(),
        x = point.coords.x,
        y = point.coords.y,
        autoDismiss = false,
    ) {
        EditCallout(idNum, this, uiState)
    }
}