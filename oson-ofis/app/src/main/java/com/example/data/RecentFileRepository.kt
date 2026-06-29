package com.example.data

import kotlinx.coroutines.flow.Flow

class RecentFileRepository(private val dao: RecentFileDao) {
    val allRecentFiles: Flow<List<RecentFile>> = dao.getAllRecentFiles()
    val favoriteFiles: Flow<List<RecentFile>> = dao.getFavoriteFiles()

    suspend fun insertRecent(recentFile: RecentFile) {
        dao.insertRecentFile(recentFile)
    }

    suspend fun toggleFavorite(uriString: String, isFavorite: Boolean) {
        dao.updateFavorite(uriString, isFavorite)
    }

    suspend fun deleteRecent(uriString: String) {
        dao.deleteRecentFile(uriString)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }
}
