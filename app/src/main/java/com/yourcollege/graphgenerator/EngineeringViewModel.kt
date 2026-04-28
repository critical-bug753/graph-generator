package com.yourcollege.graphgenerator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class EngineeringViewModel : ViewModel() {
    var xAxisTitle by mutableStateOf("X Axis")
    var yAxisTitle by mutableStateOf("Y Axis")

    var numSeries by mutableStateOf(1)
    val seriesLabels = mutableStateListOf<String>("Series 1")
    val dataPoints = mutableStateListOf<EngineeringDataPoint>()

    fun configureSeries(count: Int) {
        numSeries = count
        seriesLabels.clear()
        for (i in 1..count) {
            seriesLabels.add("Series $i")
        }
        dataPoints.clear()
    }

    fun addDataPoint(x: Double, ys: List<Double>) {
        dataPoints.add(EngineeringDataPoint(x = x, yValues = ys))
        dataPoints.sortBy { it.x }
    }

    fun removeDataPoint(point: EngineeringDataPoint) {
        dataPoints.remove(point)
    }
    
    fun updateSeriesLabel(index: Int, label: String) {
        if (index in seriesLabels.indices) {
            seriesLabels[index] = label
        }
    }
}
