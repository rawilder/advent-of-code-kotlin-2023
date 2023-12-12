package util.metrics

import kotlin.time.Duration
import kotlin.time.measureTimedValue

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
 * Returns the average of the given durations in milliseconds.
 */
fun Iterable<Duration>.averageInMillis() = map { it.inWholeMilliseconds }.average()
fun Map<String, List<Duration>>.printAverageInMillis() {
    println("Average times:")
    forEach { (name, durations) ->
        println("$name: ${durations.averageInMillis()}ms / ${durations.size}")
    }
}
