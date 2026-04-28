package com.yourcollege.graphgenerator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import net.objecthunter.exp4j.ExpressionBuilder
import kotlin.math.*

class CalculatorViewModel : ViewModel() {
    var equation by mutableStateOf("sin(x)")
    var xMin by mutableStateOf("-10")
    var xMax by mutableStateOf("10")
    var yMin by mutableStateOf("-5")
    var yMax by mutableStateOf("5")
    
    val dataPoints = mutableStateListOf<DataPoint>()
    
    // Gamification
    var currentLevel by mutableStateOf(1)
    var targetEquation by mutableStateOf("x")
    var ghostPoints = mutableStateListOf<DataPoint>()
    var score by mutableStateOf(0)
    var xp by mutableStateOf(0)

    init {
        generateGhostGraph()
        calculateGraph()
    }

    fun calculateGraph() {
        dataPoints.clear()
        val minX = xMin.toDoubleOrNull() ?: -10.0
        val maxX = xMax.toDoubleOrNull() ?: 10.0
        val step = (maxX - minX) / 200.0

        try {
            val expression = ExpressionBuilder(equation)
                .variable("x")
                .build()

            for (i in 0..200) {
                val x = minX + i * step
                expression.setVariable("x", x)
                val y = expression.evaluate()
                if (y.isFinite()) {
                    dataPoints.add(DataPoint(x = x, y = y))
                }
            }
            checkChallenge()
        } catch (e: Exception) {
            // Handle parsing error
        }
    }

    private fun generateGhostGraph() {
        ghostPoints.clear()
        targetEquation = when (currentLevel) {
            1 -> "x"
            2 -> "x^2"
            3 -> "sin(x)"
            else -> "x"
        }
        val expression = ExpressionBuilder(targetEquation).variable("x").build()
        for (i in 0..50) {
            val x = -10.0 + i * 0.4
            expression.setVariable("x", x)
            ghostPoints.add(DataPoint(x = x, y = expression.evaluate()))
        }
    }

    private fun checkChallenge() {
        if (dataPoints.isEmpty() || ghostPoints.isEmpty()) return
        
        var totalDiff = 0.0
        ghostPoints.forEach { gp ->
            val closest = dataPoints.minByOrNull { abs(it.x - gp.x) }
            if (closest != null) {
                totalDiff += abs(closest.y - gp.y)
            }
        }
        
        val matchPercent = max(0, (100 - (totalDiff * 2)).toInt())
        if (matchPercent > 90) {
            score += matchPercent
            xp += 10
        }
    }

    fun setLevel(level: Int) {
        currentLevel = level
        generateGhostGraph()
        calculateGraph()
    }
}
