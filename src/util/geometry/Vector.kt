package util.geometry

data class Vector(
    val source: Point2D,
    val direction: Direction,
    val magnitude: Long = 1
) {
    val destination = source.move(direction, magnitude)
}
