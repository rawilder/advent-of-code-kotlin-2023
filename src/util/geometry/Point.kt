package util.geometry

import kotlin.math.abs

data class Point(
    val x: Long,
    val y: Long,
): Comparable<Point> {
    fun distanceToInAMatrix(other: Point): Long {
        return abs(x - other.x) + abs(y - other.y)
    }

    operator fun plus(other: Point): Point {
        return Point(x + other.x, y + other.y)
    }

    override fun compareTo(other: Point): Int {
        return when {
            y < other.y -> -1
            y > other.y -> 1
            x < other.x -> -1
            x > other.x -> 1
            else -> 0
        }
    }

    fun move(newDirection: Direction): Point {
        return this + newDirection.movementInAMatrix()
    }

    fun vector(direction: Direction): Vector {
        return Vector(this, direction)
    }
}
