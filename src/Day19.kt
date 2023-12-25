import util.collection.allPossibleCombinations
import util.collection.combinations
import util.collection.intersectAsRange
import util.collection.size
import util.file.readInput
import util.math.pow
import util.println
import util.shouldBe

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
                            workflowAcc.withRule(
                                workflowRule.copy(
                                    operator = if (workflowRule.operator == "<") ">" else "<",
                                    value = workflowRule.value + (if (workflowRule.operator == "<") -1 else 1)
                                )
                            )
                        }
                    }
                }
            }
        }

        val dedupedValidRanges = validRanges.withIndex().fold(validRanges.map {
            PartRanges.Many(listOf(it.xRange), listOf(it.mRange), listOf(it.aRange), listOf(it.sRange))
        }) { acc, (idx, validRange) ->
            acc.subList(0, idx + 1) + acc.drop(idx + 1).map {
                it.minusDuplicatedBounds(validRange.toMany())
            }
        }

        // 167409079868000
        // 116808124428000
        // 15320205000000
        // 15320207491756
        return dedupedValidRanges.sumOf {
            it.numCombinations()
        }
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

    fun minusDuplicatedBounds(other: PartRanges): PartRanges.Many {
        val xIntersection = xRange.intersectAsRange(other.xRange)
        val mIntersection = mRange.intersectAsRange(other.mRange)
        val aIntersection = aRange.intersectAsRange(other.aRange)
        val sIntersection = sRange.intersectAsRange(other.sRange)
        if (xIntersection != null && mIntersection != null && aIntersection != null && sIntersection != null) {

            val xRanges = xRange.minusAsRanges(other.xRange)
            val mRanges = mRange.minusAsRanges(other.mRange)
            val aRanges = aRange.minusAsRanges(other.aRange)
            val sRanges = sRange.minusAsRanges(other.sRange)

            return PartRanges.Many(xRanges, mRanges, aRanges, sRanges)
        } else {
            return PartRanges.Many(listOf(xRange), listOf(mRange), listOf(aRange), listOf(sRange))
        }
    }

    fun numCombinations(): Long {
        return (xRange.size().toLong() * mRange.size().toLong() * aRange.size().toLong() * sRange.size().toLong())
    }

    fun combinations(): List<Part> {
        return xRange.flatMap { x ->
            mRange.flatMap { m ->
                aRange.flatMap { a ->
                    sRange.map { s ->
                        Part(x, m, a, s)
                    }
                }
            }
        }
    }

    fun withRule(rule: Rule): PartRanges {
        return when (rule.operator) {
            "<" -> {
                when (rule.property) {
                    Part::x -> copy(xRange = xRange.first..minOf(xRange.last, rule.value - 1))
                    Part::m -> copy(mRange = mRange.first..minOf(mRange.last, rule.value - 1))
                    Part::a -> copy(aRange = aRange.first..minOf(aRange.last, rule.value - 1))
                    Part::s -> copy(sRange = sRange.first..minOf(sRange.last, rule.value - 1))
                    else -> throw IllegalArgumentException("Unknown property ${rule.property}")
                }
            }
            ">" -> {
                when (rule.property) {
                    Part::x -> copy(xRange = maxOf(xRange.first, rule.value + 1)..xRange.last)
                    Part::m -> copy(mRange = maxOf(mRange.first, rule.value + 1)..mRange.last)
                    Part::a -> copy(aRange = maxOf(aRange.first, rule.value + 1)..aRange.last)
                    Part::s -> copy(sRange = maxOf(sRange.first, rule.value + 1)..sRange.last)
                    else -> throw IllegalArgumentException("Unknown property ${rule.property}")
                }
            }
            else -> throw IllegalArgumentException("Unknown operator ${rule.operator}")
        }
    }

    fun toMany(): PartRanges.Many {
        return PartRanges.Many(listOf(xRange), listOf(mRange), listOf(aRange), listOf(sRange))
    }

    data class Many(
        val xRanges: List<IntRange>,
        val mRanges: List<IntRange>,
        val aRanges: List<IntRange>,
        val sRanges: List<IntRange>,
    ) {

        fun minusDuplicatedBounds(other: PartRanges.Many): PartRanges.Many {
            val xIntersection = xRanges.intersectAsRanges(other.xRanges)
            val mIntersection = mRanges.intersectAsRanges(other.mRanges)
            val aIntersection = aRanges.intersectAsRanges(other.aRanges)
            val sIntersection = sRanges.intersectAsRanges(other.sRanges)
            if (xIntersection.isNotEmpty() && mIntersection.isNotEmpty() && aIntersection.isNotEmpty() && sIntersection.isNotEmpty()) {

                val dedupedXRanges = xRanges.minusAsRanges(other.xRanges)
                val dedupedMRanges = mRanges.minusAsRanges(other.mRanges)
                val dedupedARanges = aRanges.minusAsRanges(other.aRanges)
                val dedupedSRanges = sRanges.minusAsRanges(other.sRanges)

                return Many(dedupedXRanges, dedupedMRanges, dedupedARanges, dedupedSRanges)
            } else {
                return Many(xRanges, mRanges, aRanges, sRanges)
            }
        }

        fun numCombinations(): Long {
            val xSize = xRanges.sumOf { it.size() }
            val mSize = mRanges.sumOf { it.size() }
            val aSize = aRanges.sumOf { it.size() }
            val sSize = sRanges.sumOf { it.size() }
            return (xSize.toLong() * mSize.toLong() * aSize.toLong() * sSize.toLong())
        }

        fun combinations(): List<Part> {
            return xRanges.flatMap { xRange ->
                mRanges.flatMap { mRange ->
                    aRanges.flatMap { aRange ->
                        sRanges.flatMap { sRange ->
                            xRange.flatMap { x ->
                                mRange.flatMap { m ->
                                    aRange.flatMap { a ->
                                        sRange.map { s ->
                                            Part(x, m, a, s)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun IntRange.inverse(max: Int): List<IntRange> {
    return when {
        this.first == 1 && this.last == max -> {
            listOf()
        }
        this.first == 1 -> {
            listOf(this.last + 1..max)
        }
        this.last == max -> {
            listOf(1..<this.first)
        }
        else -> {
            listOf(1..<this.first, this.last + 1..max)
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

fun Iterable<PartRanges>.intersectAsRange(): PartRanges {
    return reduce { acc, partRanges ->
        val xRange = acc.xRange.intersectAsRange(partRanges.xRange) ?: 1..0
        val mRange = acc.mRange.intersectAsRange(partRanges.mRange) ?: 1..0
        val aRange = acc.aRange.intersectAsRange(partRanges.aRange) ?: 1..0
        val sRange = acc.sRange.intersectAsRange(partRanges.sRange) ?: 1..0
        PartRanges(xRange, mRange, aRange, sRange)
    }
}

fun Iterable<IntRange>.intersectAsRanges(other: Iterable<IntRange>): List<IntRange> {
    return this.flatMap {  first ->
        other.mapNotNull { second ->
            first.intersectAsRange(second)
        }
    }
}

fun IntRange.minusAsRanges(other: IntRange): List<IntRange> {
    return when {
        this.last < other.first || this.first > other.last -> {
            listOf(this)
        }
        else -> {
            val start = this.first
            val end = this.last
            val otherStart = other.first
            val otherEnd = other.last
            when {
                start < otherStart && end > otherEnd -> {
                    listOf(start..otherStart, otherEnd + 1..end)
                }
                start < otherStart -> {
                    listOf(start..otherStart)
                }
                end > otherEnd -> {
                    listOf(otherEnd + 1..end)
                }
                else -> {
                    emptyList()
                }
            }
        }
    }
}

fun List<IntRange>.minusAsRanges(other: List<IntRange>): List<IntRange> {
    return this.flatMap { range ->
        other.fold(listOf(range)) { acc, otherRange ->
            acc.flatMap { it.minusAsRanges(otherRange) }
        }
    }
}
