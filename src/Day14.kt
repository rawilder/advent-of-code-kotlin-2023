import util.file.readInput
import util.geometry.Direction
import util.geometry.Point
import util.println
import util.shouldBe
import kotlin.math.max
import kotlin.time.measureTime

object Metrics {
    var tiltMeasureCount = 0
    var tiltAverage = 0L

    val label = javax.swing.JLabel()
    val frame = javax.swing.JFrame().apply {
        add(label)
        pack()
        setSize(200, 200)
        isVisible = true
    }


    fun updateRollingAverage(tiltMeasure: Long) {
        tiltAverage = ((tiltAverage * (max(tiltMeasureCount, 1))) + tiltMeasure) / (++tiltMeasureCount)
        label.text = "tilt: $tiltAverage"
        frame.repaint()
    }

}

fun main() {

    fun part1(input: List<String>): Long {
        val rockMap = RockMap.fromInput(input)
//        println(rockMap)
        rockMap.tilt(Direction.NORTH)
//        println(rockMap)
        return rockMap.load()
    }

    fun part2(input: List<String>): Int {
        val rockMap = RockMap.fromInput(input)
        println(rockMap)
        rockMap.cycle(100_000_000_000)
        return rockMap.map.values.count { it == RockType.ROUND }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day14_part1_test")
    part1(testInput) shouldBe 136L
    part2(testInput) shouldBe 64L

    val input = readInput("Day14")
    part1(input).println()
//    part2(input).println()
}

data class RockMap(
    var map: MutableMap<Point, RockType>
) {

    private fun roundRocks(): List<Point> {
        return map.filter { it.value == RockType.ROUND }.keys.toList()
    }

    val maxX = map.keys.maxByOrNull { it.x }!!.x
    val maxY = map.keys.maxByOrNull { it.y }!!.y

    private data class TiltCacheKey(
        val roundRocksHashCode: Int,
        val direction: Direction
    )
    private val tiltCache = mutableMapOf<TiltCacheKey, List<Point>>()

    fun tilt(direction: Direction) {
        measureTime {
            val cacheKey = TiltCacheKey(map.filter { it.value == RockType.ROUND }.keys.toTypedArray().contentDeepHashCode(), direction)
            if (tiltCache.containsKey(cacheKey)) {
//                println("cache hit")
                tiltCache[cacheKey]!!.forEach { roundRockPoint ->
                    map = map.filterNot { it.value == RockType.ROUND }.toMutableMap().apply {
                        this[roundRockPoint] = RockType.ROUND
                    }
                }
            } else {
                var done: Boolean
                do {
                    done = true
                    map.filter { it.value == RockType.ROUND }.forEach { (point, rockType) ->
                        if (move(point to rockType, direction) && done) done = false
                    }
                } while (!done)
                tiltCache[cacheKey] = roundRocks()
//            println(tiltCache.size)
            }
        }
    }

    private data class CycleCacheKey(
        val roundRocksHashCode: Int,
        val direction: Direction
    )

    private val cycleCache = mutableMapOf<CycleCacheKey, List<Point>>()

    fun cycle(n: Long) {
        var i = 0L
        while (i < n) {
            measureTime {
                val cacheKey = CycleCacheKey(map.filter { it.value == RockType.ROUND }.keys.toTypedArray().contentDeepHashCode(), Direction.NORTH)
                if (cycleCache.containsKey(cacheKey)) {
//                    println("cache hit")
                    cycleCache[cacheKey]!!.forEach { roundRockPoint ->
                        map = map.filterNot { it.value == RockType.ROUND }.toMutableMap().apply {
                            this[roundRockPoint] = RockType.ROUND
                        }
                    }
                } else {
                    tilt(Direction.NORTH)
                    tilt(Direction.WEST)
                    tilt(Direction.SOUTH)
                    tilt(Direction.EAST)
                    cycleCache[cacheKey] = roundRocks()
                }
            }.also { Metrics.updateRollingAverage(it.inWholeNanoseconds) }
            i += 1
        }
    }

    private fun move(rock: Pair<Point, RockType>, direction: Direction): Boolean {
        require(rock.second == RockType.ROUND)
        val localMove = { toPoint: Point ->
            map[toPoint] = rock.second
            map.remove(rock.first)
            true
        }
        val newPoint = rock.first + direction.movementInAMatrix()
        if (newPoint.x < 0 || newPoint.x > maxX || newPoint.y < 0 || newPoint.y > maxY) return false
        return when(val atNewPoint = map[newPoint]) {
            null -> localMove(newPoint)
            RockType.ROUND -> if (move(newPoint to atNewPoint, direction)) { localMove(newPoint) } else false
            RockType.CUBE -> false
        }
    }

    private fun canMove(rock: Pair<Point, RockType>, direction: Direction): Boolean {
        require(rock.second == RockType.ROUND)
        val newPoint = rock.first + direction.movementInAMatrix()
        if (newPoint.x < 0 || newPoint.x > maxX || newPoint.y < 0 || newPoint.y > maxY) return false
        return when(val atNewPoint = map[newPoint]) {
            null -> true
            RockType.ROUND -> canMove(newPoint to atNewPoint, direction)
            RockType.CUBE -> false
        }
    }

    fun load(): Long {
        return map.entries.sumOf { load(it) }
    }

    private fun load(entry: Map.Entry<Point, RockType>): Long {
        val (point, rockType) = entry
        return when (rockType) {
            RockType.ROUND -> maxY - point.y + 1
            RockType.CUBE -> 0
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (y in 0..maxY) {
            for (x in 0..maxX) {
                val rockType = map[Point(x.toLong(), y.toLong())]
                sb.append(rockType?.toString() ?: '.')
            }
            sb.append('\n')
        }
        return sb.toString() + "\n"
    }

    companion object {
        fun fromInput(input: List<String>): RockMap {
            val map = mutableMapOf<Point, RockType>()
            input.forEachIndexed { y, line ->
                line.forEachIndexed { x, c ->
                    if (RockType.charHasRock(c)) map[Point(x.toLong(), y.toLong())] = RockType.fromChar(c)
                }
            }
            return RockMap(map)
        }
    }
}

enum class RockType {
    ROUND,
    CUBE;

    override fun toString(): String {
        return when (this) {
            ROUND -> "O"
            CUBE -> "#"
        }
    }

    companion object {
        fun fromChar(c: Char): RockType {
            return when (c) {
                'O' -> ROUND
                '#' -> CUBE
                else -> throw IllegalArgumentException("Unknown rock type: $c")
            }
        }

        fun charHasRock(c: Char): Boolean {
            return when (c) {
                'O', '#' -> true
                else -> false
            }
        }
    }
}
