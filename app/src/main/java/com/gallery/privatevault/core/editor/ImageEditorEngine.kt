package com.gallery.privatevault.core.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

data class CensorArea(
    val rect: RectF,
    val isMosaic: Boolean = true // true = pixelado/mosaico, false = barra negra de censura
)

class ImageEditorEngine(private val context: Context) {

    suspend fun loadBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Aplica rotación, volteo y áreas de censura / pixelado sobre el bitmap.
     */
    suspend fun processImage(
        originalBitmap: Bitmap,
        rotationDegrees: Float,
        isFlippedHorizontal: Boolean,
        censorAreas: List<CensorArea>,
        cropRect: RectF? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        // 1. Transformación inicial (Rotación y Volteo)
        val matrix = Matrix()
        if (rotationDegrees != 0f) {
            matrix.postRotate(rotationDegrees)
        }
        if (isFlippedHorizontal) {
            matrix.postScale(-1f, 1f)
        }

        var transformed = Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )

        // 2. Aplicar Recorte si se especificó
        if (cropRect != null) {
            val left = (cropRect.left * transformed.width).toInt().coerceIn(0, transformed.width - 1)
            val top = (cropRect.top * transformed.height).toInt().coerceIn(0, transformed.height - 1)
            val width = ((cropRect.right - cropRect.left) * transformed.width).toInt().coerceAtLeast(1).coerceAtMost(transformed.width - left)
            val height = ((cropRect.bottom - cropRect.top) * transformed.height).toInt().coerceAtLeast(1).coerceAtMost(transformed.height - top)

            val cropped = Bitmap.createBitmap(transformed, left, top, width, height)
            if (cropped != transformed) {
                transformed.recycle()
                transformed = cropped
            }
        }

        // 3. Aplicar Áreas de Censura / Pixelado
        if (censorAreas.isNotEmpty()) {
            val mutable = transformed.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutable)

            for (area in censorAreas) {
                val left = (area.rect.left * mutable.width).toInt().coerceIn(0, mutable.width - 1)
                val top = (area.rect.top * mutable.height).toInt().coerceIn(0, mutable.height - 1)
                val right = (area.rect.right * mutable.width).toInt().coerceIn(left + 1, mutable.width)
                val bottom = (area.rect.bottom * mutable.height).toInt().coerceIn(top + 1, mutable.height)
                val w = right - left
                val h = bottom - top

                if (w > 2 && h > 2) {
                    if (area.isMosaic) {
                        // Pixelado irreversible: reduce a 1/16 y agranda con filtro desactivado
                        val subBitmap = Bitmap.createBitmap(mutable, left, top, w, h)
                        val pixelSize = 16
                        val scaledDown = Bitmap.createScaledBitmap(
                            subBitmap,
                            (w / pixelSize).coerceAtLeast(1),
                            (h / pixelSize).coerceAtLeast(1),
                            false
                        )
                        val pixelated = Bitmap.createScaledBitmap(scaledDown, w, h, false)

                        canvas.drawBitmap(pixelated, left.toFloat(), top.toFloat(), null)
                        subBitmap.recycle()
                        scaledDown.recycle()
                        pixelated.recycle()
                    } else {
                        // Barra negra de censura
                        val paint = Paint().apply {
                            color = Color.BLACK
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
                    }
                }
            }

            transformed.recycle()
            transformed = mutable
        }

        transformed
    }

    /**
     * Guarda la imagen procesada de forma NO DESTRUCTIVA como un archivo nuevo en MediaStore.
     */
    suspend fun saveAsNewCopy(bitmap: Bitmap, originalName: String): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val fileName = "EDIT_${System.currentTimeMillis()}_${originalName.substringAfterLast('/')}"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PrivateGallery")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("No se pudo crear entrada en MediaStore")

            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            } ?: throw IllegalStateException("No se pudo escribir el flujo de la imagen")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            uri
        }
    }
}
