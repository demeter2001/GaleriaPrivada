package com.gallery.privatevault.core.model

import kotlinx.serialization.Serializable

/**
 * Configuración completa de Tema Visual portátil (exportable e importable en JSON).
 */
@Serializable
data class ThemeConfig(
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
