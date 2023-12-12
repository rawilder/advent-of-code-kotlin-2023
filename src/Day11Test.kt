fun main() {
    val input = """
        .......
        ..#...#
        .......
        ...#...
    """.trimIndent().lines()
    val galaxyMap = GalaxyMap.parse(input)
    galaxyMap.println()
    println("1")
    galaxyMap.withEmptySpaceExpandedTo(1).println()
    println("2")
    galaxyMap.withEmptySpaceExpandedTo(2).println()
    println("3")
    galaxyMap.withEmptySpaceExpandedTo(3).println()
    println("4")
    galaxyMap.withEmptySpaceExpandedTo(4).println()
}
