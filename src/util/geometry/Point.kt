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
}

enum class Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    fun movementInAMatrix(): Point {
        return when (this) {
            NORTH -> Point(0, -1)
            SOUTH -> Point(0, 1)
            EAST -> Point(1, 0)
            WEST -> Point(-1, 0)
        }
    }

    fun turnLeft(): Direction {
        return when (this) {
            NORTH -> WEST
            SOUTH -> EAST
            EAST -> NORTH
            WEST -> SOUTH
        }
    }

    fun turnRight(): Direction {
        return when (this) {
            NORTH -> EAST
            SOUTH -> WEST
            EAST -> SOUTH
            WEST -> NORTH
        }
    }

    fun turnAround(): Direction {
        return when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
        }
    }
}
