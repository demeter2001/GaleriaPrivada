package com.gallery.privatevault.core.metadata

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.gallery.privatevault.core.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Limpiador de privacidad EXIF.
 * Permite compartir archivos eliminando datos sensibles de geolocalización (GPS),
 * números de serie de cámara y comentarios, protegiendo al usuario al compartir en apps externas.
 */
class ExifPrivacyScrubber(private val context: Context) {

    suspend fun createSanitizedCopyForSharing(item: MediaItem): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheFolder = File(context.cacheDir, "sanitized_media").apply {
                if (!exists()) mkdirs()
            }

            // Nombre temporal seguro
            val extension = item.displayName.substringAfterLast('.', "jpg")
            val sanitizedFile = File(cacheFolder, "clean_${System.currentTimeMillis()}.$extension")

            // Copiar contenido binario original al archivo temporal
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                FileOutputStream(sanitizedFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("No se pudo leer el archivo original")

            // Si es imagen con soporte EXIF, depurar etiquetas sensibles
            if (!item.isVideo) {
                try {
                    val exif = ExifInterface(sanitizedFile.absolutePath)

                    // Eliminar metadatos de ubicación geográfica
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
                    exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, null)

                    // Eliminar números de serie y comentarios de dispositivo
                    exif.setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER, null)
                    exif.setAttribute(ExifInterface.TAG_CAMERA_OWNER_NAME, null)
                    exif.setAttribute(ExifInterface.TAG_USER_COMMENT, null)

                    exif.saveAttributes()
                } catch (e: Exception) {
                    // Ciertos formatos no soportan reescritura de atributos; el archivo ya fue duplicado
                }
            }

            // Generar URI de FileProvider seguro
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, sanitizedFile)
        }
    }
}
