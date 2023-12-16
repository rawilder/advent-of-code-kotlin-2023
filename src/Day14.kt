import util.file.readInput
import util.geometry.Direction
import util.geometry.Point
import util.println
import util.repeat
import util.shouldBe
import kotlin.time.measureTime

fun main() {
    fun part1(input: List<String>): Long {
        val rockMap = RockMap.fromInput(input)
//        println(rockMap)
        rockMap.tilt(Direction.NORTH)
//        println(rockMap)
        return rockMap.load()
    }

    fun part2(input: List<String>): Long {
        val rockMap = RockMap.fromInput(input)
//        println(rockMap)
        rockMap.cycle(1_000_000_000)
        println(rockMap.toStringWithLoad())
        return rockMap.load()
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day14_part1_test")
    part1(testInput) shouldBe 136L
    part2(testInput) shouldBe 64L

    val input = readInput("Day14")
    part1(input).println()
    part2(input).println()
}

data class RockMap(
    var map: MutableMap<Point, RockType>
) {

    private val stateTracker: MutableSet<Pair<Direction, Map<Point, RockType>>> = mutableSetOf()

    private fun roundRocks(): List<Point> {
        return map.filter { it.value == RockType.ROUND }.keys.toList()
    }

    private val maxX = map.keys.maxByOrNull { it.x }!!.x
    private val maxY = map.keys.maxByOrNull { it.y }!!.y

    fun tilt(direction: Direction) {
        val ordering = when (direction) {
            Direction.NORTH -> Comparator<Point> { p1, p2 -> p1.compareTo(p2) }
            Direction.SOUTH -> Comparator<Point> { p1, p2 -> p2.compareTo(p1) }
            Direction.WEST -> Comparator<Point> { p1, p2 -> p1.compareTo(p2) }
            Direction.EAST -> Comparator<Point> { p1, p2 -> p2.compareTo(p1) }
        }
        measureTime {
            var done: Boolean
            do {
                done = true
                map.toSortedMap(ordering).filter { it.value == RockType.ROUND }.forEach { (point, rockType) ->
                    if (move(point to rockType, direction) && done) done = false
                }
            } while (!done)
        }
    }

    val cycleOrder = listOf(Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST)

    fun checkOriginalState(n: Long, iteration: Long, direction: Direction): Boolean {
        return if ((direction to map) in stateTracker) {
            println("found start of cycle after $iteration iterations after $direction tilt")
            val start = map.toMap()
            cycleOrder.takeLastWhile { it != direction }.forEach { tilt(it) }
            // cycle starts on SOUTH
            var cycleLength = 0
            do {
                for (dir in cycleOrder) {
                    tilt(dir)
                    if (map == start) break
                }
                cycleLength += 1
            } while (map != start)
            println("found end of cycle with cycle length $cycleLength")
            val cyclesRemainingAfterN = (n - iteration - cycleLength - 1) % cycleLength
            cycleOrder.takeLastWhile { it != direction }.forEach {
                tilt(it)
            }
            repeat(cyclesRemainingAfterN) {
                cycleOrder.forEach { tilt(it) }
            }
            true
        } else {
            stateTracker.add(direction to map.toMap())
            false
        }
    }

    fun cycle(n: Long) {
        val tiltOrder = listOf(Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST)
        var i = 0L
        while (i < n) {
            tiltOrder.forEach {
                tilt(it)
                if (checkOriginalState(n, i, it)) return
            }
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
        return map.entries.sumOf { load(it.toPair()) }
    }

    private fun load(entry: Pair<Point, RockType>): Long {
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
                val rockType = map[Point(x, y)]
                sb.append(rockType?.toString() ?: '.')
            }
            sb.append('\n')
        }
        return sb.toString() + "\n"
    }

    fun toStringWithLoad(): String {
        val sb = StringBuilder()
        for (y in 0..maxY) {
            for (x in 0..maxX) {
                val rockType = map[Point(x, y)]
                sb.append(rockType?.let {
                    if (it == RockType.ROUND) load(Point(x, y) to it).toString()
                    else it.toString()
                } ?: '.')
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
