package com.yourcollege.graphgenerator

data class DataPoint(
    val id: Long = System.currentTimeMillis(),
    val x: Double,
    val y: Double
)
