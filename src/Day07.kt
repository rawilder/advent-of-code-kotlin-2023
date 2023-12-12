import util.println
import util.file.readInput

fun main() {
    fun part1(input: List<String>): Long {
        val hands = input.map { CamelCardHand.fromString(it) }.sorted()
        return hands.withIndex().sumOf { (idx, hand) -> hand.bet * (idx + 1) }
    }

    fun part2(input: List<String>): Long {
        val hands = input.map { CamelCardHand.fromString(it, isWildcardActive = true) }.sorted()
        return hands.withIndex().sumOf { (idx, hand) -> hand.bet * (idx + 1) }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day07_part1_test")
    check(part1(testInput) == 6440L)
    val testInput2 = readInput("Day07_part1_test")
    check(part2(testInput2) == 5905L)

    val input = readInput("Day07")
    part1(input).println()
    part2(input).println()
}

data class CamelCardHand(
    val cards: List<CamelCard>,
    val bet: Long,
    val isWildcardActive: Boolean = false
): Comparable<CamelCardHand> {
    init {
        require(cards.size == 5) { "A hand must have 5 cards" }
    }

    val hasWildcard by lazy { isWildcardActive && CamelCard.JWC in cards }

    override fun compareTo(other: CamelCardHand): Int {
        val thisType = CamelCardHandType.fromHand(this, isWildcardActive)
        val otherType = CamelCardHandType.fromHand(other, isWildcardActive)
        return when (val whichTypeWins = thisType.compareTo(otherType)) {
            0 -> {
                this.cards.zip(other.cards).firstNotNullOfOrNull { it.first.compareTo(it.second).takeIf { c -> c != 0 } } ?: 0
            }
            else -> whichTypeWins
        }
    }

    companion object {
        fun fromString(s: String, isWildcardActive: Boolean = false): CamelCardHand {
            val (cards, bet) = s.split(" ")
            val cardsList = cards.map { CamelCard.fromString(it.toString(), isWildcardActive) }
            val betValue = bet.toLong()
            return CamelCardHand(cardsList, betValue, isWildcardActive)
        }
    }
}

sealed class CamelCard(val value: Int): Comparable<CamelCard> {
    override fun compareTo(other: CamelCard): Int {
        return this.value.compareTo(other.value)
    }

    // Joker WildCard
    data object JWC: CamelCard(1)
    data object N2: CamelCard(2)
    data object N3: CamelCard(3)
    data object N4: CamelCard(4)
    data object N5: CamelCard(5)
    data object N6: CamelCard(6)
    data object N7: CamelCard(7)
    data object N8: CamelCard(8)
    data object N9: CamelCard(9)
    data object T: CamelCard(10)
    data object J: CamelCard(11)
    data object Q: CamelCard(12)
    data object K: CamelCard(13)
    data object A: CamelCard(14)

    companion object {
        fun fromString(s: String, isWildcardActive: Boolean): CamelCard {
            return when (s) {
                "2" -> N2
                "3" -> N3
                "4" -> N4
                "5" -> N5
                "6" -> N6
                "7" -> N7
                "8" -> N8
                "9" -> N9
                "T" -> T
                "J" -> if (isWildcardActive) JWC else J
                "Q" -> Q
                "K" -> K
                "A" -> A
                else -> throw IllegalArgumentException("Unknown card value: $s")
            }
        }
    }
}

enum class CamelCardHandType: Comparable<CamelCardHandType> {
    HighCard, // No other hand applies
    OnePair, // 2 cards of the same value
    TwoPair, // 2 cards of the same value, with another 2 cards of the same value
    ThreeOfAKind, // 3 cards of the same value
    FullHouse, // 3 cards of the same value, with the remaining 2 cards forming a pair
    FourOfAKind, // 4 cards of the same value
    FiveOfAKind; // 5 cards of the same value

    companion object {
        fun fromHand(hand: CamelCardHand, isWildcardActive: Boolean): CamelCardHandType {
            return if (isWildcardActive && hand.hasWildcard) {
                evaluateHandConsideringWildcards(hand)
            } else {
                evaluateHand(hand)
            }
        }

        private fun evaluateHand(hand: CamelCardHand): CamelCardHandType {
            val cardsByValue = hand.cards.groupBy { it.value }
            val cardCounts = cardsByValue.map { it.value.size }.sortedDescending()
            return when (cardCounts) {
                listOf(5) -> FiveOfAKind
                listOf(4, 1) -> FourOfAKind
                listOf(3, 2) -> FullHouse
                listOf(3, 1, 1) -> ThreeOfAKind
                listOf(2, 2, 1) -> TwoPair
                listOf(2, 1, 1, 1) -> OnePair
                else -> HighCard
            }
        }

        private fun evaluateHandConsideringWildcards(hand: CamelCardHand): CamelCardHandType {
            val numWildcards = hand.cards.count { it == CamelCard.JWC }
            val cardCounts = hand.cards.filter { it != CamelCard.JWC }.groupBy { it.value }.map { it.value.size }.sortedDescending()
            return when (cardCounts) {
                emptyList<Int>() -> FiveOfAKind
                listOf(5 - numWildcards) -> FiveOfAKind
                listOf(4 - numWildcards, 1) -> FourOfAKind
                listOf(3 - numWildcards, 2) -> FullHouse
                listOf(3 - numWildcards, 1, 1) -> ThreeOfAKind
                listOf(1, 1, 1, 1) -> OnePair
                listOf(1, 1, 1) -> OnePair
                else -> HighCard
            }
        }
    }
}
