package com.yourorg.objectcapture.model

data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun minus(other: Vector3): Vector3 {
        return Vector3(x - other.x, y - other.y, z - other.z)
    }

    fun length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)
}
