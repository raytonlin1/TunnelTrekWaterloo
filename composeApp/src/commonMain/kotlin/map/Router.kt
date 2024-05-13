package map

import NavigationRoute
import UIMapState
import makeMapState
import org.jgrapht.alg.shortestpath.YenKShortestPath
import ovh.plrapps.mapcompose.api.removeAllPaths
import org.jgrapht.graph.DefaultDirectedWeightedGraph

class Router(map: CampusMap) {
    private val routesOffered = 4
    private val graph = DefaultDirectedWeightedGraph<MapPoint, MapPath>(MapPath::class.java).apply {
        map.points.forEach { addVertex(it) }
        map.points.forEach { point ->
            point.paths.forEach { addEdge(it.src, it.dst, it) }
        }
    }
    private val router = YenKShortestPath(graph)
    fun findRoutes(src: MapPoint, dst: MapPoint): List<NavigationRoute>{
        val routes = router.getPaths(src, dst, routesOffered)
        return routes.map { route -> NavigationRoute(route.edgeList) }
    }
}