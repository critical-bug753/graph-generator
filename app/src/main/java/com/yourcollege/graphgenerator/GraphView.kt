package com.yourcollege.graphgenerator

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

@Composable
fun GraphView(
    dataPoints: List<DataPoint>,
    ghostPoints: List<DataPoint> = emptyList(),
    modifier: Modifier = Modifier,
    xMin: Float? = null,
    xMax: Float? = null,
    yMin: Float? = null,
    yMax: Float? = null,
    onChartCreated: (LineChart) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(true)
                isDragEnabled = true
                isScaleXEnabled = true
                isScaleYEnabled = true
                
                setBackgroundColor(Color.WHITE)
                
                xAxis.setDrawGridLines(true)
                axisLeft.setDrawGridLines(true)
                axisRight.isEnabled = false
                
                setScaleMinima(1f, 1f)
                
                onChartCreated(this)
            }
        },
        modifier = modifier,
        update = { chart ->
            val dataSets = mutableListOf<ILineDataSet>()

            if (dataPoints.isNotEmpty()) {
                val entries = dataPoints.map { Entry(it.x.toFloat(), it.y.toFloat()) }.sortedBy { it.x }
                val dataSet = LineDataSet(entries, "Your Data").apply {
                    color = Color.BLUE
                    setCircleColor(Color.BLUE)
                    setDrawCircles(true)
                    circleRadius = 4f
                    lineWidth = 2.5f
                    mode = LineDataSet.Mode.LINEAR 
                    setDrawValues(false)
                }
                dataSets.add(dataSet)
            }

            if (ghostPoints.isNotEmpty()) {
                val ghostEntries = ghostPoints.map { Entry(it.x.toFloat(), it.y.toFloat()) }.sortedBy { it.x }
                val ghostDataSet = LineDataSet(ghostEntries, "Ghost Graph").apply {
                    color = Color.LTGRAY
                    setDrawCircles(false)
                    lineWidth = 2f
                    enableDashedLine(10f, 10f, 0f)
                    setDrawValues(false)
                }
                dataSets.add(ghostDataSet)
            }

            chart.data = LineData(dataSets)
            
            if (xMin != null && xMax != null) {
                chart.xAxis.axisMinimum = xMin
                chart.xAxis.axisMaximum = xMax
            } else {
                chart.xAxis.resetAxisMinimum()
                chart.xAxis.resetAxisMaximum()
            }

            if (yMin != null && yMax != null) {
                chart.axisLeft.axisMinimum = yMin
                chart.axisLeft.axisMaximum = yMax
            } else {
                chart.axisLeft.resetAxisMinimum()
                chart.axisLeft.resetAxisMaximum()
            }

            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )
}
