package com.yourcollege.graphgenerator

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import kotlin.math.abs

/**
 * Structural-First Table Parser
 * Optimized for handwritten two-column numeric data.
 */
object TableParser {

    data class NumericNode(val value: Double, val bounds: Rect)

    fun parse(text: Text): List<DataPoint> {
        val allElements = text.textBlocks
            .flatMap { it.lines }
            .flatMap { it.elements }
        
        if (allElements.isEmpty()) return emptyList()

        // 1. Fragment Fusion: Merge pieces of numbers written with gaps
        val mergedElements = mutableListOf<Triple<String, Rect, Float>>()
        val sortedX = allElements.sortedBy { it.boundingBox?.left ?: 0 }
        val visited = mutableSetOf<Text.Element>()

        for (i in sortedX.indices) {
            val element = sortedX[i]
            if (element in visited) continue
            
            var compositeText = element.text
            var compositeBounds = element.boundingBox ?: Rect()
            visited.add(element)

            for (j in i + 1 until sortedX.size) {
                val next = sortedX[j]
                if (next in visited) continue
                val nextBounds = next.boundingBox ?: Rect()
                
                val hGap = nextBounds.left - compositeBounds.right
                val vOverlap = abs(nextBounds.centerY() - compositeBounds.centerY())
                
                // If horizontally close and vertically aligned, fuse them
                if (hGap < compositeBounds.height() && vOverlap < compositeBounds.height() * 0.4) {
                    compositeText += next.text
                    compositeBounds.union(nextBounds)
                    visited.add(next)
                }
            }
            mergedElements.add(Triple(compositeText, compositeBounds, element.confidence ?: 1.0f))
        }

        // 2. Digit-Only Filtering & correction
        val numericNodes = mergedElements.mapNotNull { (txt, bounds, _) ->
            val cleaned = cleanHandwriting(txt)
            val d = cleaned.toDoubleOrNull()
            if (d != null) NumericNode(d, bounds) else null
        }

        if (numericNodes.isEmpty()) return emptyList()

        // 3. Grid Split (Median X)
        val xCenters = numericNodes.map { it.bounds.centerX() }.sorted()
        val splitX = xCenters[xCenters.size / 2]

        val leftBucket = numericNodes.filter { it.bounds.centerX() < splitX }.sortedBy { it.bounds.centerY() }
        val rightBucket = numericNodes.filter { it.bounds.centerX() >= splitX }.sortedBy { it.bounds.centerY() }

        val dataPoints = mutableListOf<DataPoint>()
        val usedRightIndices = mutableSetOf<Int>()

        // 4. Vertical Proximity Pairing
        for (left in leftBucket) {
            var bestIdx = -1
            var minDY = Float.MAX_VALUE
            for (j in rightBucket.indices) {
                if (j in usedRightIndices) continue
                val dy = abs(left.bounds.centerY() - rightBucket[j].bounds.centerY()).toFloat()
                if (dy < minDY && dy < left.bounds.height() * 2.0f) {
                    minDY = dy
                    bestIdx = j
                }
            }
            if (bestIdx != -1) {
                dataPoints.add(DataPoint(x = left.value, y = rightBucket[bestIdx].value))
                usedRightIndices.add(bestIdx)
            }
        }

        return dataPoints.distinctBy { "${it.x}_${it.y}" }.sortedBy { it.x }
    }

    private fun cleanHandwriting(raw: String): String {
        return raw.uppercase()
            .replace("O", "0").replace("D", "0").replace("Q", "0").replace("U", "0")
            .replace("I", "1").replace("L", "1").replace("|", "1")
            .replace("S", "5").replace("$", "5")
            .replace("Z", "2")
            .replace("B", "8")
            .replace("—", "-").replace("–", "-").replace("~", "-")
            .replace(",", ".")
            .filter { it.isDigit() || it == '.' || it == '-' }
    }
}
