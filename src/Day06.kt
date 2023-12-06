import Day06.distanceRegex
import Day06.myBoat
import Day06.timeRegex
import Utils.whitespaceRegex

fun main() {
    fun part1(input: List<String>): Int {
        val inputRaces = parseInput(input)
        val counts = inputRaces.races.map { race ->
            (0..race.timeMillis).count {
                myBoat.distanceFor(it, race.timeMillis) > race.distanceMillimeters
            }
        }
        return counts.reduce { acc, i -> acc * i }
    }

    fun part1ALittleSmarter(input: List<String>): Int {
        val inputRaces = parseInput(input)
        val counts = inputRaces.races.map { race ->
            val firstSuccessfulTime = (0..race.timeMillis).first {
                myBoat.distanceFor(it, race.timeMillis) > race.distanceMillimeters
            }
            // the winning distribution is a bell curve, so we can just find the first successful time
            // and assume the count based on that since it's mirrored on the other end
            race.timeMillis - (firstSuccessfulTime * 2) + 1
        }
        return counts.reduce { acc, i -> acc * i }
    }

    fun part2(input: List<String>): Int {
        val inputRaces = parseInputForPart2(input)
        val counts = inputRaces.races.map { race ->
            (0..race.timeMillis).count {
                myBoat.distanceFor(it, race.timeMillis) > race.distanceMillimeters
            }
        }
        return counts.reduce { acc, i -> acc * i }
    }

    fun part2ALittleSmarter(input: List<String>): Int {
        val inputRace = parseInputForPart2(input).races.first()
        val firstSuccessfulTime = (0..inputRace.timeMillis).first {
            myBoat.distanceFor(it, inputRace.timeMillis) > inputRace.distanceMillimeters
        }
        return inputRace.timeMillis - (firstSuccessfulTime * 2) + 1
    }

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day06_part1_test")
    check(part1(testInput) == 288)
    val testInput2 = readInput("Day06_part2_test")
    check(part2(testInput2) == 71503)

    part2ALittleSmarter(testInput).println()

    val input = readInput("Day06")

    comparePerformance(
        "part1",
        { part1(input) },
        { part1ALittleSmarter(input) }
    )

    comparePerformance(
        "part2",
        { part2(input) },
        { part2ALittleSmarter(input) }
    )
}

data class Boat(
    val accelerationPerMilliPressed: Int,
) {
    fun distanceFor(pressedTimeMillis: Int, raceTimeTotalMillis: Int): Long {
        val speed = accelerationPerMilliPressed * pressedTimeMillis
        val time = raceTimeTotalMillis - pressedTimeMillis
        return speed.toLong() * time.toLong()
    }
}

data class Input(
    val races: List<Race>
)

data class Race(
    val timeMillis: Int,
    val distanceMillimeters: Long,
)

fun parseInput(input: List<String>): Input {
    val times = input[0].replace(timeRegex, "").split(whitespaceRegex).map { it.toInt() }
    val distances = input[1].replace(distanceRegex, "").split(whitespaceRegex).map { it.toLong() }
    val races = times.zip(distances).map {
        Race(
            timeMillis = it.first,
            distanceMillimeters = it.second
        )
    }
    return Input(races)
}

fun parseInputForPart2(input: List<String>): Input {
    val time = input[0].replace(timeRegex, "").replace(whitespaceRegex, "").toInt()
    val distance = input[1].replace(distanceRegex, "").replace(whitespaceRegex, "").toLong()
    return Input(
        races = listOf(Race(
            timeMillis = time,
            distanceMillimeters = distance
        ))
    )
}

object Day06 {
    val myBoat = Boat(
        accelerationPerMilliPressed = 1,
    )
    val timeRegex = Regex("Time: (\\s+)")
    val distanceRegex = Regex("Distance: (\\s+)")
}
