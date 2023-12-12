import util.geometry.Edge
import util.geometry.Point
import util.println
import util.file.readInput
import util.shouldBe
import util.collection.transpose

fun main() {
    fun part1(input: List<String>): Long {
        val galaxyMap = GalaxyMap.parse(input)
        return galaxyMap.withEmptySpaceExpandedTo(2).shortestPaths().values.sum()
    }

    fun part2(input: List<String>): Long {
        return GalaxyMap.parse(input).withEmptySpaceExpandedTo(1000000).shortestPaths().let {
            it.values.reduce { acc, next ->
                acc + next
            }
        }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day11_part1_test")
    val testGalaxy = GalaxyMap.parse(testInput)
    testGalaxy.withEmptySpaceExpandedTo(2) shouldBe GalaxyMap.parse(expandInput(testInput, 2))
    testGalaxy.withEmptySpaceExpandedTo(2).println().also { map ->
        val galaxies = map.galaxies
        galaxies[0].distanceToInAMatrix(galaxies[6]) shouldBe 15L
        galaxies[2].distanceToInAMatrix(galaxies[5]) shouldBe 17L
        galaxies[7].distanceToInAMatrix(galaxies[8]) shouldBe 5L
    }

    part1(testInput) shouldBe 374L
    testGalaxy.withEmptySpaceExpandedTo(10).shortestPaths().values.sum() shouldBe 1030L
    testGalaxy.withEmptySpaceExpandedTo(100).shortestPaths().values.sum() shouldBe 8410L

    val input = readInput("Day11")

    GalaxyMap.parse(input).println().withEmptySpaceExpandedTo(2).println() shouldBe GalaxyMap.parse(expandInput(input, 2)).println()

    part1(input).println()
    part2(input).println()
}

data class GalaxyMap(
    val galaxies: List<Point>
) {

    fun shortestPaths(): Map<Edge, Long> {
        val edges = galaxies.fold(emptySet<Edge>()) { acc, galaxy ->
            acc + galaxies.fold(emptySet()) { innerAcc, otherGalaxy ->
                val edge = Edge(galaxy, otherGalaxy)
                if (galaxy != otherGalaxy && edge.reverse() !in acc) {
                    innerAcc + edge
                } else {
                    innerAcc
                }
            }
        }
        return edges.associateWith(Edge::length)
    }

    fun withEmptySpaceExpandedTo(size: Int): GalaxyMap {
        data class Expand(val accumulatedSize: Long = 0, val results: Map<Long, Long> = emptyMap())

        fun expandedValues(property: Point.() -> Long): Map<Long, Long> {
            return galaxies.map { it.property() }.distinct().sorted().fold(Expand()) { acc, nextOriginal ->
                // handle first row being empty
                val prevOriginal = acc.results.keys.lastOrNull() ?: -1
                // y = 3 and y = 4 have 0 empty space between them
                val emptySpaceBetween = nextOriginal - prevOriginal - 1
                require(emptySpaceBetween >= 0)
                // original already accounts for existing empty space, so -1
                val newEmptySpaceBetweenSize = emptySpaceBetween * (size - 1)
                val newAccumulatedSize = acc.accumulatedSize + newEmptySpaceBetweenSize
                val newResults = acc.results + (nextOriginal to nextOriginal + newAccumulatedSize)
                Expand(newAccumulatedSize, newResults)
            }.results
        }
        val newYValues = expandedValues { y }
        val newXValues = expandedValues { x }
        val maybeNewYs = galaxies.map { it.copy(y = newYValues[it.y]!!) }
        val maybeNewXs = maybeNewYs.map { it.copy(x = newXValues[it.x]!!) }
        return copy(galaxies = maybeNewXs.sorted())
    }

    fun println(): GalaxyMap {
        val maxX = galaxies.maxOf { it.x }
        val maxY = galaxies.maxOf { it.y }
        val map = List(maxY.toInt() + 1) { y ->
            List(maxX.toInt() + 1) { x ->
                val noGalaxyInRow = galaxies.none { it.y == y.toLong() }
                val noGalaxyInColumn = galaxies.none { it.x == x.toLong() }
                when {
                    Point(x.toLong(), y.toLong()) in galaxies -> '#'
                    noGalaxyInRow || noGalaxyInColumn -> 'O'
                    else -> '.'
                }
            }.joinToString("")
        }.joinToString("\n")
        println(map + "\n")
        return this
    }

    companion object {
        fun parse(input: List<String>): GalaxyMap {
            return GalaxyMap(
                input.mapIndexed { y, line ->
                    line.mapIndexedNotNull { x, char ->
                        if (char == '#') Point(x.toLong(), y.toLong()) else null
                    }
                }.flatten()
            )
        }
    }
}

// doesn't work for part 2 but keep for comparing to other solutions for smaller inputs
fun expandInput(input: List<String>, size: Int): List<String> {
    val expandedVertically = input.flatMap { line ->
        if (line.all { it == '.' }) {
            List(size) { line }
        } else listOf(line)
    }
    return expandedVertically.transpose().flatMap { column ->
        if (column.all { it == '.' }) {
            List(size) { column }
        } else listOf(column)
    }.transpose()
}
