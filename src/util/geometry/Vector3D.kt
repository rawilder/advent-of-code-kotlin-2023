package util.geometry

import java.math.BigDecimal

data class Vector3D(
    val source: Point3D,
    val velocity: Point3D,
) {
    val destination = source + velocity

    /**
     * Returns true if the vector is heading towards the given point. Does not guarantee that the point is on the vector.
     */
    fun isHeadingTowards(point2D: Pair<BigDecimal, BigDecimal>): Boolean {
        val (x, y) = point2D
        val xCheck = if (velocity.x > 0) {
            x >= source.x.toBigDecimal()
        } else {
            x <= source.x.toBigDecimal()
        }
        val yCheck = if (velocity.y > 0) {
            y >= source.y.toBigDecimal()
        } else {
            y <= source.y.toBigDecimal()
        }
        return xCheck && yCheck
    }

    fun isHeadingTowards(point3D: Triple<BigDecimal, BigDecimal, BigDecimal>): Boolean {
        val (x, y, z) = point3D
        val zCheck = if (velocity.z > 0) {
            z >= source.z.toBigDecimal()
        } else {
            z <= source.z.toBigDecimal()
        }
        return isHeadingTowards(x to y) && zCheck
    }

    @JvmName("isHeadingTowardsDoubles")
    fun isHeadingTowards(point2D: Pair<Double, Double>): Boolean {
        val (x, y) = point2D
        return (x - source.x.toDouble() < 0) == (velocity.x < 0) && (y - source.y.toDouble() < 0) == (velocity.y < 0)
    }
}

data class Vector3DBigDecimal(
    val source: Point3DBigDecimal,
    val velocity: Point3DBigDecimal,
) {
    val destination = source + velocity

    /**
     * Returns true if the vector is heading towards the given point. Does not guarantee that the point is on the vector.
     */
    fun isHeadingTowards(point2D: Pair<BigDecimal, BigDecimal>): Boolean {
        val (x, y) = point2D
        val xCheck = if (velocity.x > BigDecimal.ZERO) {
            x >= source.x
        } else {
            x <= source.x
        }
        val yCheck = if (velocity.y > BigDecimal.ZERO) {
            y >= source.y
        } else {
            y <= source.y
        }
        return xCheck && yCheck
    }

    fun isHeadingTowards(point3D: Point3DBigDecimal): Boolean {
        val (x, y, z) = point3D
        val zCheck = if (velocity.z > BigDecimal.ZERO) {
            z >= source.z
        } else {
            z <= source.z
        }
        return isHeadingTowards(x to y) && zCheck
    }

    @JvmName("isHeadingTowardsDoubles")
    fun isHeadingTowards(point2D: Pair<Double, Double>): Boolean {
        val (x, y) = point2D
        return (x - source.x.toDouble() < 0) == (velocity.x < BigDecimal.ZERO) && (y - source.y.toDouble() < 0) == (velocity.y < BigDecimal.ZERO)
    }
}
