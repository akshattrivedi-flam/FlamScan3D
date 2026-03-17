package com.yourorg.objectcapture.model

data class OrbitSegment(
    val index: Int,
    val elevationRangeDegrees: ClosedFloatingPointRange<Float>
)
