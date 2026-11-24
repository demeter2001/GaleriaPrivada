package com.gallery.privatevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val primaryColorHex: String,
    val onPrimaryColorHex: String,
    val surfaceColorHex: String,
    val onSurfaceColorHex: String,
    val backgroundColorHex: String,
    val onBackgroundColorHex: String,
    val accentColorHex: String,
    val cardCornerRadiusDp: Int = 16,
    val gridDensityColumns: Int = 3,
    val barAlpha: Float = 0.92f,
    val barBlurEnabled: Boolean = true,
    val backgroundMediaUri: String? = null,
    val backgroundBlurRadius: Float = 15f,
    val backgroundAlpha: Float = 0.85f,
    val isDark: Boolean = true
)
