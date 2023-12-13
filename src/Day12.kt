import util.collection.indexesOfCondition
import util.collection.indexesOfSublistsWithCondition
import util.file.readInput
import util.println
import util.shouldBe

fun main() {
    fun part1(input: List<String>): Int {
        val hotSpringRecords = HotSpringRecords.fromInput(input)
        val result = hotSpringRecords.records.map { record ->
            val possible = record.possibleStates()
            possible.forEach {
                record.validateState(it)
            }
            println("")
            possible
        }
        return result.sumOf { it.size }
    }

    fun part2(input: List<String>): Int {
        return input.size
    }

    Record.fromLine("..?##????????.?#?.? 10,2").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }.forEach { r.validateState(it) }
    }

    Record.fromLine("#???##?.???#? 2,3,5").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }.forEach { r.validateState(it) }
    }

    Record.fromLine("#??.#??.??#?#????#?# 2,1,6,3").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }.forEach { r.validateState(it) }
    }

    Record.fromLine("?.#??.????..?.??? 1,2").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }.forEach { r.validateState(it) }
    }

    Record.fromLine("??.#??#?????##? 2,1,3,4").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }.forEach { r.validateState(it) }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day12_part1_test")

    val hotSpringRecords = HotSpringRecords.fromInput(testInput)
    hotSpringRecords.records[0].possibleStates().size shouldBe 1
    hotSpringRecords.records[1].possibleStates().size shouldBe 4
    hotSpringRecords.records[2].possibleStates().size shouldBe 1
    hotSpringRecords.records[3].possibleStates().size shouldBe 1
    hotSpringRecords.records[4].possibleStates().size shouldBe 4
    hotSpringRecords.records[5].possibleStates().size shouldBe 10
    part1(testInput) shouldBe 21

    val input = readInput("Day12")
    part1(input).println()
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

    fun validateState(stateToValidate: List<HotSpringState>) {
        println("")
        println("${this.state.map { it.symbol }.joinToString("")} ${this.contiguousGroupsOfDamaged.joinToString(",")}")
        stateToValidate.println()

        require(stateToValidate.size == state.size) { "sizes aren't the same" }

        val areCountsValid = stateToValidate.withIndex().fold(0 to emptyList<Int>()) { (count, list), (idx, state) ->
            if (state == HotSpringState.MAYBE_DAMAGED || state == HotSpringState.DAMAGED) {
                if (idx == stateToValidate.size - 1) {
                    0 to list + (count + 1)
                } else {
                    count + 1 to list
                }
            } else if (count > 0) {
                0 to list + count
            } else {
                0 to list
            }
        }.second == contiguousGroupsOfDamaged
        require(areCountsValid) { "counts are not valid" }

        require(stateToValidate.count { it == HotSpringState.MAYBE_DAMAGED || it == HotSpringState.DAMAGED } == contiguousGroupsOfDamaged.sum()) { "sums of damaged do not match" }

        state.withIndex().zip(stateToValidate).forEach { (idxAndA, b) ->
            val (idx, a) = idxAndA
            require(
                when (b) {
                    a -> true
                    HotSpringState.MAYBE_DAMAGED -> a in setOf(HotSpringState.DAMAGED, HotSpringState.UNKNOWN)
                    else -> false
                }
            ) { "$state at index $idx is not valid ($a, $b)" }
        }
        stateToValidate.withIndex().zipWithNext().forEach { (idxA, idxB) ->
            val (idx, a) = idxA
            val (_, b) = idxB
            require(
                when(a) {
                    HotSpringState.MAYBE_DAMAGED -> b != HotSpringState.DAMAGED
                    HotSpringState.DAMAGED -> b != HotSpringState.MAYBE_DAMAGED
                    else -> true
                }
            ) { "$state at index $idx is not valid ($a, $b)" }
        }
    }

    fun possibleStates(): List<List<HotSpringState>> {
        return possibleStatesRecursive(
            currentStates = state.potentialGroupings(contiguousGroupsOfDamaged.first(), false),
            groupSizes = contiguousGroupsOfDamaged.drop(1)
        )
    }

    private tailrec fun possibleStatesRecursive(currentStates: List<List<HotSpringState>>, groupSizes: List<Int>, results: List<List<HotSpringState>> = emptyList()): List<List<HotSpringState>> {
        val groupSize = groupSizes.first()
        val possibleGroupings = currentStates
            .filter { it.canHaveGrouping(groupSize) }
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

        private fun List<HotSpringState>.potentialGroupings(n: Int, amLastGroup: Boolean): List<List<HotSpringState>> {
            val lastMaybeDamagedIdx = this.indexOfLast { it == HotSpringState.MAYBE_DAMAGED }
            val damagedIdxs: List<Int> = this.indexesOfCondition { it == HotSpringState.DAMAGED }
            return indexesOfSublistsWithCondition(n) { sublist -> condition(n, sublist) }
                .asSequence()
                // ensure we use all damaged left
                .filter { idx -> if (amLastGroup) (idx until idx+n).toList().containsAll(damagedIdxs) else true }
                // ensure we only check passed the last maybe damaged
                .filter { idx -> lastMaybeDamagedIdx == -1 || idx > lastMaybeDamagedIdx }
                // ensure we aren't adjacent to damaged or maybe damaged
                .filter { idx -> this.getOrNull(idx - 1) !in setOf(HotSpringState.DAMAGED, HotSpringState.MAYBE_DAMAGED) && this.getOrNull(idx + n) !in setOf(HotSpringState.DAMAGED, HotSpringState.MAYBE_DAMAGED) }
                .let { seq ->
                    val completeGroup = seq.take(1).find { idx -> this.subList(idx, idx + n).all { it == HotSpringState.DAMAGED } }
                    completeGroup?.let { sequenceOf(it) } ?: seq
                }
                .map { idx ->
                    if (idx == -1) emptyList()
                    else this.take(idx) + List(n) { HotSpringState.MAYBE_DAMAGED } + this.drop(idx + n)
                }
                .distinct()
                .toList()
        }

        private fun List<HotSpringState>.canHaveGrouping(n: Int): Boolean {
            val lastMaybeDamaged = this.indexOfLast { it == HotSpringState.MAYBE_DAMAGED }
            return indexesOfSublistsWithCondition(n) { sublist -> condition(n, sublist) }
                .filter { idx -> lastMaybeDamaged == -1 || idx > lastMaybeDamaged }
                .any { idx ->
                    idx != -1 && this.getOrNull(idx - 1) !in setOf(HotSpringState.DAMAGED, HotSpringState.MAYBE_DAMAGED) && this.getOrNull(idx + n) !in setOf(HotSpringState.DAMAGED, HotSpringState.MAYBE_DAMAGED)
                }
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
