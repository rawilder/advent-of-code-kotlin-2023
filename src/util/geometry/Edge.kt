package util.geometry

data class Edge(
    val start: Point2D,
    val end: Point2D,
) {
    fun length(): Long = start.distanceToInAMatrix(end)
    fun reverse() = Edge(end, start)
}
