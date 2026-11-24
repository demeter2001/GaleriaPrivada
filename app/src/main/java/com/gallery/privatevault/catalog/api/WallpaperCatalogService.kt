package com.gallery.privatevault.catalog.api

import com.gallery.privatevault.catalog.model.OnlineWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Servicio aislado del catálogo gratuito de fondos en línea.
 * CERO TELEMETRÍA - CERO RASTREO.
 * Solo se invoca bajo petición explícita del usuario desde la pantalla de fondos.
 */
class WallpaperCatalogService {

    suspend fun searchWallpapers(
        query: String = "nature",
        orientation: String = "portrait", // portrait (9x16), landscape (16x9)
        minResolution: String = "1080x1920"
    ): Result<List<OnlineWallpaper>> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val ratios = if (orientation == "portrait") "9x16,10x16" else "16x9,16x10"
            val urlString = "https://wallhaven.cc/api/v1/search?q=$encodedQuery&ratios=$ratios&sorting=toplist"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "PrivateGalleryApp/1.0 (Android; OpenSource)")
            }

            if (connection.responseCode != 200) {
                throw IllegalStateException("Servidor no disponible (${connection.responseCode})")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val responseText = reader.readText()
            reader.close()

            val jsonObject = JSONObject(responseText)
            val dataArray = jsonObject.optJSONArray("data") ?: return@runCatching emptyList()

            val list = mutableListOf<OnlineWallpaper>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id")
                val thumbs = item.optJSONObject("thumbs")
                val previewUrl = thumbs?.optString("original") ?: thumbs?.optString("large") ?: ""
                val fullUrl = item.optString("path")
                val resolution = item.optString("resolution")
                val dimX = item.optInt("dimension_x", 1080)
                val dimY = item.optInt("dimension_y", 1920)

                if (previewUrl.isNotEmpty() && fullUrl.isNotEmpty()) {
                    list.add(
                        OnlineWallpaper(
                            id = id,
                            previewUrl = previewUrl,
                            fullUrl = fullUrl,
                            resolution = resolution,
                            dimensionX = dimX,
                            dimensionY = dimY
                        )
                    )
                }
            }

            list
        }
    }
}
