package map

data class MutableCampusMap(
    var meta: MapMetadata,
    var size: MapSize,
    var topLeftAbsCoords: MapCoordinateAbs,
    val points: HashMap<Int, RawMapPoint>,
    val paths: HashMap<Int, RawMapPath>) {
    private var nextPointId: Int = 0
    private var nextPathId: Int = 0

    private val pathsBySrc: HashMap<Int, HashSet<Int>> = hashMapOf()
    private val pathsByDst: HashMap<Int, HashSet<Int>> = hashMapOf()

    fun forEachPathWithPoint(id: Int, f: (Int) -> Unit) {
        pathsBySrc[id]?.forEach { f(it) }
        pathsByDst[id]?.forEach { f(it) }
    }

    fun deletePoint(id: Int) {
        points.remove(id)
        forEachPathWithPoint(id) { paths.remove(it) }
        pathsByDst.remove(id)
        pathsBySrc.remove(id)
        if (id == nextPointId - 1)
            nextPointId = id
    }

    fun deletePath(id: Int) {
        val path = paths[id]!!
        paths.remove(id)
        pathsBySrc[path.srcId]!!.remove(id)
        pathsByDst[path.dstId]!!.remove(id)
        if (id == nextPathId - 1)
            nextPathId = id
    }

    fun addPath(path: RawMapPath): Int {
        paths[nextPathId] = path
        pathsBySrc.getOrPut(path.srcId) { hashSetOf() }.add(nextPathId)
        pathsByDst.getOrPut(path.dstId) { hashSetOf() }.add(nextPathId)
        return nextPathId++
    }

    fun addPoint(point: RawMapPoint): Int {
        points[nextPointId] = point
        return nextPointId++
    }

    constructor(map: CampusMap) : this(
        map.meta,
        map.size,
        map.topLeftAbsCoords,
        map.points
            .map { it.id to it.toRaw() }
            .toMap()
            .let { HashMap(it) },
        map.rawPaths()
            .mapIndexed { i, it -> i to it }
            .toMap().let { HashMap(it) }
    ) {
        nextPointId = points.size
        nextPathId = paths.size
        paths.forEach { (i, path) ->
            pathsBySrc.getOrPut(path.srcId) { hashSetOf() }.add(i)
            pathsByDst.getOrPut(path.dstId) { hashSetOf() }.add(i)
        }
    }

    fun toRaw(): RawCampusMap {
        val correctionMap = hashMapOf<Int, Int>()
        val newPoints = mutableListOf<RawMapPoint>()
        points.forEach { (id, point) ->
            correctionMap[id] = newPoints.size
            newPoints.add(point)
        }

        // correct point IDs
        val newPaths = paths.map { (_, path) ->
            path.copy(
                srcId = correctionMap[path.srcId]!!,
                dstId = correctionMap[path.dstId]!!
            )
        }

        return RawCampusMap(
            size,
            newPoints,
            topLeftAbsCoords,
            newPaths
        )
    }
}