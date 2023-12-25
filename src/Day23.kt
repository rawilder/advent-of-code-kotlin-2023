import Point2DNode.Companion.toPathString
import util.collection.map
import util.file.readInput
import util.geometry.Direction
import util.geometry.Point2D
import util.println
import util.shouldBe
import kotlin.time.measureTimedValue

fun main() {
    fun part1(input: List<String>): Int {
        val forestMap = ForestMap.fromInput(input)
        val nodeGraph = measureTimedValue {
            forestMap.toNodeGraph(forestMap.paths.first { it.y == 0L })
        }.also { println("to graph: ${it.duration}") }.value
        val lastPoint = forestMap.paths.find { it.y == input.size - 1L }!!
        val pathsSizesToLastPoint = measureTimedValue {
            nodeGraph.allPathSizesTo(lastPoint)
        }.also { println("all paths: ${it.duration}") }.value
        return pathsSizesToLastPoint.maxOrNull()!! - 1 // for start
    }

    fun part2(input: List<String>): Int {
        val forestMap = ForestMap.fromInput(input)
        val nodeGraph = measureTimedValue {
            forestMap.toNodeGraph(forestMap.paths.first { it.y == 0L }, areSlopesClimbable = true)
        }.also { println("part2 to graph: ${it.duration}") }.value
        val lastPoint = forestMap.paths.find { it.y == input.size - 1L }!!
        measureTimedValue {
            nodeGraph.allPathsTo(lastPoint)
        }.also { println("part2 all paths (not sizes): ${it.duration}") }.value.also {
            it.maxBy { it.size }.toPathString().println()
        }
        val pathsToLastPoint = measureTimedValue {
            nodeGraph.allPathSizesTo(lastPoint)
        }.also { println("part2 all paths: ${it.duration}") }.value
        return pathsToLastPoint.maxOrNull()!! - 1 // for start
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day23_part1_test")
    part1(testInput) shouldBe 94
    part2(testInput) shouldBe 154

    val input = readInput("Day23")
    part1(input) shouldBe 2018
    part2(input).println()
}

data class ForestMap(
    val paths: Set<Point2D>,
    val slopes: Map<Point2D, Direction>
) {

    fun toNodeGraph(startPoint2D: Point2D, areSlopesClimbable: Boolean = false): Point2DNode {
        return toNodeGraph(startPoint2D, areSlopesClimbable, paths, slopes)
    }

    private fun toNodeGraph(startPoint2D: Point2D, areSlopesClimbable: Boolean = false, inPaths: Set<Point2D> = paths, inSlopes: Map<Point2D, Direction> = slopes): Point2DNode {
        val (slopeNeighbors, pathNeighbors) = startPoint2D.neighbors().filter { it in inPaths || it in inSlopes }.partition { it in inSlopes }.map(List<Point2D>::toSet)
        val slopesThatDontComeBack = slopeNeighbors.filter { areSlopesClimbable || it.move(inSlopes[it]!!) != startPoint2D }.toSet()
        val children: List<Point2DNode> = if (slopesThatDontComeBack.isNotEmpty()) {
            slopesThatDontComeBack.map { toNodeGraph(it, areSlopesClimbable, (inPaths - pathNeighbors) - startPoint2D, (inSlopes - slopeNeighbors) - startPoint2D) }
        } else {
            pathNeighbors.map { toNodeGraph(it, areSlopesClimbable, (inPaths - pathNeighbors) - startPoint2D, inSlopes) }
        }
        return Point2DNode(startPoint2D, children)
    }

    companion object {
        fun fromInput(input: List<String>): ForestMap {
            val paths = mutableSetOf<Point2D>()
            val slopes = mutableMapOf<Point2D, Direction>()
            input.forEachIndexed { y, line ->
                line.forEachIndexed { x, char ->
                    val point2D = Point2D(x, y)
                    when (char) {
                        '.' -> paths.add(point2D)
                        '^', 'v', '<', '>' -> slopes[point2D] = Direction.fromChar(char)
                    }
                }
            }
            return ForestMap(paths, slopes)
        }
    }
}

data class Point2DNode(
    val point2D: Point2D,
    val children: List<Point2DNode>
) {

    private val cache = mutableMapOf<Point2D, List<List<Point2DNode>>>()

    fun allPathsTo(endPoint2D: Point2D): List<List<Point2DNode>> {
        val cached = cache[endPoint2D]
        if (cached != null) {
            return cached
        }
        return if (point2D == endPoint2D) {
            listOf(listOf(this))
        } else {
            children.flatMap { it.allPathsTo(endPoint2D) }.map { listOf(this) + it }.also {
                cache[endPoint2D] = it
            }
        }
    }

    private val sizeCache = mutableMapOf<Point2D, List<Int>>()

    fun allPathSizesTo(endPoint2D: Point2D): List<Int> {
        val cached = sizeCache[endPoint2D]
        if (cached != null) {
            return cached
        }
        return if (point2D == endPoint2D) {
            listOf(1)
        } else {
            children.flatMap { it.allPathSizesTo(endPoint2D) }.map { it + 1 }.also {
                sizeCache[endPoint2D] = it
            }
        }
    }

    companion object {
        fun List<Point2DNode>.toPathString(): String {
            val maxX = maxOf(Point2DNode::point2D).x
            val maxY = maxOf(Point2DNode::point2D).y
            val thisAsPoints = map(Point2DNode::point2D).toSet()
            return (0..maxY).map { y ->
                (0..maxX).joinToString("") { x ->
                    val point2D = Point2D(x, y)
                    if (point2D in thisAsPoints) {
                        "0"
                    } else {
                        "."
                    }
                }
            }.joinToString("\n") { it } + "\n"
        }
    }
}
