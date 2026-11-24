package com.gallery.privatevault.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class GalleryDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Inicio", Icons.Filled.Home, Icons.Outlined.Home),
    IMAGES("Imágenes", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    VIDEOS("Videos", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    FOLDERS("Carpetas", Icons.Filled.Folder, Icons.Outlined.Folder),
    THEMES("Temas y Fondos", Icons.Filled.Palette, Icons.Outlined.Palette),
    SETTINGS("Configuración", Icons.Filled.Settings, Icons.Outlined.Settings)
}
