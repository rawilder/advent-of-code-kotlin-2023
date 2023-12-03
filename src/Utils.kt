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
fun comparePerformance(name: String, vararg implementations: () -> Int) {
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

fun Iterable<Duration>.averageInMillis() = map { it.inWholeMilliseconds }.average()

fun Map<Int, Set<Int>>.isVisited(x: Int, y: Int) = this[y]?.contains(x) ?: false

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
}

data class Coord(val x: Int, val y: Int)
