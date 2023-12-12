import util.collection.collapseRanges
import util.metrics.comparePerformance
import util.collection.intersectionsIn
import util.collection.liesBefore
import util.collection.overlaps
import util.println
import util.file.readInput
import util.shouldBe
import util.takeIf

fun main() {
    fun part1(input: List<String>): Long {
        val almanac = parseAlmanac(input)
        val locations = almanac.seeds.map {
            almanac.maps.entries.fold(it) { acc, (_, map) ->
                map.destinationValueForSource(acc)
            }
        }
        return locations.min()
    }

    fun part2(input: List<String>): Long {
        val almanac = parseAlmanac(input)
        val locationRanges = almanac.finalDestinationRanges()
        return locationRanges.minOf { it.first }
    }

    fun part2Functional(input: List<String>): Long {
        val almanac = parseAlmanac(input)
        val locationRanges = almanac.finalDestinationRangesFunctional()
        return locationRanges.minOf { it.first }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day05_part1_test")
    part1(testInput) shouldBe 35L
    val testInput2 = readInput("Day05_part2_test")
    part2(testInput2) shouldBe 46L

    val input = readInput("Day05")
    part1(input).println()
    comparePerformance("part2", { part2(input) }, { part2Functional(input) })
}

data class Almanac(
    val seeds: List<Long>,
    val maps: Map<Pair<String, String>, AlmanacMap>
) {
    val seedRanges: List<LongRange> = seeds.chunked(2).map {
        it.first() until (it.first() + it.last())
    }

    fun finalDestinationRanges(): List<LongRange> {
        return maps.entries.fold(seedRanges) { acc, (_, map) ->
            val result = map.destinationRangesForSourceRanges(acc)
            result
        }
    }

    fun finalDestinationRangesFunctional(): List<LongRange> {
        return maps.entries.fold(seedRanges) { acc, (_, map) ->
            val result = map.destinationRangesForSourceRangesFunctional(acc)
            result
        }
    }
}

data class AlmanacMap(
    val ranges: List<AlmanacRange>,
) {

    private val sortedRanges by lazy { ranges.sortedBy { it.sourceRangeStart } }

    fun destinationValueForSource(source: Long): Long {
        return ranges.firstNotNullOfOrNull {
            it.destinationValueForSource(source)
        } ?: source
    }

    fun destinationRangesForSourceRanges(inputRanges: List<LongRange>): List<LongRange> {
        val inputQueue = inputRanges.sortedBy { it.first }.toMutableList()
        return collapseRanges(sortedRanges.flatMap {  almanacRange ->
            val destinationRanges = mutableListOf<LongRange>()
            while ((inputQueue.firstOrNull()?.last ?: Long.MAX_VALUE) < almanacRange.sourceRange.first) {
                destinationRanges.add(inputQueue.removeFirst())
            }
            while (inputQueue.firstOrNull()?.overlaps(almanacRange.sourceRange) == true) {
                val inputRange = inputQueue.removeFirst()
                inputRange.intersectionsIn(almanacRange.sourceRange)?.let { (intersectStart, intersectEnd) ->
                    if (inputRange.first < intersectStart) {
                        destinationRanges.add(inputRange.first until intersectStart)
                    }
                    if (inputRange.last > intersectEnd) {
                        inputQueue.add(intersectEnd + 1..inputRange.last)
                    }
                    destinationRanges.add(almanacRange.destinationValueForSource(intersectStart)!!..almanacRange.destinationValueForSource(intersectEnd)!!)
                } ?: run {
                    // since it is sorted we know that this inputRange is not going to intersect with any other almanacRange
                    if (inputRange.last < almanacRange.sourceRange.first) {
                        destinationRanges.add(inputRange)
                    }
                    if (inputRange.first > almanacRange.sourceRange.last) {
                        inputQueue.add(inputRange)
                    }
                }
                inputQueue.sortBy { it.first }
            }
            inputQueue.sortBy { it.first }
            destinationRanges
        } + inputQueue)
    }

    fun destinationRangesForSourceRangesFunctional(inputRanges: List<LongRange>): List<LongRange> {
        return destinationRangesForSourceRangesFunctionalRecursive(inputRanges)
    }

    private tailrec fun destinationRangesForSourceRangesFunctionalRecursive(inputRanges: List<LongRange>, result: List<LongRange> = emptyList()): List<LongRange> {
        val inputRangesSorted = inputRanges.sortedBy { it.first }
        return if (inputRangesSorted.isEmpty()) {
            collapseRanges(result)
        } else {
            val inputRange = inputRangesSorted.first()
            val (newInputs, newResults) = sortedRanges.firstNotNullOfOrNull { almanacRange ->
                if (inputRange.overlaps(almanacRange.sourceRange)) {
                    inputRange.intersectionsIn(almanacRange.sourceRange)?.let { (intersectStart, intersectEnd) ->
                        val newResults = listOfNotNull(
                            { inputRange.first until intersectStart }.takeIf(inputRange.first < intersectStart),
                            almanacRange.destinationValueForSource(intersectStart)!!..almanacRange.destinationValueForSource(
                                intersectEnd
                            )!!
                        )
                        val newInputRanges = inputRangesSorted.drop(1) + listOfNotNull(
                            { intersectEnd + 1..inputRange.last }.takeIf(inputRange.last > intersectEnd)
                        )
                        newInputRanges to (result + newResults)
                    }
                } else {
                    when {
                        inputRange.liesBefore(almanacRange.sourceRange) -> inputRangesSorted.drop(1) to result + listOf(
                            inputRange
                        )

                        else -> null
                    }
                }
            } ?: (inputRangesSorted.drop(1) to result + listOf(inputRange))
            destinationRangesForSourceRangesFunctionalRecursive(newInputs, newResults)
        }

    }
}

data class AlmanacRange(
    val destinationRangeStart: Long,
    val sourceRangeStart: Long,
    val rangeLength: Long,
) {
    val sourceRange = sourceRangeStart until (sourceRangeStart + rangeLength)

    fun destinationValueForSource(source: Long): Long? {
        return if (source in sourceRangeStart until sourceRangeStart + rangeLength) {
            destinationRangeStart + (source - sourceRangeStart)
        } else null
    }
}

fun parseAlmanac(input: List<String>): Almanac {
    val seeds = input.first().replace("seeds: ", "").split(" ").map { it.toLong() }
    val maps = parseAlmanacMaps(input.drop(2))
    return Almanac(seeds, maps)
}

tailrec fun parseAlmanacMaps(input: List<String>, maps: Map<Pair<String, String>, AlmanacMap> = emptyMap()): Map<Pair<String, String>, AlmanacMap> {
    val mapStartRegex = Regex(".* map:")
    val pairRegex = Regex("(.*)-to-(.*) map:")
    val nextMap = input.dropWhile { !it.matches(mapStartRegex) }
    val nextMapInput = nextMap.drop(1)
        .takeWhile { !it.matches(mapStartRegex) }
        .filter { it.isNotBlank() }
    return if (nextMapInput.isEmpty()) {
        maps
    } else {
        val (source, destination) = pairRegex.matchEntire(nextMap.first())!!.destructured
        parseAlmanacMaps(input.drop(nextMapInput.size + 1), maps + ((source to destination) to parseAlmanacMap(nextMapInput)))
    }
}

fun parseAlmanacMap(input: List<String>) = AlmanacMap(
    input.map {
        val nums = it.split(" ")
        AlmanacRange(
            destinationRangeStart = nums[0].toLong(),
            sourceRangeStart = nums[1].toLong(),
            rangeLength = nums[2].toLong(),
        )
    }
)
