package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC")
    fun getAllRecentFiles(): Flow<List<RecentFile>>

    @Query("SELECT * FROM recent_files WHERE isFavorite = 1 ORDER BY lastOpened DESC")
    fun getFavoriteFiles(): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFile)

    @Query("UPDATE recent_files SET isFavorite = :isFav WHERE uriString = :uriStr")
    suspend fun updateFavorite(uriStr: String, isFav: Boolean)

    @Query("DELETE FROM recent_files WHERE uriString = :uriStr")
    suspend fun deleteRecentFile(uriStr: String)

    @Query("DELETE FROM recent_files")
    suspend fun clearHistory()
}
