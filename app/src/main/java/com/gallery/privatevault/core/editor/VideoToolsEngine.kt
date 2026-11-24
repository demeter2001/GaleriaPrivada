package com.gallery.privatevault.core.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoToolsEngine(private val context: Context) {

    /**
     * Extrae un fotograma del video en alta resolución en la posición indicada (ms).
     */
    suspend fun extractFrame(videoUri: Uri, positionMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val timeMicros = positionMs * 1000L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    timeMicros,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    -1,
                    -1
                )
            } else {
                retriever.getFrameAtTime(timeMicros, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }

    /**
     * Guarda el fotograma extraído como una imagen independiente en Pictures/PrivateGallery.
     */
    suspend fun saveExtractedFrame(bitmap: Bitmap, videoName: String): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val baseName = videoName.substringAfterLast('/').substringBeforeLast('.')
            val fileName = "FRAME_${baseName}_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PrivateGallery")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("No se pudo registrar la miniatura extraída")

            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            } ?: throw IllegalStateException("Error al guardar el fotograma")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            uri
        }
    }
}
