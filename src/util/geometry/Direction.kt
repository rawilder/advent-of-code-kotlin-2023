package util.geometry

enum class Direction(val symbol: Char) {
    NORTH('^'),
    SOUTH('v'),
    EAST('>'),
    WEST('<');

    fun movementInAMatrix(): Point2D {
        return movementInAMatrix(1)
    }

    fun movementInAMatrix(magnitude: Long): Point2D {
        return when (this) {
            NORTH -> Point2D(0, -magnitude)
            SOUTH -> Point2D(0, magnitude)
            EAST -> Point2D(magnitude, 0)
            WEST -> Point2D(-magnitude, 0)
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

    companion object {
        fun fromPoints(source: Point2D, destination: Point2D): Direction {
            return when {
                source.x == destination.x && source.y > destination.y -> NORTH
                source.x == destination.x && source.y < destination.y -> SOUTH
                source.x > destination.x && source.y == destination.y -> WEST
                source.x < destination.x && source.y == destination.y -> EAST
                else -> throw IllegalArgumentException("only supports exact directions on the same axis")
            }
        }

        fun fromString(string: String): Direction {
            return when (string) {
                "N", "U" -> NORTH
                "S", "D" -> SOUTH
                "E", "R" -> EAST
                "W", "L" -> WEST
                else -> throw IllegalArgumentException("Unknown direction: $string")
            }
        }
    }
}
