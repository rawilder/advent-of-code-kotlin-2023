import util.println
import util.file.readInput
import util.geometry.Direction
import util.geometry.Point2D
import util.geometry.Point2D.Companion.perimeter
import util.geometry.Point2D.Companion.shoelaceArea
import util.geometry.Vector
import util.shouldBe

fun main() {
    fun part1(input: List<String>): Long {
        val trenchMap = TrenchMap.fromInput(input)
        println(trenchMap)
        return trenchMap.calculateArea() + (trenchMap.points.perimeter() / 2) + 1
    }

    fun part2(input: List<String>): Long {
        val trenchMap = TrenchMap.fromInputPart2(input)
        return trenchMap.calculateArea() + (trenchMap.points.perimeter() / 2) + 1
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day18_part1_test")
    part1(testInput) shouldBe 62L
    part2(testInput) shouldBe 952408144115L

    val input = readInput("Day18")
    part1(input).println()
    part2(input).println()
}

data class TrenchMap(
    val edgeToColor: Map<Vector, String>
) {

    val points = edgeToColor.keys.map { it.source }

    fun calculateArea(): Long {
        return edgeToColor.keys
            .map { it.source }
            .shoelaceArea()
    }

    override fun toString(): String {
        val minX = edgeToColor.keys.minByOrNull { it.source.x }?.source?.x ?: 0
        val maxX = edgeToColor.keys.maxByOrNull { it.source.x }?.source?.x ?: 0
        val minY = edgeToColor.keys.minByOrNull { it.source.y }?.source?.y ?: 0
        val maxY = edgeToColor.keys.maxByOrNull { it.source.y }?.source?.y ?: 0

        return (minY..maxY).joinToString("\n") { y ->
            (minX..maxX).joinToString("") { x ->
                val any = edgeToColor.keys.find { it.source == Point2D(x, y) }
                (any?.let { '#' } ?: '.').toString()
            }
        } + "\n"
    }

    companion object {
        private val inputRegex = Regex("""([RLDU]) (\d+) \((#[0-9a-f]{6})\)""")
        fun fromInput(input: List<String>): TrenchMap {
            return input.fold(emptyList<Pair<Vector, String>>()) { acc, line ->
                val (directionString, magnitudeString, colorString) = inputRegex.matchEntire(line)!!.destructured
                val direction = Direction.fromString(directionString)
                val magnitude = magnitudeString.toLong()
                if (acc.isEmpty()) {
                    listOf(Vector(Point2D(0, 0), direction, magnitude) to colorString)
                } else {
                    val (lastVector, _) = acc.last()
                    val newVector = Vector(lastVector.destination, direction, magnitude)
                    acc + (newVector to colorString)
                }
            }.toMap().let {
                TrenchMap(it)
            }
        }

        fun fromInputPart2(input: List<String>): TrenchMap {
            return input.fold(emptyList<Pair<Vector, String>>()) { acc, line ->
                val (_, _, colorString) = inputRegex.matchEntire(line)!!.destructured
                val directionString = when(colorString.takeLast(1)) {
                    "0" -> "R"
                    "1" -> "D"
                    "2" -> "L"
                    "3" -> "U"
                    else -> throw IllegalArgumentException("direction cannot be extracted")
                }
                val direction = Direction.fromString(directionString)
                val magnitude = colorString.drop(1).take(5).toLong(16)
                if (acc.isEmpty()) {
                    listOf(Vector(Point2D(0, 0), direction, magnitude) to colorString)
                } else {
                    val (lastVector, _) = acc.last()
                    val newVector = Vector(lastVector.destination, direction, magnitude)
                    acc + (newVector to colorString)
                }
            }.toMap().let {
                TrenchMap(it)
            }
        }
    }
}
