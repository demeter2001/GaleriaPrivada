package com.gallery.privatevault.ui.viewer

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.privatevault.core.metadata.ExifMetadata
import com.gallery.privatevault.core.metadata.ExifMetadataExtractor
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.theme.GalleryTheme
import com.gallery.privatevault.ui.viewer.image.ZoomableImageView
import com.gallery.privatevault.ui.viewer.video.ExoVideoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediaViewerScreen(
    initialItem: MediaItem,
    contextList: List<MediaItem>,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onDeleteMedia: (MediaItem) -> Unit,
    onEditImage: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (contextList.isEmpty()) {
        onClose()
        return
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val exifExtractor = remember { ExifMetadataExtractor(context) }

    // Encontrar índice inicial respetando el contexto de origen
    val initialIndex = remember {
        val found = contextList.indexOfFirst { it.id == initialItem.id }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { contextList.size }
    )

    val currentItem = contextList.getOrNull(pagerState.currentPage) ?: initialItem

    var areBarsVisible by remember { mutableStateOf(true) }
    var isExifSheetVisible by remember { mutableStateOf(false) }
    var exifMetadata by remember { mutableStateOf<ExifMetadata?>(null) }
    var isSlideshowActive by remember { mutableStateOf(false) }

    // Manejo de Brillo Máximo Automático en Visor
    DisposableEffect(Unit) {
        val originalBrightness = activity?.window?.attributes?.screenBrightness
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (activity != null && originalBrightness != null) {
                val lp = activity.window.attributes
                lp.screenBrightness = originalBrightness
                activity.window.attributes = lp
            }
        }
    }

    // Modo Presentación de Diapositivas (Slideshow)
    LaunchedEffect(isSlideshowActive, pagerState.currentPage) {
        if (isSlideshowActive) {
            delay(3500)
            if (pagerState.currentPage < contextList.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            } else {
                pagerState.animateScrollToPage(0) // Reiniciar en bucle
            }
        }
    }

    // Cargar metadatos EXIF cuando cambia de página o se abre el panel
    LaunchedEffect(currentItem.id, isExifSheetVisible) {
        if (isExifSheetVisible) {
            exifMetadata = null
            exifMetadata = exifExtractor.extractMetadata(currentItem)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Deslizador continuo HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = true
        ) { page ->
            val pageItem = contextList[page]

            if (pageItem.isVideo) {
                ExoVideoPlayer(
                    item = pageItem,
                    isVisible = page == pagerState.currentPage,
                    onCloseClick = onClose
                )
            } else {
                ZoomableImageView(
                    item = pageItem,
                    onDismiss = onClose,
                    onOpenExif = { isExifSheetVisible = true },
                    onTap = { areBarsVisible = !areBarsVisible }
                )
            }
        }

        // Barra Superior Flotante
        AnimatedVisibility(
            visible = areBarsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentItem.displayName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} de ${contextList.size}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    // Botón Slideshow
                    IconButton(onClick = { isSlideshowActive = !isSlideshowActive }) {
                        Icon(
                            imageVector = if (isSlideshowActive) Icons.Default.PauseCircleFilled else Icons.Default.Slideshow,
                            contentDescription = "Diapositivas",
                            tint = if (isSlideshowActive) GalleryTheme.colors.primary else Color.White
                        )
                    }

                    // Botón Detalles EXIF
                    IconButton(onClick = { isExifSheetVisible = true }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Información", tint = Color.White)
                    }
                }
            }
        }

        // Barra Inferior Flotante (Favoritos, Compartir, Borrar)
        AnimatedVisibility(
            visible = areBarsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorito
                    IconButton(onClick = { onToggleFavorite(currentItem) }) {
                        Icon(
                            imageVector = if (currentItem.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (currentItem.isFavorite) Color.Red else Color.White
                        )
                    }

                    // Editor Rápido No Destructivo (o Extraer Fotograma de Video)
                    IconButton(onClick = {
                        if (!currentItem.isVideo) {
                            onEditImage(currentItem)
                        } else {
                            scope.launch {
                                val videoEngine = com.gallery.privatevault.core.editor.VideoToolsEngine(context)
                                val frame = videoEngine.extractFrame(currentItem.uri, 1000L)
                                if (frame != null) {
                                    videoEngine.saveExtractedFrame(frame, currentItem.displayName)
                                        .onSuccess { android.widget.Toast.makeText(context, "Fotograma guardado en Galería", android.widget.Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (currentItem.isVideo) Icons.Default.Camera else Icons.Default.Edit,
                            contentDescription = if (currentItem.isVideo) "Extraer Fotograma" else "Editar Imagen",
                            tint = Color.White
                        )
                    }

                    // Compartir estándar
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = currentItem.mimeType
                            putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir multimedia"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Compartir", tint = Color.White)
                    }

                    // Papelera Interna
                    IconButton(onClick = {
                        onDeleteMedia(currentItem)
                        if (contextList.size <= 1) {
                            onClose()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Mover a papelera", tint = Color(0xFFFF5252))
                    }
                }
            }
        }

        // BottomSheet de Metadatos EXIF
        if (isExifSheetVisible) {
            ExifDetailsSheet(
                item = currentItem,
                metadata = exifMetadata,
                onDismiss = { isExifSheetVisible = false }
            )
        }
    }
}
