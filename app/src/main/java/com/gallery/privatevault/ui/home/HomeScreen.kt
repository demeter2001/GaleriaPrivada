package com.gallery.privatevault.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.model.VirtualFolder
import com.gallery.privatevault.core.theme.GalleryTheme
import com.gallery.privatevault.ui.components.MediaThumbnailItem
import com.gallery.privatevault.ui.components.PinnedFoldersRow
import com.gallery.privatevault.ui.components.ProfileSwitcherSheet

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToMediaViewer: (item: MediaItem, contextList: List<MediaItem>) -> Unit,
    onNavigateToImages: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Mostrar avisos de seguridad / feedback
    LaunchedEffect(uiState.securityNoticeMessage) {
        uiState.securityNoticeMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearSecurityNotice()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GalleryTheme.colors.background)
    ) {
        // Fondo personalizado si el tema lo define (con desenfoque y opacidad)
        GalleryTheme.current.backgroundMediaUri?.let { bgUri ->
            AsyncImage(
                model = bgUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(GalleryTheme.current.backgroundBlurRadius.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GalleryTheme.colors.background.copy(alpha = GalleryTheme.current.backgroundAlpha))
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Barra Superior de Inicio (Selector de Perfil + Búsqueda)
            HomeHeaderBar(
                profileName = uiState.activeProfile?.name ?: "General",
                isProfileProtected = uiState.activeProfile?.isProtected ?: false,
                isReadOnly = uiState.activeProfile?.isReadOnly ?: false,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onProfileClick = { viewModel.setProfileSheetVisible(true) },
                onSettingsClick = onNavigateToSettings
            )

            // Contenido Principal
            if (uiState.isSearchActive) {
                SearchResultsView(
                    results = uiState.searchResults,
                    onMediaClick = { item -> onNavigateToMediaViewer(item, uiState.searchResults) },
                    onToggleFavorite = viewModel::toggleFavorite
                )
            } else {
                HomeDashboardContent(
                    uiState = uiState,
                    onFolderClick = { folder -> /* Navegación a carpeta virtual */ },
                    onMediaClick = { item, list -> onNavigateToMediaViewer(item, list) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onSeeAllImages = onNavigateToImages,
                    onSeeAllVideos = onNavigateToVideos
                )
            }
        }

        // Bottom Sheet de Selección de Perfil
        if (uiState.isProfileSheetVisible) {
            ProfileSwitcherSheet(
                activeProfileId = uiState.activeProfile?.id ?: 1L,
                profiles = uiState.availableProfiles,
                onSelectProfile = { profile ->
                    if (activity != null) {
                        viewModel.selectProfile(profile, activity)
                    }
                },
                onCreateProfileClick = onNavigateToSettings,
                onDismiss = { viewModel.setProfileSheetVisible(false) }
            )
        }
    }
}

@Composable
fun HomeHeaderBar(
    profileName: String,
    isProfileProtected: Boolean,
    isReadOnly: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val cornerRadius = GalleryTheme.current.cardCornerRadius / 1.5f

    Surface(
        color = GalleryTheme.colors.barBackground,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Selector Rápido de Perfil Activo
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(GalleryTheme.colors.primary.copy(alpha = 0.15f))
                        .clickable { onProfileClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isProfileProtected) Icons.Default.Lock else Icons.Default.AccountCircle,
                        contentDescription = "Perfil",
                        tint = GalleryTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = profileName,
                        color = GalleryTheme.colors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Desplegar perfiles",
                        tint = GalleryTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = GalleryTheme.colors.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de Búsqueda Rápida del Perfil
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Buscar en este perfil...",
                        color = GalleryTheme.colors.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Buscar",
                        tint = GalleryTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar",
                                tint = GalleryTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GalleryTheme.colors.surface,
                    unfocusedContainerColor = GalleryTheme.colors.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = GalleryTheme.colors.onSurface,
                    unfocusedTextColor = GalleryTheme.colors.onSurface
                ),
                shape = RoundedCornerShape(cornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Composable
fun HomeDashboardContent(
    uiState: HomeUiState,
    onFolderClick: (VirtualFolder) -> Unit,
    onMediaClick: (MediaItem, List<MediaItem>) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onSeeAllImages: () -> Unit,
    onSeeAllVideos: () -> Unit
) {
    if (uiState.recentAllMedia.isEmpty() && !uiState.isLoading) {
        EmptyProfileNotice()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Carpetas Fijadas
        if (uiState.pinnedFolders.isNotEmpty()) {
            item {
                PinnedFoldersRow(
                    folders = uiState.pinnedFolders,
                    onFolderClick = onFolderClick
                )
            }
        }

        // Bloque: Imágenes Recientes
        if (uiState.recentImages.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Imágenes Recientes",
                    count = uiState.recentImages.size,
                    onSeeAllClick = onSeeAllImages
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.recentImages, key = { it.id }) { item ->
                        MediaThumbnailItem(
                            item = item,
                            onClick = { onMediaClick(item, uiState.recentImages) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            modifier = Modifier
                                .size(width = 130.dp, height = 130.dp)
                        )
                    }
                }
            }
        }

        // Bloque: Videos Recientes
        if (uiState.recentVideos.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Videos Recientes",
                    count = uiState.recentVideos.size,
                    onSeeAllClick = onSeeAllVideos
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.recentVideos, key = { it.id }) { item ->
                        MediaThumbnailItem(
                            item = item,
                            onClick = { onMediaClick(item, uiState.recentVideos) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            modifier = Modifier
                                .size(width = 170.dp, height = 120.dp)
                        )
                    }
                }
            }
        }

        // Bloque: Todo lo Reciente (Cuadrícula del perfil)
        item {
            SectionHeader(
                title = "Todos los archivos recientes",
                count = uiState.recentAllMedia.size,
                onSeeAllClick = null
            )
        }

        val columns = GalleryTheme.current.gridDensityColumns
        val chunked = uiState.recentAllMedia.chunked(columns)

        items(chunked) { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (item in rowItems) {
                    MediaThumbnailItem(
                        item = item,
                        onClick = { onMediaClick(item, uiState.recentAllMedia) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
                // Relleno de espacio si la última fila está incompleta
                for (i in 0 until (columns - rowItems.size)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    onSeeAllClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GalleryTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "($count)",
                fontSize = 13.sp,
                color = GalleryTheme.colors.onBackground.copy(alpha = 0.5f)
            )
        }

        if (onSeeAllClick != null) {
            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Ver todas",
                    color = GalleryTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SearchResultsView(
    results: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit
) {
    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No se encontraron coincidencias en este perfil",
                color = GalleryTheme.colors.onBackground.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
        return
    }

    val columns = GalleryTheme.current.gridDensityColumns
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(results, key = { it.id }) { item ->
            MediaThumbnailItem(
                item = item,
                onClick = { onMediaClick(item) },
                onToggleFavorite = { onToggleFavorite(item) },
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}

@Composable
fun EmptyProfileNotice() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOff,
                contentDescription = null,
                tint = GalleryTheme.colors.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No hay archivos en este perfil",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GalleryTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Verifica las reglas de inclusión/exclusión del perfil o agrega carpetas en Configuración.",
                fontSize = 13.sp,
                color = GalleryTheme.colors.onBackground.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
