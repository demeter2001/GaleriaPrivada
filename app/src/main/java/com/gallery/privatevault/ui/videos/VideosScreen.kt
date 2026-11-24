package com.gallery.privatevault.ui.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.theme.GalleryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    viewModel: VideosViewModel,
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
                            text = "Videos",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GalleryTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${uiState.filteredVideos.size})",
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

                        // Cambiar Columnas (1, 2, 3)
                        IconButton(onClick = viewModel::onToggleColumnCount) {
                            Icon(
                                imageVector = Icons.Default.ViewAgenda,
                                contentDescription = "Cambiar vista (${uiState.columnCount})",
                                tint = GalleryTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Filtros de Resolución
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(VideoResolutionFilter.values()) { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onFilterSelected(filter) },
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

        // Cuadrícula de Videos
        if (uiState.filteredVideos.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = GalleryTheme.colors.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay videos que coincidan",
                        color = GalleryTheme.colors.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(uiState.columnCount),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredVideos, key = { it.id }) { item ->
                    VideoCardItem(
                        item = item,
                        onClick = { onNavigateToMediaViewer(item, uiState.filteredVideos) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoCardItem(
    item: MediaItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val cornerRadius = GalleryTheme.current.cardCornerRadius
    val context = LocalContext.current

    // Etiqueta de resolución inteligente
    val maxDimension = maxOf(item.width, item.height)
    val resolutionBadge = when {
        maxDimension >= 3800 -> "4K"
        maxDimension >= 1900 -> "1080p"
        maxDimension >= 1200 -> "720p"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(cornerRadius))
            .background(GalleryTheme.colors.surface)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Botón Central de Reproducción
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Reproducir",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Insignia de Resolución (Izquierda)
        if (resolutionBadge != null) {
            Text(
                text = resolutionBadge,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(GalleryTheme.colors.primary.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }

        // Insignia de Duración (Derecha)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.durationFormatted,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Botón de Favorito
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorito",
                tint = if (item.isFavorite) Color.Red else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
