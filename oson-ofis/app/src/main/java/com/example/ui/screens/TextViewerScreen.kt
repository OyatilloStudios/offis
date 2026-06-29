package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ReaderTheme(val nameUz: String, val bgColor: Color, val textColor: Color) {
    WHITE("Qog'oz", Color(0xFFFAFAFA), Color(0xFF1C1B1F)),
    SEPIA("Sepiya", Color(0xFFF4ECD8), Color(0xFF3E2723)),
    NIGHT("Tun", Color(0xFF121212), Color(0xFFECEFF1))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewerScreen(
    textContent: String,
    fileName: String,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(16f) }
    var currentTheme by remember { mutableStateOf(ReaderTheme.WHITE) }
    var showSettings by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Process annotated string with highlighted text search matches
    val annotatedText = remember(textContent, searchQuery) {
        if (searchQuery.isEmpty()) {
            AnnotatedString(textContent)
        } else {
            buildAnnotatedString {
                var startIdx = 0
                while (true) {
                    val idx = textContent.indexOf(searchQuery, startIdx, ignoreCase = true)
                    if (idx == -1) {
                        append(textContent.substring(startIdx))
                        break
                    }
                    append(textContent.substring(startIdx, idx))
                    pushStyle(SpanStyle(background = Color(0xFFFFD54F), color = Color.Black, fontWeight = FontWeight.Bold))
                    append(textContent.substring(idx, idx + searchQuery.length))
                    pop()
                    startIdx = idx + searchQuery.length
                }
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
                                text = "${textContent.length} belgidan iborat matn",
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
                    actions = {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Search,
                                contentDescription = "Matndan qidirish"
                            )
                        }
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Default.Settings, contentDescription = "Sozlamalar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                // Optional Search field
                if (isSearchActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Matndan qidirish...") },
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
                                .testTag("viewer_search_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(currentTheme.bgColor)
        ) {
            // Settings panel drawer/dropdown overlay
            if (showSettings) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "O'qish Sozlamalari",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )

                        // Theme Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mavzu:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            ReaderTheme.entries.forEach { theme ->
                                FilterChip(
                                    selected = currentTheme == theme,
                                    onClick = { currentTheme = theme },
                                    label = { Text(theme.nameUz) }
                                )
                            }
                        }

                        // Font size slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Shrift o'lchami: ${fontSize.toInt()}sp",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                valueRange = 12f..28f,
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    }
                }
            }

            // Scrollable Text View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (textContent.isEmpty()) {
                    Text(
                        text = "Hujjat bo'sh",
                        color = currentTheme.textColor.copy(alpha = 0.5f),
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Text(
                        text = annotatedText,
                        color = currentTheme.textColor,
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = (fontSize * 1.5).sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
