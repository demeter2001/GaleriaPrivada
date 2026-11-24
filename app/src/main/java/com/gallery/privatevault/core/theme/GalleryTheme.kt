package com.gallery.privatevault.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gallery.privatevault.core.model.ThemeConfig

@Immutable
data class GalleryColors(
    val primary: Color,
    val onPrimary: Color,
    val surface: Color,
    val onSurface: Color,
    val background: Color,
    val onBackground: Color,
    val accent: Color,
    val barBackground: Color,
    val isDark: Boolean
)

@Immutable
data class GalleryExtendedTheme(
    val colors: GalleryColors,
    val cardCornerRadius: Dp,
    val gridDensityColumns: Int,
    val barAlpha: Float,
    val barBlurEnabled: Boolean,
    val backgroundMediaUri: String?,
    val backgroundBlurRadius: Float,
    val backgroundAlpha: Float
)

val LocalGalleryTheme = staticCompositionLocalOf {
    GalleryExtendedTheme(
        colors = GalleryColors(
            primary = Color(0xFF64B5F6),
            onPrimary = Color.Black,
            surface = Color(0xFF121212),
            onSurface = Color.White,
            background = Color.Black,
            onBackground = Color(0xFFEEEEEE),
            accent = Color(0xFF82B1FF),
            barBackground = Color(0xFF121212),
            isDark = true
        ),
        cardCornerRadius = 16.dp,
        gridDensityColumns = 3,
        barAlpha = 0.94f,
        barBlurEnabled = true,
        backgroundMediaUri = null,
        backgroundBlurRadius = 15f,
        backgroundAlpha = 0.85f
    )
}

object GalleryTheme {
    val current: GalleryExtendedTheme
        @Composable
        get() = LocalGalleryTheme.current

    val colors: GalleryColors
        @Composable
        get() = LocalGalleryTheme.current.colors
}

@Composable
fun PrivateGalleryTheme(
    themeConfig: ThemeConfig,
    content: @Composable () -> Unit
) {
    val primary = ColorUtils.parseHexColor(themeConfig.primaryColorHex, Color(0xFF64B5F6))
    val onPrimary = ColorUtils.parseHexColor(themeConfig.onPrimaryColorHex, Color.Black)
    val surface = ColorUtils.parseHexColor(themeConfig.surfaceColorHex, Color(0xFF121212))
    val onSurface = ColorUtils.parseHexColor(themeConfig.onSurfaceColorHex, Color.White)
    val background = ColorUtils.parseHexColor(themeConfig.backgroundColorHex, Color.Black)
    val onBackground = ColorUtils.parseHexColor(themeConfig.onBackgroundColorHex, Color(0xFFEEEEEE))
    val accent = ColorUtils.parseHexColor(themeConfig.accentColorHex, Color(0xFF82B1FF))

    val galleryColors = remember(themeConfig) {
        GalleryColors(
            primary = primary,
            onPrimary = onPrimary,
            surface = surface,
            onSurface = onSurface,
            background = background,
            onBackground = onBackground,
            accent = accent,
            barBackground = surface.copy(alpha = themeConfig.barAlpha),
            isDark = themeConfig.isDark
        )
    }

    val extendedTheme = remember(themeConfig, galleryColors) {
        GalleryExtendedTheme(
            colors = galleryColors,
            cardCornerRadius = themeConfig.cardCornerRadiusDp.dp,
            gridDensityColumns = themeConfig.gridDensityColumns,
            barAlpha = themeConfig.barAlpha,
            barBlurEnabled = themeConfig.barBlurEnabled,
            backgroundMediaUri = themeConfig.backgroundMediaUri,
            backgroundBlurRadius = themeConfig.backgroundBlurRadius,
            backgroundAlpha = themeConfig.backgroundAlpha
        )
    }

    val m3ColorScheme = if (themeConfig.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            surface = surface,
            onSurface = onSurface,
            background = background,
            onBackground = onBackground,
            secondary = accent
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            surface = surface,
            onSurface = onSurface,
            background = background,
            onBackground = onBackground,
            secondary = accent
        )
    }

    val m3Shapes = Shapes(
        small = RoundedCornerShape(themeConfig.cardCornerRadiusDp.dp / 2),
        medium = RoundedCornerShape(themeConfig.cardCornerRadiusDp.dp),
        large = RoundedCornerShape(themeConfig.cardCornerRadiusDp.dp * 1.5f)
    )

    CompositionLocalProvider(LocalGalleryTheme provides extendedTheme) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            shapes = m3Shapes,
            content = content
        )
    }
}
