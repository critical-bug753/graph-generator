package com.yourcollege.graphgenerator

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class PlotViewModel : ViewModel() {
    val dataPoints = mutableStateListOf<DataPoint>()

    fun addDataPoint(x: Double, y: Double) {
        dataPoints.add(DataPoint(x = x, y = y))
    }

    fun removeDataPoint(dataPoint: DataPoint) {
        dataPoints.remove(dataPoint)
    }
}
