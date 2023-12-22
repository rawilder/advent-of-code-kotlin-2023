import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import util.collection.combinations
import util.collection.intersectAsRange
import util.collection.size
import util.file.readInput
import util.println
import util.shouldBe
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

suspend fun main() {
    fun part1(input: List<String>): Int {
        val system = parseSystemFromInput(input)
        return system.acceptedParts().sumOf {
            it.x + it.m + it.a + it.s
        }
    }

    suspend fun part2(input: List<String>, max: Int): Long {
        val system = parseSystemFromInput(input)
        val routes = system.routesToAcceptance()
        val validRanges = routes.map { route ->
            route.fold(PartRanges(1..max, 1..max, 1..max, 1..max)) { acc, (workflowId, rule) ->
                when (rule.property) {
                    Part::x, Part::m, Part::a, Part::s -> {
                        acc.withRule(rule)
                    }
                    else -> {
                        val workflow = system.workflows[workflowId]!!
                        workflow.rules.filter { it.operator.isNotBlank() }.fold(acc) { workflowAcc, workflowRule ->
                            workflowAcc.withRule(workflowRule.copy(
                                operator = if (workflowRule.operator == "<") ">" else "<",
                                value = workflowRule.value + (if (workflowRule.operator == "<") -1 else 1)
                            ))
                        }
                    }
                }
            }
        }

        val mathSum = validRanges.sumOf { validRanges ->
            validRanges.numCombinations()
        }

        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

        val dupeCount = AtomicLong(0)
        var lock = CompletableDeferred<Unit>()
        val visited = ConcurrentHashMap.newKeySet<PartRanges>()
        validRanges.forEachIndexed { index, partRanges ->
            partRanges.allCombinations().forEach { part ->
                if (visited.none { it.contains(part) }) {
                    coroutineScope.launch {
                        val counts = countPartsInOtherRanges(index to part, validRanges)
                        dupeCount.addAndGet(counts.toLong())
                    }
                }
            }.also {
                visited.add(partRanges)
                if (index == validRanges.lastIndex) {
                    lock.complete(Unit)
                }
            }
        }

        lock.await()
        return mathSum - dupeCount.get()
    }

//    val testInput2 = readInput("Day19_part2_test")
//    part2(testInput2, 2) shouldBe 1

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day19_part1_test")
//    part1(testInput) shouldBe 19114
    part2(testInput, 4000) shouldBe 167409079868000L

    val input = readInput("Day19")
    part1(input).println()
    part2(input, 4000).println()
}

fun parseSystemFromInput(input: List<String>): System {
    val firstEmptyLine = input.indexOfFirst { it.isBlank() }
    val workflows = Workflow.listFromInput(input.subList(0, firstEmptyLine))
    val parts = Part.listFromInput(input.subList(firstEmptyLine + 1, input.size))
    return System(workflows.associateBy { it.id }, parts)
}

data class System(
    val workflows: Map<WorkflowId, Workflow>,
    val parts: List<Part>,
) {
    fun acceptedParts(): List<Part> {
        val accepted = mutableSetOf<Part>()
        val partsQueue: MutableList<Pair<Part, WorkflowId>> = parts.map { it to "in" }.toMutableList()
        while (partsQueue.isNotEmpty()) {
            val (part, workflowId) = partsQueue.removeFirst()
            val workflow = workflows[workflowId]!!
            when(val nextWorkflowId = workflow.processPart(part)) {
                "A" -> accepted.add(part)
                "R" -> {}
                else -> partsQueue.add(part to nextWorkflowId)
            }
        }
        return accepted.toList()
    }

    fun routesToAcceptance(): List<List<Pair<WorkflowId, Rule>>> {
        return routesToAcceptanceRecursiveWorkflowId("in")
    }

    private fun routesToAcceptanceRecursive(workflowId: WorkflowId): List<List<Rule>> {
        val rules = workflows[workflowId]!!.rules
        return rules.flatMap {
            when (it.sendTo) {
                "A" -> listOf(listOf(it))
                "R" -> listOf()
                else -> routesToAcceptanceRecursive(it.sendTo).map { route -> listOf(it) + route }
            }
        }
    }

    private fun routesToAcceptanceRecursiveWorkflowId(workflowId: WorkflowId): List<List<Pair<WorkflowId, Rule>>> {
        val rules = workflows[workflowId]!!.rules
        return rules.flatMap { rule ->
            when (rule.sendTo) {
                "A" -> listOf(listOf(workflowId to rule))
                "R" -> listOf()
                else -> {
                    routesToAcceptanceRecursiveWorkflowId(rule.sendTo).map { route -> listOf(workflowId to rule) + route }
                }
            }
        }
    }
}

typealias WorkflowId = String
data class Workflow(
    val id: WorkflowId,
    val rules: List<Rule>,
) {

    fun processPart(part: Part): WorkflowId {
        return rules.first { it.condition(part) }.sendTo
    }

    companion object {
        // list of rules inside a workflow
        private val regex = """(\w+)\{(.*)}""".toRegex()
        fun listFromInput(input: List<String>): List<Workflow> {
            return input.map {
                val (id, rulesString) = regex.matchEntire(it)!!.destructured
                Workflow(id, parseRules(rulesString))
            }
        }

        private val rulesRegex = """(\w+)|(\w+)([<>])(\d+):(\w+)""".toRegex()
        private fun parseRules(rulesString: String): List<Rule> {
            return rulesString.split(",")
                .map {
                    val regexMatch = rulesRegex.matchEntire(it)!!
                    val (default, partPropertyString, operator, value, sendTo) = regexMatch.destructured
                    if (default.isEmpty()) {
                        val partProperty = Part.propertyFromString(partPropertyString)
                        val condition = when (operator) {
                            "<" -> { part: Part -> part.partProperty() < value.toInt() }
                            ">" -> { part: Part -> part.partProperty() > value.toInt() }
                            else -> throw IllegalArgumentException("Unknown operator $operator")
                        }
                        Rule(condition, sendTo, partProperty, operator, value.toInt())
                    } else {
                        Rule({ true }, default, { 0 }, "", 0)
                    }
                }
        }
    }
}

data class Rule(
    val condition: (Part) -> Boolean,
    val sendTo: WorkflowId,
    val property: Part.() -> Int,
    val operator: String,
    val value: Int
)

data class Part(
    val x: Int, // extremely cool looking
    val m: Int, // musical
    val a: Int, // aerodynamic
    val s: Int, // shiny
) {

    companion object {
        fun propertyFromString(part: String): Part.() -> Int {
            return when (part) {
                "x" -> Part::x
                "m" -> Part::m
                "a" -> Part::a
                "s" -> Part::s
                else -> throw IllegalArgumentException("Unknown part $part")
            }
        }

        // {x=787,m=2655,a=1222,s=2876}
        private val partRegex = """\{x=(\d+),m=(\d+),a=(\d+),s=(\d+)}""".toRegex()
        fun listFromInput(input: List<String>): List<Part> {
            return input.map {
                val (x, m, a, s) = partRegex.matchEntire(it)!!.destructured
                Part(x.toInt(), m.toInt(), a.toInt(), s.toInt())
            }
        }
    }
}

data class PartRanges(
    val xRange: IntRange,
    val mRange: IntRange,
    val aRange: IntRange,
    val sRange: IntRange,
) {

    fun numCombinations(): Long {
        return (xRange.size().toLong() * mRange.size().toLong() * aRange.size().toLong() * sRange.size().toLong())
    }

    fun withRule(rule: Rule): PartRanges {
        return when (rule.property) {
            Part::x -> {
                when (rule.operator) {
                    "<" -> copy(xRange = xRange.first..minOf(xRange.last, rule.value - 1))
                    ">" -> copy(xRange = maxOf(xRange.first, rule.value + 1)..xRange.last)
                    else -> throw IllegalArgumentException("Unknown operator ${rule.operator}")
                }
            }
            Part::m -> {
                when (rule.operator) {
                    "<" -> copy(mRange = mRange.first..minOf(mRange.last, rule.value - 1))
                    ">" -> copy(mRange = maxOf(mRange.first, rule.value + 1)..mRange.last)
                    else -> throw IllegalArgumentException("Unknown operator ${rule.operator}")
                }
            }
            Part::a -> {
                when (rule.operator) {
                    "<" -> copy(aRange = aRange.first..minOf(aRange.last, rule.value - 1))
                    ">" -> copy(aRange = maxOf(aRange.first, rule.value + 1)..aRange.last)
                    else -> throw IllegalArgumentException("Unknown operator ${rule.operator}")
                }
            }
            Part::s -> {
                when (rule.operator) {
                    "<" -> copy(sRange = sRange.first..minOf(sRange.last, rule.value - 1))
                    ">" -> copy(sRange = maxOf(sRange.first, rule.value + 1)..sRange.last)
                    else -> throw IllegalArgumentException("Unknown operator ${rule.operator}")
                }
            }
            else -> throw IllegalArgumentException("Unknown property ${rule.property}")
        }
    }
}

fun (Part.() -> Int).string(): String {
    return when (this) {
        Part::x -> "x"
        Part::m -> "m"
        Part::a -> "a"
        Part::s -> "s"
        else -> throw IllegalArgumentException("Unknown property $this")
    }
}
