package com.gallery.privatevault.catalog

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gallery.privatevault.catalog.model.OnlineWallpaper
import com.gallery.privatevault.core.model.ThemeConfig
import com.gallery.privatevault.data.repository.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class WallpaperActionManager(
    private val context: Context,
    private val themeRepository: ThemeRepository
) {

    /**
     * Aplica el recurso online como fondo interno de la interfaz con desenfoque y opacidad ajustables.
     */
    suspend fun applyAsGalleryBackground(wallpaper: OnlineWallpaper, currentTheme: ThemeConfig) {
        val updated = currentTheme.copy(
            backgroundMediaUri = wallpaper.fullUrl,
            backgroundBlurRadius = 18f,
            backgroundAlpha = 0.80f
        )
        themeRepository.saveTheme(updated)
        themeRepository.setActiveTheme(updated.id)
    }

    /**
     * Establece el fondo directamente en el sistema Android (Pantalla de inicio / bloqueo).
     */
    suspend fun setAsSystemWallpaper(wallpaper: OnlineWallpaper): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(wallpaper.fullUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val input: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()

            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap)
        }
    }

    /**
     * Descarga el fondo en el almacenamiento local en Pictures/PrivateGallery/Wallpapers.
     */
    suspend fun downloadWallpaperLocally(wallpaper: OnlineWallpaper): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(wallpaper.fullUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val input: InputStream = connection.inputStream

            val resolver = context.contentResolver
            val fileName = "WALLPAPER_${wallpaper.id}_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PrivateGallery/Wallpapers")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Error al registrar fondo")

            resolver.openOutputStream(uri)?.use { output ->
                input.copyTo(output)
            }
            input.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }
    }
}
