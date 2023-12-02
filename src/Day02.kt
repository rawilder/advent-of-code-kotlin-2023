import java.util.Locale
import kotlin.math.max

fun main() {
    fun part1(input: List<String>, cubeCount: Map<CubeColor, Int>): Int {
        val games = gamesParser(input)

        val idsOfGamesPossibleWithCubeCount = games.filter { game ->
            game.sets.all { set ->
                set.all { (color, count) ->
                    (cubeCount[color] ?: 0) >= count
                }
            }
        }.map { it.id }

        return idsOfGamesPossibleWithCubeCount.sum()
    }

    fun part2(input: List<String>): Int {
        val games = gamesParser(input)

        val minimumPossibleCubeCounts = games.map { game ->
            game.sets.fold(emptyMap<CubeColor, Int>()) { acc, set ->
                acc + set.map { (color, count) ->
                    color to max(acc[color] ?: Int.MIN_VALUE, count)
                }.toMap()
            }
        }

        val minimumsMultiplied = minimumPossibleCubeCounts.map {
            it.values.reduce(Int::times)
        }

        return minimumsMultiplied.sum()
    }

    val cubeCounts = mapOf(
        CubeColor.RED to 12,
        CubeColor.GREEN to 13,
        CubeColor.BLUE to 14,
    )

    // test if implementation meets criteria from the description, like:
    val testInput = readInput("Day02_part1_test")
    check(part1(testInput, cubeCounts) == 8)

    val testInputPart2 = readInput("Day02_part2_test")
    check(part2(testInputPart2) == 2286)

    val input = readInput("Day02")
    part1(input, cubeCounts).println()
    part2(input).println()
}

data class Game(val id: Int, val sets: List<Map<CubeColor, Int>>)

fun gamesParser(input: List<String>): List<Game> {
    return input.map {
        Game(
            it.substringBefore(":").substringAfter(" ").toInt(),
            it.substringAfter(":").split(";").map {
                setsParser(it)
            }
        )
    }
}

fun setsParser(input: String): Map<CubeColor, Int> {
    return input.split(",").associate {
        cubeParser(it)
    }
}

fun cubeParser(input: String): Pair<CubeColor, Int> {
    input.trim().split(" ").let {
        return Pair(CubeColor.valueOf(it[1].uppercase(Locale.getDefault())), it[0].toInt())
    }
}

enum class CubeColor {
    RED,
    GREEN,
    BLUE,
}
