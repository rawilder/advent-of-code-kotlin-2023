package util.geometry

data class Edge(
    val start: Point,
    val end: Point,
) {
    fun length(): Long = start.distanceToInAMatrix(end)
    fun reverse() = Edge(end, start)
}
