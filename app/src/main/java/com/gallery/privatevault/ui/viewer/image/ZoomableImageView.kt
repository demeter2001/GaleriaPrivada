package com.gallery.privatevault.ui.viewer.image

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.privatevault.core.model.MediaItem
import kotlin.math.roundToInt

@Composable
fun ZoomableImageView(
    item: MediaItem,
    onDismiss: () -> Unit,
    onOpenExif: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Detección de arrastre vertical para gestos de Cerrar (abajo) o EXIF (arriba)
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            // Centrar hacia el punto pulsado
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            offset = Offset(
                                (centerX - tapOffset.x) * 1.5f,
                                (centerY - tapOffset.y) * 1.5f
                            )
                        }
                    },
                    onTap = {
                        onTap()
                    }
                )
            }
            .pointerInput(scale) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (scale > 1f) {
                        // Modo Zoom: Traslación y escalado de imagen
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f

                        val newOffsetX = (offset.x + pan.x * newScale).coerceIn(-maxOffsetX, maxOffsetX)
                        val newOffsetY = (offset.y + pan.y * newScale).coerceIn(-maxOffsetY, maxOffsetY)

                        scale = newScale
                        offset = Offset(newOffsetX, newOffsetY)
                    } else {
                        // Escala 1.0f: Evaluar gestos verticales (Cerrar o EXIF)
                        dragOffsetY += pan.y
                        if (dragOffsetY > 160f) {
                            onDismiss()
                        } else if (dragOffsetY < -160f) {
                            dragOffsetY = 0f
                            onOpenExif()
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
