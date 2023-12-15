import util.collection.containsIndex
import util.collection.indexesOfCondition
import util.collection.indexesOfSublistsWithCondition
import util.collection.repeat
import util.file.readInput
import util.metrics.comparePerformance
import util.println
import util.shouldBe
import kotlin.math.min

fun main() {
    fun part1(input: List<String>): Int {
        val hotSpringRecords = HotSpringRecords.fromInput(input)
        val result = hotSpringRecords.records.map { record ->
            record.possibleStates()
        }
        return result.sumOf { it.size }
    }

    fun part1Alt(input: List<String>): Long {
        val hotSpringRecords = HotSpringRecords.fromInput(input)
        return hotSpringRecords.records.sumOf { record ->
            record.numPossibleStates()
        }
    }

    fun part2(input: List<String>): Long {
        return input.sumOf {
            Record.fromLine(repeatInput(it, 5)).numPossibleStates()
        }
    }
    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day12_part1_test")
    part1(testInput) shouldBe 21
    part1Alt(testInput) shouldBe 21
    part2(testInput) shouldBe 525152

    val input = readInput("Day12")
    part1Alt(input) shouldBe 7541
    comparePerformance("part1", { part1(input) }, { part1Alt(input) })
    part2(input).println()
}

data class HotSpringRecords(
    val records: List<Record>
) {
    companion object {
        fun fromInput(input: List<String>): HotSpringRecords {
            return HotSpringRecords(
                records = input.map { Record.fromLine(it) }
            )
        }
    }
}

data class Record(
    val state: List<HotSpringState>,
    val contiguousGroupsOfDamaged: List<Int>
) {
    fun possibleStates(): List<List<HotSpringState>> {
        return possibleStatesRecursive(
            currentStates = state.potentialGroupings(contiguousGroupsOfDamaged.first(), false),
            groupSizes = contiguousGroupsOfDamaged.drop(1)
        )
    }

    fun numPossibleStates(): Long {
        return numPossibleStatesRecursive(
            inState = state,
            groupSizes = contiguousGroupsOfDamaged
        ).let {
            require(it.second.groupBy({ it }, { it.length }).none { it.value.size > 1 }) {
                "duplicate states found"
            }
            it.second.onEach {
                require(it.count { it == '#' } == contiguousGroupsOfDamaged.sum()) {
                    println()
                    "invalid state: $it"
                }
            }
            it.first
        }
    }

    private data class NumPossibleStatesCacheKey(val state: List<HotSpringState>, val groupSizes: List<Int>)
    private val numPossibleStatesCache = mutableMapOf<NumPossibleStatesCacheKey, Pair<Long, List<String>>>()


    private fun numPossibleStatesRecursive(inState: List<HotSpringState>, groupSizes: List<Int>): Pair<Long, List<String>> {
        return numPossibleStatesCache.getOrPut(NumPossibleStatesCacheKey(inState, groupSizes)) {
            val groupSize = groupSizes.first()
            val nextDamagedIdx = inState.indexOf(HotSpringState.DAMAGED).takeIf { it != -1 }
            val onlyConsider = when {
                nextDamagedIdx == null -> inState.withIndex()
                else -> inState.subList(0, min(nextDamagedIdx + groupSize, inState.size)).withIndex()
            }

            val possibleGroupings = onlyConsider.asSequence()
                .windowed(groupSize)
                // should all be damaged or unknown
                .filter { sublist -> sublist.all { it.value in setOf(HotSpringState.DAMAGED, HotSpringState.UNKNOWN) } }
                // must contain last damaged if it's the last group
                .filter { sublist -> groupSizes.size > 1 || inState.lastIndexOf(HotSpringState.DAMAGED) == -1 || sublist.containsIndex(inState.lastIndexOf(HotSpringState.DAMAGED)) }
                // must not be adjacent to damaged
                .filter { sublist ->
                    (sublist.first().index == 0 || inState[sublist.first().index - 1] != HotSpringState.DAMAGED) &&
                            (sublist.last().index == inState.size - 1 || inState[sublist.last().index + 1] != HotSpringState.DAMAGED)
                }
                .toList()
            if (groupSizes.drop(1).isEmpty()) {
                possibleGroupings.size.toLong() to possibleGroupings.map {
//                    val beforeDamaged = inState.take(max(it.first().index, 0)).symbolString()
//                    val possibleDamaged = "#".repeat(groupSize)
//                    val afterDamaged = inState.drop(it.last().index + 1).symbolString()
                    // debug
//                    "$beforeDamaged$possibleDamaged$afterDamaged"
                    ""
                }
            } else {
                val nextStates: Pair<Long, List<String>> = possibleGroupings.fold(0L to emptyList()) { acc, grouping ->
                    val leftovers = inState.drop(grouping.last().index + 2)
//                    val beforeDamaged = inState.take(max(grouping.first().index, 0)).symbolString()
//                    val possibleDamaged = "#".repeat(groupSize)
//                    val afterDamaged = inState.drop(grouping.last().index + 1).take(1).symbolString()
                    val (count, strings) = numPossibleStatesRecursive(leftovers, groupSizes.drop(1))
                    acc.first + count to emptyList() // debug acc.second + strings.map { "$beforeDamaged$possibleDamaged$afterDamaged$it" }
                }
                nextStates
            }
        }
    }

    private tailrec fun possibleStatesRecursive(currentStates: List<List<HotSpringState>>, groupSizes: List<Int>, results: List<List<HotSpringState>> = emptyList()): List<List<HotSpringState>> {
        val groupSize = groupSizes.first()
        val possibleGroupings = currentStates
            .flatMap { it.potentialGroupings(groupSize, groupSizes.drop(1).isEmpty()) }
            .distinct()
        return if (possibleGroupings.isEmpty()) {
            if (groupSizes.drop(1).isNotEmpty()) emptyList() else results
        } else if (groupSizes.drop(1).isEmpty()) {
            possibleGroupings
        } else {
            possibleStatesRecursive(
                currentStates = possibleGroupings,
                groupSizes = groupSizes.drop(1),
                results = possibleGroupings
            )
        }
    }

    private data class PossibleGroupingsCacheKey(val state: List<HotSpringState>, val n: Int, val amLastGroup: Boolean)
    private val possibleGroupingsCache = mutableMapOf<PossibleGroupingsCacheKey, List<List<HotSpringState>>>()

    private fun List<HotSpringState>.potentialGroupings(n: Int, amLastGroup: Boolean): List<List<HotSpringState>> {
        return possibleGroupingsCache.getOrPut(PossibleGroupingsCacheKey(this, n, amLastGroup)) {
            val lastMaybeDamagedIdx = this.indexOfLast { it == HotSpringState.MAYBE_DAMAGED }
            val damagedIdxs: List<Int> = this.indexesOfCondition { it == HotSpringState.DAMAGED }
            indexesOfSublistsWithCondition(n) { sublist -> condition(n, sublist) }
                .asSequence()
                // ensure we use all damaged left
                .filter { idx -> if (amLastGroup) (idx until idx + n).toList().containsAll(damagedIdxs) else true }
                // ensure we only check passed the last maybe damaged
                .filter { idx -> lastMaybeDamagedIdx == -1 || idx > lastMaybeDamagedIdx }
                // ensure we aren't adjacent to damaged or maybe damaged
                .filter { idx ->
                    this.getOrNull(idx - 1) !in setOf(
                        HotSpringState.DAMAGED,
                        HotSpringState.MAYBE_DAMAGED
                    ) && this.getOrNull(idx + n) !in setOf(HotSpringState.DAMAGED, HotSpringState.MAYBE_DAMAGED)
                }
                .let { seq ->
                    val completeGroup =
                        seq.take(1).find { idx -> this.subList(idx, idx + n).all { it == HotSpringState.DAMAGED } }
                    completeGroup?.let { sequenceOf(it) } ?: seq
                }
                .map { idx ->
                    if (idx == -1) emptyList()
                    else this.take(idx) + List(n) { HotSpringState.MAYBE_DAMAGED } + this.drop(idx + n)
                }
                .distinct()
                .toList()
        }
    }

    fun println() {
        println(state.map { it.symbol }.joinToString("") + " " + contiguousGroupsOfDamaged.joinToString(","))
    }

    companion object {
        fun fromLine(line: String): Record {
            val (state, contiguousGroupsOfDamaged) = line.split(" ")
            return Record(
                state = state.map { HotSpringState.fromSymbol(it) },
                contiguousGroupsOfDamaged = contiguousGroupsOfDamaged.split(",").map { it.toInt() }
            )
        }

        fun List<HotSpringState>.println(): List<HotSpringState> {
            println(this.map {
                when(it) {
                    HotSpringState.OK -> '.'
                    HotSpringState.DAMAGED -> '#'
                    HotSpringState.UNKNOWN -> '.'
                    HotSpringState.MAYBE_DAMAGED -> '#'
                }
            }.joinToString(""))
            return this
        }

        private fun List<HotSpringState>.symbolString(): String {
            return this.map { it.symbol }.joinToString("")
        }

        private fun condition(n: Int, sublist: List<HotSpringState>): Boolean {
            val canHoldNDamaged by lazy { sublist.count { it == HotSpringState.DAMAGED || it == HotSpringState.UNKNOWN } == n }
            val hasNoMaybeDamaged by lazy { sublist.count { it == HotSpringState.MAYBE_DAMAGED } == 0 }
            val isAllDamaged by lazy { sublist.count { it == HotSpringState.DAMAGED } == n }
            return isAllDamaged || (canHoldNDamaged && hasNoMaybeDamaged)
        }
    }
}

enum class HotSpringState(val symbol: Char) {
    OK('.'),
    DAMAGED('#'),
    UNKNOWN('?'),
    MAYBE_DAMAGED('!');

    companion object {
        fun fromSymbol(symbol: Char): HotSpringState {
            return when (symbol) {
                '.' -> OK
                '#' -> DAMAGED
                '?' -> UNKNOWN
                else -> throw IllegalArgumentException("Unknown symbol: $symbol")
            }
        }
    }
}

fun repeatInput(input: String, n: Int): String {
    val (state, groups) =  input.split(" ")
    return "${state.repeat(n, "?")} ${groups.repeat(n, ",")}"
}
