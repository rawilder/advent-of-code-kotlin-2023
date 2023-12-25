import util.geometry.Point3D
import util.geometry.Vector3D
import util.shouldBe

fun main() {
    Vector3D(Point3D(0, 0, 0), Point3D(1, 1, 0)).let { vector3D ->
        vector3D.isHeadingTowards(2.0 to 5.0) shouldBe true
        vector3D.isHeadingTowards(2.0 to -5.0) shouldBe false
        vector3D.isHeadingTowards(-2.0 to 1.0) shouldBe false
        vector3D.isHeadingTowards(-2.0 to -3.0) shouldBe false
    }

    Vector3D(Point3D(0, 0, 0), Point3D(-1, 1, 0)).let { vector3D ->
        vector3D.isHeadingTowards(2.0 to 5.0) shouldBe false
        vector3D.isHeadingTowards(2.0 to -5.0) shouldBe false
        vector3D.isHeadingTowards(-2.0 to 1.0) shouldBe true
        vector3D.isHeadingTowards(-2.0 to -3.0) shouldBe false
    }

    Vector3D(Point3D(0, 0, 0), Point3D(1, -1, 0)).let { vector3D ->
        vector3D.isHeadingTowards(2.0 to 5.0) shouldBe false
        vector3D.isHeadingTowards(2.0 to -5.0) shouldBe true
        vector3D.isHeadingTowards(-2.0 to 1.0) shouldBe false
        vector3D.isHeadingTowards(-2.0 to -3.0) shouldBe false
    }

    Vector3D(Point3D(0, 0, 0), Point3D(-1, -1, 0)).let { vector3D ->
        vector3D.isHeadingTowards(2.0 to 5.0) shouldBe false
        vector3D.isHeadingTowards(2.0 to -5.0) shouldBe false
        vector3D.isHeadingTowards(-2.0 to 1.0) shouldBe false
        vector3D.isHeadingTowards(-2.0 to -3.0) shouldBe true
    }
}
