package com.yourcollege.graphgenerator

import java.util.UUID

data class DataPoint(
    val id: String = UUID.randomUUID().toString(),
    val x: Double,
    val y: Double
)

data class EngineeringDataPoint(
    val id: String = UUID.randomUUID().toString(),
    val x: Double,
    val yValues: List<Double>
)
