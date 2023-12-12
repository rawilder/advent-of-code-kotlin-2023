fun part2(input: List<String>): Int {
    val pipeMap = PipeMap.fromInput(input)
    println("")
    val enclosed = pipeMap.findEnclosed()
    pipeMap.printMap(mapOf(enclosed.keys to 'I', pipeMap.dijkstrasDistances().keys to null), true)
    return enclosed.size
}

fun main() {
    fun part1(input: List<String>): Int {
        val pipeMap = PipeMap.fromInput(input)
        return pipeMap.dijkstrasDistances().maxOf { it.value }
    }

    fun part1Recursive(input: List<String>): Int {
        val pipeMap = PipeMap.fromInput(input)
        return pipeMap.dijkstrasDistancesRecursive().maxOf { it.value }
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day10_part1_test")
    part1(testInput) shouldBe 8
    part1Recursive(testInput) shouldBe 8
    val testInput2 = readInput("Day10_part2_test")
    part2(testInput2) shouldBe 8

    val input = readInput("Day10")
    part1(input) shouldBe 6867
    comparePerformance("part1", { part1(input) }, { part1Recursive(input) })
    part2(input).println()
}

data class Node(
    val position: Pair<Int, Int>,
    val type: NodeType,
) {
    fun allNeighbors(): Set<Pair<Int, Int>> {
        return listOfNotNull(
            position.first to position.second - 1,
            position.first to position.second + 1,
            position.first - 1 to position.second,
            position.first + 1 to position.second,
        ).toSet()
    }
}

data class PipeMap(val nodes: Map<Pair<Int, Int>, Node>) {

    private val maxFirst = nodes.keys.maxOf { it.first }
    private val maxSecond = nodes.keys.maxOf { it.second }
    private val start = nodes.values.first { it.type == NodeType.START }

    fun dijkstrasDistances(): Map<Pair<Int, Int>, Int> {
        val distances = mutableMapOf<Pair<Int, Int>, Int>()
        val visited = mutableSetOf<Pair<Int, Int>>()
        val start = nodes.values.first { it.type == NodeType.START }
        val unvisited = mutableSetOf(start.position)
        distances[start.position] = 0
        while (unvisited.isNotEmpty()) {
            val current = unvisited.minByOrNull { distances[it] ?: Int.MAX_VALUE }!!
            val currentDistance = distances[current] ?: Int.MAX_VALUE
            val currentNeighbors = nodes[current]!!.type.calculateNeighbors(nodes[current]!!, this).filter {
                nodes[it]!!.type != NodeType.GROUND
            }.toSet() - visited
            currentNeighbors.forEach { position ->
                val neighborDistance = distances[position] ?: Int.MAX_VALUE
                val newDistance = currentDistance + 1
                if (newDistance < neighborDistance) {
                    distances[position] = newDistance
                }
                if (position !in visited) unvisited.add(position)
            }
            visited.add(current)
            unvisited.remove(current)
        }
        return distances
    }

    fun dijkstrasDistancesRecursive(): Map<Pair<Int, Int>, Int> {
        val start = nodes.values.first { it.type == NodeType.START }
        return dijkstrasDistancesRecursive(mapOf(start.position to 0), nodes.keys, emptySet())
    }

    private tailrec fun dijkstrasDistancesRecursive(
        distances: Map<Pair<Int, Int>, Int>,
        unvisited: Set<Pair<Int, Int>>,
        visited: Set<Pair<Int, Int>>,
    ): Map<Pair<Int, Int>, Int> {
        val current = unvisited.minByOrNull { distances[it] ?: Int.MAX_VALUE }
        if (current == null) {
            return distances
        } else {
            val currentDistance = distances[current] ?: Int.MAX_VALUE
            val currentNeighbors = nodes[current]!!.type.calculateNeighbors(nodes[current]!!, this).filter {
                nodes[it]!!.type != NodeType.GROUND
            }.toSet() - visited
            val newDistances = distances + currentNeighbors.mapNotNull { position ->
                val neighborDistance = distances[position] ?: Int.MAX_VALUE
                val newDistance = currentDistance + 1
                if (newDistance < neighborDistance) {
                    position to newDistance
                } else null
            }.toMap()
            return dijkstrasDistancesRecursive(
                newDistances,
                unvisited - current,
                visited + current,
            )
        }
    }

    fun findEnclosed(): Map<Pair<Int, Int>, EnclosedType> {

        // only consider the pipe
        val enclosingPipes = dijkstrasDistances().keys
        val unvisited: MutableList<Pair<Int, Int>> = (enclosingPipes.groupBy { it.second }.flatMap { entry ->
            val first = entry.value.minOf { it.first }
            val last = entry.value.maxOf { it.first }
            (first..last).map { x -> x to entry.key }
        }.toSet() - enclosingPipes).toMutableList()
        val visited: MutableSet<Pair<Int, Int>> = mutableSetOf()
        val results: MutableMap<Pair<Int, Int>, EnclosedType> = mutableMapOf()

        while(unvisited.isNotEmpty()) {
            val current = unvisited.firstOrNull() ?: break
            visited.add(current)
            unvisited.remove(current)
            val currentNeighbors = listOf(
                current.first to current.second - 1,
                current.first to current.second + 1,
                current.first - 1 to current.second,
                current.first + 1 to current.second,
            ).filter { it !in enclosingPipes }
            when {
                currentNeighbors.any { it !in visited && it !in unvisited && it !in enclosingPipes } -> results[current] = EnclosedType.NOT_ENCLOSED
                current.first == 0 || current.second == 0 || current.first == maxFirst || current.second == maxSecond -> results[current] = EnclosedType.NOT_ENCLOSED
                currentNeighbors.any { results[it] == EnclosedType.NOT_ENCLOSED } -> results[current] = EnclosedType.NOT_ENCLOSED
                currentNeighbors.all { results[it] in setOf(EnclosedType.ENCLOSED) || it in enclosingPipes } -> results[current] = EnclosedType.ENCLOSED
                else -> {
                    if (unvisited.size > 1) {
                        results[current] = EnclosedType.ENCLOSED
                        visited.remove(current)
                        unvisited.add(current)
                    } else {
                        // this is the last one, so it must not be enclosed
                        results[current] = EnclosedType.NOT_ENCLOSED
                    }
                }
            }
            if (results[current] == EnclosedType.NOT_ENCLOSED) {
                currentNeighbors.filter { it in visited || it in unvisited }.forEach { neighbor ->
                    if (results[neighbor] != EnclosedType.NOT_ENCLOSED) {
                        results[neighbor] = EnclosedType.NOT_ENCLOSED
                        unvisited.add(neighbor)
                        visited.remove(neighbor)
                    }
                }
            }
        }
        val allInside = results.filter { it.value == EnclosedType.ENCLOSED }.toMutableMap()

        fun Pair<Int, Int>.isInLoop(): Boolean {
            var amInLoop = false
            val currentYOfPipes = (0 .. this.first)
                .filter { it to this.second in enclosingPipes }
                .also { list ->
                    println("")
                    list.forEach {
                        if (nodes[it to this.second]!!.type == NodeType.START) print(NodeType.coerceToPipeType(nodes[it to this.second]!!, this@PipeMap).toBoxDrawingChar())
                        else print(nodes[it to this.second]!!.type.toBoxDrawingChar())
                    }
                    println("")
                }
                .map { nodes[it to this.second]!!.type }
                .filter { it !in setOf(NodeType.H_PIPE, NodeType.GROUND) }
                .toMutableList()

            currentYOfPipes.withIndex().firstOrNull { it.value == NodeType.START }?.let {
                currentYOfPipes[it.index] = NodeType.coerceToPipeType(start, this@PipeMap)
            }

            val pipesToCheck = mutableListOf<NodeType>()
            while(currentYOfPipes.isNotEmpty()) {
                val currentType = currentYOfPipes.removeFirstOrNull() ?: break
                if (pipesToCheck.isEmpty()) { amInLoop = true }
                if (NodeType.endOf[pipesToCheck.lastOrNull()]?.contains(listOf(currentType)) == true) {
                    pipesToCheck.removeLast()
                    if (pipesToCheck.isEmpty()) amInLoop = false
                } else pipesToCheck.add(currentType)
                if (pipesToCheck.size > 1) {
                    val endings = NodeType.endOf[pipesToCheck.first()] ?: emptySet()
                    val isEndingFound = endings.contains(pipesToCheck.drop(1)) || pipesToCheck.drop(1).windowed(2, 1).any {
                        endings.contains(it.take(1)) || endings.contains(it.takeLast(1)) || endings.contains(it)
                    }
                    if (isEndingFound) {
                        pipesToCheck.clear()
                        amInLoop = false
                    }
                }
            }
            println(if (!amInLoop) "not in loop" else "in loop")
            return amInLoop
        }

        // unset nodes which are not in a loop
        val visitedLoopDetection = mutableSetOf<Pair<Int, Int>>()
        val unvisitedIslands = allInside.keys.toMutableList()
        while(unvisitedIslands.isNotEmpty()) {
            val nextF = unvisitedIslands.removeFirstOrNull() ?: break
            visitedLoopDetection.add(nextF)
            val nextFNode = nodes[nextF]!!
            val nextFInLoop = nextF.isInLoop()
            if (!nextFInLoop) {
                allInside.remove(nextF)
            }
            val unvisitedLoopDetection = (nextFNode.allNeighbors().filter { it in allInside } - visitedLoopDetection).toMutableList()
            while (unvisitedLoopDetection.isNotEmpty()) {
                val current = unvisitedLoopDetection.removeFirstOrNull() ?: break
                unvisitedIslands.remove(current)
                val currentNode = nodes[current]!!
                if (!nextFInLoop) {
                    allInside.remove(current)
                }
                visitedLoopDetection.add(current)
                unvisitedLoopDetection.remove(current)
                val currentNeighbors =
                    currentNode.allNeighbors().filter { it in allInside } - visitedLoopDetection
                unvisitedLoopDetection.addAll(currentNeighbors)
            }
        }
        return allInside
    }

    fun printMap(identify: Map<Set<Pair<Int, Int>>, Char?> = emptyMap(), hideEverythingElse: Boolean = false) {
        (0..maxSecond).forEach { second ->
            (0..maxFirst).forEach { first ->
                val position = first to second
                val node = nodes[position]
                if (node != null) {
                    if (identify.isNotEmpty()) {
                        val entry = identify.entries.firstOrNull { it.key.contains(position) }
                        if (entry != null) {
                            print(entry.value ?: node.type.toBoxDrawingChar())
                        } else {
                            if (hideEverythingElse) print(".") else print(node.type.toBoxDrawingChar())
                        }
                    } else {
                        if (hideEverythingElse) print(".") else print(node.type.toBoxDrawingChar())
                    }
                } else {
                    print("X")
                }
                if (position.first > 0 && position.first % maxFirst == 0) {
                    kotlin.io.println()
                }
            }
        }
        println("")
    }

    companion object {
        fun fromInput(input: List<String>): PipeMap {
            val nodes = input.fold(emptyMap<Pair<Int, Int>, Node>()) { acc, line ->
                val newNodes = line.mapIndexed { x, char ->
                    val node = Node(x to acc.size / line.length, NodeType.fromChar(char))
                    node.position to node
                }.toMap()
                acc + newNodes
            }
            return PipeMap(nodes)
        }
    }
}

enum class NodeType(val calculateNeighbors: (Node, PipeMap) -> Set<Pair<Int, Int>>) {
    V_PIPE({ node, pipeMap ->
        val (x, y) = node.position
        setOfNotNull(
            pipeMap.nodes[x to y - 1]?.takeIf { it.type in setOf(V_PIPE, F, N7, START) },
            pipeMap.nodes[x to y + 1]?.takeIf { it.type in setOf(V_PIPE, L, J, START) },
        )
            .map { it.position }
            .toSet()
    }),
    H_PIPE({ node, pipeMap ->
        val (x, y) = node.position
        setOfNotNull(
            pipeMap.nodes[x - 1 to y]?.takeIf { it.type in setOf(H_PIPE, L, F, START) },
            pipeMap.nodes[x + 1 to y]?.takeIf { it.type in setOf(H_PIPE, N7, J, START) }
        )
            .map { it.position }
            .toSet()
    }),
    L({ node, pipeMap ->
        val (x, y) = node.position
        setOfNotNull(
            pipeMap.nodes[x to y - 1]?.takeIf { it.type in setOf(V_PIPE, F, N7, START) },
            pipeMap.nodes[x + 1 to y]?.takeIf { it.type in setOf(H_PIPE, N7, J, START) }
        )
            .map { it.position }
            .toSet()
    }),
    J({ node, pipeMap ->
        val (x, y) = node.position
        setOfNotNull(
            pipeMap.nodes[x to y - 1]?.takeIf { it.type in setOf(V_PIPE, N7, F, START) },
            pipeMap.nodes[x - 1 to y]?.takeIf { it.type in setOf(H_PIPE, F, L, START) }
        )
            .map { it.position }
            .toSet()
    }),
    N7({ node, pipeMap ->
        val (x, y) = node.position
        setOfNotNull(
            pipeMap.nodes[x to y + 1]?.takeIf { it.type in setOf(V_PIPE, J, L, START) },
            pipeMap.nodes[x - 1 to y]?.takeIf { it.type in setOf(H_PIPE, L, F, START) }
        )
            .map { it.position }
            .toSet()
    }),
    F({ node, pipeMap ->
        val (x, y) = node.position
        setOfNotNull(
            pipeMap.nodes[x to y + 1]?.takeIf { it.type in setOf(V_PIPE, L, J, START) },
            pipeMap.nodes[x + 1 to y]?.takeIf { it.type in setOf(H_PIPE, J, N7, START) }
        )
            .map { it.position }
            .toSet()
    }),
    START({ node, pipeMap ->
        // need to find the neighbors which connect to this node
        val (x, y) = node.position
        val neighbors = setOfNotNull(
            pipeMap.nodes[x to y - 1]?.takeIf { it.type in setOf(V_PIPE, F, N7) },
            pipeMap.nodes[x to y + 1]?.takeIf { it.type in setOf(V_PIPE, L, J) },
            pipeMap.nodes[x - 1 to y]?.takeIf { it.type in setOf(H_PIPE, L, F) },
            pipeMap.nodes[x + 1 to y]?.takeIf { it.type in setOf(H_PIPE, N7, J) }
        ).map { it.position }.toSet()
        neighbors
    }),
    GROUND({ _, _ -> emptySet() });

    fun toChar(): Char {
        return when (this) {
            V_PIPE -> '|'
            H_PIPE -> '-'
            L -> 'L'
            J -> 'J'
            N7 -> '7'
            F -> 'F'
            GROUND -> '.'
            START -> 'S'
        }
    }

    fun toBoxDrawingChar(): Char {
        return when (this) {
            V_PIPE -> '║'
            H_PIPE -> '═'
            L -> '╚'
            J -> '╝'
            N7 -> '╗'
            F -> '╔'
            GROUND -> ' '
            START -> 'S'
        }
    }

    fun toSimpleBoxDrawingChar(): Char {
        return when (this) {
            V_PIPE -> '│'
            H_PIPE -> '─'
            L -> '└'
            J -> '┘'
            N7 -> '┐'
            F -> '┌'
            GROUND -> ' '
            START -> 'S'
        }
    }

    companion object {
        fun fromChar(char: Char): NodeType {
            return when (char) {
                '|' -> V_PIPE
                '-' -> H_PIPE
                'L' -> L
                'J' -> J
                '7' -> N7
                'F' -> F
                '.' -> GROUND
                'S' -> START
                else -> throw IllegalArgumentException("Unknown char $char")
            }
        }

        private val alwaysEnds = setOf(
            listOf(V_PIPE),
            listOf(L, N7),
            listOf(F, J),
        )

        val endOf: Map<NodeType, Set<List<NodeType>>> = mapOf(
            V_PIPE to alwaysEnds,
            L to setOf(listOf(J)) + alwaysEnds,
            F to setOf(listOf(N7)) + alwaysEnds,
        )

        fun coerceToPipeType(node: Node, pipeMap: PipeMap): NodeType {
            return when (node.type) {
                V_PIPE -> V_PIPE
                H_PIPE -> H_PIPE
                L -> L
                J -> J
                N7 -> N7
                F -> F
                GROUND -> throw IllegalArgumentException("Cannot coerce GROUND to pipe type")
                START -> {
                    val up = pipeMap.nodes[node.position.first to node.position.second - 1]
                    val down = pipeMap.nodes[node.position.first to node.position.second + 1]
                    val left = pipeMap.nodes[node.position.first - 1 to node.position.second]
                    val right = pipeMap.nodes[node.position.first + 1 to node.position.second]
                    val neighbors = setOfNotNull(
                        up?.takeIf { it.type.calculateNeighbors(up, pipeMap).contains(node.position) }?.position,
                        down?.takeIf { it.type.calculateNeighbors(down, pipeMap).contains(node.position) }?.position,
                        left?.takeIf { it.type.calculateNeighbors(left, pipeMap).contains(node.position) }?.position,
                        right?.takeIf { it.type.calculateNeighbors(right, pipeMap).contains(node.position) }?.position,
                    )
                    listOf(V_PIPE, H_PIPE, L, J, N7, F).first { it.calculateNeighbors(node, pipeMap) == neighbors }
                }
            }
        }
    }
}

enum class EnclosedType {
    ENCLOSED,
    NOT_ENCLOSED,
}
