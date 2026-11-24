package com.gallery.privatevault.ui.images

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.theme.GalleryTheme
import com.gallery.privatevault.ui.components.MediaThumbnailItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesScreen(
    viewModel: ImagesViewModel,
    onNavigateToMediaViewer: (item: MediaItem, contextList: List<MediaItem>) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GalleryTheme.colors.background)
    ) {
        // Barra Superior
        Surface(
            color = GalleryTheme.colors.barBackground,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onOpenDrawer != null) {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menú",
                                    tint = GalleryTheme.colors.onSurface
                                )
                            }
                        }
                        Text(
                            text = "Imágenes",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GalleryTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${uiState.filteredImages.size})",
                            fontSize = 14.sp,
                            color = GalleryTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Filtro de Solo Favoritos
                        IconButton(onClick = viewModel::onToggleFavoritesOnly) {
                            Icon(
                                imageVector = if (uiState.onlyFavorites) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Solo favoritos",
                                tint = if (uiState.onlyFavorites) Color.Red else GalleryTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        // Cambiar Densidad de Cuadrícula (Columnas)
                        IconButton(onClick = viewModel::onToggleColumnCount) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Cambiar columnas (${uiState.columnCount})",
                                tint = GalleryTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Carrusel de Filtros Rápidos por Formato
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(ImageFormatFilter.values()) { filter ->
                        val isSelected = uiState.selectedFormat == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onFormatFilterSelected(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GalleryTheme.colors.primary,
                                selectedLabelColor = GalleryTheme.colors.onPrimary,
                                containerColor = GalleryTheme.colors.surface,
                                labelColor = GalleryTheme.colors.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.Transparent,
                                selectedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        // Cuadrícula de Imágenes
        if (uiState.filteredImages.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ImageNotSupported,
                        contentDescription = null,
                        tint = GalleryTheme.colors.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay imágenes para este filtro",
                        color = GalleryTheme.colors.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(uiState.columnCount),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredImages, key = { it.id }) { item ->
                    MediaThumbnailItem(
                        item = item,
                        onClick = { onNavigateToMediaViewer(item, uiState.filteredImages) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        modifier = Modifier.aspectRatio(1f)
                    )
                }
            }
        }
    }
}
