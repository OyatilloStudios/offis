package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

data class UriMetadata(
    val name: String,
    val size: Long,
    val extension: String
)

object UriMetadataHelper {
    fun getMetadata(context: Context, uri: Uri): UriMetadata {
        var name = "Noma'lum"
        var size = 0L
        var extension = ""

        if (uri.scheme == "content") {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = it.getString(nameIndex) ?: "Noma'lum"
                        }
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            size = it.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (uri.scheme == "file") {
            val path = uri.path
            if (path != null) {
                val file = File(path)
                name = file.name
                size = file.length()
            }
        }

        // If the name is still default or empty, fallback
        if (name.isEmpty() || name == "Noma'lum") {
            name = uri.lastPathSegment ?: "document"
        }

        val dotIndex = name.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < name.length - 1) {
            extension = name.substring(dotIndex + 1).lowercase()
        } else {
            // fallback if no extension detected
            extension = "txt"
        }

        return UriMetadata(name, size, extension)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format("%.1f %s", value, units[digitGroups])
    }
}
