package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey val uriString: String,
    val name: String,
    val size: Long,
    val extension: String,
    val lastOpened: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
