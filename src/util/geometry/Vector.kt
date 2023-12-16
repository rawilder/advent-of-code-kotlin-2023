package util.geometry

data class Vector(
    val source: Point,
    val direction: Direction
) {
    val destination = source.move(direction)
}
