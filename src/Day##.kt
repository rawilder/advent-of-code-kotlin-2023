import util.println
import util.file.readInput
import util.shouldBe

fun main() {
    fun part1(input: List<String>): Int {
        return input.size
    }

    fun part2(input: List<String>): Int {
        return input.size
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day##_part1_test")
    part1(testInput) shouldBe 1

    val input = readInput("Day##")
    part1(input).println()
    part2(input).println()
}
