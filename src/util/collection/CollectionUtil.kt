package util.collection

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

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

/**
 * Returns ranges collapsed, i.e. [0..5, 6..10, 12..15] -> [0..10, 11..15]
 */
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

@JvmName("intCollapseRanges")
fun collapseRanges(ranges: List<IntRange>): List<IntRange> {
    return collapseRanges(
        ranges.map {
            it.first.toLong()..it.last.toLong()
        }
    ).map {
        it.first.toInt()..it.last.toInt()
    }
}

/**
 * Maps in parallel using the default dispatcher.
 */
suspend fun <T, R> Iterable<T>.mapAsync(block: suspend (T) -> R): List<R> {
    return withContext(Dispatchers.Default) {
        map { element ->
            async {
                block(element)
            }
        }
    }.awaitAll()
}

/**
 * Returns all indexes where condition is true for the sublist of the given size.
 */
fun <T> List<T>.indexesOfSublistsWithCondition(n: Int, condition: (List<T>) -> Boolean): List<Int> {
    return windowed(n).mapIndexedNotNull { index, it ->
        if (condition(it)) index else null
    }
}

/**
 * Returns all indexes of elements that match the given condition.
 */
fun <T> List<T>.indexesOfCondition(condition: (T) -> Boolean): List<Int> {
    return mapIndexedNotNull { index, it ->
        if (condition(it)) index else null
    }
}

/**
 * Transposes an iterable of iterables.
 */
@JvmName("iterableTranspose")
fun <T> Iterable<Iterable<T>>.transpose(): List<List<T>> {
    return map { it.toList() }.fold(emptyList()) { acc, list ->
        if (acc.isEmpty()) {
            list.map { listOf(it) }
        } else {
            acc.zip(list) { a, b -> a + b }
        }
    }
}

/**
 * Transposes a list of strings because CharSequences are not Iterable<Char>.
 */
fun Iterable<String>.transpose(): List<String> {
    return map { it.asIterable() }.transpose().map { it.joinToString("") }
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
 * Repeats this string n times with the given separator.
 */
fun String.repeat(n: Int, separator: String = ""): String {
    return (1..n).joinToString(separator) { this }
}

/**
 * Returns the list of values from an iterable of indexed values.
 */
fun <T> Iterable<IndexedValue<T>>.values(): List<T> = map { it.value }

/**
 * Returns the list of indexes from an iterable of indexed values.
 */
fun <T> Iterable<IndexedValue<T>>.containsIndex(index: Int): Boolean = any { it.index == index }

inline fun <T> Iterable<T>.countLong(predicate: (T) -> Boolean): Long {
    if (this is Collection && isEmpty()) return 0
    var count = 0L
    for (element in this) if (predicate(element)) ++count
    return count
}

fun IntRange.size(): Int {
    return (this.last - this.first + 1)
}

infix fun IntRange.intersectAsRange(other: IntRange): IntRange? {
    return when {
        this.last < other.first || this.first > other.last -> {
            null
        }
        else -> {
            val start = maxOf(this.first, other.first)
            val end = minOf(this.last, other.last)
            start..end
        }
    }
}

infix fun LongRange.intersectAsRange(other: LongRange): LongRange? {
    return when {
        this.last < other.first || this.first > other.last -> {
            null
        }
        else -> {
            val start = maxOf(this.first, other.first)
            val end = minOf(this.last, other.last)
            start..end
        }
    }
}

/**
 * Returns the combinations of the two lists with the given size.
 *
 * i.e. listOf(1, 2).combinations(listOf(1, 2), size = 2) -> [[1, 1], [1, 2], [2, 2]]
 */
fun <T> Iterable<T>.combinations(size: Int): Sequence<List<T>> {
    return sequence {
        val pool = this@combinations as? List<T> ?: toList()
        val n = pool.size
        if (size > n) return@sequence
        val indices = IntArray(size) { it }
        while (true) {
            yield(indices.map { pool[it] })
            var i = size
            do {
                i--
                if (i == -1) return@sequence
            } while (indices[i] == i + n - size)
            indices[i]++
            for (j in i + 1 until size) indices[j] = indices[j - 1] + 1
        }
    }
}

/**
 * Returns all possible combinations of the elements in this iterable of all sizes.
 */
fun <T> Iterable<T>.allPossibleCombinations(min: Int= 1, max: Int? = null): Sequence<List<T>> {
    return sequence {
        val pool: List<T> = this@allPossibleCombinations as? List<T> ?: toList()
        val n = max ?: pool.size
        for (size in min..n) {
            yieldAll(pool.combinations(size))
        }
    }
}

fun <T, R> Pair<T, T>.map(block: (T) -> R): Pair<R, R> {
    return block(first) to block(second)
}

suspend fun <T> List<Deferred<T?>>.awaitFirstNotNullAndCancelRestOrNull(): T? {
    val total = AtomicInteger(this.size)
    val result = CompletableDeferred<T?>()
    CoroutineScope(Dispatchers.Default).launch {
        forEach { asyncResult ->
            launch {
                val value = asyncResult.await()
                if (value != null) {
                    result.complete(value)
                    minus(asyncResult).forEach { it.cancel() }
                } else {
                    if (total.decrementAndGet() == 0) {
                        result.complete(null)
                    }
                }
            }
        }
    }
    return result.await()
}
