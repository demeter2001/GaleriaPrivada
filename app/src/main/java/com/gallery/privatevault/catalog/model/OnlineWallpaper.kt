package com.gallery.privatevault.catalog.model

import kotlinx.serialization.Serializable

@Serializable
data class OnlineWallpaper(
    val id: String,
    val previewUrl: String,
    val fullUrl: String,
    val resolution: String,
    val format: String = "jpg",
    val dimensionX: Int = 1920,
    val dimensionY: Int = 1080
)
