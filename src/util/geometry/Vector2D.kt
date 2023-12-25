package util.geometry

data class Vector2D(
    val source: Point2D,
    val direction: Direction,
    val magnitude: Long = 1
) {
    val destination = source.move(direction, magnitude)
}
