package util.math

import kotlin.math.max
import kotlin.math.min

/**
 * return the greatest common divisor of two numbers
 */
tailrec fun Long.gcd(other: Long): Long {
    return if (other == 0L) {
        this
    } else {
        min(this, other).gcd(max(this, other) % min(this, other))
    }
}

/**
 * return the least common multiple of a list of numbers
 */
fun List<Long>.lcm(): Long {
    return reduce { acc, l -> acc.lcm(l) }
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
 * return the least common multiple of two numbers
 */
fun Long.lcm(other: Long): Long {
    return this * other / this.gcd(other)
}
