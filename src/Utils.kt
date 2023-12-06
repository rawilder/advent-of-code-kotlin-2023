import java.math.BigInteger
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * Reads lines from the given input txt file.
 */
fun readInput(name: String) = Path("src/$name.txt").readLines()

/**
 * Reads lines from the given filename.
 */
fun readFile(name: String) = Path("src/$name").readLines()

/**
 * Converts string to md5 hash.
 */
fun String.md5() = BigInteger(1, MessageDigest.getInstance("MD5").digest(toByteArray()))
    .toString(16)
    .padStart(32, '0')

/**
 * The cleaner shorthand for printing output.
 */
fun Any?.println() = println(this)

/**
 * Returns a sequence of substrings, stepping by one character, and yielding substrings of the given sizes in the order
 * the sizes are provided.
 */
fun CharSequence.windowedSequenceVariableSize(sizes: List<Int>, step: Int = 1): Sequence<String> {
    return sequence {
        var pos = 0
        while (pos < length) {
            sizes.forEach { size ->
                if (pos + size <= length) {
                    yield(subSequence(pos, pos + size).toString())
                }
            }
            pos += step
        }
    }
}

/**
 * Returns substring or null if the given indices are out of bounds.
 */
fun CharSequence.substringOrNull(startIndex: Int, endIndex: Int): String? {
    return if (startIndex < 0 || endIndex > length || startIndex > endIndex) {
        null
    } else {
        substring(startIndex, endIndex)
    }
}

/**
 * Compares performance specifically in the context of Advent of Code.
 */
fun comparePerformance(name: String, vararg implementations: () -> Number) {
    implementations.withIndex().forEach { (idx, implementation) ->
        measureTimedValue {
            implementation()
        }.also {
            println("$name implementation ${idx + 1} took ${it.duration}; result: ${it.value}")
        }
    }
}

/**
 * Converts a list of strings into a matrix of characters.
 */
fun matrixFromStringList(input: List<String>): List<List<Char>> {
    return input.map { line ->
        line.toList()
    }
}

/**
 * Int power function for positive exponents.
 * Returns the result as Long to avoid overflow.
 */
fun pow(base: Int, exponent: Int): Long {
    require(exponent >= 0)
    tailrec fun recursivePow(base: Long, exponent: Long, result: Long): Long {
        return when {
            exponent == 0L -> result
            else -> recursivePow(base, exponent - 1, result * base)
        }
    }

    return recursivePow(base.toLong(), exponent.toLong(), 1)
}

/**
 * Returns the result of the block if the condition is true, otherwise null.
 */
inline fun <reified T> (() -> T).takeIf(condition: Boolean): T? {
    return if (condition) {
        this()
    } else null
}

/**
 * Returns if this range is before the other range with no overlap.
 */
fun LongRange.liesBefore(other: LongRange): Boolean {
    return this.last < other.first
}

/**
 * Returns if this range is after the other range with no overlap.
 */
fun LongRange.liesAfter(other: LongRange): Boolean {
    return this.first > other.last
}

/**
 * Returns if this range overlaps with the other range, whether entirely, partially, or is a subrange of the other.
 */
fun LongRange.overlaps(other: LongRange): Boolean {
    return when {
        this.last < other.first || this.first > other.last -> {
            false
        }
        else -> {
            true
        }
    }
}

/**
 * Returns the intersection of this range with the other range, or null if there is no intersection.
 *
 * It limits the intersections to the minimum bounds, i.e. (0..5).intersectsIn(4..10) -> Pair(4, 5)
 */
fun LongRange.intersectionsIn(other: LongRange): Pair<Long, Long>? {
    // should return a pair (start, end) of where this range intersects with other range
    // [0..5].intersectsIn(4..10) -> null..5
    // [0..5].intersectsIn(2..3) -> 2..3
    return when {
        this.last < other.first || this.first > other.last -> {
            null
        }
        else -> {
            val start = maxOf(this.first, other.first)
            val end = minOf(this.last, other.last)
            start to end
        }
    }
}

fun collapseRanges(ranges: List<LongRange>): List<LongRange> {
    val sortedRanges = ranges.sortedBy { it.first }
    val result = sortedRanges.fold(emptyList<LongRange>()) { acc, range ->
        if (acc.isEmpty()) {
            listOf(range)
        } else {
            when {
                range.first > acc.last().last + 1 -> {
                    acc + listOf(range)
                }
                range.last > acc.last().last -> {
                    acc.dropLast(1) + listOf(acc.last().first..range.last)
                }
                else -> {
                    acc
                }
            }
        }
    }
    return result
}

/**
 * Returns the average of the given durations in milliseconds.
 */
fun Iterable<Duration>.averageInMillis() = map { it.inWholeMilliseconds }.average()

object Utils {
    val digitStringsToInts = mapOf(
        "one" to 1,
        "two" to 2,
        "three" to 3,
        "four" to 4,
        "five" to 5,
        "six" to 6,
        "seven" to 7,
        "eight" to 8,
        "nine" to 9,
    )

    val whitespaceRegex = Regex("\\s+")
}

data class Coord(val x: Int, val y: Int)
