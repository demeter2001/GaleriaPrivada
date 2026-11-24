package com.gallery.privatevault.core.model

import android.net.Uri

/**
 * Representa un archivo multimedia (imagen o video) en memoria.
 * Diseñado para operaciones fluidas con miles de elementos sin sobrecargar memoria.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long = 0L,
    val isVideo: Boolean = mimeType.startsWith("video/"),
    val bucketId: String = "",
    val bucketDisplayName: String = "",
    // Metadatos locales desde Room
    val isFavorite: Boolean = false,
    val rating: Int = 0,
    val tags: List<String> = emptyList(),
    val isTrash: Boolean = false
) {
    val durationFormatted: String
        get() {
            if (!isVideo || durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val resolutionFormatted: String
        get() = if (width > 0 && height > 0) "${width}x$height" else ""

    val sizeFormatted: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) digitGroups = units.size - 1
            return String.format("%.1f %s", sizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
}
