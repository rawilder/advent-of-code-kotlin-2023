package util

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Converts string to md5 hash.
 */
fun String.md5() = BigInteger(1, MessageDigest.getInstance("MD5").digest(toByteArray()))
    .toString(16)
    .padStart(32, '0')

/**
 * The cleaner shorthand for printing output.
 */
fun <T> T.println(): T = this.also { println(it) }

/**
 * Returns the result of the block if the condition is true, otherwise null.
 */
inline fun <reified T> (() -> T).takeIf(condition: Boolean): T? {
    return if (condition) {
        this()
    } else null
}

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
