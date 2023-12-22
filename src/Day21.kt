import util.println
import util.file.readInput
import util.geometry.Direction
import util.geometry.Point2D
import util.geometry.Point2D.Companion.dijkstras
import util.shouldBe
import kotlin.math.abs

fun main() {
    fun part1(input: List<String>, numSteps: Int): Int {
        val gardenMap = GardenMap.fromInput(input)
        val distances = gardenMap.map.dijkstras(gardenMap.start)
        return distances.count {
            it.value <= numSteps && it.value % 2 == numSteps % 2
        }
    }

    fun part2(input: List<String>, numSteps: Int): Long {
        val gardenMap = GardenMap.fromInput(input)
        val distances = gardenMap.map.dijkstras(gardenMap.start)
        return 0L
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day21_part1_test")
    part1(testInput, 6) shouldBe 16
    part2(testInput, 6) shouldBe 16
    part2(testInput, 10) shouldBe 50
    part2(testInput, 50) shouldBe 1594
    part2(testInput, 100) shouldBe 6536
    part2(testInput, 500) shouldBe 167004
    part2(testInput, 1000) shouldBe 668697
    part2(testInput, 5000) shouldBe 16733044

    val input = readInput("Day21")
    part1(input, 64).println()
    part2(input, 0).println()
}

data class GardenMap(
    val map: List<Point2D>,
    val start: Point2D
) {
    companion object {
        fun fromInput(input: List<String>): GardenMap {
            // # = rock (can't be traversed), . = garden plot (can be), S = starting point
            return input.foldIndexed(GardenMap(emptyList(), Point2D(0, 0))) { y, map, line ->
                val (newPoints, startPoint) = line.foldIndexed(emptyList<Point2D>() to null as Point2D?) { x, (points, startPoint), char ->
                    val newPoint2D = Point2D(x, y)
                    when (char) {
                        '#' -> points to startPoint
                        '.' -> points + newPoint2D to startPoint
                        'S' -> points + newPoint2D to newPoint2D
                        else -> throw IllegalArgumentException("Invalid character $char")
                    }
                }
                map.copy(map = map.map + newPoints, start = startPoint ?: map.start)
            }
        }
    }
}

fun Collection<Point2D>.move(direction: Direction, n: Int): Set<Point2D> {
    return move(direction, n.toLong())
}

fun Collection<Point2D>.move(direction: Direction, n: Long): Set<Point2D> {
    return this.map { it.move(direction, n) }.toSet()
}

fun Map<Point2D, Int>.moveKeys(direction: Direction, n: Int): Map<Point2D, Int> {
    return moveKeys(direction, n.toLong())
}

fun Map<Point2D, Int>.moveKeys(direction: Direction, n: Long): Map<Point2D, Int> {
    return this.mapKeys { it.key.move(direction, n) }
}

fun Map<Point2D, Int>.plus(other: Map<Point2D, Int>): Map<Point2D, Int> {
    return this.toSortedMap().entries.zip(other.toSortedMap().entries).associate {
        it.first.key to (it.first.value + it.second.value)
    }
}

fun Map<Point2D, Int>.minus(other: Map<Point2D, Int>): Map<Point2D, Int> {
    return this.toSortedMap().entries.zip(other.toSortedMap().entries).associate {
        it.first.key to (it.first.value - it.second.value)
    }
}

fun Array<Array<Int?>>.plus(other: Array<Array<Int?>>): Array<Array<Int?>> {
    this.operator(other) { thisValue, otherValue ->
        thisValue?.plus(otherValue!!)
    }.let {
        return it
    }
}

fun Array<Array<Int?>>.minus(other: Array<Array<Int?>>): Array<Array<Int?>> {
    this.operator(other) { thisValue, otherValue ->
        thisValue?.minus(otherValue!!)
    }.let {
        return it
    }
}

fun Array<Array<Int?>>.operator(other: Array<Array<Int?>>, operation: (Int?, Int?) -> Int?): Array<Array<Int?>> {
    require(this.size == other.size)
    require(this[0].size == other[0].size)
    return Array(this.size) { y ->
        Array(this[0].size) { x ->
            val thisValue = this[y][x]
            val otherValue = other[y][x]
            require((thisValue == null && otherValue == null) || (thisValue != null && otherValue != null))
            operation(thisValue, otherValue)
        }
    }
}

fun Map<Point2D, Int>.toGridString(): String {
    val minX = this.keys.minByOrNull { it.x }?.x ?: 0
    val maxX = this.keys.maxByOrNull { it.x }?.x ?: 0
    val minY = this.keys.minByOrNull { it.y }?.y ?: 0
    val maxY = this.keys.maxByOrNull { it.y }?.y ?: 0
    val maxDistanceStringLength = (this.values.maxByOrNull { abs(it) }?.toString()?.length ?: 1) + 1
    return (minY..maxY).joinToString("\n") { y ->
        (minX..maxX).joinToString("") { x ->
            val point2D = Point2D(x, y)
            val distance = this[point2D]
            distance?.toString()?.padStart(maxDistanceStringLength) ?: " ".repeat(maxDistanceStringLength)
        }
    } + "\n"
}

fun Map<Point2D, Int>.diffsFrom(point2D: Point2D): Map<Point2D, Int> {
    val pointValue = this[point2D] ?: throw IllegalArgumentException("Point $point2D not in map")
    return this.mapValues { it.value - pointValue }
}
