package com.yourcollege.graphgenerator

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import net.objecthunter.exp4j.ExpressionBuilder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubleIntegrationScreen(navController: NavController, viewModel: DoubleIntegrationViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Double Integration Visualizer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Region Plot
            IntegrationCanvas(viewModel, modifier = Modifier.fillMaxWidth().height(350.dp))

            viewModel.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Info Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Region Analysis", fontWeight = FontWeight.Bold)
                    Text("Outer Limits: [${viewModel.outerMin}, ${viewModel.outerMax}]")
                    Text("Integrand: f(x,y) = ${viewModel.integrand}")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Inputs
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = viewModel.outerMin,
                    onValueChange = { viewModel.outerMin = it },
                    label = { Text("a (Outer Min)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = viewModel.outerMax,
                    onValueChange = { viewModel.outerMax = it },
                    label = { Text("b (Outer Max)") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            OutlinedTextField(
                value = viewModel.innerLower,
                onValueChange = { viewModel.innerLower = it },
                label = { Text("g1(x) (Lower Locus)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.innerUpper,
                onValueChange = { viewModel.innerUpper = it },
                label = { Text("g2(x) (Upper Locus)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = viewModel.integrand,
                onValueChange = { viewModel.integrand = it },
                label = { Text("f(x,y) (Integrand)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.calculate() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Visualize & Integrate")
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (viewModel.result.isNaN()) "Math Error" else "Value ≈ ${"%.5f".format(viewModel.result)}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun IntegrationCanvas(viewModel: DoubleIntegrationViewModel, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "strip")
    val stripProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pos"
    )

    Box(modifier = modifier
        .background(Color(0xFFFDFDFD))
        .border(1.dp, Color.LightGray)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bounds = viewModel.plotBounds
            val canvasWidth = size.width
            val canvasHeight = size.height

            fun xToPx(x: Float): Float = (x - bounds.xMin) / (bounds.xMax - bounds.xMin) * canvasWidth
            fun yToPx(y: Float): Float = canvasHeight - (y - bounds.yMin) / (bounds.yMax - bounds.yMin) * canvasHeight

            // Grid & Axis
            drawLine(Color(0xFFEEEEEE), Offset(xToPx(0f), 0f), Offset(xToPx(0f), canvasHeight), 2f)
            drawLine(Color(0xFFEEEEEE), Offset(0f, yToPx(0f)), Offset(canvasWidth, yToPx(0f)), 2f)

            if (viewModel.boundaryCurves.isEmpty()) return@Canvas

            // Shaded Region (strictly between outer limits and curves)
            val lowerCurve = viewModel.boundaryCurves.find { it.id == "lower" }
            val upperCurve = viewModel.boundaryCurves.find { it.id == "upper" }
            
            if (lowerCurve != null && upperCurve != null) {
                val path = Path()
                lowerCurve.points.forEachIndexed { i, p ->
                    if (i == 0) path.moveTo(xToPx(p.x), yToPx(p.y)) else path.lineTo(xToPx(p.x), yToPx(p.y))
                }
                for (i in upperCurve.points.size - 1 downTo 0) {
                    val p = upperCurve.points[i]
                    path.lineTo(xToPx(p.x), yToPx(p.y))
                }
                path.close()
                drawPath(path, Color.Gray.copy(alpha = 0.15f), style = Fill)
            }

            // Draw Boundary Curves with Labels
            viewModel.boundaryCurves.forEach { curve ->
                val path = Path()
                curve.points.forEachIndexed { i, p ->
                    val px = xToPx(p.x)
                    val py = yToPx(p.y)
                    if (px.isFinite() && py.isFinite()) {
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                }
                drawPath(path, curve.color, style = Stroke(width = 3f))

                // On-graph Equation Labels
                if (curve.points.isNotEmpty()) {
                    val labelIdx = curve.points.size / 2
                    val labelPos = curve.points[labelIdx]
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(
                                (curve.color.alpha * 255).toInt(),
                                (curve.color.red * 255).toInt(),
                                (curve.color.green * 255).toInt(),
                                (curve.color.blue * 255).toInt()
                            )
                            textSize = 32f
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        drawText(curve.equation, xToPx(labelPos.x) + 10f, yToPx(labelPos.y) - 10f, paint)
                    }
                }
            }

            // Animated Scanning Strip
            if (lowerCurve != null && upperCurve != null && lowerCurve.points.isNotEmpty()) {
                val idx = (stripProgress * (lowerCurve.points.size - 1)).toInt()
                val p1 = lowerCurve.points[idx]
                val p2 = upperCurve.points[idx]
                
                drawLine(
                    color = Color.Black.copy(alpha = 0.4f),
                    start = Offset(xToPx(p1.x), yToPx(p1.y)),
                    end = Offset(xToPx(p2.x), yToPx(p2.y)),
                    strokeWidth = 6f
                )
            }
        }
    }
}
