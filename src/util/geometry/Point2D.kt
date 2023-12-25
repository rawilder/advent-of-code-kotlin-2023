package util.geometry

import kotlin.math.abs

data class Point2D(
    val x: Long,
    val y: Long,
): Comparable<Point2D> {
    constructor(x: Int, y: Int): this(x.toLong(), y.toLong())
    fun distanceToInAMatrix(other: Point2D): Long {
        return abs(x - other.x) + abs(y - other.y)
    }

    fun directionTo(other: Point2D): Direction {
        return Direction.fromPoints(this, other)
    }

    fun neighbors(): Set<Point2D> {
        return setOf(
            Point2D(x + 1, y),
            Point2D(x - 1, y),
            Point2D(x, y + 1),
            Point2D(x, y - 1),
        )
    }

    operator fun plus(other: Point2D): Point2D {
        return Point2D(x + other.x, y + other.y)
    }

    override fun compareTo(other: Point2D): Int {
        return when {
            y < other.y -> -1
            y > other.y -> 1
            x < other.x -> -1
            x > other.x -> 1
            else -> 0
        }
    }

    fun move(newDirection: Direction): Point2D {
        return this + newDirection.movementInAMatrix()
    }

    fun move(newDirection: Direction, magnitude: Long): Point2D {
        return this + newDirection.movementInAMatrix(magnitude)
    }

    fun vector(direction: Direction): Vector2D {
        return Vector2D(this, direction, 1)
    }

    companion object {
        /**
         * Calculates the area of a polygon defined by a list of points.
         *
         * This assumes the list is well-ordered, i.e. the order of points goes clockwise or counter-clockwise around the polygon.
         *
         * If the last point is not the same as the first point, it will be added to the end of the list.
         */
        fun Iterable<Point2D>.shoelaceArea(): Long {
            (if (this.first() != this.last()) this + this.first() else this)
                .zipWithNext()
                .fold(0.0) { acc, point ->
                    acc + (point.first.x * point.second.y - point.second.x * point.first.y)
                }.let {
                    return abs(it.toLong() / 2)
                }
        }

        /**
         * Calculates the perimeter of a polygon defined by a list of points.
         *
         * This assumes the list is well-ordered, i.e. the order of points goes clockwise or counter-clockwise around the polygon.
         *
         * If the last point is not the same as the first point, it will be added to the end of the list.
         */
        fun Iterable<Point2D>.perimeter(): Long {
            (if (this.first() != this.last()) this + this.first() else this)
                .zipWithNext()
                .fold(0L) { acc, point ->
                    acc + point.first.distanceToInAMatrix(point.second)
                }.let {
                    return it
                }
        }

        fun Collection<Point2D>.dijkstras(start: Point2D): Map<Point2D, Int> {
            tailrec fun dijkstrasDistancesRecursive(
                distances: Map<Point2D, Int>,
                unvisited: Set<Point2D>
            ): Map<Point2D, Int> {
                val current = unvisited.minByOrNull { distances[it] ?: Int.MAX_VALUE }
                if (current == null) {
                    return distances
                } else {
                    val currentDistance = distances[current] ?: Int.MAX_VALUE
                    val currentNeighbors = current.neighbors().filter(unvisited::contains)
                    val newDistances = distances + currentNeighbors.mapNotNull { position ->
                        val neighborDistance = distances[position] ?: Int.MAX_VALUE
                        val newDistance = currentDistance + 1
                        if (newDistance < neighborDistance) {
                            position to newDistance
                        } else null
                    }.toMap()
                    return dijkstrasDistancesRecursive(
                        newDistances,
                        unvisited - current
                    )
                }
            }

            return dijkstrasDistancesRecursive(mapOf(start to 0), this.toSet())
        }
    }
}
