import util.file.readInput
import util.geometry.Direction
import util.geometry.Point2D
import util.println
import util.shouldBe
import kotlin.time.measureTimedValue

fun main() {
    fun part1(input: List<String>): Int {
        val forestMap = ForestMap.fromInput(input)
        val lastPoint = forestMap.paths.find { it.y == input.size - 1L }!!
        val nodeGraph = measureTimedValue {
            forestMap.toNodeGraph(forestMap.paths.first { it.y == 0L }, lastPoint)
        }.also { println("to graph: ${it.duration}") }.value
        val pathsSizesToLastPoint = measureTimedValue {
            nodeGraph.allPathsTo(lastPoint)
        }.also { println("all paths: ${it.duration}") }.value
        return pathsSizesToLastPoint.maxOf { it.maxOf { it.distance } }
    }

    fun part2(input: List<String>): Int {
        val forestMap = ForestMap.fromInput(input)
        val lastPoint = forestMap.paths.find { it.y == input.size - 1L }!!
        val nodeGraph = measureTimedValue {
            forestMap.toNodeGraph(forestMap.paths.first { it.y == 0L }, lastPoint, areSlopesClimbable = true)
        }.also { println("part2 to graph: ${it.duration}") }.value
        measureTimedValue {
            nodeGraph.allPathsTo(lastPoint)
        }.also { println("part2 all paths (not sizes): ${it.duration}") }.value.also {
            it.maxBy { it.size }
        }
        val pathsToLastPoint = measureTimedValue {
            nodeGraph.allPathsTo(lastPoint)
        }.also { println("part2 all paths: ${it.duration}") }.value
        return pathsToLastPoint.maxOf { it.maxOf { it.distance } }
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

    fun toNodeGraph(startPoint2D: Point2D, endPoint2D: Point2D, areSlopesClimbable: Boolean = false): Point2DNode {
        return toNodeGraph(startPoint2D, endPoint2D, 0, areSlopesClimbable)!!
    }

    private fun toNodeGraph(startPoint2D: Point2D, endPoint2D: Point2D, inDistance: Int, areSlopesClimbable: Boolean = false, inVisited: Set<Point2D> = emptySet(), inPaths: Set<Point2D> = paths, inSlopes: Map<Point2D, Direction> = slopes): Point2DNode? {
        lateinit var neighbors: Set<Point2D>
        var point = startPoint2D
        val visited = mutableSetOf(startPoint2D)
        visited.addAll(inVisited)
        var distance = inDistance
        while(true) {
            val slopesFilter = { it: Point2D -> areSlopesClimbable || it.move(inSlopes[it]!!) != point }
            neighbors = point.neighbors().filter { (it in inPaths || (it in inSlopes && slopesFilter(it))) && it !in visited }.toSet()

            visited.addAll(neighbors)

//            Draw.updateText(toPathStringMarkVisitedHtml(visited))
//            readLine()
            if (neighbors.size > 1) {
                break
            } else if (neighbors.size == 1) {
                point = neighbors.first()
                distance++
            } else {
                return if (point == endPoint2D) {
                    Point2DNode(point, distance, emptyList())
                } else {
                    null
                }
            }
        }
        val children: List<Point2DNode> = neighbors.mapNotNull {
            toNodeGraph(it, endPoint2D, distance + 1, areSlopesClimbable, visited, inPaths, inSlopes)
        }
        return Point2DNode(startPoint2D, inDistance, children)
    }

    fun toPathString(): String {
        val maxX = paths.maxOf { it.x }
        val maxY = paths.maxOf { it.y }
        return (0..maxY).map { y ->
            (0..maxX).joinToString("") { x ->
                when (val point2D = Point2D(x, y)) {
                    in paths -> "."
                    in slopes -> slopes[point2D]!!.symbol.toString()
                    else -> "#"
                }
            }
        }.joinToString("\n") { it } + "\n"
    }

    fun toPathStringMarkVisited(visited: Set<Point2D>): String {
        val maxX = paths.maxOf { it.x }
        val maxY = paths.maxOf { it.y }
        return (0..maxY).map { y ->
            (0..maxX).joinToString("") { x ->
                when (val point2D = Point2D(x, y)) {
                    in visited -> "0"
                    in paths -> "."
                    in slopes -> slopes[point2D]!!.symbol.toString()
                    else -> "#"
                }
            }
        }.joinToString("\n") { it } + "\n"
    }

    fun toPathStringMarkVisitedHtml(visited: Set<Point2D>): String {
        return "<html>" + toPathStringMarkVisited(visited).replace("\n", "<br>") + "</html>"
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
    val distance: Int,
    val children: List<Point2DNode>
) {
    fun allPathsTo(endPoint2D: Point2D): List<List<Point2DNode>> {
        return if (point2D == endPoint2D) {
            listOf(listOf(this))
        } else {
            children.flatMap { it.allPathsTo(endPoint2D) }.map { listOf(this) + it }
        }
    }

    private val sizeCache = mutableMapOf<Point2D, List<Int>>()

    companion object {
        fun Collection<Point2DNode>.toPathString(): String {
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

        @JvmName("toPathStringPoint2D")
        fun Collection<Point2D>.toPathString(): String {
            val maxX = max().x
            val maxY = max().y
            val thisAsPoints = toSet()
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
