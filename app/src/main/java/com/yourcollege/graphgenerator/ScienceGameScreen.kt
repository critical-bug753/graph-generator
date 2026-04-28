package com.yourcollege.graphgenerator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.LineChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScienceGameScreen(navController: NavController, viewModel: GameViewModel = viewModel()) {
    val context = LocalContext.current
    var chartInstance by remember { mutableStateOf<LineChart?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val mathOps = listOf("sin(x)", "cos(x)", "tan(x)", "asin(x)", "acos(x)", "atan(x)", "sqrt(x)", "abs(x)", "log10(x)", "exp(x)", "x^2", "x^3")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Graphing Game - Level ${viewModel.level}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        chartInstance?.let { exportGamePdf(context, it, viewModel) }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Score: ${viewModel.score}", style = MaterialTheme.typography.headlineSmall)
            GraphView(
                dataPoints = viewModel.userDataPoints,
                ghostPoints = viewModel.targetDataPoints,
                modifier = Modifier.fillMaxWidth().height(300.dp),
                onChartCreated = { chartInstance = it }
            )
            Spacer(Modifier.height(16.dp))
            
            Text("Quick Operators:", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val quickOps = listOf("sin(", "cos(", "tan(", "sqrt(", "abs(", "^")
                quickOps.forEach { op ->
                    SuggestionChip(
                        onClick = { viewModel.userEquation += op },
                        label = { Text(op) }
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = viewModel.userEquation,
                    onValueChange = { viewModel.userEquation = it },
                    readOnly = false,
                    label = { Text("Build Equation y = ...") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    mathOps.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op) },
                            onClick = {
                                viewModel.userEquation += op
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.evaluateUserEquation() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Evaluate & Score")
            }
        }
    }
}
