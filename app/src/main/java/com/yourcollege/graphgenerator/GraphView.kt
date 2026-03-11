package com.yourcollege.graphgenerator

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun GraphView(
    dataPoints: List<DataPoint>,
    modifier: Modifier = Modifier,
    onChartCreated: (LineChart) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(true)
                onChartCreated(this)
            }
        },
        modifier = modifier,
        update = { chart ->
            val entries = dataPoints.map { Entry(it.x.toFloat(), it.y.toFloat()) }.sortedBy { it.x }
            val dataSet = LineDataSet(entries, "Data Plot").apply {
                color = Color.BLUE
                setCircleColor(Color.BLUE)
                lineWidth = 2f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawValues(false)
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
