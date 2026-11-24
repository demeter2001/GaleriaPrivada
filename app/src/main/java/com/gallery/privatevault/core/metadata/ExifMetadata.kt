package com.gallery.privatevault.core.metadata

/**
 * Modelo de datos para metadatos EXIF locales y privacidad.
 */
data class ExifMetadata(
    val filePath: String,
    val fileName: String,
    val fileSizeFormatted: String,
    val resolutionFormatted: String,
    val mimeType: String,
    val dateTaken: String? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val iso: String? = null,
    val exposureTime: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null
) {
    val hasLocation: Boolean
        get() = latitude != null && longitude != null

    val locationCoordinatesFormatted: String?
        get() = if (hasLocation) String.format("%.6f, %.6f", latitude, longitude) else null

    val cameraSummary: String?
        get() {
            val parts = listOfNotNull(cameraMake, cameraModel).distinct()
            return if (parts.isNotEmpty()) parts.joinToString(" ") else null
        }

    val exposureSummary: String?
        get() {
            val list = mutableListOf<String>()
            aperture?.let { list.add(it) }
            exposureTime?.let { list.add("${it}s") }
            focalLength?.let { list.add(it) }
            iso?.let { list.add("ISO $it") }
            return if (list.isNotEmpty()) list.joinToString(" · ") else null
        }
}
