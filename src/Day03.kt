import util.metrics.averageInMillis
import util.metrics.comparePerformance
import util.collection.matrixFromStringList
import util.println
import util.file.readInput
import java.awt.Point
import kotlin.time.Duration
import kotlin.time.measureTimedValue

object Stats {
    var part1FindAdjacentNumbers = mutableListOf<Duration>()
    var part1FindContiguousDigitsOnRow = mutableListOf<Duration>()

    var part1FunctionalFindAdjacentNumbers = mutableListOf<Duration>()
    var part1FunctionalFindContiguousDigitsOnRow = mutableListOf<Duration>()

    fun clear() {
        part1FindAdjacentNumbers.clear()
        part1FindContiguousDigitsOnRow.clear()
        part1FunctionalFindAdjacentNumbers.clear()
        part1FunctionalFindContiguousDigitsOnRow.clear()
    }
}

fun main() {
    fun part1(input: List<String>): Int {
        val matrix = matrixFromStringList(input)
        val visited = mutableSetOf<Pair<Int, Int>>()
        val numbers = mutableListOf<Int>()
        val isValidStartingChar = { c: Char -> !c.isDigit() && c != '.' }
        for (x in matrix.indices) {
            for (y in matrix[x].indices) {
                if (isValidStartingChar(matrix[y][x])) {
                    findAdjacentNumbers(matrix, x, y, visited).let {
                        if (it.isNotEmpty()) {
                            numbers.addAll(it)
                        }
                    }
                }
            }
        }
        return numbers.sum()
    }

    fun part2(input: List<String>): Int {
        val matrix = matrixFromStringList(input)
        val visited = mutableSetOf<Pair<Int, Int>>()
        val numbers = mutableListOf<Int>()
        val isValidStartingChar = { c: Char -> c == '*' }
        for (x in matrix.indices) {
            for (y in matrix[x].indices) {
                if (isValidStartingChar(matrix[y][x])) {
                    findAdjacentNumbers(matrix, x, y, visited).let {
                        if (it.isNotEmpty() && it.size == 2) {
                            numbers.add(it.reduce(Int::times))
                        }
                    }
                }
            }
        }
        return numbers.sum()
    }

    fun part1Functional(input: List<String>): Int {
        val matrix = matrixFromStringList(input)
        val isValidStartingChar = { c: Char -> !c.isDigit() && c != '.' }
        return matrix.indices.map { x ->
            matrix[x].indices.fold(VisitResult(emptyList(), emptySet())) { acc, y ->
                if (isValidStartingChar(matrix[y][x])) {
                    findAdjacentNumbersFunctionally(matrix, x, y, acc.visited).let {
                        VisitResult(acc.numbers + it.numbers, acc.visited + it.visited)
                    }
                } else {
                    acc
                }
            }
        }.flatMap { it.numbers }.sum()
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day03_part1_test")
    check(part1(testInput) == 4361)
    val testInput2 = readInput("Day03_part2_test")
    check(part2(testInput2) == 467835)

    Stats.clear()

    val input = readInput("Day03")

    comparePerformance("part1", { part1(testInput) }, { part1Functional(testInput) })
    println("part1FindAdjacentNumbers: ${Stats.part1FindAdjacentNumbers.averageInMillis()}")
    println("part1FindContiguousDigitsOnRow: ${Stats.part1FindContiguousDigitsOnRow.averageInMillis()}")
    println("part1FunctionalFindAdjacentNumbers: ${Stats.part1FunctionalFindAdjacentNumbers.averageInMillis()}")
    println("part1FunctionalFindContiguousDigitsOnRow: ${Stats.part1FunctionalFindContiguousDigitsOnRow.averageInMillis()}")

    part1(input).println()
    part2(input).println()
}

data class VisitResult(val numbers: List<Int>, val visited: Set<Point>)

fun findAdjacentNumbersFunctionally(matrix: List<List<Char>>, x: Int, y: Int, visited: Set<Point>): VisitResult = measureTimedValue {
    val adjacentCoords = listOf(
        Point(x - 1, y - 1),
        Point(x, y - 1),
        Point(x + 1, y - 1),
        Point(x - 1, y),
        Point(x + 1, y),
        Point(x - 1, y + 1),
        Point(x, y + 1),
        Point(x + 1, y + 1),
    ).filter { it !in visited && it.y >= 0 && it.y < matrix.size && it.x >= 0 && it.x < matrix[it.y].size && matrix[it.y][it.x].isDigit() }

    val adjacentNumbers = adjacentCoords.fold(VisitResult(emptyList(), visited)) { acc, coord ->
        if (coord in acc.visited) {
            acc
        } else {
            findContiguousDigitsOnRowFunctionally(matrix, coord.x, coord.y).let {
                VisitResult(acc.numbers + it.numbers, acc.visited + it.visited)
            }
        }
    }.numbers

    VisitResult(adjacentNumbers, adjacentCoords.toSet())
}.also {
    Stats.part1FunctionalFindAdjacentNumbers.add(it.duration)
}.value

fun findContiguousDigitsOnRowFunctionally(matrix: List<List<Char>>, x: Int, y: Int): VisitResult = measureTimedValue {
    val leftX = (x - 1 downTo 0).firstOrNull { !matrix[y][it].isDigit() }?.let { it + 1 } ?: 0
    val rightX = (x + 1 until matrix[y].size).firstOrNull { !matrix[y][it].isDigit() }?.let { it - 1 } ?: (matrix[y].size - 1)
    VisitResult(
        listOf(
            matrix[y].subList(leftX, rightX + 1).joinToString("").toInt()
        ),
        (leftX..rightX).map { Point(it, y) }.toSet()
    )
}.also {
    Stats.part1FunctionalFindContiguousDigitsOnRow.add(it.duration)
}.value

fun findAdjacentNumbers(matrix: List<List<Char>>, x: Int, y: Int, visited: MutableSet<Pair<Int, Int>>): List<Int> = measureTimedValue {
    val adjacentNumbers = mutableListOf<Int>()
    for (newX in x - 1..x + 1) {
        for (newY in y - 1..y + 1) {
            if (newX == x && newY == y) continue
            if (Pair(newX, newY) in visited) continue
            if (newY < 0 || newY >= matrix.size) continue
            if (newX < 0 || newX >= matrix[newY].size) continue
            if (matrix[newY][newX].isDigit()) {
                adjacentNumbers.add(findContiguousDigitsOnRow(matrix, newX, newY, visited).joinToString("").toInt())
            }
            visited.add(Pair(newX, newY))
        }
    }
    adjacentNumbers
}.also {
    Stats.part1FindAdjacentNumbers.add(it.duration)
}.value

fun findContiguousDigitsOnRow(matrix: List<List<Char>>, x: Int, y: Int, visited: MutableSet<Pair<Int, Int>>): List<Char> = measureTimedValue {
    val contiguousDigits = mutableListOf<Char>()
    var newX = x
    while (newX < matrix.size && matrix[y][newX].isDigit()) {
        if (Pair(newX, y) in visited) break
        contiguousDigits.add(matrix[y][newX])
        visited.add(Pair(newX, y))
        newX += 1
    }
    newX = x - 1
    while (newX >= 0 && matrix[y][newX].isDigit()) {
        if (Pair(newX, y) in visited) break
        contiguousDigits.add(0, matrix[y][newX])
        visited.add(Pair(newX, y))
        newX -= 1
    }
    contiguousDigits
}.also {
    Stats.part1FindContiguousDigitsOnRow.add(it.duration)
}.value
