fun main() {
    fun part1(input: List<String>): Long {
        val report = Report.fromInput(input)
        return report.history.sumOf {
            val res = it.extrapolate()
            res
        }
    }

    fun part2(input: List<String>): Long {
        val report = Report.fromInput(input)
        return report.history.sumOf {
            val res = it.extrapolateBackwards()
            res
        }
    }

    val testInput = readInput("Day09_part1_test")
    part1(testInput) shouldBe 114L
    part2(testInput) shouldBe 2L

    val input = readInput("Day09")
    part1(input).println()
    part2(input).println()
}

data class Report(
    val history: List<History>,
) {
    companion object {
        fun fromInput(input: List<String>): Report {
            return Report(
                history = input.map { s ->
                    History(
                        s.split(" ").map { it.toLong() }
                    )
                }
            )
        }
    }
}

data class History(
    val numbers: List<Long>,
) {
    fun extrapolate(): Long {
        return extrapolateRecursive(
            numbers,
            emptyList(),
            { it.last() },
            { it.sum() }
        )
    }

    fun extrapolateBackwards(): Long {
        return extrapolateRecursive(
            numbers,
            emptyList(),
            { it.first() },
            { it.reversed().reduce { acc, l -> l - acc } }
        )
    }

    private tailrec fun extrapolateRecursive(numbersIn: List<Long>, lastInHistory: List<Long>, pickNumber: (List<Long>) -> Long, collect: (List<Long>) -> Long): Long {
        require(numbersIn.isNotEmpty())
        return if (numbersIn.all { it == 0L }) {
            collect(lastInHistory)
        } else {
            val newNumbers = numbersIn.withIndex().toList().drop(1).map { (idx, num) ->
                num - numbersIn[idx - 1]
            }
            extrapolateRecursive(newNumbers, lastInHistory + pickNumber(numbersIn), pickNumber, collect)
        }
    }
}
