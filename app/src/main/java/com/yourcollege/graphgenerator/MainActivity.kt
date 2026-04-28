package com.yourcollege.graphgenerator

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onNavigateToMain = {
                navController.navigate("menu") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("menu") {
            MainMenuScreen(navController)
        }
        composable("engineering_plot") {
            EngineeringPlotScreen(navController)
        }
        composable("science_game") {
            ScienceGameScreen(navController)
        }
        composable("double_integration") {
            DoubleIntegrationScreen(navController)
        }
    }
}

@Composable
fun SplashScreen(onNavigateToMain: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val splashBackground = Color(0xFF1B2631)

    LaunchedEffect(Unit) {
        visible = true
        delay(1000)
        onNavigateToMain()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBackground),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Text(
                text = "Scientific Grapher Suite",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MainMenuScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Main Menu", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))
        
        MenuButton(
            text = "Engineering Multi-Plot",
            icon = Icons.AutoMirrored.Filled.List,
            onClick = { navController.navigate("engineering_plot") }
        )
        
        Spacer(Modifier.height(16.dp))
        
        MenuButton(
            text = "Scientific Graphing Game",
            icon = Icons.Default.Star,
            onClick = { navController.navigate("science_game") }
        )
        
        Spacer(Modifier.height(16.dp))
        
        MenuButton(
            text = "Double Integration Visualizer",
            icon = Icons.Default.Build,
            onClick = { navController.navigate("double_integration") }
        )
    }
}

@Composable
fun MenuButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineeringPlotScreen(navController: NavController, viewModel: EngineeringViewModel = viewModel()) {
    val context = LocalContext.current
    var xInput by remember { mutableStateOf("") }
    val yInputs = remember { mutableStateListOf<String>().apply { repeat(viewModel.numSeries) { add("") } } }
    var chartInstance by remember { mutableStateOf<LineChart?>(null) }
    var showConfig by remember { mutableStateOf(viewModel.dataPoints.isEmpty()) }

    LaunchedEffect(viewModel.numSeries) {
        yInputs.clear()
        repeat(viewModel.numSeries) { yInputs.add("") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Engineering Plotter") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showConfig = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Config")
                    }
                    IconButton(onClick = {
                        chartInstance?.let { exportEngineeringPdf(context, it, viewModel) }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            EngineeringGraphView(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth().height(300.dp),
                onChartCreated = { chartInstance = it }
            )
            
            Spacer(Modifier.height(16.dp))

            // Multi-Series Input Row
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = xInput,
                        onValueChange = { xInput = it },
                        label = { Text("X (${viewModel.xAxisTitle})") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val x = xInput.toDoubleOrNull()
                        val ys = yInputs.map { it.toDoubleOrNull() ?: 0.0 }
                        if (x != null) {
                            viewModel.addDataPoint(x, ys)
                            xInput = ""
                            yInputs.indices.forEach { yInputs[it] = "" }
                        }
                    }) {
                        Text("Add Row")
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Horizontal scrolling Y-inputs
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    yInputs.indices.forEach { index ->
                        TextField(
                            value = yInputs[index],
                            onValueChange = { yInputs[index] = it },
                            label = { Text(viewModel.seriesLabels[index]) },
                            modifier = Modifier.width(100.dp).padding(end = 4.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Data Table
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)) {
                        Text("X", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold)
                        viewModel.seriesLabels.forEach { label ->
                            Text(label, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
                items(viewModel.dataPoints) { point ->
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("%.2f".format(point.x), modifier = Modifier.width(60.dp))
                        point.yValues.forEach { y ->
                            Text("%.2f".format(y), modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
                        }
                        IconButton(onClick = { viewModel.removeDataPoint(point) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        if (showConfig) {
            AlertDialog(
                onDismissRequest = { showConfig = false },
                title = { Text("Configure Plot") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        TextField(value = viewModel.xAxisTitle, onValueChange = { viewModel.xAxisTitle = it }, label = { Text("X-Axis Title") }, modifier = Modifier.fillMaxWidth())
                        TextField(value = viewModel.yAxisTitle, onValueChange = { viewModel.yAxisTitle = it }, label = { Text("Y-Axis Title") }, modifier = Modifier.fillMaxWidth())
                        
                        Spacer(Modifier.height(16.dp))
                        Text("Number of Y-Series:")
                        Slider(
                            value = viewModel.numSeries.toFloat(),
                            onValueChange = { viewModel.configureSeries(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                        Text("${viewModel.numSeries} Series selected")
                        
                        Spacer(Modifier.height(16.dp))
                        Text("Series Labels:")
                        viewModel.seriesLabels.indices.forEach { index ->
                            TextField(
                                value = viewModel.seriesLabels[index],
                                onValueChange = { viewModel.updateSeriesLabel(index, it) },
                                label = { Text("Series ${index+1}") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showConfig = false }) { Text("Done") }
                }
            )
        }
    }
}

@Composable
fun EngineeringGraphView(
    viewModel: EngineeringViewModel,
    modifier: Modifier = Modifier,
    onChartCreated: (LineChart) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(true)
                legend.isEnabled = true
                legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
                legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
                
                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                axisRight.isEnabled = false
                onChartCreated(this)
            }
        },
        modifier = modifier,
        update = { chart ->
            val dataPoints = viewModel.dataPoints
            val seriesLabels = viewModel.seriesLabels
            
            if (dataPoints.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            // MPAndroidChart uses axis.description or separate Text objects for titles
            chart.xAxis.setDrawLabels(true)
            
            val dataSets = mutableListOf<ILineDataSet>()
            val colors = ColorTemplate.MATERIAL_COLORS.toList()

            val numSeriesInPoints = dataPoints.firstOrNull()?.yValues?.size ?: 0
            for (i in 0 until numSeriesInPoints) {
                val entries = dataPoints.map { point ->
                    Entry(point.x.toFloat(), point.yValues.getOrElse(i) { 0.0 }.toFloat())
                }
                val dataSet = LineDataSet(entries, seriesLabels.getOrElse(i) { "Series ${i+1}" }).apply {
                    val color = colors[i % colors.size]
                    this.color = color
                    setCircleColor(color)
                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawValues(false)
                }
                dataSets.add(dataSet)
            }

            chart.data = LineData(dataSets)
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )
}

fun exportEngineeringPdf(context: Context, chart: LineChart, viewModel: EngineeringViewModel) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "Engineering Report"

    printManager.print(jobName, object : PrintDocumentAdapter() {
        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?) {
            callback.onLayoutFinished(PrintDocumentInfo.Builder("report.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true)
        }

        override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: WriteResultCallback?) {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            val margin = 28.35f // 1cm margin

            // --- PAGE 1: FULL PAGE GRAPH ---
            val page1Info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page1 = pdfDocument.startPage(page1Info)
            val canvas1 = page1.canvas
            val paint = Paint()

            // Header
            paint.textSize = 24f
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            canvas1.drawText("Engineering Characteristics Report", pageWidth / 2f, margin + 40f, paint)

            // Capture and scale Graph to fill most of the page
            val chartBitmap = Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888)
            chart.draw(Canvas(chartBitmap))
            
            val graphWidth = pageWidth - (2 * margin)
            val graphHeight = 500f // Large graph area
            val scaledGraph = Bitmap.createScaledBitmap(chartBitmap, graphWidth.toInt(), graphHeight.toInt(), true)
            canvas1.drawBitmap(scaledGraph, margin, margin + 80f, paint)

            // Labels under graph
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas1.drawText("X-Axis: ${viewModel.xAxisTitle}", margin + 20f, margin + 610f, paint)
            canvas1.drawText("Y-Axis: ${viewModel.yAxisTitle}", margin + 20f, margin + 630f, paint)

            // Watermark
            paint.textSize = 12f
            paint.color = android.graphics.Color.GRAY
            paint.textAlign = Paint.Align.RIGHT
            canvas1.drawText("by Critical_bug", pageWidth - margin, pageHeight - margin, paint)

            pdfDocument.finishPage(page1)

            // --- PAGE 2+: RAW DATA TABLE ---
            var dataIndex = 0
            var pageNum = 2
            while (dataIndex < viewModel.dataPoints.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                var currentY = margin + 40f

                paint.color = android.graphics.Color.BLACK
                paint.textSize = 18f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Numerical Data Table (Cont.)", margin, currentY, paint)
                currentY += 40f

                // Table Header
                paint.textSize = 12f
                canvas.drawText("X (${viewModel.xAxisTitle})", margin, currentY, paint)
                viewModel.seriesLabels.forEachIndexed { i, label ->
                    canvas.drawText(label, margin + 120f + (i * 85f), currentY, paint)
                }
                currentY += 10f
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)
                currentY += 25f

                // Table Rows
                paint.isFakeBoldText = false
                while (dataIndex < viewModel.dataPoints.size && currentY < pageHeight - margin - 20f) {
                    val point = viewModel.dataPoints[dataIndex]
                    canvas.drawText("%.2f".format(point.x), margin, currentY, paint)
                    point.yValues.forEachIndexed { i, y ->
                        canvas.drawText("%.2f".format(y), margin + 120f + (i * 85f), currentY, paint)
                    }
                    currentY += 20f
                    dataIndex++
                }

                pdfDocument.finishPage(page)
                pageNum++
            }

            try {
                pdfDocument.writeTo(FileOutputStream(destination?.fileDescriptor))
            } catch (e: IOException) {
                callback?.onWriteFailed(e.toString())
                return
            } finally {
                pdfDocument.close()
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }
    }, null)
}

fun exportGamePdf(context: Context, chart: LineChart, viewModel: GameViewModel) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    printManager.print("Game Report", object : PrintDocumentAdapter() {
        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?) {
            callback.onLayoutFinished(PrintDocumentInfo.Builder("game_report.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true)
        }
        override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: WriteResultCallback?) {
            val pdfDocument = PdfDocument()
            val page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            val paint = Paint()

            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("Scientific Game Performance Report", 50f, 50f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("Equation: y = ${viewModel.userEquation}", 50f, 90f, paint)
            canvas.drawText("Score Achieved: ${viewModel.score}", 50f, 115f, paint)

            val chartBitmap = Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888)
            val chartCanvas = Canvas(chartBitmap)
            chart.draw(chartCanvas)
            
            val scaledBitmap = Bitmap.createScaledBitmap(chartBitmap, 500, 300, true)
            canvas.drawBitmap(scaledBitmap, 50f, 150f, paint)

            pdfDocument.finishPage(page)
            try {
                pdfDocument.writeTo(FileOutputStream(destination?.fileDescriptor))
            } catch (e: IOException) {
                callback?.onWriteFailed(e.toString())
                return
            } finally {
                pdfDocument.close()
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }
    }, null)
}
