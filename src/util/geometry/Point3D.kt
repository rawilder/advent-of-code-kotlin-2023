package util.geometry

import kotlin.math.abs

data class Point3D(
    val x: Long,
    val y: Long,
    val z: Long
): Comparable<Point3D> {
    constructor(x: Int, y: Int, z: Int): this(x.toLong(), y.toLong(), z.toLong())

    operator fun plus(other: Point3D): Point3D {
        return Point3D(x + other.x, y + other.y, z + other.z)
    }

    fun distanceTo(other: Point3D): Long {
        return abs(x - other.x) + abs(y - other.y) + abs(z - other.z)
    }

    override fun compareTo(other: Point3D): Int {
        return when {
            z < other.z -> -1
            z > other.z -> 1
            y < other.y -> -1
            y > other.y -> 1
            x < other.x -> -1
            x > other.x -> 1
            else -> 0
        }
    }
}
