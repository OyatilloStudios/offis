package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.TableViewerScreen
import com.example.ui.screens.TextViewerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

sealed class Screen {
    object Dashboard : Screen()
    object PdfViewer : Screen()
    object TextViewer : Screen()
    object TableViewer : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
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
    val viewModel: MainViewModel = viewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val recentFiles by viewModel.recentFiles.collectAsState()
    val favoriteFiles by viewModel.favoriteFiles.collectAsState()
    val isViewingLoading by viewModel.isViewingLoading.collectAsState()
    val viewingError by viewModel.viewingError.collectAsState()
    val metadata by viewModel.currentFileMetadata.collectAsState()

    val currentPdfFile by viewModel.currentPdfFile.collectAsState()
    val currentPdfPageCount by viewModel.currentPdfPageCount.collectAsState()
    val currentTextContent by viewModel.currentTextContent.collectAsState()
    val currentTableData by viewModel.currentTableData.collectAsState()

    var showErrorDialog by remember { mutableStateOf(false) }

    // Intercept back button when inside a viewer to return to Dashboard
    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        currentScreen = Screen.Dashboard
    }

    // React to file loading success and automatically switch screen
    LaunchedEffect(isViewingLoading, viewingError, metadata) {
        if (!isViewingLoading && metadata != null) {
            if (viewingError == null) {
                currentScreen = when (metadata!!.extension.lowercase()) {
                    "pdf" -> Screen.PdfViewer
                    "docx" -> Screen.TextViewer
                    "xlsx", "csv" -> Screen.TableViewer
                    else -> Screen.TextViewer
                }
            } else {
                showErrorDialog = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            is Screen.Dashboard -> {
                DashboardScreen(
                    recentFiles = recentFiles,
                    favoriteFiles = favoriteFiles,
                    onOpenFile = { uri -> viewModel.openFile(uri) },
                    onToggleFavorite = { uriStr, isFav -> viewModel.toggleFavorite(uriStr, isFav) },
                    onDeleteRecent = { uriStr -> viewModel.deleteRecent(uriStr) },
                    onClearHistory = { viewModel.clearHistory() }
                )
            }
            is Screen.PdfViewer -> {
                if (currentPdfFile != null && metadata != null) {
                    PdfViewerScreen(
                        pdfFile = currentPdfFile!!,
                        pageCount = currentPdfPageCount,
                        fileName = metadata!!.name,
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }
            }
            is Screen.TextViewer -> {
                if (currentTextContent != null && metadata != null) {
                    TextViewerScreen(
                        textContent = currentTextContent!!,
                        fileName = metadata!!.name,
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }
            }
            is Screen.TableViewer -> {
                if (currentTableData != null && metadata != null) {
                    TableViewerScreen(
                        tableData = currentTableData!!,
                        fileName = metadata!!.name,
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }
            }
        }

        // Beautiful full-screen Loading Overlay
        AnimatedVisibility(
            visible = isViewingLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Hujjat yuklanmoqda...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Iltimos, kuting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Error Dialog
        if (showErrorDialog && viewingError != null) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("Tushundim")
                    }
                },
                title = {
                    Text(
                        text = "Xatolik",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Text(text = viewingError ?: "Faylni ochishda noma'lum xatolik yuz berdi.")
                }
            )
        }
    }
}
