package com.yourcollege.graphgenerator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import net.objecthunter.exp4j.ExpressionBuilder
import kotlin.math.*

class GameViewModel : ViewModel() {
    var userEquation by mutableStateOf("")
    var targetEquation by mutableStateOf("sin(x)")
    var score by mutableStateOf(0)
    var level by mutableStateOf(1)
    
    val userDataPoints = mutableStateListOf<DataPoint>()
    val targetDataPoints = mutableStateListOf<DataPoint>()

    init {
        generateTargetGraph()
    }

    fun generateTargetGraph() {
        targetDataPoints.clear()
        targetEquation = when (level) {
            1 -> "sin(x)"
            2 -> "cos(x)*0.5"
            3 -> "x^2/10"
            else -> "sin(x)"
        }
        val expr = ExpressionBuilder(targetEquation).variable("x").build()
        for (i in -50..50) {
            val x = i * 0.2
            expr.setVariable("x", x)
            targetDataPoints.add(DataPoint(x = x, y = expr.evaluate()))
        }
    }

    fun evaluateUserEquation() {
        userDataPoints.clear()
        if (userEquation.isBlank()) return
        try {
            val expr = ExpressionBuilder(userEquation).variable("x").build()
            for (i in -50..50) {
                val x = i * 0.2
                expr.setVariable("x", x)
                val y = expr.evaluate()
                if (y.isFinite()) {
                    userDataPoints.add(DataPoint(x = x, y = y))
                }
            }
            calculateScore()
        } catch (e: Exception) {
            // Invalid equation
        }
    }

    private fun calculateScore() {
        if (userDataPoints.isEmpty()) return
        var totalError = 0.0
        val pointsToCompare = min(userDataPoints.size, targetDataPoints.size)
        for (i in 0 until pointsToCompare) {
            totalError += abs(targetDataPoints[i].y - userDataPoints[i].y)
        }
        val avgError = if (pointsToCompare > 0) totalError / pointsToCompare else 0.0
        score = max(0, (100 - (avgError * 20)).toInt())
        if (score > 90 && level < 3) {
            level++
            generateTargetGraph()
            userEquation = ""
            userDataPoints.clear()
        }
    }
}
