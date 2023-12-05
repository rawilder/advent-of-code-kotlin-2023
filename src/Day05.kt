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

    fun part2(input: List<String>): Int {
        return input.size
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day05_part1_test")
    check(part1(testInput) == 35L)

    val input = readInput("Day05")
    part1(input).println()
    part2(input).println()
}

data class Almanac(
    val seeds: List<Long>,
    val maps: Map<Pair<String, String>, AlmanacMap>
)

data class AlmanacMap(
    val ranges: List<AlmanacRange>,
) {
    fun destinationValueForSource(source: Long): Long {
        return ranges.firstNotNullOfOrNull {
            it.destinationValueForSource(source)
        } ?: source
    }
}

data class AlmanacRange(
    val destinationRangeStart: Long,
    val sourceRangeStart: Long,
    val rangeLength: Long,
) {
    fun destinationValueForSource(source: Long): Long? {
        return if (source in sourceRangeStart..sourceRangeStart + rangeLength) {
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
