import util.println
import util.file.readInput
import util.shouldBe

fun main() {
    fun part1(input: List<String>): Int {
        return input.first().split(",").sumOf { it.day15HashCode() }
    }

    val lensRegex = Regex("([a-z]+)([-=])([0-9]*)")
    fun part2(input: List<String>): Int {
        return input.first().split(",")
            .fold(emptyMap<Int, Map<String, Int>>()) { acc, it ->
                val match = lensRegex.matchEntire(it) ?: error("invalid input")
                val label = match.groupValues[1]
                val boxNum = label.day15HashCode()
                val box = acc[boxNum] ?: emptyMap()
                val operation = match.groupValues[2]
                when(operation) {
                    "-" -> acc + (boxNum to box - label)
                    "=" -> acc + (boxNum to box + (label to match.groupValues[3].toInt()))
                    else -> error("invalid input")
                }
            }.entries.fold(0) { acc, (boxNum, box) ->
                acc + box.values.withIndex().sumOf { (boxNum + 1) * (it.index + 1) * (it.value) }
            }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day15_part1_test")
    part1(testInput) shouldBe 1320
    part2(testInput) shouldBe 145

    val input = readInput("Day15")
    part1(input).println()
    part2(input).println()
}

fun String.day15HashCode(): Int {
    return fold(0) { acc, it ->
        (acc + it.code) * 17 % 256
    }
}
