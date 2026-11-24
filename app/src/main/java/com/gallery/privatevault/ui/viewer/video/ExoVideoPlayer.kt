package com.gallery.privatevault.ui.viewer.video

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.gallery.privatevault.core.model.MediaItem
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun ExoVideoPlayer(
    item: MediaItem,
    isVisible: Boolean,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(item.durationMs) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isTouchLocked by remember { mutableStateOf(false) }

    // Feedback visual de gestos
    var gestureStatusText by remember { mutableStateOf<String?>(null) }
    var gestureIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }

    val exoPlayer = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            prepare()
            playWhenReady = isVisible
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        totalDurationMs = duration.coerceAtLeast(0L)
                    }
                }
            })
        }
    }

    // Actualizador de tiempo de reproducción en segundo plano
    LaunchedEffect(isPlaying, isVisible) {
        while (isPlaying && isVisible) {
            currentPositionMs = exoPlayer.currentPosition
            delay(500)
        }
    }

    // Ocultamiento automático de controles tras inactividad
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying && !isTouchLocked) {
            delay(4000)
            areControlsVisible = false
        }
    }

    DisposableEffect(item.uri) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isTouchLocked) {
                detectTapGestures(
                    onTap = {
                        areControlsVisible = !areControlsVisible
                    }
                )
            }
            .pointerInput(isTouchLocked) {
                if (isTouchLocked) return@pointerInput

                var accumulatedDragX = 0f
                var accumulatedDragY = 0f
                var isHorizontalDrag = false
                var isLeftVertical = false

                detectDragGestures(
                    onDragStart = { offset ->
                        accumulatedDragX = 0f
                        accumulatedDragY = 0f
                        isHorizontalDrag = false
                        isLeftVertical = offset.x < (size.width / 2)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragX += dragAmount.x
                        accumulatedDragY += dragAmount.y

                        if (abs(accumulatedDragX) > abs(accumulatedDragY) && abs(accumulatedDragX) > 20f) {
                            isHorizontalDrag = true
                            val seekDeltaSeconds = (accumulatedDragX / 15).toInt()
                            val sign = if (seekDeltaSeconds >= 0) "+" else ""
                            gestureIcon = Icons.Default.FastForward
                            gestureStatusText = "$sign${seekDeltaSeconds}s"
                        } else if (!isHorizontalDrag && abs(accumulatedDragY) > 20f) {
                            if (isLeftVertical && activity != null) {
                                // Brillo en el lado izquierdo
                                val deltaBrightness = -accumulatedDragY / 500f
                                val lp = activity.window.attributes
                                val current = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                val newBrightness = (current + deltaBrightness).coerceIn(0.05f, 1.0f)
                                lp.screenBrightness = newBrightness
                                activity.window.attributes = lp
                                gestureIcon = Icons.Default.BrightnessMedium
                                gestureStatusText = "Brillo: ${(newBrightness * 100).toInt()}%"
                            } else {
                                // Volumen en el lado derecho
                                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val deltaVol = (-accumulatedDragY / 50).toInt()
                                val targetVol = (currentVolume + deltaVol).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                gestureIcon = if (targetVol > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff
                                gestureStatusText = "Volumen: ${(targetVol * 100 / maxVolume)}%"
                            }
                        }
                    },
                    onDragEnd = {
                        if (isHorizontalDrag) {
                            val seekDeltaMs = ((accumulatedDragX / 15).toInt()) * 1000L
                            val newPos = (exoPlayer.currentPosition + seekDeltaMs).coerceIn(0L, totalDurationMs)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                        }
                        gestureStatusText = null
                        gestureIcon = null
                    },
                    onDragCancel = {
                        gestureStatusText = null
                        gestureIcon = null
                    }
                )
            }
    ) {
        // Vista de Superficie de Video AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Usamos nuestros propios controles Compose de alto rendimiento
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay central de retroalimentación de gestos (Brillo, Volumen, Seek)
        if (gestureStatusText != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    gestureIcon?.let { icon ->
                        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = gestureStatusText ?: "",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Capa de Controles del Reproductor (Play, Seekbar, Bloqueo táctil, Reproductor externo)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
            ) {
                // Barra superior: Cerrar, Nombre, Botón de Bloqueo, Reproductor Externo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onCloseClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }

                    Text(
                        text = item.displayName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Bloqueo táctil
                    IconButton(
                        onClick = { isTouchLocked = !isTouchLocked }
                    ) {
                        Icon(
                            imageVector = if (isTouchLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Bloquear gestos",
                            tint = if (isTouchLocked) Color(0xFFFF5252) else Color.White
                        )
                    }

                    // Abrir en reproductor externo
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(item.uri, item.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Abrir con"))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Reproductor externo", tint = Color.White)
                    }
                }

                if (!isTouchLocked) {
                    // Botón Central Play / Pause
                    IconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Barra inferior: Slider de tiempo y duración
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatDuration(totalDurationMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        Slider(
                            value = currentPositionMs.toFloat(),
                            onValueChange = { newPos ->
                                currentPositionMs = newPos.toLong()
                                exoPlayer.seekTo(currentPositionMs)
                            },
                            valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
