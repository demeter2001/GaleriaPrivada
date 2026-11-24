package com.gallery.privatevault.ui.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.gallery.privatevault.core.duplicates.DuplicateGroup
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.theme.GalleryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscador de Duplicados", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GalleryTheme.colors.barBackground,
                    titleContentColor = GalleryTheme.colors.onSurface,
                    navigationIconContentColor = GalleryTheme.colors.onSurface
                )
            )
        },
        bottomBar = {
            if (uiState.selectedItemIds.isNotEmpty()) {
                Surface(
                    color = GalleryTheme.colors.barBackground,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${uiState.selectedItemIds.size} seleccionados",
                                color = GalleryTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Mover a la papelera",
                                color = GalleryTheme.colors.onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = viewModel::moveSelectedToTrash,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Limpiar Seleccionados")
                        }
                    }
                }
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
            // Panel de Control del Escaneo
            Surface(
                color = GalleryTheme.colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(GalleryTheme.current.cardCornerRadius)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Escaneo 100% Offline por Hash SHA-256",
                        fontWeight = FontWeight.Bold,
                        color = GalleryTheme.colors.primary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Compara el tamaño exacto y la firma criptográfica para encontrar fotos y videos idénticos sin conexión.",
                        color = GalleryTheme.colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (uiState.isScanning) {
                        val progress = if (uiState.progressTotal > 0) {
                            uiState.progressCurrent.toFloat() / uiState.progressTotal.toFloat()
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = GalleryTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Analizando ${uiState.progressCurrent} de ${uiState.progressTotal} archivos...",
                            color = GalleryTheme.colors.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    } else {
                        Button(
                            onClick = viewModel::startScan,
                            colors = ButtonDefaults.buttonColors(containerColor = GalleryTheme.colors.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isCompleted) "Escanear de Nuevo" else "Iniciar Búsqueda de Duplicados")
                        }
                    }
                }
            }

            // Resultados de Duplicados
            if (uiState.isCompleted && uiState.duplicateGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "¡Excelente! No se encontraron duplicados",
                            fontWeight = FontWeight.Bold,
                            color = GalleryTheme.colors.onBackground
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.duplicateGroups) { group ->
                        DuplicateGroupCard(
                            group = group,
                            selectedItemIds = uiState.selectedItemIds,
                            onToggleSelection = viewModel::toggleItemSelection
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedItemIds: Set<Long>,
    onToggleSelection: (Long) -> Unit
) {
    val cornerRadius = GalleryTheme.current.cardCornerRadius / 1.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(GalleryTheme.colors.surface)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${group.items.size} archivos idénticos",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = GalleryTheme.colors.onSurface
            )
            Text(
                text = "Desperdicio: ${group.formattedWastedSize}",
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            group.items.forEachIndexed { index, item ->
                val isSelected = selectedItemIds.contains(item.id)
                val isOriginal = index == 0

                DuplicateItemPreview(
                    item = item,
                    isOriginal = isOriginal,
                    isSelected = isSelected,
                    onToggle = { onToggleSelection(item.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DuplicateItemPreview(
    item: MediaItem,
    isOriginal: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFFF5252).copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onToggle() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.align(Alignment.TopEnd)
            )

            if (isOriginal) {
                Text(
                    text = "Conservar",
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp,
            color = GalleryTheme.colors.onSurface
        )
        Text(
            text = item.sizeFormatted,
            fontSize = 10.sp,
            color = GalleryTheme.colors.onSurface.copy(alpha = 0.5f)
        )
    }
}
