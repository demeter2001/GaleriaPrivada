package com.gallery.privatevault.ui.viewer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.privatevault.core.metadata.ExifMetadata
import com.gallery.privatevault.core.metadata.ExifPrivacyScrubber
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.theme.GalleryTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifDetailsSheet(
    item: MediaItem,
    metadata: ExifMetadata?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrubber = remember { ExifPrivacyScrubber(context) }
    var isScrubbing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GalleryTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GalleryTheme.colors.onSurface.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detalles y Metadatos EXIF",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GalleryTheme.colors.onSurface
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = GalleryTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (metadata == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GalleryTheme.colors.primary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    // Botón Destacado: Compartir sin Metadatos Sensibles (Privacidad)
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    isScrubbing = true
                                    scrubber.createSanitizedCopyForSharing(item)
                                        .onSuccess { cleanUri ->
                                            isScrubbing = false
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = item.mimeType
                                                putExtra(Intent.EXTRA_STREAM, cleanUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Compartir sin metadatos"))
                                        }
                                        .onFailure {
                                            isScrubbing = false
                                            Toast.makeText(context, "Error al limpiar metadatos", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            enabled = !isScrubbing,
                            colors = ButtonDefaults.buttonColors(containerColor = GalleryTheme.colors.primary),
                            shape = RoundedCornerShape(GalleryTheme.current.cardCornerRadius / 1.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = GalleryTheme.colors.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isScrubbing) "Limpiando datos sensibles..." else "Compartir seguro (Sin GPS/EXIF)",
                                color = GalleryTheme.colors.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Archivo y Almacenamiento
                    item {
                        ExifSection(title = "Archivo") {
                            ExifItemRow(icon = Icons.Default.Description, label = "Nombre", value = metadata.fileName)
                            ExifItemRow(icon = Icons.Default.Folder, label = "Ruta", value = metadata.filePath)
                            ExifItemRow(icon = Icons.Default.Storage, label = "Tamaño", value = metadata.fileSizeFormatted)
                            ExifItemRow(icon = Icons.Default.AspectRatio, label = "Resolución", value = metadata.resolutionFormatted)
                            ExifItemRow(icon = Icons.Default.Code, label = "Tipo MIME", value = metadata.mimeType)
                        }
                    }

                    // Cámara y Óptica
                    if (metadata.cameraSummary != null || metadata.exposureSummary != null) {
                        item {
                            ExifSection(title = "Cámara y Captura") {
                                metadata.cameraSummary?.let {
                                    ExifItemRow(icon = Icons.Default.CameraAlt, label = "Dispositivo", value = it)
                                }
                                metadata.lensModel?.let {
                                    ExifItemRow(icon = Icons.Default.FilterCenterFocus, label = "Lente", value = it)
                                }
                                metadata.exposureSummary?.let {
                                    ExifItemRow(icon = Icons.Default.Exposure, label = "Exposición", value = it)
                                }
                                metadata.flash?.let {
                                    ExifItemRow(icon = Icons.Default.FlashOn, label = "Flash", value = it)
                                }
                                metadata.whiteBalance?.let {
                                    ExifItemRow(icon = Icons.Default.WbSunny, label = "Balance de blancos", value = it)
                                }
                                metadata.dateTaken?.let {
                                    ExifItemRow(icon = Icons.Default.CalendarToday, label = "Fecha de captura", value = it)
                                }
                            }
                        }
                    }

                    // Ubicación GPS Local
                    if (metadata.hasLocation) {
                        item {
                            ExifSection(title = "Geolocalización Local (Sin conexión)") {
                                ExifItemRow(
                                    icon = Icons.Default.LocationOn,
                                    label = "Coordenadas GPS",
                                    value = metadata.locationCoordinatesFormatted ?: ""
                                )
                                metadata.altitudeMeters?.let {
                                    ExifItemRow(icon = Icons.Default.Terrain, label = "Altitud", value = "${String.format("%.1f", it)} m")
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        val geoUri = Uri.parse("geo:${metadata.latitude},${metadata.longitude}?q=${metadata.latitude},${metadata.longitude}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        context.startActivity(Intent.createChooser(mapIntent, "Ver ubicación"))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ver coordenadas en app de mapas")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExifSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GalleryTheme.colors.background.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = GalleryTheme.colors.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun ExifItemRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GalleryTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = GalleryTheme.colors.onSurface.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
            Text(
                text = value,
                color = GalleryTheme.colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
