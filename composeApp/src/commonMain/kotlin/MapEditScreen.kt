import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import map.CampusMap
import map.MapCoordinateRel
import map.MapsProvider
import map.MutableCampusMap
import map.RawMapPath
import map.RawMapPoint
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.centroidX
import ovh.plrapps.mapcompose.api.centroidY
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onPathClick
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.removeAllCallouts
import ovh.plrapps.mapcompose.api.removeAllPaths
import ovh.plrapps.mapcompose.api.removeCallout
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.MapUI

class MapEditScreenModel(map: CampusMap, provider: MapsProvider, viewState: UIMapState?) : ScreenModel {
    val mutableMap = MutableCampusMap(map)
    val uiState = UIMapState(map.meta.makeMapState(provider) {
        viewState?.let {
            scale(it.map.scale)
            scroll(it.map.centroidX, it.map.centroidY)
        }
    })

    enum class Mode { Normal, AddPoint, EditPath }
    var mode by mutableStateOf(Mode.Normal)
    var showAddPlacePopup by mutableStateOf(false)
    var lastTapCoords = MapCoordinateRel(Double.NaN, Double.NaN)
    private var lastTapPath = mutableListOf<MapCoordinateRel>()

    fun switchMode(new: Mode) {
        mode = new
        if (new != Mode.EditPath) {
            uiState.map.removeAllPaths()
            clearNewPath()
            return
        }

        uiState.map.removeAllCallouts()
        // render all paths
        mutableMap.renderPaths(uiState)

        clearNewPath()
    }

    private fun clearNewPath() {
        lastTapPath.clear()
        uiState.map.removePath("new")
        uiState.selectedMarkers.clear()
    }

    init {
        mutableMap.renderMarkers(uiState)
        uiState.map.onMarkerClick { id, _, _ -> when (mode) {
            Mode.Normal, Mode.AddPoint -> {
                mutableMap.renderPointCallout(uiState, id)
            }
            Mode.EditPath -> {
                val idNum = id.toInt()
                if (uiState.selectedMarkers.contains(idNum)) {
                    uiState.selectedMarkers.remove(idNum)
                    clearNewPath()
                    return@onMarkerClick
                }

                uiState.selectedMarkers.add(idNum)
                if (uiState.selectedMarkers.size < 2)
                    return@onMarkerClick

                // TODO: floor connections, directions, asymmetry
                val path = RawMapPath(
                    uiState.selectedMarkers[0],
                    uiState.selectedMarkers[1],
                    middlePoints = lastTapPath.toList()
                )

                mutableMap.addPath(uiState, path)
                clearNewPath()
            }
        } }

        uiState.map.onTap { x, y -> when (mode) {
            Mode.Normal -> {}
            Mode.AddPoint -> {
                lastTapCoords = MapCoordinateRel(x, y)
                showAddPlacePopup = true
            }
            Mode.EditPath -> {
                if (uiState.selectedMarkers.isEmpty())
                    return@onTap

                lastTapPath.add(MapCoordinateRel(x, y))
                uiState.map.removePath("new")
                uiState.map.addPath("new", color = theme.edit_new_path_color) {
                    val (sx, sy) = mutableMap.points[uiState.selectedMarkers.first()]!!.coords
                    addPoint(sx, sy)
                    addPoints(lastTapPath.map { it.toPair() })
                }
            }
        } }
        
        uiState.map.onPathClick { id, x, y ->
            if (uiState.selectedMarkers.isNotEmpty())
                return@onPathClick

            //val path = pathById[id.toInt()]
            uiState.map.addCallout(id, x, y, autoDismiss = false) {
                Row {
                    Button(onClick = {
                        mutableMap.deletePath(uiState, id)
                        uiState.map.removeCallout(id)
                    }) { Icon(Icons.Filled.Delete, "delete") }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = {
                        uiState.map.removeCallout(id)
                    }) { Icon(Icons.Filled.Clear, "cancel") }
                }
            }
        }
    }
}

class MapEditScreen(val map: CampusMap, private val provider: MapsProvider, private val viewState: UIMapState? = null) : Screen {
    private fun title(mode: MapEditScreenModel.Mode): String =
        when (mode) {
            MapEditScreenModel.Mode.Normal -> "Edit Map"
            MapEditScreenModel.Mode.AddPoint -> "Add Map Point"
            MapEditScreenModel.Mode.EditPath -> "Add/Edit Map Path"
        }

    @Composable
    override fun Content() {
        val model = rememberScreenModel { MapEditScreenModel(map, provider, viewState) }
        ScreenLayout("${title(model.mode)}: ${model.mutableMap.meta.name}") {
            val navigator = LocalNavigator.currentOrThrow
            val subMenuItems = listOf(
                SubMenuItem(Icons.Filled.Place, "Add Point") {
                    model.switchMode(MapEditScreenModel.Mode.AddPoint)
                },
                SubMenuItem(Icons.Filled.ArrowForward, "Add/Edit Path") {
                    model.switchMode(MapEditScreenModel.Mode.EditPath)
                },
                SubMenuItem(Icons.Filled.Clear, "Stop Adding") {
                    model.switchMode(MapEditScreenModel.Mode.Normal)
                },
                SubMenuItem(Icons.Filled.Check, "Save") {
                    provider.saveMap(model.mutableMap.meta, model.mutableMap.toRaw())
                    navigator.pop()
                },
            )

            var isSubMenuExpanded by remember { mutableStateOf(false) }
            Scaffold(
                floatingActionButton = {
                    FloatingMenu(subMenuItems, isSubMenuExpanded) {
                        isSubMenuExpanded = !isSubMenuExpanded
                    }
                }
            ) {
                Column {
                    MapUI(Modifier.weight(1f), state = model.uiState.map)
                }
            }

            var name by remember { mutableStateOf("") }
            var isPlace by remember { mutableStateOf(true) }

            if (model.showAddPlacePopup) {
                MinimalDialog(onDismissRequest = {
                    name = ""
                    isPlace = true
                    model.showAddPlacePopup = false
                }, onConfirmation = {
                    val point = RawMapPoint(
                        model.lastTapCoords,
                        name,
                        isPlace = isPlace
                    )

                    model.mutableMap.addPoint(model.uiState, point)
                    model.showAddPlacePopup = false
                }) {
                    Text("Add Point", fontSize = 24.sp)
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        label = { Text("Name") },
                        value = name,
                        onValueChange = { name = it }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Place")
                        Switch(isPlace, onCheckedChange = { isPlace = it })
                    }
                }
            }
        }
    }
}