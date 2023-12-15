import util.file.readInput
import util.shouldBe

fun main() {
    // debugging lines i see fail
    Record.fromLine("..?##????????.?#?.? 10,2").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }
    }
    Record.fromLine("#???##?.???#? 2,3,5").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }
    }
    Record.fromLine("#??.#??.??#?#????#?# 2,1,6,3").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }
    }
    Record.fromLine("?.#??.????..?.??? 1,2").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }
    }
    Record.fromLine("??.#??#?????##? 2,1,3,4").let { r ->
        r.possibleStates().also { require(it.isNotEmpty()) }
    }

    // test part 1 for consistency and checking different implementations
    val testInput = readInput("Day12_part1_test")
    val hotSpringRecords = HotSpringRecords.fromInput(testInput)
    hotSpringRecords.records[0].possibleStates().size shouldBe 1
    hotSpringRecords.records[0].numPossibleStates() shouldBe 1
    hotSpringRecords.records[1].possibleStates().size shouldBe 4
    hotSpringRecords.records[1].numPossibleStates() shouldBe 4
    hotSpringRecords.records[2].possibleStates().size shouldBe 1
    hotSpringRecords.records[2].numPossibleStates() shouldBe 1
    hotSpringRecords.records[3].possibleStates().size shouldBe 1
    hotSpringRecords.records[3].numPossibleStates() shouldBe 1
    hotSpringRecords.records[4].possibleStates().size shouldBe 4
    hotSpringRecords.records[4].numPossibleStates() shouldBe 4
    hotSpringRecords.records[5].possibleStates().size shouldBe 10
    hotSpringRecords.records[5].numPossibleStates() shouldBe 10

    // part 2 implementation one off checks
    Record.fromLine(repeatInput("???.### 1,1,3", 5)).numPossibleStates() shouldBe 1
    Record.fromLine(repeatInput(".??..??...?##. 1,1,3", 5)).numPossibleStates() shouldBe 16384
    Record.fromLine(repeatInput("?#?#?#?#?#?#?#? 1,3,1,6", 5)).numPossibleStates() shouldBe 1
    Record.fromLine(repeatInput("????.#...#... 4,1,1", 5)).numPossibleStates() shouldBe 16
    Record.fromLine(repeatInput("????.######..#####. 1,6,5", 5)).numPossibleStates() shouldBe 2500
    Record.fromLine(repeatInput("?###???????? 3,2,1", 5)).numPossibleStates() shouldBe 506250
}
