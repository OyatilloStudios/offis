package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RecentFile
import com.example.data.RecentFileRepository
import com.example.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RecentFileRepository
    val recentFiles: StateFlow<List<RecentFile>>
    val favoriteFiles: StateFlow<List<RecentFile>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RecentFileRepository(database.recentFileDao())
        recentFiles = repository.allRecentFiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        favoriteFiles = repository.favoriteFiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private val _isViewingLoading = MutableStateFlow(false)
    val isViewingLoading = _isViewingLoading.asStateFlow()

    private val _viewingError = MutableStateFlow<String?>(null)
    val viewingError = _viewingError.asStateFlow()

    private val _currentFileMetadata = MutableStateFlow<UriMetadata?>(null)
    val currentFileMetadata = _currentFileMetadata.asStateFlow()

    // For Text / Word Viewers
    private val _currentTextContent = MutableStateFlow<String?>(null)
    val currentTextContent = _currentTextContent.asStateFlow()

    // For PDF Viewers
    private val _currentPdfFile = MutableStateFlow<File?>(null)
    val currentPdfFile = _currentPdfFile.asStateFlow()
    private val _currentPdfPageCount = MutableStateFlow(0)
    val currentPdfPageCount = _currentPdfPageCount.asStateFlow()

    // For Excel / CSV Viewers
    private val _currentTableData = MutableStateFlow<List<List<String>>?>(null)
    val currentTableData = _currentTableData.asStateFlow()

    fun openFile(uri: Uri) {
        viewModelScope.launch {
            _isViewingLoading.value = true
            _viewingError.value = null
            _currentTextContent.value = null
            _currentPdfFile.value = null
            _currentPdfPageCount.value = 0
            _currentTableData.value = null

            val context = getApplication<Application>().applicationContext
            val metadata = withContext(Dispatchers.IO) {
                UriMetadataHelper.getMetadata(context, uri)
            }
            _currentFileMetadata.value = metadata

            // Insert into Recent Database
            withContext(Dispatchers.IO) {
                repository.insertRecent(
                    RecentFile(
                        uriString = uri.toString(),
                        name = metadata.name,
                        size = metadata.size,
                        extension = metadata.extension
                    )
                )
            }

            try {
                when (metadata.extension.lowercase()) {
                    "pdf" -> {
                        val pdfFile = withContext(Dispatchers.IO) {
                            PdfHelper.copyUriToCacheFile(context, uri)
                        }
                        if (pdfFile != null && pdfFile.exists()) {
                            _currentPdfFile.value = pdfFile
                            _currentPdfPageCount.value = withContext(Dispatchers.IO) {
                                PdfHelper.getPageCount(pdfFile)
                            }
                        } else {
                            _viewingError.value = "PDF faylini yuklashda xatolik yuz berdi"
                        }
                    }
                    "docx" -> {
                        val text = withContext(Dispatchers.IO) {
                            DocxParser.extractText(context, uri)
                        }
                        _currentTextContent.value = text
                    }
                    "xlsx" -> {
                        val table = withContext(Dispatchers.IO) {
                            XlsxParser.parseExcel(context, uri)
                        }
                        if (table.isNotEmpty()) {
                            _currentTableData.value = table
                        } else {
                            _viewingError.value = "Excel fayli bo'sh yoki o'qib bo'lmadi"
                        }
                    }
                    "csv" -> {
                        val table = withContext(Dispatchers.IO) {
                            CsvParser.parseCsv(context, uri)
                        }
                        if (table.isNotEmpty()) {
                            _currentTableData.value = table
                        } else {
                            _viewingError.value = "CSV fayli bo'sh yoki o'qib bo'lmadi"
                        }
                    }
                    else -> {
                        // Treat as plain text
                        val text = withContext(Dispatchers.IO) {
                            readTextFile(context, uri)
                        }
                        _currentTextContent.value = text
                    }
                }
            } catch (e: Exception) {
                _viewingError.value = "Faylni ochishda xatolik: ${e.localizedMessage}"
            } finally {
                _isViewingLoading.value = false
            }
        }
    }

    private fun readTextFile(context: Context, uri: Uri): String {
        val result = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    var count = 0
                    while (line != null) {
                        result.append(line).append("\n")
                        line = reader.readLine()
                        count++
                        if (count > 10000) {
                            result.append("\n[Hujjat juda katta bo'lgani sababli faqat dastlabki 10 000 qator ko'rsatildi]")
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return "Faylni o'qishda xatolik yuz berdi: ${e.localizedMessage}"
        }
        return result.toString()
    }

    fun toggleFavorite(uriString: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(uriString, isFavorite)
        }
    }

    fun deleteRecent(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecent(uriString)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }
}
