import util.shouldBe
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.system.exitProcess

suspend fun main() {
//    repeat(1000) {
//        val minX = Random.nextInt(1, 100 + 1)
//        val maxX = Random.nextInt(minX, 100 + 1)
//        val minM = Random.nextInt(max(minX - 100, 0), 100 + 1)
//        val maxM = Random.nextInt(minM, 100 + 1)
//
//        val range = minX..maxX
//        val range2 = minM..maxM
//
//        (range.intersectAsRange(range2)?.toSet() ?: emptySet()) shouldBe range.intersect(range2)
//    }
//
//    repeat(1000) {
//        generateTest(9, 10).also {
//            bruteForceNonDistinct(it) shouldBe it.sumOf { validRanges ->
//                validRanges.numCombinations()
//            }.toInt()
//        }
//    }

    listOf(
        PartRanges(xRange=1..1, mRange=1..2, aRange=1..2, sRange=2..2),
        PartRanges(xRange=1..1, mRange=2..2, aRange=1..1, sRange=2..2),
        PartRanges(xRange=1..1, mRange=2..2, aRange=1..1, sRange=2..2)
    ).let {
        maybeSolution(it) shouldBe bruteForce(it).toLong()
    }

    data class TestResult(val x: Set<Int>, val m: Set<Int>, val a: Set<Int>, val s: Set<Int>) {
        fun anyNotEmpty(): Boolean {
            return x.isNotEmpty() || m.isNotEmpty() || a.isNotEmpty() || s.isNotEmpty()
        }
    }

    while (true) {
        generateTest(4, 4).also { range ->
            runCatching {
                maybeSolution(range) shouldBe bruteForce(range).toLong()
            }.onFailure {
                println(range)
                exitProcess(1)
            }
        }
    }

    repeat(100) {
        generateTest(9, 100).also {
            maybeSolution(it) shouldBe bruteForce(it).toLong()
        }
    }
}

fun bruteForceNonDistinct(ranges: List<PartRanges>): Int {
    val allCombos = ranges.flatMap { (xR, mR, aR, sR) ->
        xR.flatMap {  x ->
            mR.flatMap { m ->
                aR.flatMap { a ->
                    sR.map { s ->
                        Part(x, m, a, s)
                    }
                }
            }
        }
    }

    return allCombos.size
}

fun bruteForce(ranges: List<PartRanges>): Int {
    val allCombos = ranges.flatMap { (xR, mR, aR, sR) ->
        xR.flatMap {  x ->
            mR.flatMap { m ->
                aR.flatMap { a ->
                    sR.map { s ->
                        Part(x, m, a, s)
                    }
                }
            }
        }
    }.distinct()

    return allCombos.size
}

suspend fun maybeSolution(ranges: List<PartRanges>): Long {
    val mathSum = ranges.sumOf { validRanges ->
        validRanges.numCombinations()
    }

    val visited = ConcurrentHashMap.newKeySet<PartRanges>()
    val dupeCount = ranges.withIndex().map { (index, partRanges) ->
        partRanges.allCombinations().fold(0L) { totalCount, part ->
            if (visited.any { it.contains(part) }) return@fold totalCount
            val counts = countPartsInOtherRanges(index to part, ranges)
            if (counts > 0) {
                totalCount + counts
            } else {
                totalCount
            }
        }.also {
            visited.add(partRanges)
            }
    }.sum()

    return mathSum - dupeCount
}

fun partInOtherRanges(part: Pair<Int, Part>, invalidRanges: List<PartRanges>): Boolean {
    val (originIndex, partToCheck) = part
    return invalidRanges.withIndex().any { (index, partRanges) -> index != originIndex && partRanges.contains(partToCheck) }
}

fun countPartsInOtherRanges(part: Pair<Int, Part>, invalidRanges: List<PartRanges>): Int {
    val (originIndex, partToCheck) = part
    return invalidRanges.filterIndexed { index, partRanges -> index != originIndex && partRanges.contains(partToCheck) }.count()
}

fun PartRanges.contains(part: Part): Boolean {
    return part.x in xRange && part.m in mRange && part.a in aRange && part.s in sRange
}

fun PartRanges.allCombinations(): Sequence<Part> {
    return sequence {
        xRange.forEach { x ->
            mRange.forEach { m ->
                aRange.forEach { a ->
                    sRange.forEach { s ->
                        yield(Part(x, m, a, s))
                    }
                }
            }
        }
    }
}

fun generateTest(maxListSize: Int, maxRange: Int): List<PartRanges> {
    require(maxListSize >= 2)
    return (0..Random.nextInt(2, maxListSize + 1)).map {
        val minX = Random.nextInt(1, maxRange + 1)
        val maxX = Random.nextInt(minX, maxRange + 1)
        val minM = Random.nextInt(1, maxRange + 1)
        val maxM = Random.nextInt(minM, maxRange + 1)
        val minA = Random.nextInt(1, maxRange + 1)
        val maxA = Random.nextInt(minA, maxRange + 1)
        val minS = Random.nextInt(1, maxRange + 1)
        val maxS = Random.nextInt(minS, maxRange + 1)
        PartRanges(
            minX..maxX,
            minM..maxM,
            minA..maxA,
            minS..maxS
        )
    }
}
