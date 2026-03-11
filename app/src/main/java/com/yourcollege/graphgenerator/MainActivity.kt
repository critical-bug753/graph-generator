package com.yourcollege.graphgenerator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.mikephil.charting.charts.LineChart
import kotlinx.coroutines.delay
import java.io.FileOutputStream
import java.io.IOException

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
                navController.navigate("main") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("main") {
            MainScreen()
        }
    }
}

@Composable
fun SplashScreen(onNavigateToMain: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val splashBackground = Color(0xFF1B2631) // Deep Blue/Slate

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
                text = "An App by Critical Bug",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MainScreen(viewModel: PlotViewModel = viewModel()) {
    var xInput by remember { mutableStateOf("") }
    var yInput by remember { mutableStateOf("") }
    var chartInstance by remember { mutableStateOf<LineChart?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            text = "Data Plotter",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Graph Area
        GraphView(
            dataPoints = viewModel.dataPoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            onChartCreated = { chartInstance = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = xInput,
                onValueChange = { xInput = it },
                label = { Text("X") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = yInput,
                onValueChange = { yInput = it },
                label = { Text("Y") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val x = xInput.toDoubleOrNull()
                val y = yInput.toDoubleOrNull()
                if (x != null && y != null) {
                    viewModel.addDataPoint(x, y)
                    xInput = ""
                    yInput = ""
                }
            }) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data List
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(viewModel.dataPoints, key = { it.id }) { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("X: ${point.x}, Y: ${point.y}")
                    IconButton(onClick = { viewModel.removeDataPoint(point) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                chartInstance?.let { chart ->
                    if (viewModel.dataPoints.isNotEmpty()) {
                        exportToPdf(context, chart, viewModel.dataPoints)
                    } else {
                        Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
                    }
                } ?: Toast.makeText(context, "Chart not ready", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export to PDF / Print")
        }
    }
}

fun exportToPdf(context: Context, chart: LineChart, dataPoints: List<DataPoint>) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "Data Plot Document"

    printManager.print(jobName, object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }

            val pdi = PrintDocumentInfo.Builder("data_plot.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(pdi, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // 1. Draw Title
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("Data Plot Report", 50f, 60f, paint)

            // 2. Draw Graph
            val chartBitmap = chart.chartBitmap
            val scaledWidth = 500
            val scaledHeight = (chartBitmap.height * (scaledWidth.toFloat() / chartBitmap.width)).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(chartBitmap, scaledWidth, scaledHeight, true)
            canvas.drawBitmap(scaledBitmap, 50f, 100f, paint)

            // 3. Draw Table Header
            var currentY = 100f + scaledHeight + 40f
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("Data Table", 50f, currentY, paint)
            currentY += 30f
            
            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("X Value", 70f, currentY, paint)
            canvas.drawText("Y Value", 270f, currentY, paint)
            currentY += 10f
            canvas.drawLine(50f, currentY, 545f, currentY, paint)
            currentY += 25f

            // 4. Draw Table Rows
            dataPoints.forEach { point ->
                if (currentY < 800f) {
                    canvas.drawText("%.2f".format(point.x), 70f, currentY, paint)
                    canvas.drawText("%.2f".format(point.y), 270f, currentY, paint)
                    currentY += 20f
                }
            }

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
