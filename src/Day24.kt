import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import util.collection.awaitFirstNotNullAndCancelRestOrNull
import util.collection.combinations
import util.file.readInput
import util.geometry.Point3DBigDecimal
import util.geometry.Vector3DBigDecimal
import util.println
import util.shouldBe
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

suspend fun main() {
    fun part1(input: List<String>, xMin: BigDecimal, xMax: BigDecimal, yMin: BigDecimal, yMax: BigDecimal): Int {
        val hailstones = Hailstones.fromInput(input)
        val intersections = hailstones.intersectionsXYAlt(xMin, xMax, yMin, yMax)
        return intersections.size
    }

    suspend fun part2(input: List<String>): BigDecimal {
        val hailstones = Hailstones.fromInput(input)
        val result = Hailstones.recursePart2BruteForce(hailstones)
        requireNotNull(result)
        val velocities = result.windowed(3).map { (first, second) ->
            val firstDiff = (second.second - first.second) / (second.first - first.first).toBigDecimal()
            firstDiff
        }
        require(velocities.windowed(2).all { it.first() == it.last() }) {
            "result is not in a straight line"
        }
        val impliedStart = result.first().second - velocities.first()
        return impliedStart.let { (x, y, z) -> x + y + z }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day24_part1_test")
    val testMin = BigDecimal("7")
    val testMax = BigDecimal("27")
    part1(testInput, testMin, testMax, testMin, testMax) shouldBe 2
    part2(testInput) shouldBe BigDecimal.valueOf(47)

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
    val hail: List<Vector3DBigDecimal>
) {
    /**
     * my own workings of y = mx + b extrapolated to mx + b = m2 x + b2
     * yielding x = (b2 - b1) / (m1 - m2)
     * this wasn't working for my input, worked for test
     */
    fun intersectionsXY(xMin: BigDecimal, xMax: BigDecimal, yMin: BigDecimal, yMax: BigDecimal): List<Pair<BigDecimal, BigDecimal>> {
        return hail.combinations(2).mapNotNull { (hail1, hail2) ->
            val hail1M = hail1.velocity.x / hail1.velocity.y
            val hail1B = hail1.source.y - hail1M * hail1.source.x

            val hail2M = hail2.velocity.x / hail2.velocity.y
            val hail2B = hail2.source.y - hail2M * hail2.source.x

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
                hail1.velocity.x * hail1.source.y - hail1.velocity.y * hail1.source.x
            val hail2B =
                hail2.velocity.x * hail2.source.y - hail2.velocity.y * hail2.source.x
            val hailMX1 = -hail1.velocity.x
            val hailMX2 = -hail2.velocity.x
            val hailMY1 = hail1.velocity.y
            val hailMY2 = hail2.velocity.y
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

    fun tickForward() = Hailstones(hail.map { it.copy(source = it.destination) }.sortedBy { it.source.z })

    /**
     * tick forward n times
     */
    fun tickForward(n: Int): Hailstones {
        var hailstones = this
        repeat(n) {
            hailstones = hailstones.tickForward()
        }
        return hailstones
    }

    fun drop(n: Int) = Hailstones(hail.drop(n))

    fun isEmpty() = hail.isEmpty()
    fun isNotEmpty() = hail.isNotEmpty()

    companion object {
        fun fromInput(input: List<String>): Hailstones {
            return Hailstones(
                input.map { line ->
                    val (origin, velocity) = line.split(" @ ")
                    val (x, y, z) = origin.split(",").map { it.trim().toLong() }
                    val (vx, vy, vz) = velocity.split(",").map { it.trim().toLong() }
                    Vector3DBigDecimal(
                        Point3DBigDecimal(x.toBigDecimal(), y.toBigDecimal(), z.toBigDecimal()),
                        Point3DBigDecimal(vx.toBigDecimal(), vy.toBigDecimal(), vz.toBigDecimal())
                    )
                }
            )
        }

        private val scope = CoroutineScope(Dispatchers.Default)

        // so basically, recursively call tick and check for valid 3d line along all points, and if it ever returns a non match, it will tick up to check the next
        suspend fun recursePart2BruteForce(
            hailstones: Hailstones,
            currentStonePoints: List<Pair<Int, Point3DBigDecimal>> = emptyList(), tickIn: Int = 0
        ): List<Pair<Int, Point3DBigDecimal>>? {
            if (hailstones.isEmpty()) return currentStonePoints
            if (currentStonePoints.isEmpty()) {
                var tickStart = tickIn + 1
                while(true) {
                    println("checking ticks $tickStart to ${tickStart + 9}")
                    (tickStart..tickStart + 9).map { ticks ->
                        scope.async {
                            val tickedForwardHailstones = hailstones.tickForward(ticks)
                            val nextRockPoint = tickedForwardHailstones.hail.first().source
                            recursePart2BruteForce(tickedForwardHailstones.drop(1), listOf(ticks to nextRockPoint), ticks)
                        }
                    }.awaitFirstNotNullAndCancelRestOrNull()?.let {
                        return it
                    }
                    println("no match, continuing")
                    tickStart += 10
                }
            } else if (currentStonePoints.size == 1) {
                var nextHailstones: Hailstones = hailstones
                var tick = tickIn
                while (true) {
                    nextHailstones = nextHailstones.tickForward()
                    val nextRockPoint = nextHailstones.hail.first().source
                    tick++
                    val result = recursePart2BruteForce(nextHailstones.drop(1), currentStonePoints + (tick to nextRockPoint), tick)
                    if (result != null) return result
                }
            } else {
                val lastRockPoint = currentStonePoints.last().second
                val lastTickDiff = currentStonePoints.last().first - currentStonePoints[currentStonePoints.size - 2].first
                val lastDiff = (lastRockPoint - currentStonePoints[currentStonePoints.size - 2].second) / lastTickDiff.toBigDecimal()

                var nextHailstones: Hailstones = hailstones
                var tick = tickIn
                while (true) {
                    nextHailstones = nextHailstones.tickForward()
                    val nextRockPoint = nextHailstones.hail.first().source
                    tick++
                    val nextRockPointVelocity = nextHailstones.hail.first().velocity
                    val nextDiff = nextRockPoint - lastRockPoint
                    if (nextDiff == lastDiff) {
                        val result = recursePart2BruteForce(nextHailstones.drop(1), currentStonePoints + (tick to nextRockPoint), tick)
                        if (result != null) return result
                    }
                    // if we're just moving further away, there's no point in continuing
                    if (!velocityIsHeadingTowards(nextRockPointVelocity, nextDiff, lastDiff)) return null
                }
            }
        }

        private fun velocityIsHeadingTowards(velocity: Point3DBigDecimal, fromDiff: Point3DBigDecimal, toDiff: Point3DBigDecimal): Boolean {
            return Vector3DBigDecimal(fromDiff, velocity).isHeadingTowards(toDiff)
        }
    }
}
