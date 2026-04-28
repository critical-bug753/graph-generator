package com.yourcollege.graphgenerator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import kotlin.math.*

data class BoundaryCurve(
    val id: String,
    val equation: String,
    val color: Color,
    val points: List<Offset> = emptyList(),
    val labelPos: Offset = Offset.Zero
)

class DoubleIntegrationViewModel : ViewModel() {
    var outerMin by mutableStateOf("0")
    var outerMax by mutableStateOf("pi/2")
    var innerLower by mutableStateOf("0")
    var innerUpper by mutableStateOf("sin(x)")
    var integrand by mutableStateOf("1")

    var result by mutableStateOf(0.0)
    var isVerticalStrip by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    val boundaryCurves = mutableStateListOf<BoundaryCurve>()
    var plotBounds by mutableStateOf(RectBounds(-1f, 1f, -1f, 1f))

    data class RectBounds(val xMin: Float, val xMax: Float, val yMin: Float, val yMax: Float)

    // Custom functions for exp4j to support full trig suite
    private val customFunctions = listOf(
        object : Function("sec", 1) { override fun apply(vararg args: Double) = 1.0 / cos(args[0]) },
        object : Function("csc", 1) { override fun apply(vararg args: Double) = 1.0 / sin(args[0]) },
        object : Function("cot", 1) { override fun apply(vararg args: Double) = 1.0 / tan(args[0]) }
    )

    fun calculate() {
        errorMessage = null
        boundaryCurves.clear()
        
        val a = parseMath(outerMin) ?: 0.0
        val b = parseMath(outerMax) ?: 1.0
        
        if (a >= b) {
            errorMessage = "Outer limits error: a must be less than b"
            return
        }

        try {
            val g1Expr = ExpressionBuilder(innerLower).variable("x").functions(customFunctions).build()
            val g2Expr = ExpressionBuilder(innerUpper).variable("x").functions(customFunctions).build()
            
            val samplingCount = 200
            val dx = (b - a) / samplingCount
            
            val lowerPoints = mutableListOf<Offset>()
            val upperPoints = mutableListOf<Offset>()
            
            var minValY = Float.MAX_VALUE
            var maxValY = Float.MIN_VALUE

            for (i in 0..samplingCount) {
                val x = a + i * dx
                g1Expr.setVariable("x", x)
                g2Expr.setVariable("x", x)
                
                val y1 = g1Expr.evaluate().toFloat()
                val y2 = g2Expr.evaluate().toFloat()
                
                if (y1.isInfinite() || y1.isNaN() || y2.isInfinite() || y2.isNaN()) {
                    errorMessage = "Region is not closed or contains discontinuities."
                    return
                }

                lowerPoints.add(Offset(x.toFloat(), y1))
                upperPoints.add(Offset(x.toFloat(), y2))
                
                minValY = min(minValY, min(y1, y2))
                maxValY = max(maxValY, max(y1, y2))
            }

            boundaryCurves.add(BoundaryCurve("lower", "y=$innerLower", Color.Blue, lowerPoints, lowerPoints[samplingCount / 2]))
            boundaryCurves.add(BoundaryCurve("upper", "y=$innerUpper", Color.Red, upperPoints, upperPoints[samplingCount / 2]))
            
            // Vertical boundaries
            val leftPoints = listOf(Offset(a.toFloat(), lowerPoints.first().y), Offset(a.toFloat(), upperPoints.first().y))
            val rightPoints = listOf(Offset(b.toFloat(), lowerPoints.last().y), Offset(b.toFloat(), upperPoints.last().y))
            
            boundaryCurves.add(BoundaryCurve("left", "x=$outerMin", Color(0xFF388E3C), leftPoints, Offset(a.toFloat(), (leftPoints[0].y + leftPoints[1].y)/2)))
            boundaryCurves.add(BoundaryCurve("right", "x=$outerMax", Color(0xFF7B1FA2), rightPoints, Offset(b.toFloat(), (rightPoints[0].y + rightPoints[1].y)/2)))

            // Auto-scaling logic
            val paddingX = max(0.5, (b - a) * 0.2).toFloat()
            val paddingY = max(0.5, (maxValY - minValY) * 0.2).toFloat()
            plotBounds = RectBounds(
                (a - paddingX).toFloat(), 
                (b + paddingX).toFloat(), 
                minValY - paddingY, 
                maxValY + paddingY
            )

            computeIntegralValue(a, b)
        } catch (e: Exception) {
            errorMessage = "Math Error: ${e.message}"
        }
    }

    private fun computeIntegralValue(a: Double, b: Double) {
        try {
            val f = ExpressionBuilder(integrand).variables("x", "y").functions(customFunctions).build()
            val g1 = ExpressionBuilder(innerLower).variable("x").functions(customFunctions).build()
            val g2 = ExpressionBuilder(innerUpper).variable("x").functions(customFunctions).build()

            val n = 100 
            val hx = (b - a) / n
            var totalSum = 0.0

            for (i in 0..n) {
                val x = a + i * hx
                val weightX = if (i == 0 || i == n) 1 else if (i % 2 == 1) 4 else 2
                
                g1.setVariable("x", x)
                g2.setVariable("x", x)
                val yStart = g1.evaluate()
                val yEnd = g2.evaluate()
                
                val hy = (yEnd - yStart) / n
                var innerSum = 0.0
                
                for (j in 0..n) {
                    val y = yStart + j * hy
                    val weightY = if (j == 0 || j == n) 1 else if (j % 2 == 1) 4 else 2
                    
                    f.setVariable("x", x)
                    f.setVariable("y", y)
                    innerSum += weightY * f.evaluate()
                }
                totalSum += weightX * (hy / 3.0) * innerSum
            }
            result = (hx / 3.0) * totalSum
        } catch (e: Exception) {
            result = Double.NaN
        }
    }

    private fun parseMath(input: String): Double? {
        return try {
            ExpressionBuilder(input).build().evaluate()
        } catch (e: Exception) {
            null
        }
    }

    fun getCurveType(expr: String): String {
        val e = expr.lowercase()
        return when {
            e.contains("sqrt") && e.contains("x^2") -> "Circle Segment"
            e.contains("x^2") -> "Parabola"
            e.contains("sin") || e.contains("cos") || e.contains("tan") -> "Trig Curve"
            !e.contains("^") && e.contains("x") -> "Linear"
            !e.contains("x") -> "Constant"
            else -> "Curve"
        }
    }
}
