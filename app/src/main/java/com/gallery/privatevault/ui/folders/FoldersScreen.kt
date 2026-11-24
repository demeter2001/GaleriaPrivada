package com.gallery.privatevault.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.model.VirtualFolder
import com.gallery.privatevault.core.theme.GalleryTheme
import com.gallery.privatevault.ui.components.MediaThumbnailItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    viewModel: FoldersViewModel,
    onNavigateToMediaViewer: (item: MediaItem, contextList: List<MediaItem>) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Si hay una carpeta abierta, mostrar vista de detalle de esa carpeta
    if (uiState.activeOpenedFolder != null) {
        val folder = uiState.activeOpenedFolder!!
        FolderDetailView(
            title = folder.name,
            items = folder.items,
            onBack = viewModel::closeActiveFolder,
            onMediaClick = { item -> onNavigateToMediaViewer(item, folder.items) }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateVirtualFolderDialog(true) },
                containerColor = GalleryTheme.colors.primary,
                contentColor = GalleryTheme.colors.onPrimary
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Nueva Carpeta Virtual")
            }
        },
        containerColor = GalleryTheme.colors.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            text = "Explorador de Carpetas",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GalleryTheme.colors.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pestañas: Todas, Reales, Virtuales
                    TabRow(
                        selectedTabIndex = uiState.selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = GalleryTheme.colors.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = uiState.selectedTab == FolderTabFilter.ALL,
                            onClick = { viewModel.onSelectTab(FolderTabFilter.ALL) },
                            text = { Text("Todas", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = uiState.selectedTab == FolderTabFilter.REAL,
                            onClick = { viewModel.onSelectTab(FolderTabFilter.REAL) },
                            text = { Text("Reales (${uiState.realFolders.size})", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = uiState.selectedTab == FolderTabFilter.VIRTUAL,
                            onClick = { viewModel.onSelectTab(FolderTabFilter.VIRTUAL) },
                            text = { Text("Virtuales (${uiState.virtualFolders.size})", fontSize = 13.sp) }
                        )
                    }
                }
            }

            // Lista de Carpetas
            val shouldShowReal = uiState.selectedTab == FolderTabFilter.ALL || uiState.selectedTab == FolderTabFilter.REAL
            val shouldShowVirtual = uiState.selectedTab == FolderTabFilter.ALL || uiState.selectedTab == FolderTabFilter.VIRTUAL

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Carpetas Virtuales No Destructivas
                if (shouldShowVirtual) {
                    items(uiState.virtualFolders, key = { "virtual_${it.id}" }) { folder ->
                        VirtualFolderCard(
                            folder = folder,
                            onClick = { viewModel.openVirtualFolder(folder) }
                        )
                    }
                }

                // Carpetas Reales del Almacenamiento
                if (shouldShowReal) {
                    items(uiState.realFolders, key = { "real_${it.bucketId}" }) { folder ->
                        RealFolderCard(
                            folder = folder,
                            onClick = { viewModel.openRealFolder(folder) }
                        )
                    }
                }
            }
        }

        // Diálogo para crear Carpeta Virtual
        if (uiState.isCreateVirtualFolderDialogVisible) {
            CreateVirtualFolderDialog(
                onConfirm = viewModel::createVirtualFolder,
                onDismiss = { viewModel.showCreateVirtualFolderDialog(false) }
            )
        }
    }
}

@Composable
fun RealFolderCard(
    folder: RealFolder,
    onClick: () -> Unit
) {
    val cornerRadius = GalleryTheme.current.cardCornerRadius
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(GalleryTheme.colors.surface)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .background(GalleryTheme.colors.surface.copy(alpha = 0.5f))
        ) {
            if (folder.coverItem != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(folder.coverItem.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = folder.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = GalleryTheme.colors.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = folder.name,
                color = GalleryTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${folder.itemCount} archivos",
                color = GalleryTheme.colors.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun VirtualFolderCard(
    folder: VirtualFolder,
    onClick: () -> Unit
) {
    val cornerRadius = GalleryTheme.current.cardCornerRadius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(GalleryTheme.colors.primary.copy(alpha = 0.12f))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderSpecial,
                contentDescription = folder.name,
                tint = GalleryTheme.colors.primary,
                modifier = Modifier.size(52.dp)
            )
        }

        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = folder.name,
                    color = GalleryTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Virtual",
                    fontSize = 9.sp,
                    color = GalleryTheme.colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(GalleryTheme.colors.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Carpeta No Destructiva",
                color = GalleryTheme.colors.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun FolderDetailView(
    title: String,
    items: List<MediaItem>,
    onBack: () -> Unit,
    onMediaClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryTheme.colors.background)
    ) {
        Surface(
            color = GalleryTheme.colors.barBackground,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = GalleryTheme.colors.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GalleryTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${items.size} elementos",
                        fontSize = 12.sp,
                        color = GalleryTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Esta carpeta está vacía",
                    color = GalleryTheme.colors.onBackground.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    MediaThumbnailItem(
                        item = item,
                        onClick = { onMediaClick(item) },
                        modifier = Modifier.aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateVirtualFolderDialog(
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva Carpeta Virtual",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Las carpetas virtuales no mueven ni duplican archivos en el almacenamiento físico; solo crean enlaces no destructivos.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Nombre de la carpeta (ej. Favoritas)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
