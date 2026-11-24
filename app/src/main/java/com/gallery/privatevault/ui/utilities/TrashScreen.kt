package com.gallery.privatevault.ui.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.gallery.privatevault.core.theme.GalleryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Papelera de Reciclaje", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        IconButton(onClick = { showPurgeConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Vaciar",
                                tint = Color(0xFFFF5252)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GalleryTheme.colors.barBackground,
                    titleContentColor = GalleryTheme.colors.onSurface,
                    navigationIconContentColor = GalleryTheme.colors.onSurface
                )
            )
        },
        containerColor = GalleryTheme.colors.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Aviso de Retención
            Surface(
                color = GalleryTheme.colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = GalleryTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Los elementos se retienen durante ${uiState.retentionDays} días antes de la purga automática del sistema.",
                        fontSize = 12.sp,
                        color = GalleryTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = GalleryTheme.colors.onBackground.copy(alpha = 0.35f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "La papelera está vacía",
                            fontWeight = FontWeight.Bold,
                            color = GalleryTheme.colors.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.items, key = { it.mediaItem.id }) { trashItem ->
                        TrashCard(
                            item = trashItem,
                            onRestore = { viewModel.restoreItem(trashItem.mediaItem.id) }
                        )
                    }
                }
            }
        }

        if (showPurgeConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showPurgeConfirmDialog = false },
                title = { Text("¿Vaciar papelera?") },
                text = { Text("Esta acción eliminará todos los registros de la papelera permanentemente.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.purgeAll()
                            showPurgeConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Vaciar Definitivamente")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPurgeConfirmDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun TrashCard(
    item: TrashItem,
    onRestore: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(GalleryTheme.colors.surface)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(item.mediaItem.uri).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Días restantes badge
        Text(
            text = "${item.daysRemaining}d",
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )

        // Botón Restaurar
        IconButton(
            onClick = onRestore,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(32.dp)
                .background(GalleryTheme.colors.primary, RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = "Restaurar",
                tint = GalleryTheme.colors.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
