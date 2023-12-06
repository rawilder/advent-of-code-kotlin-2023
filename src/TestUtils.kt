inline infix fun <reified T> T.shouldBe (expected: T): T {
    if (this != expected) {
        throw AssertionError("Expected $expected but got $this")
    }
    return this
}

infix fun List<LongRange>.shouldBeRanges (expected: List<LongRange>): List<LongRange> {
    collapseRanges(this) shouldBe collapseRanges(expected)
    return this
}
