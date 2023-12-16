import util.println
import util.file.readInput
import util.geometry.Direction
import util.geometry.Point
import util.geometry.Vector
import util.shouldBe

fun main() {
    fun part1(input: List<String>): Int {
        val laserMap = LaserMap.fromInput(input)
        println(laserMap)
        val laserPath = laserMap.energizedTiles(Point(0, 0).vector(Direction.EAST))
        println(laserMap.toString(laserPath))
        return laserPath.size
    }

    fun part2(input: List<String>): Int {
        val laserMap = LaserMap.fromInput(input)
        val startingVectors = laserMap.allPossibleStartingVectors()
        val laserPaths = startingVectors.map { laserMap.energizedTiles(it) }
        return laserPaths.maxOf { it.size }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day16_part1_test")
    part1(testInput) shouldBe 46
    part2(testInput) shouldBe 51

    val input = readInput("Day16")
    part1(input).println()
    part2(input).println()
}

data class LaserMap(
    val elements: Map<Point, LaserMapElement>
) {

    private val minX = elements.keys.minByOrNull { it.x }?.x ?: 0
    private val maxX = elements.keys.maxByOrNull { it.x }?.x ?: 0
    private val minY = elements.keys.minByOrNull { it.y }?.y ?: 0
    private val maxY = elements.keys.maxByOrNull { it.y }?.y ?: 0

    private fun inBounds(point: Point): Boolean {
        return point.x in minX..maxX && point.y in minY..maxY
    }

    fun allPossibleStartingVectors(): Set<Vector> {
        return ((minY..maxY).flatMap { y ->
            listOf(
                Vector(Point(minX, y), Direction.EAST),
                Vector(Point(maxX, y), Direction.WEST)
            )
        } + (minX..maxX).flatMap { x ->
            listOf(
                Vector(Point(x, minY), Direction.SOUTH),
                Vector(Point(x, maxY), Direction.NORTH)
            )
        }).toSet()
    }

    fun energizedTiles(sourceVector: Vector): Set<Point> {
        val sourceConsideringElement = elements[sourceVector.source]?.laserInteraction(sourceVector.direction)?.firstOrNull() ?: sourceVector
        val lasers = mutableSetOf(sourceConsideringElement)
        val vectorsToProcess = mutableListOf(sourceConsideringElement)
        while (vectorsToProcess.isNotEmpty()) {
            val vector = vectorsToProcess.removeFirstOrNull() ?: break
            val element = elements[vector.destination] ?: continue
            val newLasers = element.laserInteraction(vector.direction).filter { inBounds(it.source) }
            val newVectors = newLasers.filter { !lasers.contains(it) }
            lasers.addAll(newLasers)
            vectorsToProcess.addAll(newVectors)
        }
        return lasers.map { it.source }.toSet()
    }

    fun toString(energizedTiles: Set<Point>): String {
        return (minY..maxY).joinToString("\n") { y ->
            (minX..maxX).joinToString("") { x ->
                val point = Point(x, y)
                val element = elements[point]
                (if (energizedTiles.any { it == point }) {
                    '#'
                } else {
                    element?.symbol ?: ' '
                }).toString()
            }
        } + "\n"
    }

    override fun toString(): String {
        return (minY..maxY).joinToString("\n") { y ->
            (minX..maxX).joinToString("") { x ->
                val point = Point(x, y)
                val element = elements[point]
                (element?.symbol ?: ' ').toString()
            }
        } + "\n"
    }

    companion object {
        fun fromInput(input: List<String>): LaserMap {
            val elements = input.mapIndexed { y, line ->
                line.mapIndexed { x, char ->
                    Point(x.toLong(), y.toLong()) to LaserMapElement.fromChar(char, x.toLong(), y.toLong())
                }
            }.flatten().toMap()
            return LaserMap(elements)
        }
    }
}

sealed interface LaserMapElement {
    val point: Point
    val symbol: Char

    fun laserInteraction(direction: Direction): Set<Vector>

    companion object {
        fun fromChar(char: Char, x: Long, y: Long): LaserMapElement {
            val point = Point(x, y)
            return when (char) {
                '.' -> Empty(point)
                '/' -> ForwardSlashMirror(point)
                '\\' -> BackwardSlashMirror(point)
                '|' -> VerticalSplitter(point)
                '-' -> HorizontalSplitter(point)
                else -> throw IllegalArgumentException("Unknown char: $char")
            }
        }
    }
}
data class Empty(override val point: Point) : LaserMapElement {
    override val symbol: Char = '.'
    override fun laserInteraction(direction: Direction): Set<Vector> {
        return setOf(point.vector(direction))
    }
}

data class ForwardSlashMirror(override val point: Point) : LaserMapElement {
    override val symbol: Char = '/'
    override fun laserInteraction(direction: Direction): Set<Vector> {
        val newDirection = when (direction) {
            Direction.NORTH -> Direction.EAST
            Direction.EAST -> Direction.NORTH
            Direction.SOUTH -> Direction.WEST
            Direction.WEST -> Direction.SOUTH
        }
        return setOf(point.vector(newDirection))
    }
}
data class BackwardSlashMirror(override val point: Point) : LaserMapElement {
    override val symbol: Char = '\\'
    override fun laserInteraction(direction: Direction): Set<Vector> {
        val newDirection = when (direction) {
            Direction.NORTH -> Direction.WEST
            Direction.WEST -> Direction.NORTH
            Direction.SOUTH -> Direction.EAST
            Direction.EAST -> Direction.SOUTH
        }
        return setOf(point.vector(newDirection))
    }
}
data class VerticalSplitter(override val point: Point) : LaserMapElement {
    override val symbol: Char = '|'
    override fun laserInteraction(direction: Direction): Set<Vector> {
        val newDirections = when (direction) {
            Direction.NORTH -> listOf(Direction.NORTH)
            Direction.SOUTH -> listOf(Direction.SOUTH)
            Direction.EAST -> listOf(Direction.NORTH, Direction.SOUTH)
            Direction.WEST -> listOf(Direction.NORTH, Direction.SOUTH)
        }
        return newDirections.map { point.vector(it) }.toSet()
    }
}
data class HorizontalSplitter(override val point: Point) : LaserMapElement {
    override val symbol: Char = '-'
    override fun laserInteraction(direction: Direction): Set<Vector> {
        val newDirections = when (direction) {
            Direction.NORTH -> listOf(Direction.WEST, Direction.EAST)
            Direction.SOUTH -> listOf(Direction.WEST, Direction.EAST)
            Direction.EAST -> listOf(Direction.EAST)
            Direction.WEST -> listOf(Direction.WEST)
        }
        return newDirections.map { point.vector(it) }.toSet()
    }
}
