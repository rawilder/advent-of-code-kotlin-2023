import util.math.pow
import util.println
import util.file.readInput

fun main() {
    fun part1(input: List<String>): Long {
        return input.sumOf {
            val matches = ScratchCard.fromString(it).countOfMatchingNumbers
            if (matches == 0) 0 else pow(2, matches - 1)
        }
    }

    fun part2(input: List<String>): Int {
        val scratchCards = input.associate {
            val card = ScratchCard.fromString(it)
            card.id to card
        }

        val initialCounts = scratchCards.keys.associateWith { 1 }

        val countOfCardsWithWinnings = scratchCards.entries.fold(initialCounts) { acc, (cardId, card) ->
            val currentCount = acc[cardId] ?: 1
            acc + (cardId + 1..cardId + card.countOfMatchingNumbers).associateWith { cardIdToCopy -> (acc[cardIdToCopy]?.plus(currentCount) ?: (1 + currentCount)) }
        }

        return countOfCardsWithWinnings.values.sum()
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day04_part1_test")
    check(part1(testInput) == 13L)
    val testInput2 = readInput("Day04_part2_test")
    check(part2(testInput2) == 30)

    val input = readInput("Day04")
    part1(input).println()
    part2(input).println()
}

data class ScratchCard(
    val id: Int,
    val winningNumbers: Set<Int>,
    val myNumbers: Set<Int>,
) {
    companion object {
        fun fromString(input: String): ScratchCard {
            val (idAndWinningNumbers, myNumbers) = input.split(" | ")
            val (id, winningNumbers) = idAndWinningNumbers.split(": ")
            return ScratchCard(
                id = id.removePrefix("Card").trim().toInt(),
                winningNumbers = winningNumbers.split(" ").filter(String::isNotBlank).map { it.toInt() }.toSet(),
                myNumbers = myNumbers.split(" ").filter(String::isNotBlank).map { it.toInt() }.toSet(),
            )
        }
    }

    val countOfMatchingNumbers by lazy { myNumbers.intersect(winningNumbers).size }
}
