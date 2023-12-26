import util.file.readInput
import util.jetbrains.research.louvain.Link
import util.println
import util.shouldBe

/**
 * Louvain taken from https://github.com/JetBrains-Research/louvain
 */
fun main() {
    fun part1(input: List<String>): Int {
        val graph = UndirectedGraph.fromInput(input)
        val communityResults = util.jetbrains.research.louvain.getPartition(graph.toListOfLinks())
        return communityResults.entries.groupingBy { it.value }.eachCount().values.reduce(Int::times)
    }

    fun part2(input: List<String>): Int {
        return input.size
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day25_part1_test")
    part1(testInput) shouldBe 54

    val input = readInput("Day25")
    part1(input).println()
    part2(input).println()
}

data class UndirectedGraph<T>(
    val adjacencyMap: Map<T, Set<T>>
) {
    constructor() : this(emptyMap())

    fun withEdges(from: T, to: List<T>): UndirectedGraph<T> {
        val newAdjacencyMap = adjacencyMap.toMutableMap()
        newAdjacencyMap[from] = newAdjacencyMap[from].orEmpty() + to
        to.forEach { newAdjacencyMap[it] = newAdjacencyMap[it].orEmpty() + from }
        return UndirectedGraph(newAdjacencyMap)
    }

    fun toListOfLinks(): List<AsLink> {
        var id = 0
        val nodeToId = adjacencyMap.keys.associateWith { id++ }
        return adjacencyMap.flatMap { (from, tos) ->
            tos.map { to ->
                AsLink(nodeToId[from]!!, nodeToId[to]!!, 1.0)
            }
        }
    }

    companion object {
        data class AsLink(val source: Int, val target: Int, val weigh: Double) : Link {
            override fun source(): Int = source
            override fun target(): Int = target
            override fun weight(): Double = weigh
        }

        fun fromInput(input: List<String>): UndirectedGraph<String> {
            val graph = UndirectedGraph<String>()
            return input.fold(graph) { acc, line ->
                val (from, to) = line.split(": ")
                acc.withEdges(from, to.split(" "))
            }
        }
    }
}
