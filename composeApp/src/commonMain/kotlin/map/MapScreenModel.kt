package map

import NavigationRoute
import UIMapState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import makeMapState
import makeTileStreamProvider
import org.jgrapht.alg.shortestpath.YenKShortestPath
import org.jgrapht.graph.DefaultDirectedWeightedGraph
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.removeAllCallouts
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.removeAllPaths
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import renderCallout
import renderMarkers
import kotlin.math.max

class MapScreenModel(private val provider: MapsProvider, private val mapMeta: MapMetadata) : ScreenModel {
    var sourceString by mutableStateOf("")
    var destinationString by mutableStateOf("")
    var srcErr by mutableStateOf(false)
    var dstErr by  mutableStateOf(false)

    val uiState = UIMapState(mapMeta.makeMapState(provider))

    // TODO: this transient label fixes crashes, but may cause state issues on resume
    //       or maybe not. I have no idea, honestly
    lateinit var map: CampusMap
    private lateinit var router: Router
    lateinit var mapPoints: List<String>

    private fun updateSrcDst() {
        sourceString = uiState.selectedMarkers.getOrNull(0)?.let {
            map.points[it].name
        } ?: ""
        destinationString = uiState.selectedMarkers.getOrNull(1)?.let {
            map.points[it].name
        } ?: ""
    }

    fun loadMap() {
        resetAll()
        map = provider.loadMap(mapMeta)
        router = Router(map)
        mapPoints = map.points.filter { it.isPlace }.map { it.name }
        map.renderMarkers(uiState)
        uiState.map.onMarkerClick { id, _, _ ->
            val idNum = id.toInt()
            if (uiState.selectedMarkers.contains(idNum)) {
                uiState.selectedMarkers.remove(idNum)
                updateSrcDst()
                resetRoutes()
                return@onMarkerClick
            }

            val point = map.points[idNum]
            point.renderCallout(uiState)

            if (uiState.selectedMarkers.size == 2)
                return@onMarkerClick

            uiState.selectedMarkers.add(idNum)
            updateSrcDst()
        }
    }

    //highlight map selection in UI

    //call the render in the handler itself

    // defer rendering until someone selects path

    //store selected path in UI state

    // pass selected path to MapScreenModel -> how?

    // startNavigation Does not render paths

    // Create a list (only one element can be selected) -> pass index here

    fun getSourceVertex() = map.points.firstOrNull { it.name == sourceString }
    fun getDestinationVertex() = map.points.firstOrNull { it.name == destinationString }

    fun findRoutes(src: MapPoint, dst: MapPoint){
        uiState.activeRoutes = router.findRoutes(src, dst)
    }

    fun resetRoutes() {
        uiState.activeRoutes = listOf()
        uiState.map.removeAllPaths()
        uiState.routeSelectionCollapsed = true
        uiState.directionsListCollapsed = true
    }

    fun resetAll() {
        resetRoutes()
        uiState.map.removeAllMarkers()
        uiState.map.removeAllCallouts()
    }
}