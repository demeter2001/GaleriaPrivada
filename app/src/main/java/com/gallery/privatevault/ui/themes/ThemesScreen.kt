package com.gallery.privatevault.ui.themes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.privatevault.catalog.WallpaperActionManager
import com.gallery.privatevault.catalog.api.WallpaperCatalogService
import com.gallery.privatevault.catalog.model.OnlineWallpaper
import com.gallery.privatevault.core.model.ThemeConfig
import com.gallery.privatevault.core.theme.ColorUtils
import com.gallery.privatevault.core.theme.GalleryTheme
import kotlinx.coroutines.launch

enum class ThemeTab {
    LOCAL_THEMES,
    ONLINE_WALLPAPERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    viewModel: ThemesViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()

    var activeTab by remember { mutableStateOf(ThemeTab.LOCAL_THEMES) }
    val catalogService = remember { WallpaperCatalogService() }
    val actionManager = remember { WallpaperActionManager(context, com.gallery.privatevault.GalleryApplication().themeRepository) }

    // Estado del buscador online de fondos
    var searchQuery by remember { mutableStateOf("nature") }
    var onlineWallpapers by remember { mutableStateOf<List<OnlineWallpaper>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var selectedWallpaperForAction by remember { mutableStateOf<OnlineWallpaper?>(null) }
    var isImportDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Temas y Fondos", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú")
                        }
                    }
                },
                actions = {
                    if (activeTab == ThemeTab.LOCAL_THEMES) {
                        IconButton(onClick = { isImportDialogOpen = true }) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Importar JSON")
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
            // Selector de Pestañas: Temas Locales vs Catálogo Online
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = GalleryTheme.colors.surface,
                contentColor = GalleryTheme.colors.primary
            ) {
                Tab(
                    selected = activeTab == ThemeTab.LOCAL_THEMES,
                    onClick = { activeTab = ThemeTab.LOCAL_THEMES },
                    text = { Text("Temas de Interfaz", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == ThemeTab.ONLINE_WALLPAPERS,
                    onClick = {
                        activeTab = ThemeTab.ONLINE_WALLPAPERS
                        if (onlineWallpapers.isEmpty() && !isSearchingOnline) {
                            scope.launch {
                                isSearchingOnline = true
                                catalogService.searchWallpapers(searchQuery)
                                    .onSuccess { onlineWallpapers = it }
                                isSearchingOnline = false
                            }
                        }
                    },
                    text = { Text("Fondos Gratuitos (Online)", fontWeight = FontWeight.Bold) }
                )
            }

            when (activeTab) {
                ThemeTab.LOCAL_THEMES -> {
                    LocalThemesTabContent(
                        uiState = uiState,
                        onSelectTheme = viewModel::onSelectTheme,
                        onCustomizeTheme = viewModel::startCustomizingTheme,
                        onExportJson = { theme ->
                            viewModel.exportThemeToJson(theme)
                            clipboardManager.setText(AnnotatedString(com.gallery.privatevault.GalleryApplication().themeRepository.exportThemeToJson(theme)))
                            Toast.makeText(context, "JSON copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                ThemeTab.ONLINE_WALLPAPERS -> {
                    OnlineWallpapersTabContent(
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {
                            scope.launch {
                                isSearchingOnline = true
                                catalogService.searchWallpapers(searchQuery)
                                    .onSuccess { onlineWallpapers = it }
                                    .onFailure { Toast.makeText(context, "Error al conectar con el catálogo", Toast.LENGTH_SHORT).show() }
                                isSearchingOnline = false
                            }
                        },
                        wallpapers = onlineWallpapers,
                        isSearching = isSearchingOnline,
                        onSelectWallpaper = { selectedWallpaperForAction = it }
                    )
                }
            }
        }

        // Diálogo de Acciones de Fondo Online Seleccionado
        if (selectedWallpaperForAction != null) {
            val wp = selectedWallpaperForAction!!
            AlertDialog(
                onDismissRequest = { selectedWallpaperForAction = null },
                title = { Text("Aplicar Fondo Gratuito (${wp.resolution})") },
                text = { Text("¿Cómo deseas utilizar esta imagen?") },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            actionManager.applyAsGalleryBackground(wp, uiState.currentTheme)
                            Toast.makeText(context, "Aplicado como fondo interno", Toast.LENGTH_SHORT).show()
                            selectedWallpaperForAction = null
                        }
                    }) {
                        Text("Fondo Interno Galería")
                    }
                },
                dismissButton = {
                    Column {
                        TextButton(onClick = {
                            scope.launch {
                                actionManager.setAsSystemWallpaper(wp)
                                    .onSuccess { Toast.makeText(context, "Fondo de Android actualizado", Toast.LENGTH_SHORT).show() }
                                selectedWallpaperForAction = null
                            }
                        }) {
                            Text("Fondo de Pantalla Android")
                        }
                        TextButton(onClick = {
                            scope.launch {
                                actionManager.downloadWallpaperLocally(wp)
                                    .onSuccess { Toast.makeText(context, "Descargado en Galería", Toast.LENGTH_SHORT).show() }
                                selectedWallpaperForAction = null
                            }
                        }) {
                            Text("Descargar en Almacenamiento")
                        }
                    }
                }
            )
        }

        // Diálogo para Importar JSON
        if (isImportDialogOpen) {
            var jsonInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { isImportDialogOpen = false },
                title = { Text("Importar Tema desde JSON") },
                text = {
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        placeholder = { Text("Pega la configuración JSON aquí...") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.importThemeFromJson(jsonInput)
                        isImportDialogOpen = false
                    }) {
                        Text("Importar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isImportDialogOpen = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun LocalThemesTabContent(
    uiState: ThemesUiState,
    onSelectTheme: (ThemeConfig) -> Unit,
    onCustomizeTheme: (ThemeConfig) -> Unit,
    onExportJson: (ThemeConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Temas Preinstalados y Personalizados",
                fontWeight = FontWeight.Bold,
                color = GalleryTheme.colors.primary,
                fontSize = 15.sp
            )
        }

        items(uiState.availableThemes) { theme ->
            val isCurrent = theme.id == uiState.currentTheme.id
            ThemePresetCard(
                theme = theme,
                isCurrent = isCurrent,
                onSelect = { onSelectTheme(theme) },
                onCustomize = { onCustomizeTheme(theme) },
                onExport = { onExportJson(theme) }
            )
        }
    }
}

@Composable
fun ThemePresetCard(
    theme: ThemeConfig,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onCustomize: () -> Unit,
    onExport: () -> Unit
) {
    val primary = ColorUtils.parseHexColor(theme.primaryColorHex)
    val surface = ColorUtils.parseHexColor(theme.surfaceColorHex)
    val bg = ColorUtils.parseHexColor(theme.backgroundColorHex)

    Surface(
        color = GalleryTheme.colors.surface,
        shape = RoundedCornerShape(GalleryTheme.current.cardCornerRadius / 1.5f),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, GalleryTheme.colors.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Muestra de color
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .border(2.dp, primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(surface)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GalleryTheme.colors.onSurface
                )
                Text(
                    text = if (theme.isDark) "Modo Oscuro · Esquinas: ${theme.cardCornerRadiusDp}dp" else "Modo Claro · Esquinas: ${theme.cardCornerRadiusDp}dp",
                    fontSize = 12.sp,
                    color = GalleryTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onExport) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Exportar JSON", tint = GalleryTheme.colors.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun OnlineWallpapersTabContent(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    wallpapers: List<OnlineWallpaper>,
    isSearching: Boolean,
    onSelectWallpaper: (OnlineWallpaper) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de Búsqueda Online
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar fondos (ej. nature, anime, space)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSearch,
                colors = ButtonDefaults.buttonColors(containerColor = GalleryTheme.colors.primary)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
            }
        }

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GalleryTheme.colors.primary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(wallpapers, key = { it.id }) { wp ->
                    OnlineWallpaperCard(
                        wallpaper = wp,
                        onClick = { onSelectWallpaper(wp) }
                    )
                }
            }
        }
    }
}

@Composable
fun OnlineWallpaperCard(
    wallpaper: OnlineWallpaper,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .background(GalleryTheme.colors.surface)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(wallpaper.previewUrl).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = wallpaper.resolution,
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(vertical = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
