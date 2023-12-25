import util.collection.combinations
import util.file.readInput
import util.geometry.Point3D
import util.geometry.Vector3D
import util.println
import util.shouldBe
import java.math.BigDecimal
import java.math.RoundingMode

fun main() {
    fun part1(input: List<String>, xMin: BigDecimal, xMax: BigDecimal, yMin: BigDecimal, yMax: BigDecimal): Int {
        val hailstones = Hailstones.fromInput(input)
        val intersections = hailstones.intersectionsXYAlt(xMin, xMax, yMin, yMax)
        return intersections.size
    }

    fun part2(input: List<String>): Int {
        return input.size
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day24_part1_test")
    val testMin = BigDecimal("7")
    val testMax = BigDecimal("27")
    part1(testInput, testMin, testMax, testMin, testMax) shouldBe 2

    // 12750 too low
    // 11912 too low
    // 4957
    val input = readInput("Day24")
    val min = BigDecimal("200000000000000")
    val max = BigDecimal("400000000000000")
    part1(input, min, max, min, max).println()
    part2(input).println()
}

data class Hailstones(
    val hail: Set<Vector3D>
) {
    /**
     * my own workings of y = mx + b extrapolated to mx + b = m2 x + b2
     * yielding x = (b2 - b1) / (m1 - m2)
     * this wasn't working for my input, worked for test
     */
    fun intersectionsXY(xMin: BigDecimal, xMax: BigDecimal, yMin: BigDecimal, yMax: BigDecimal): List<Pair<BigDecimal, BigDecimal>> {
        return hail.combinations(2).mapNotNull { (hail1, hail2) ->
            val hail1M = hail1.velocity.x.toBigDecimal() / hail1.velocity.y.toBigDecimal()
            val hail1B = hail1.source.y.toBigDecimal() - hail1M * hail1.source.x.toBigDecimal()

            val hail2M = hail2.velocity.x.toBigDecimal() / hail2.velocity.y.toBigDecimal()
            val hail2B = hail2.source.y.toBigDecimal() - hail2M * hail2.source.x.toBigDecimal()

            if (hail1M - hail2M == BigDecimal.ZERO) {
                null
            } else {
                val x: BigDecimal = (hail2B - hail1B).divide(hail1M - hail2M, 100, RoundingMode.HALF_UP)
                val y: BigDecimal = hail1M * x + hail1B
                require((hail1M * x + hail1B).minus(hail2M * x + hail2B) < BigDecimal("0.0000001")) {
                    "${(hail1M * x + hail1B)} != ${(hail2M * x + hail2B)}"
                }
                (x to y).takeIf {
                    // must be in the direction of the hailstones
                    hail1.isHeadingTowards(it) && hail2.isHeadingTowards(it) &&
                        // must be in range to check
                        it.first >= xMin && it.first <= xMax && it.second >= yMin && it.second <= yMax
                }
            }
        }.toList()
    }

    /**
     * a googled formula to intercepting lines
     */
    fun intersectionsXYAlt(xMin: BigDecimal, xMax: BigDecimal, yMin: BigDecimal, yMax: BigDecimal): List<Pair<BigDecimal, BigDecimal>> {
        return hail.combinations(2).mapNotNull { (hail1, hail2) ->
            val hail1B =
                hail1.velocity.x.toBigDecimal() * hail1.source.y.toBigDecimal() - hail1.velocity.y.toBigDecimal() * hail1.source.x.toBigDecimal()
            val hail2B =
                hail2.velocity.x.toBigDecimal() * hail2.source.y.toBigDecimal() - hail2.velocity.y.toBigDecimal() * hail2.source.x.toBigDecimal()
            val hailMX1 = -hail1.velocity.x.toBigDecimal()
            val hailMX2 = -hail2.velocity.x.toBigDecimal()
            val hailMY1 = hail1.velocity.y.toBigDecimal()
            val hailMY2 = hail2.velocity.y.toBigDecimal()
            val divisor = hailMY1 * hailMX2 - hailMY2 * hailMX1
            if (divisor == BigDecimal.ZERO) return@mapNotNull null
            val x = (hailMX1 * hail2B - hailMX2 * hail1B) / divisor
            val y = (hailMY2 * hail1B - hailMY1 * hail2B) / divisor
            (x to y).takeIf {
                // must be in the direction of the hailstones
                hail1.isHeadingTowards(it) && hail2.isHeadingTowards(it) &&
                        // must be in range to check
                        it.first >= xMin && it.first <= xMax && it.second >= yMin && it.second <= yMax
            }
        }.toList()
    }

    companion object {
        fun fromInput(input: List<String>): Hailstones {
            return Hailstones(
                input.map { line ->
                    val (origin, velocity) = line.split(" @ ")
                    val (x, y, z) = origin.split(",").map { it.trim().toLong() }
                    val (vx, vy, vz) = velocity.split(",").map { it.trim().toLong() }
                    Vector3D(
                        Point3D(x, y, z),
                        Point3D(vx, vy, vz)
                    )
                }.toSet()
            )
        }
    }
}
