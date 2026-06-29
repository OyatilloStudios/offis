package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableViewerScreen(
    tableData: List<List<String>>,
    fileName: String,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val horizontalScrollState = rememberScrollState()

    // Find max column count in entire spreadsheet to prevent row indexing crashes
    val colCount = remember(tableData) {
        tableData.maxOfOrNull { it.size } ?: 0
    }

    // Filter rows based on search query
    val filteredRowsWithIndex = remember(tableData, searchQuery) {
        if (searchQuery.isEmpty()) {
            tableData.mapIndexed { index, row -> index to row }
        } else {
            tableData.mapIndexed { index, row -> index to row }.filter { pair ->
                pair.second.any { cell -> cell.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${tableData.size} qator × $colCount ustunli jadval",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                // Inline Table Search
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Jadvaldan qidirish...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Tozalash")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("table_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }
    ) { innerPadding ->
        if (tableData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Jadvalda ma'lumot topilmadi", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Horizontal scroll wrapper to support arbitrary column width scroll!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Column {
                        // Table Column Headers Row (A, B, C...)
                        Row(
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            // Blank cell for corner row number header
                            Box(
                                modifier = Modifier
                                    .size(width = 50.dp, height = 36.dp)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("#", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }

                            // Alphabetical columns
                            for (colIdx in 0 until colCount) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 130.dp, height = 36.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getColumnLetter(colIdx),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Table Grid Body (Filtered rows)
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            itemsIndexed(filteredRowsWithIndex) { listIdx, pair ->
                                val origRowIdx = pair.first
                                val rowCells = pair.second

                                Row(
                                    modifier = Modifier.background(
                                        if (listIdx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    // Row number header cell (1, 2, 3...)
                                    Box(
                                        modifier = Modifier
                                            .size(width = 50.dp, height = 48.dp)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (origRowIdx + 1).toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Content cells
                                    for (colIdx in 0 until colCount) {
                                        val cellValue = if (colIdx in rowCells.indices) rowCells[colIdx] else ""
                                        val isHighlighted = searchQuery.isNotEmpty() && cellValue.contains(searchQuery, ignoreCase = true)

                                        Box(
                                            modifier = Modifier
                                                .size(width = 130.dp, height = 48.dp)
                                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                .background(if (isHighlighted) Color(0xFFFFD54F) else Color.Transparent)
                                                .padding(horizontal = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = cellValue,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = 13.sp,
                                                color = if (isHighlighted) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Generates A, B, C, ... AA, AB etc. matching spreadsheet standard
fun getColumnLetter(colIndex: Int): String {
    var temp = colIndex
    var colLetter = ""
    while (temp >= 0) {
        colLetter = ('A' + (temp % 26)).toChar() + colLetter
        temp = (temp / 26) - 1
    }
    return colLetter
}
