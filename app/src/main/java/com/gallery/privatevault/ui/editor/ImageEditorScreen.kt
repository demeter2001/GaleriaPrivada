package com.gallery.privatevault.ui.editor

import android.graphics.Bitmap
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.privatevault.core.editor.CensorArea
import com.gallery.privatevault.core.editor.ImageEditorEngine
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.theme.GalleryTheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    item: MediaItem,
    onClose: () -> Unit,
    onSavedSuccessfully: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { ImageEditorEngine(context) }

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    val censorAreas = remember { mutableStateListOf<CensorArea>() }

    var isCensorModeActive by remember { mutableStateOf(false) }
    var isMosaicMode by remember { mutableStateOf(true) } // true: pixelado, false: barra negra
    var isSaving by remember { mutableStateOf(false) }

    // Coordenadas del arrastre activo de censura
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }

    // Cargar Bitmap
    LaunchedEffect(item.uri) {
        originalBitmap = engine.loadBitmap(item.uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor No Destructivo", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (originalBitmap != null) {
                                scope.launch {
                                    isSaving = true
                                    val processed = engine.processImage(
                                        originalBitmap = originalBitmap!!,
                                        rotationDegrees = rotationDegrees,
                                        isFlippedHorizontal = isFlippedHorizontal,
                                        censorAreas = censorAreas
                                    )
                                    engine.saveAsNewCopy(processed, item.displayName)
                                        .onSuccess {
                                            isSaving = false
                                            Toast.makeText(context, "Copia guardada en Galería", Toast.LENGTH_SHORT).show()
                                            onSavedSuccessfully()
                                        }
                                        .onFailure {
                                            isSaving = false
                                            Toast.makeText(context, "Error al guardar imagen", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        },
                        enabled = !isSaving && originalBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = GalleryTheme.colors.primary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = GalleryTheme.colors.onPrimary, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Guardar Copia")
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
        bottomBar = {
            Surface(
                color = GalleryTheme.colors.barBackground,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (isCensorModeActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Arrastra sobre la imagen para pixelar datos sensibles",
                                fontSize = 12.sp,
                                color = GalleryTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row {
                                IconButton(
                                    onClick = { if (censorAreas.isNotEmpty()) censorAreas.removeAt(censorAreas.lastIndex) },
                                    enabled = censorAreas.isNotEmpty()
                                ) {
                                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Deshacer")
                                }
                                IconButton(onClick = { isCensorModeActive = false }) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Listo")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotar 90
                        IconButton(onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }) {
                            Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Rotar 90°", tint = GalleryTheme.colors.onSurface)
                        }

                        // Voltear Horizontal
                        IconButton(onClick = { isFlippedHorizontal = !isFlippedHorizontal }) {
                            Icon(imageVector = Icons.Default.Flip, contentDescription = "Voltear horizontal", tint = if (isFlippedHorizontal) GalleryTheme.colors.primary else GalleryTheme.colors.onSurface)
                        }

                        // Modo Censura / Pixelado
                        IconButton(
                            onClick = { isCensorModeActive = !isCensorModeActive },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCensorModeActive) GalleryTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BlurOn,
                                contentDescription = "Censurar/Pixelar",
                                tint = if (isCensorModeActive) GalleryTheme.colors.primary else GalleryTheme.colors.onSurface
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Black,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (originalBitmap == null) {
                CircularProgressIndicator(color = GalleryTheme.colors.primary)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isCensorModeActive) {
                            if (!isCensorModeActive) return@pointerInput

                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStart = offset
                                    dragCurrent = offset
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    dragCurrent = change.position
                                },
                                onDragEnd = {
                                    val start = dragStart
                                    val current = dragCurrent
                                    if (start != null && current != null) {
                                        val left = min(start.x, current.x) / size.width
                                        val top = min(start.y, current.y) / size.height
                                        val right = max(start.x, current.x) / size.width
                                        val bottom = max(start.y, current.y) / size.height

                                        censorAreas.add(
                                            CensorArea(
                                                rect = RectF(left, top, right, bottom),
                                                isMosaic = isMosaicMode
                                            )
                                        )
                                    }
                                    dragStart = null
                                    dragCurrent = null
                                },
                                onDragCancel = {
                                    dragStart = null
                                    dragCurrent = null
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = originalBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Capa Canvas para dibujar rectángulos de censura y vista previa en vivo
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        // Dibujar áreas de censura ya registradas
                        for (area in censorAreas) {
                            drawRect(
                                color = Color.Black.copy(alpha = 0.85f),
                                topLeft = Offset(area.rect.left * canvasW, area.rect.top * canvasH),
                                size = Size(
                                    (area.rect.right - area.rect.left) * canvasW,
                                    (area.rect.bottom - area.rect.top) * canvasH
                                )
                            )
                        }

                        // Dibujar rectángulo activo de arrastre
                        val start = dragStart
                        val curr = dragCurrent
                        if (start != null && curr != null) {
                            val x = min(start.x, curr.x)
                            val y = min(start.y, curr.y)
                            val w = max(start.x, curr.x) - x
                            val h = max(start.y, curr.y) - y

                            drawRect(
                                color = Color.Red.copy(alpha = 0.4f),
                                topLeft = Offset(x, y),
                                size = Size(w, h)
                            )
                            drawRect(
                                color = Color.Red,
                                topLeft = Offset(x, y),
                                size = Size(w, h),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}
