import util.collection.intersectAsRange
import util.println
import util.file.readInput
import util.geometry.Point3D
import util.geometry.RectangularPrism
import util.shouldBe
import java.util.SortedMap
import java.util.TreeMap

fun main() {
    fun part1(input: List<String>): Long {
        var bricks = fromInput(input)
        val bricksDeepCopy = fromInput(input)
        var bricksMoving = true
        while (bricksMoving) {
            bricksMoving = false
            bricks = bricks.entries.fold(TreeMap()) { acc, (z, brickList) ->

                val fallenBricks = brickList.map { brick ->
                    if (brick.prism.start.z == 1L) {
                        z to brick
                    } else {
                        val bricksBelow = acc.values.flatten()
                        if (bricksBelow.any { brick.restsOn(it) }) {
                            z to brick
                        } else {
                            bricksMoving = true
                            brick.fall().let {
                                it.prism.start.z to it
                            }
                        }
                    }
                }

                fallenBricks.forEach {
                    acc.computeIfAbsent(it.first) { emptyList() }.let { list ->
                        acc[it.first] = list + it.second
                    }
                }
                acc
            }
        }
        require(bricks.keys.zipWithNext().fold(true) { acc, (l, nL) -> acc && (nL - l == 1L) }) { "bricks are not sorted" }
        bricks.values.flatten().forEach { brick ->
            bricks.values.flatten().forEach { otherBrick ->
                require(brick == otherBrick || !brick.intersects(otherBrick)) {
                    val originalBrick = bricksDeepCopy.values.flatten().find { it.id == brick.id }!!
                    val originalOtherBrick = bricksDeepCopy.values.flatten().find { it.id == otherBrick.id }!!
                    "bricks intersect: $brick and $otherBrick, original: $originalBrick and $originalOtherBrick"
                }
            }
        }
        val topBlockSupportedBy = mutableMapOf<Brick, Set<Brick>>()
        return bricks.entries.fold(0L) { acc, (z, brickList) ->
            if (z == bricks.lastKey()) return@fold acc + brickList.count()

            val aboveBricks = bricks.entries.filter { it.key > z }.flatMap { it.value }

            // bricks which have no bricks above them
            val (bottomBricksWithNoTops, bottomBricksWithTops) = brickList.partition { bottomBrick ->
                aboveBricks.none { it.restsOn(bottomBrick) }
            }


            bottomBricksWithTops.forEach { bottomBrick ->
                aboveBricks.filter { it.restsOn(bottomBrick) }.forEach { topBrick ->
                    topBlockSupportedBy[topBrick] = topBlockSupportedBy.getOrDefault(topBrick, emptySet()) + bottomBrick
                }
            }

            acc + bottomBricksWithNoTops.size
        }.let { acc ->
            val (topBlocksWith1Support, topBlocksWithMoreThan1Support) = topBlockSupportedBy.values.partition { it.size == 1 }
            acc + (topBlocksWithMoreThan1Support.flatten().toSet() - topBlocksWith1Support.flatten().toSet()).size
        }
    }

    fun part2(input: List<String>): Int {
        return input.size
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day22_part1_test")
    part1(testInput) shouldBe 5

    val input = readInput("Day22")
    part1(input).println()
    part2(input).println()
}

data class Brick(
    val id: Int,
    val prism: RectangularPrism
) {
    fun fall(): Brick {
        return Brick(id, prism.fall())
    }

    fun restsOn(other: Brick): Boolean {
        return prism.restsOn(other.prism)
    }

    fun intersects(other: Brick): Boolean {
        return prism.intersects(other.prism)
    }
}

fun fromInput(input: List<String>): SortedMap<Long, List<Brick>> {
    var id = 0
    val sortedMap = TreeMap<Long, List<Brick>>()
    input.forEach { line ->
        val (start, end) = line.split("~")
        val (x1, y1, z1) = start.split(",").map { it.toLong() }
        val (x2, y2, z2) = end.split(",").map { it.toLong() }
        val prism = RectangularPrism(
            Point3D(x1, y1, z1),
            Point3D(x2, y2, z2)
        )
        sortedMap.computeIfAbsent(z1) { emptyList() }.let { list ->
            sortedMap[z1] = list + Brick(id++, prism)
        }
    }
    return sortedMap
}

fun RectangularPrism.restsOn(other: RectangularPrism): Boolean {
    val isOnTopOf = this.start.z == other.end.z + 1
    val areOverlappingX = (this.start.x..this.end.x intersectAsRange  other.start.x..other.end.x) != null
    val areOverlappingY = (this.start.y..this.end.y intersectAsRange  other.start.y..other.end.y) != null
    return isOnTopOf && areOverlappingX && areOverlappingY
}

fun RectangularPrism.intersects(other: RectangularPrism): Boolean {
    val areOverlappingZ = (this.start.z..this.end.z intersectAsRange  other.start.z..other.end.z) != null
    val areOverlappingX = (this.start.x..this.end.x intersectAsRange  other.start.x..other.end.x) != null
    val areOverlappingY = (this.start.y..this.end.y intersectAsRange  other.start.y..other.end.y) != null

    return areOverlappingZ && areOverlappingX && areOverlappingY
}

fun SortedMap<Long, List<RectangularPrism>>.reversed(): SortedMap<Long, List<RectangularPrism>> {
    return this.toSortedMap(this.comparator().reversed())
}
