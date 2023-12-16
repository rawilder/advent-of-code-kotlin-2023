package util.geometry

enum class Direction(val symbol: Char) {
    NORTH('^'),
    SOUTH('v'),
    EAST('>'),
    WEST('<');

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
