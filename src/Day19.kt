import util.collection.collapseRanges
import util.collection.intersectAsRange
import util.collection.inverse
import util.collection.size
import util.collection.sizeSum
import util.println
import util.file.readInput
import util.math.pow
import util.shouldBe

fun main() {
    fun part1(input: List<String>): Int {
        val system = parseSystemFromInput(input)
        return system.acceptedParts().sumOf {
            it.x + it.m + it.a + it.s
        }
    }

    fun part2(input: List<String>, max: Int): Long {
        val system = parseSystemFromInput(input)
        val routes = system.routesToAcceptance()
        val validRanges = routes.map { route ->
            route.fold(ValidRangeForAcceptance(1, max, 1, max, 1, max, 1, max)) { acc, rule ->
                when (rule.property) {
                    Part::x -> {
                        when (rule.operator) {
                            "<" -> acc.copy(xMax = minOf(acc.xMax, rule.value - 1))
                            ">" -> acc.copy(xMin = maxOf(acc.xMin, rule.value + 1))
                            else -> throw IllegalArgumentException("Unknown operator ${rule.operator}")
                        }
                    }
                    Part::m -> {
                        when (rule.operator) {
                            "<" -> acc.copy(mMax = minOf(acc.mMax, rule.value - 1))
                            ">" -> acc.copy(mMin = maxOf(acc.mMin, rule.value + 1))
                            else -> throw IllegalArgumentException("Unknown operator ${rule.operator}")
                        }
                    }
                    Part::a -> {
                        when (rule.operator) {
                            "<" -> acc.copy(aMax = minOf(acc.aMax, rule.value - 1))
                            ">" -> acc.copy(aMin = maxOf(acc.aMin, rule.value + 1))
                            else -> throw IllegalArgumentException("Unknown operator ${rule.operator}")
                        }
                    }
                    Part::s -> {
                        when (rule.operator) {
                            "<" -> acc.copy(sMax = minOf(acc.sMax, rule.value - 1))
                            ">" -> acc.copy(sMin = maxOf(acc.sMin, rule.value + 1))
                            else -> throw IllegalArgumentException("Unknown operator ${rule.operator}")
                        }
                    }
                    else -> acc
                }
            }
        }

        fun toValidRange(vararg ranges: IntRange): ValidRangeForAcceptance {
            return ValidRangeForAcceptance(
                ranges[0].first, ranges[0].last,
                ranges[1].first, ranges[1].last,
                ranges[2].first, ranges[2].last,
                ranges[3].first, ranges[3].last,
            )
        }

        val maxPossibleStatesPerRange = pow(max, 4)
        // invalid states are the AND of outside the ranges
        val invalidStatesCount = validRanges.reduce { acc, validRange ->
            val xRange = acc.xMin..acc.xMax
            val mRange = acc.mMin..acc.mMax
            val aRange = acc.aMin..acc.aMax
            val sRange = acc.sMin..acc.sMax

            val accXRange = validRange.xMin..validRange.xMax
            val accMRange = validRange.mMin..validRange.mMax
            val accARange = validRange.aMin..validRange.aMax
            val accSRange = validRange.sMin..validRange.sMax


            toValidRange(
                xRange.intersectAsRange(accXRange) ?: 1..0,
                mRange.intersectAsRange(accMRange) ?: 1..0,
                aRange.intersectAsRange(accARange) ?: 1..0,
                sRange.intersectAsRange(accSRange) ?: 1..0,
            )
        }.let { invalid ->
            invalid
        }
        return maxPossibleStatesPerRange
    }

    val testInput2 = readInput("Day19_part2_test")
//    part2(testInput2, 2)

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

    fun routesToAcceptance(): List<List<Rule>> {
        return routesToAcceptanceRecursive("in")
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

data class ValidRangeForAcceptance(
    val xMin: Int,
    val xMax: Int,

    val mMin: Int,
    val mMax: Int,

    val aMin: Int,
    val aMax: Int,

    val sMin: Int,
    val sMax: Int,
) {
    fun inverse(max: Int): Invalid {
        fun makeRanges(mrMin: Int, mrMax: Int ): List<IntRange> {
            return listOfNotNull(
                (1 until mrMin).takeIf { mrMin != 1 },
                (mrMax + 1..max).takeIf { mrMax != max }
            )
        }
        return Invalid(
            makeRanges(xMin, xMax),
            makeRanges(mMin, mMax),
            makeRanges(aMin, aMax),
            makeRanges(sMin, sMax),
        )
    }

    data class Invalid(
        val xRanges: List<IntRange>,
        val mRanges: List<IntRange>,
        val aRanges: List<IntRange>,
        val sRanges: List<IntRange>,
    )
}
