import kotlin.time.measureTime

fun main() {
    fun part1(input: List<String>): Int {
        return input.sumOf {
            val firstDigit = it.first(Char::isDigit)
            val lastDigit = it.last(Char::isDigit)
            "$firstDigit$lastDigit".toInt()
        }
    }

    fun part2(input: List<String>): Int {
        val replacedInput = input.map { line ->
            line.windowedSequenceVariableSize(listOf(1, 3, 4, 5))
                .map {
                    when {
                        it.length == 1 && it.first().isDigit() -> it
                        else -> Utils.digitStringsToInts[it] ?: ""
                    }
                }
                .joinToString("")
        }

        return part1(replacedInput)
    }

    fun part2Optimized(input: List<String>): Int {
        val replacedInput = input.map { line ->
            var pos = 0
            var result = StringBuilder()
            while (pos < line.length) {
                when {
                    line[pos].isDigit() -> {
                        result.append(line[pos])
                    }
                    else -> {
                        line.substringOrNull(pos, pos + 3)?.let {
                            Utils.digitStringsToInts[it]?.let { digit ->
                                result.append(digit)
                            }
                        }
                        line.substringOrNull(pos, pos + 4)?.let {
                            Utils.digitStringsToInts[it]?.let { digit ->
                                result.append(digit)
                            }
                        }
                        line.substringOrNull(pos, pos + 5)?.let {
                            Utils.digitStringsToInts[it]?.let { digit ->
                                result.append(digit)
                            }
                        }
                    }
                }
                pos += 1
            }
            result.toString()
        }
        return part1(replacedInput)
    }

    // test if implementation meets criteria from the description, like:
    val part1TestInput = readInput("Day01_part1_test")
    check(part1(part1TestInput) == 142)

    val part2TestInput = readInput("Day01_part2_test")
    check(part2Optimized(part2TestInput) == 281)

    val input = readInput("Day01")
    part1(input).println()
    val part2Time = measureTime { part2(input).println() }.also {
        println("part2: $it")
    }
    val part2OptimizedTime = measureTime { part2Optimized(input).println() }.also {
        println("part2Optimized: $it")
    }
    println("part2Optimized is ${part2Time / part2OptimizedTime} times faster")
}
