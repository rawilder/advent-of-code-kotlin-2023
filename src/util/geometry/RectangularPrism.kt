package util.geometry

data class RectangularPrism(
    val start: Point3D,
    val end: Point3D,
) {
    fun fall(): RectangularPrism {
        return RectangularPrism(
            start.copy(z = start.z - 1),
            end.copy(z = end.z - 1),
        )
    }

    override fun toString(): String {
        return "($start), ($end)"
    }
}
