fun main() {
    println("testing original implementation")
    testInputWith(AlmanacMap::destinationRangesForSourceRanges)
    println("\ntesting functional implementation")
    testInputWith(AlmanacMap::destinationRangesForSourceRangesFunctional)
}

fun testInputWith(implementation: AlmanacMap.(List<LongRange>) -> List<LongRange>) {
    val almanac = parseAlmanac(readInput("Day05_part2_test"))
    val seeds = almanac.seedRanges
    println("finding soil")
    val soil = almanac.maps["seed" to "soil"]!!
        .implementation(seeds) shouldBeRanges listOf(81L..94L, 57L..69L)
    println("finding fertilizer")
    val fertilizer = almanac.maps["soil" to "fertilizer"]!!
        .implementation(soil) shouldBeRanges listOf(81L..94L, 57L..69L)
    println("finding water")
    val water = almanac.maps["fertilizer" to "water"]!!
        .implementation(fertilizer) shouldBeRanges listOf(81L..94L, 53L..56L, 61L..69L)
    println("finding light")
    val light = almanac.maps["water" to "light"]!!
        .implementation(water) shouldBeRanges listOf(74L..87L, 46L..49L, 54L..62L)
    println("finding temperature")
    val temperature = almanac.maps["light" to "temperature"]!!
        .implementation(light) shouldBeRanges listOf(78L..80L, 45L..55L, 82L..85L, 90L..98L)
    println("finding humidity")
    val humidity = almanac.maps["temperature" to "humidity"]!!
        .implementation(temperature) shouldBeRanges listOf(78L..80L, 46L..56L, 82L..85L, 90L..98L)
    println("finding location")
    val location = almanac.maps["humidity" to "location"]!!
        .implementation(humidity) shouldBeRanges listOf(82L..84L, 46L..55L, 60L..60L, 86L..89L, 94L..96L, 56L..59L, 97L..98L)
    location.minBy { it.first }.first shouldBe 46L
}
