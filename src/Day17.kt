import util.println
import util.file.readInput
import util.geometry.Direction
import util.geometry.Point2D
import util.shouldBe

fun main() {
    fun part1(input: List<String>): Long {
        val city = City.fromInput(input)
        return city.lowestWeightPath(
            Point2D(0, 0),
            Point2D(city.maxX, city.maxY)
        ) { _, neighborKey ->
            neighborKey.numBlocks <= 3
        }
    }

    fun part2(input: List<String>): Long {
        val city = City.fromInput(input)
        return city.lowestWeightPath(
            Point2D(0, 0),
            Point2D(city.maxX, city.maxY)
        ) { currentKey, neighborKey ->
            when {
                neighborKey.position == Point2D(city.maxX, city.maxY) -> neighborKey.numBlocks in 4..10
                neighborKey.direction == currentKey.direction -> neighborKey.numBlocks <= 10
                else -> currentKey.numBlocks >= 4
            }
        }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day17_part1_test")
    part1(testInput) shouldBe 102L
    part2(testInput) shouldBe 94L
    val part2TestInput = readInput("Day17_part2_test")
    part2(part2TestInput) shouldBe 71L

    val input = readInput("Day17")
    part1(input).println()
    part2(input).println()
}

data class City(
    val blocks: Map<Point2D, CityBlock>
) {

    val maxX = blocks.keys.maxOf { it.x }
    val maxY = blocks.keys.maxOf { it.y }

    fun lowestWeightPath(source: Point2D, destination: Point2D, neighborFilter: (StateKey, StateKey) -> Boolean): Long {
        return aStarSearch(source, destination, neighborFilter)
    }

    data class StateKey(
        val position: Point2D,
        val direction: Direction,
        val numBlocks: Int,
    )

    private fun aStarSearch(source: Point2D, destination: Point2D, neighborFilter: (StateKey, StateKey) -> Boolean): Long {
        val openSet = mutableSetOf(StateKey(source, Direction.EAST, 0))
        val closedSet = mutableSetOf<StateKey>()
        val cameFrom = mutableMapOf<StateKey, StateKey>()
        val gScore = mutableMapOf<StateKey, Long>().withDefault { Long.MAX_VALUE }
        val fScore = mutableMapOf<StateKey, Long>().withDefault { Long.MAX_VALUE }
        Direction.entries.forEach { direction ->
            if (source.move(direction) in blocks) {
                val key = StateKey(source, direction, 0)
                gScore[key] = 0
                fScore[key] = heuristicCostEstimate(source, destination)
            }
        }

        while (openSet.isNotEmpty()) {
            val currentKey = openSet.minByOrNull { fScore.getValue(it) }!!
            val (current, direction) = currentKey
            if (current == destination) {
                printPathTo(currentKey, cameFrom)
                return gScore.getValue(currentKey)
            }
            openSet.remove(currentKey)

            val neighbors = current.neighbors()
                .map {
                    val neighborDirection = current.directionTo(it)
                    StateKey(it, neighborDirection, if (neighborDirection == direction) currentKey.numBlocks + 1 else 1)
                }
                .filter { neighborKey ->
                    val (neighborPoint, neighborDirection, neighborNumBlocks) = neighborKey
                    blocks.containsKey(neighborPoint)
                            && neighborDirection != direction.turnAround()
                            && neighborKey != cameFrom[neighborKey]
                            && neighborKey !in closedSet
                            && neighborFilter(currentKey, neighborKey)
                }
            for (neighborKey in neighbors) {
                val (neighborPoint, _) = neighborKey
                val tentativeGScore = gScore.getValue(currentKey) + blocks.getValue(neighborPoint).weight
                val neighborGScore = gScore.getValue(neighborKey)
                if (tentativeGScore < neighborGScore) {
                    cameFrom[neighborKey] = currentKey
                    gScore[neighborKey] = tentativeGScore
                    fScore[neighborKey] = tentativeGScore + heuristicCostEstimate(neighborPoint, destination)
                    openSet.add(neighborKey)
                }
            }
            closedSet.add(currentKey)
        }
        throw IllegalStateException("no path found")
    }

    private fun printCity(highlight: Point2D? = null) {
        // Everything after this is in red
        val red = "\u001b[31m"

        // Resets previous color codes
        val reset = "\u001b[0m"
        blocks.entries.sortedBy { it.key.x }.sortedBy { it.key.y }.forEach {
            if (highlight == it.key) {
                print(red)
            }
            print(it.value.weight)
            if (highlight == it.key) {
                print(reset)
            }
            if (it.key.x == maxX) {
                println("")
            }
        }
        println("")
    }

    private fun printPathTo(destinationKey: StateKey, cameFrom: Map<StateKey, StateKey>) {
        var current = destinationKey
        val path = mutableListOf<Point2D>()
        while (current in cameFrom) {
            path.add(current.position)
            current = cameFrom.getValue(current)
        }
        blocks.entries.sortedBy { it.key.x }.sortedBy { it.key.y }.forEach {
            if (it.key in path) {
                print("#")
            } else {
                print('.')
            }
            if (it.key.x == maxX) {
                println("")
            }
        }
        println("")
    }

    private fun heuristicCostEstimate(source: Point2D, destination: Point2D): Long {
        return source.distanceToInAMatrix(destination)
    }

    companion object {
        fun fromInput(input: List<String>): City {
            val blocks = input.flatMapIndexed { y, line ->
                line.mapIndexed { x, char ->
                    CityBlock(Point2D(x.toLong(), y.toLong()), char.digitToInt())
                }
            }
            return City(blocks.associateBy { it.position })
        }

        private fun printWeights(city: City, totalWeights: Map<Point2D, Long>) {
            val maxNumbers = totalWeights.values.maxOf { it.toString().length + 1 }
            city.blocks.keys.sortedBy { it.x }.sortedBy { it.y }.forEach {
                val weight = totalWeights[it] ?: -1
                val padding = maxNumbers - weight.toString().length
                print(" ".repeat(padding))
                print(weight)
                if (it.x == city.maxX) {
                    println("")
                }
            }
            println("")
        }
    }
}

data class CityBlock(
    val position: Point2D,
    val weight: Int
)
