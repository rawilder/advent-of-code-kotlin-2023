import util.collection.repeat
import util.println
import util.file.readInput
import util.geometry.Point
import util.shouldBe
import kotlin.math.max
import kotlin.math.min

fun main() {
    fun part1(input: List<String>): Int {
        val ashAndRocksMaps = AshAndRocks.fromInput(input)
        val mirrorIndexes = ashAndRocksMaps.map { it.identifyMirrorIndexes() }
        return mirrorIndexes.withIndex().fold(0) { acc, mirrorIndex ->
            when (val indexValue = mirrorIndex.value) {
                is MirrorIndex.Row -> acc + (indexValue.rowIndex + 1) * 100
                is MirrorIndex.Column -> acc + indexValue.columnIndex + 1
            }
        }
    }

    fun part2(input: List<String>): Int {
        val ashAndRocksMaps = AshAndRocks.fromInput(input)
        val mirrorIndexes = ashAndRocksMaps.map { it.identifyMirrorIndexesWithSmudge() }
        return mirrorIndexes.withIndex().fold(0) { acc, mirrorIndex ->
            when (val indexValue = mirrorIndex.value) {
                is MirrorIndex.Row -> acc + (indexValue.rowIndex + 1) * 100
                is MirrorIndex.Column -> acc + indexValue.columnIndex + 1
            }
        }
    }

    """
        ####.##.#######
        .####..####....
        ####.##.#######
        .###.##.###.##.
        .##..##..##....
        .#...##...#.##.
        #..######..####
        #..#....#..####
        .###.##.###.##.
        #.#..##.##.####
        #.#.#..#.#.#..#
        ####....#######
        .##......##.##.
    """.trimIndent().lines().let { part2(it) }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day13_part1_test")
    part1(testInput) shouldBe 405

    val input = readInput("Day13")
    part1(input).println()
    part2(input).println()
}

data class AshAndRocks(
    val map: Map<Point, Tile>
) {
    private val maxX = map.keys.maxOfOrNull { it.x }
    private val maxY = map.keys.maxOfOrNull { it.y }

    private val rows = if (maxX == null || maxY == null) emptyList() else
            (0..maxY).map { y ->
                (0..maxX).map { x ->
                    requireNotNull(map[Point(x, y)]) {
                        "No tile found at $x, $y"
                    }
                }
    }
    private val columns = if (maxX == null || maxY == null) emptyList() else
            (0..maxX).map { x ->
                (0..maxY).map { y ->
                    requireNotNull(map[Point(x, y)]) {
                        "No tile found at $x, $y"
                    }
                }
    }

    fun identifyMirrorIndexes(): MirrorIndex {
        fun findInRowsOrColumns(rowsOrColumns: List<List<Tile>>, createResult: (Int) -> MirrorIndex): MirrorIndex? {
            return rowsOrColumns.withIndex().toList().dropLast(1).find { (idx, _) ->
                val maxDistance = max(min(rowsOrColumns.size - 2 - idx, idx), 0)
                (0 .. maxDistance).asSequence().map { distanceFromIdx ->
                    require(idx + distanceFromIdx + 1 < rowsOrColumns.size) {
                        "idx + distanceFromIdx + 1 < rowsOrColumns.size: $idx + $distanceFromIdx + 1 < ${rowsOrColumns.size}"
                    }
                    rowsOrColumns[idx - distanceFromIdx] == rowsOrColumns[idx + distanceFromIdx + 1]
                }.ifEmpty { sequenceOf(false) }.all { it }
            }?.let { createResult(it.index) }
        }
        return (findInRowsOrColumns(rows) { MirrorIndex.ofRow(it) }
            ?: findInRowsOrColumns(columns) { MirrorIndex.ofColumn(it) }
            ?: throw IllegalStateException("No mirror index found \n$this")).also {
            println(this.toString(it))
            println("")
        }
    }

    fun identifyMirrorIndexesWithSmudge(): MirrorIndex {
        fun findInRowsOrColumns(rowsOrColumns: List<List<Tile>>, createResult: (Int) -> MirrorIndex): MirrorIndex? {
            return rowsOrColumns.withIndex().toList().dropLast(1).find { (idx, _) ->
                // there must be exactly one difference (the smudge)
                var oneDifferenceCount = 0
                val maxDistance = max(min(rowsOrColumns.size - 2 - idx, idx), 0)
                (0 .. maxDistance).asSequence().map { distanceFromIdx ->
                    require(idx + distanceFromIdx + 1 < rowsOrColumns.size) {
                        "idx + distanceFromIdx + 1 < rowsOrColumns.size: $idx + $distanceFromIdx + 1 < ${rowsOrColumns.size}"
                    }
                    val differenceByTile = rowsOrColumns[idx - distanceFromIdx].zip(rowsOrColumns[idx + distanceFromIdx + 1]).filter {
                        it.first != it.second
                    }
                    if (differenceByTile.size == 1) {
                        // track smudge count
                        oneDifferenceCount++
                    }
                    // continue if we're still mirroring but always leave the opportunity for the smudge
                    differenceByTile.count() in setOf(0, 1)
                }.ifEmpty { sequenceOf(false) }.all { it } &&
                        // check the smudge
                        oneDifferenceCount == 1
            }?.let { createResult(it.index) }
        }
        return (findInRowsOrColumns(rows) { MirrorIndex.ofRow(it) }
            ?: findInRowsOrColumns(columns) { MirrorIndex.ofColumn(it) }
            ?: throw IllegalStateException("No mirror index found \n$this")).also {
            println(this.toString(it))
            println("")
        }
    }

    override fun toString(): String {
        return rows.joinToString("\n") { row ->
            row.joinToString("") { it.toString() }
        }
    }

    private fun toString(pointOut: MirrorIndex): String {
        return when (pointOut) {
            is MirrorIndex.Row -> rows.withIndex().joinToString("\n") { row ->
                if (row.index == pointOut.rowIndex) {
                    ">" + row.value.joinToString("") { it.toString() } + "<"
                } else {
                    " " + row.value.joinToString("") { it.toString() }
                }
            }
            is MirrorIndex.Column -> {
                " ".repeat(pointOut.columnIndex + 1) + "v\n" +
                rows.withIndex().joinToString("\n") { row ->
                    " " + row.value.joinToString("") { it.toString() }
                } + "\n" + " ".repeat(pointOut.columnIndex + 1) + "^\n"
            }
        }
    }

    companion object {
        fun fromInput(input: List<String>): List<AshAndRocks> {
            return input.fold(listOf(AshAndRocks(emptyMap()))) { acc, line ->
                if (line.isEmpty()) {
                    acc + AshAndRocks(emptyMap())
                } else {
                    val y = if (acc.last().map.isEmpty()) 0 else acc.last().maxY!! + 1
                    val map = line.mapIndexed { x, c ->
                        Point(x.toLong(), y) to Tile.fromSymbol(c)
                    }.toMap()
                    acc.dropLast(1) + acc.last().copy(map = acc.last().map + map)
                }
            }
        }
    }
}

sealed interface MirrorIndex {
    data class Row(val rowIndex: Int) : MirrorIndex
    data class Column(val columnIndex: Int) : MirrorIndex

    companion object {
        fun ofRow(rowIndex: Int): MirrorIndex = Row(rowIndex)
        fun ofColumn(columnIndex: Int): MirrorIndex = Column(columnIndex)
    }
}

enum class Tile(val symbol: Char) {
    ASH('.'),
    ROCK('#');

    override fun toString(): String {
        return symbol.toString()
    }

    companion object {
        fun fromSymbol(symbol: Char): Tile {
            return entries.first { it.symbol == symbol }
        }
    }
}
