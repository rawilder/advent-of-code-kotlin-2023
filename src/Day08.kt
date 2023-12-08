suspend fun main() {
    fun part1(input: List<String>): Int {
        val routeMap = RouteMap.fromString(input)
        return routeMap.numStepsToDestination("AAA", "ZZZ")
    }

    suspend fun part2(input: List<String>): Long {
        val routeMap = RouteMap.fromString(input)
        return routeMap.numGhostStepsToDestination('A', 'Z')
    }

    10L.gcd(5) shouldBe 5L
    10L.gcd(3) shouldBe 1L
    10L.gcd(6) shouldBe 2L
    3L.gcd(9) shouldBe 3L

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day08_part1_test")
    val testPart1Input2 = readInput("Day08_part1_test2")
    part1(testInput) shouldBe 2
    part1(testPart1Input2) shouldBe 6
    val testInputPart2 = readInput("Day08_part2_test")
    part2(testInputPart2) shouldBe 6L

    val input = readInput("Day08")
    part1(input).println()
    part2(input).println()
}

data class RouteMap(
    val instructions: List<Instruction>,
    val routes: Map<String, Pair<String, String>>
) {
    suspend fun numGhostStepsToDestination(sourceChar: Char, destinationChar: Char): Long {
        val allStartingPoints = routes.filter { it.key.last() == sourceChar }.keys.toList()
        return numGhostStepsToDestination(allStartingPoints, destinationChar, 0)
    }

    private tailrec suspend fun numGhostStepsToDestination(sources: List<String>, destinationChar: Char, numSteps: Int, destinationsStepsFound: List<Long> = emptyList()): Long {
        if (sources.isEmpty()) {
            return destinationsStepsFound.lcm()
        }
        val (destinations, nextSteps) = sources.mapAsync {
            nextStep(it, numSteps)
        }.partition {
            it.last() == destinationChar
        }
        val newDestinationsFound = if (destinations.isNotEmpty()) {
            destinationsStepsFound + (numSteps + 1L)
        } else {
            destinationsStepsFound
        }
        return numGhostStepsToDestination(nextSteps, destinationChar, numSteps + 1, newDestinationsFound)
    }

    fun numStepsToDestination(source: String, destination: String): Int {
        return numStepsToDestination(source, destination, 0)
    }

    private tailrec fun numStepsToDestination(source: String, destination: String, numSteps: Int): Int {
        val nextStep = nextStep(source, numSteps)
        return if (nextStep == destination) {
            return numSteps + 1
        } else {
            numStepsToDestination(nextStep, destination, numSteps + 1)
        }
    }

    private fun nextStep(source: String, numSteps: Int): String {
        val (left, right) = routes[source]!!
        return if (instructions[numSteps % instructions.size] == Instruction.RIGHT) {
            right
        } else {
            left
        }
    }

    companion object {
        fun fromString(input: List<String>): RouteMap {
            val instructions = input.first().map { Instruction.fromString(it) }
            val routes = input.drop(2).associate { line ->
                val (source, destinations) = line.split(" = ")
                val (left, right) = destinations.split(", ").let {
                    it.first().drop(1) to it.last().dropLast(1)
                }
                source to (left to right)
            }
            return RouteMap(instructions, routes)
        }
    }
}

enum class Instruction {
    RIGHT,
    LEFT;

    companion object {
        fun fromString(input: Char): Instruction {
            return when (input) {
                'R' -> RIGHT
                'L' -> LEFT
                else -> throw IllegalArgumentException("Unknown instruction: $input")
            }
        }
    }
}
