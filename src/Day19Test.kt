import util.collection.allPossibleCombinations
import util.println
import util.shouldBe
import kotlin.random.Random
import kotlin.system.exitProcess

fun main() {
//    repeat(1000) {
//        generateTest(9, 10).also {
//            bruteForceNonDistinct(it) shouldBe it.sumOf { validRanges ->
//                validRanges.numCombinations()
//            }.toInt()
//        }
//    }

    // [PartRanges(xRange=1..2, mRange=2..2, aRange=1..2, sRange=3..3), PartRanges(xRange=2..2, mRange=1..3, aRange=1..1, sRange=2..3), PartRanges(xRange=1..3, mRange=2..3, aRange=3..3, sRange=2..3)]

    val parts1 = PartRanges(xRange=1..2, mRange=2..2, aRange=1..2, sRange=3..3)
    val parts2 = PartRanges(xRange=2..2, mRange=1..3, aRange=1..1, sRange=2..3)

    // 2..2 1..1 1..1 2..3

    // 2..2 2..2 1..1 2..2

    // 2..2 3..3 1..1 2..3

    /**
     * Part(x=2, m=1, a=1, s=2)
     * Part(x=2, m=1, a=1, s=3)
     * Part(x=2, m=2, a=1, s=2)
     * Part(x=2, m=3, a=1, s=2)
     * Part(x=2, m=3, a=1, s=3)
     */

    listOf(parts1, parts2).forEach {
        println(it.numCombinations())
    }

    bruteForceResults(listOf(parts1, parts2)).forEach {
        println(it)
    }

    println("----")

    val dedupedValidRanges = listOf(parts1, parts2).withIndex().fold(listOf(parts1, parts2).map {
        PartRanges.Many(listOf(it.xRange), listOf(it.mRange), listOf(it.aRange), listOf(it.sRange))
    }) { acc, (idx, validRange) ->
        acc.subList(0, idx + 1) + acc.drop(idx + 1).map {
            it.minusDuplicatedBounds(validRange.toMany())
        }
    }.forEach {
        println(it)
        println(it.combinations())
    }

    exitProcess(0)

    listOf(
        PartRanges(xRange=1..2, mRange=2..2, aRange=1..2, sRange=3..3),
        PartRanges(xRange=2..2, mRange=1..3, aRange=1..1, sRange=2..3),
        PartRanges(xRange=1..3, mRange=2..3, aRange=3..3, sRange=2..3)
    ).let {
        bruteForce(it).println()
        maybeSolution(it) shouldBe bruteForce(it).toLong()
    }

    while (true) {
        generateTest(2, 3).also { range ->
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

fun bruteForceResults(ranges: List<PartRanges>): List<Part> {
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

    return allCombos
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

fun maybeSolution(validRanges: List<PartRanges>): Long {
    val dedupedValidRanges = validRanges.withIndex().fold(validRanges.map {
        PartRanges.Many(listOf(it.xRange), listOf(it.mRange), listOf(it.aRange), listOf(it.sRange))
    }) { acc, (idx, validRange) ->
        acc.subList(0, idx + 1) + acc.drop(idx + 1).map {
            it.minusDuplicatedBounds(validRange.toMany())
        }
    }

    return dedupedValidRanges.sumOf {
        it.numCombinations()
    }
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
