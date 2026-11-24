package com.gallery.privatevault.core.duplicates

import android.content.Context
import com.gallery.privatevault.core.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

data class DuplicateGroup(
    val hash: String,
    val sizeBytes: Long,
    val items: List<MediaItem>
) {
    val wastedBytes: Long
        get() = if (items.size > 1) (items.size - 1) * sizeBytes else 0L

    val formattedWastedSize: String
        get() {
            if (wastedBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var digitGroups = (Math.log10(wastedBytes.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) digitGroups = units.size - 1
            return String.format("%.1f %s", wastedBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
}

class DuplicateFinder(private val context: Context) {

    /**
     * Escanea localmente sin conexión por duplicados exactos usando tamaño y SHA-256.
     */
    suspend fun findDuplicates(
        items: List<MediaItem>,
        onProgress: (scanned: Int, total: Int) -> Unit = { _, _ -> }
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        // Paso 1: Agrupar por tamaño exacto (Filtro instantáneo en memoria)
        val candidateGroups = items
            .filter { it.sizeBytes > 0 }
            .groupBy { it.sizeBytes }
            .filter { it.value.size > 1 }

        val totalCandidates = candidateGroups.values.sumOf { it.size }
        var scannedCount = 0

        val hashGroups = mutableMapOf<String, MutableList<MediaItem>>()

        // Paso 2: Para archivos con tamaño colisionado, calcular hash SHA-256
        for ((size, candidateList) in candidateGroups) {
            for (item in candidateList) {
                val hash = calculateSha256(item)
                if (hash != null) {
                    val key = "$size-$hash"
                    val list = hashGroups.getOrPut(key) { mutableListOf() }
                    list.add(item)
                }
                scannedCount++
                onProgress(scannedCount, totalCandidates)
            }
        }

        // Paso 3: Retornar solo grupos que tengan 2 o más duplicados reales
        hashGroups.values
            .filter { it.size > 1 }
            .map { duplicateList ->
                DuplicateGroup(
                    hash = duplicateList.first().id.toString(),
                    sizeBytes = duplicateList.first().sizeBytes,
                    items = duplicateList.sortedBy { it.dateAddedSec }
                )
            }
            .sortedByDescending { it.wastedBytes }
    }

    private fun calculateSha256(item: MediaItem): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            val inputStream: InputStream? = context.contentResolver.openInputStream(item.uri)
            inputStream?.use { stream ->
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            } ?: return null
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
