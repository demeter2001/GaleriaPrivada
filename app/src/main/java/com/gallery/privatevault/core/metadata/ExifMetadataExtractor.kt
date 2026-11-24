package com.gallery.privatevault.core.metadata

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.gallery.privatevault.core.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Extractor de metadatos EXIF local y 100% offline.
 * No realiza conexiones a internet para geolocalización inversa ni telemetría.
 */
class ExifMetadataExtractor(private val context: Context) {

    suspend fun extractMetadata(item: MediaItem): ExifMetadata = withContext(Dispatchers.IO) {
        var cameraMake: String? = null
        var cameraModel: String? = null
        var lensModel: String? = null
        var aperture: String? = null
        var focalLength: String? = null
        var iso: String? = null
        var exposureTime: String? = null
        var flash: String? = null
        var whiteBalance: String? = null
        var dateTaken: String? = null
        var lat: Double? = null
        var lng: Double? = null
        var alt: Double? = null

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(item.uri)
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)

                cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()
                dateTaken = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                if (fNumber > 0) {
                    aperture = "f/${String.format("%.1f", fNumber)}"
                }

                val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                if (focal > 0) {
                    focalLength = "${String.format("%.1f", focal)}mm"
                }

                val isoVal = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                if (isoVal > 0) {
                    iso = isoVal.toString()
                }

                val expSec = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                if (expSec > 0) {
                    exposureTime = if (expSec < 1.0) {
                        "1/${(1.0 / expSec).toInt()}"
                    } else {
                        String.format("%.1f", expSec)
                    }
                }

                val flashVal = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
                if (flashVal != -1) {
                    flash = if ((flashVal and 1) != 0) "Activado" else "Desactivado"
                }

                val wbVal = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
                if (wbVal != -1) {
                    whiteBalance = if (wbVal == ExifInterface.WHITE_BALANCE_AUTO) "Automático" else "Manual"
                }

                val latLong = exif.latLong
                if (latLong != null && latLong.size >= 2) {
                    lat = latLong[0]
                    lng = latLong[1]
                    val altitudeVal = exif.getAltitude(0.0)
                    if (altitudeVal != 0.0) {
                        alt = altitudeVal
                    }
                }
            }
        } catch (e: Exception) {
            // Manejo silencioso ante formatos sin soporte EXIF
        }

        ExifMetadata(
            filePath = item.path,
            fileName = item.displayName,
            fileSizeFormatted = item.sizeFormatted,
            resolutionFormatted = item.resolutionFormatted,
            mimeType = item.mimeType,
            dateTaken = dateTaken,
            cameraMake = cameraMake,
            cameraModel = cameraModel,
            lensModel = lensModel,
            aperture = aperture,
            focalLength = focalLength,
            iso = iso,
            exposureTime = exposureTime,
            flash = flash,
            whiteBalance = whiteBalance,
            latitude = lat,
            longitude = lng,
            altitudeMeters = alt
        )
    }
}
